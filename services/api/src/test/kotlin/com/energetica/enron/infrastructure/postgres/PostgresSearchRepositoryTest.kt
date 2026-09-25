package com.energetica.enron.infrastructure.postgres

import com.energetica.enron.application.SearchQueryParser
import com.energetica.enron.application.SearchRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Testcontainers
class PostgresSearchRepositoryTest {
	@Autowired
	private lateinit var searchRepository: SearchRepository

	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate

	private val parser = SearchQueryParser()

	@BeforeEach
	fun seed() {
		jdbcTemplate.update("DELETE FROM search_terms")
		jdbcTemplate.update("DELETE FROM emails")
		insert("gas-subject", "Gas desk", "A short note.")
		insert("body-only", "Weekly update", "The gas nomination is attached.")
		insert(
			"gas-contract",
			"Scheduling",
			"Please review the supply contract for the gas desk tomorrow.",
		)
		jdbcTemplate.update(
			"""
			INSERT INTO search_terms (term)
			SELECT DISTINCT lexeme
			FROM emails AS source
			CROSS JOIN LATERAL unnest(source.search_vector) AS lex(lexeme, positions, weights)
			WHERE char_length(lexeme) >= 2
			ON CONFLICT DO NOTHING
			""".trimIndent(),
		)
	}

	@Test
	fun `matches keywords in any order and returns rank`() {
		val results = searchRepository.search(parser.parse("contract gas"), 100)

		assertEquals(listOf("gas-contract"), results.map { it.id })
		assertTrue(results.all { it.score > 0.0 })
		assertEquals("Scheduling", results.single().subject)
	}

	@Test
	fun `returns one row for each message id`() {
		jdbcTemplate.update(
			"""
			INSERT INTO emails (id, message_id, sender, date, subject, body, raw_message)
			VALUES (?, ?, ?, ?, ?, ?, ?)
			""".trimIndent(),
			"inbox-copy",
			"<gas-contract@enron.com>",
			"legal@enron.com",
			java.time.OffsetDateTime.parse("2001-05-16T13:30:00Z"),
			"Scheduling",
			"Please review the supply contract for the gas desk tomorrow.",
			"copy",
		)
		jdbcTemplate.update("UPDATE emails SET message_id = ? WHERE id = ?", "<gas-contract@enron.com>", "gas-contract")

		val results = searchRepository.search(parser.parse("contract gas"), 100)

		assertEquals(listOf("gas-contract"), results.map { it.id })
	}

	@Test
	fun `ranks a subject match above a body-only match`() {
		val results = searchRepository.search(parser.parse("gas"), 100)

		assertEquals("gas-subject", results.first().id)
		assertTrue(results.zipWithNext().all { (left, right) -> left.score >= right.score })
	}

	@Test
	fun `resolves a misspelled term through the vocabulary`() {
		val results = searchRepository.search(parser.parse("contrct"), 100)

		assertEquals(listOf("gas-contract"), results.map { it.id })
	}

	@Test
	fun `does not execute hostile input as sql`() {
		val results = searchRepository.search(parser.parse("gas'; DROP TABLE emails;--"), 100)

		assertTrue(results.any { it.id == "gas-subject" })
		val remaining = jdbcTemplate.queryForObject("SELECT count(*) FROM emails", Int::class.java)
		assertEquals(3, remaining)
	}

	@Test
	fun `uses the full-text index for document lookup`() {
		val plan = jdbcTemplate.queryForList(
			"""
			EXPLAIN
			SELECT id
			FROM emails
			WHERE search_vector @@ plainto_tsquery('simple', 'gas')
			""".trimIndent(),
			String::class.java,
		).joinToString("\n")

		jdbcTemplate.execute("SET enable_seqscan = off")
		val indexed = jdbcTemplate.queryForList(
			"""
			EXPLAIN
			SELECT id
			FROM emails
			WHERE search_vector @@ plainto_tsquery('simple', 'gas')
			""".trimIndent(),
			String::class.java,
		).joinToString("\n")
		jdbcTemplate.execute("SET enable_seqscan = on")

		assertTrue(plan.isNotBlank())
		assertTrue(indexed.contains("idx_emails_search_vector"), indexed)
	}

	private fun insert(id: String, subject: String, body: String) {
		jdbcTemplate.update(
			"""
			INSERT INTO emails (id, sender, date, subject, body, raw_message)
			VALUES (?, ?, ?, ?, ?, ?)
			""".trimIndent(),
			id,
			"legal@enron.com",
			java.time.OffsetDateTime.parse("2001-05-16T13:30:00Z"),
			subject,
			body,
			body,
		)
	}

	companion object {
		@Container
		@JvmStatic
		val postgres = PostgreSQLContainer("postgres:17")

		@DynamicPropertySource
		@JvmStatic
		fun properties(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url", postgres::getJdbcUrl)
			registry.add("spring.datasource.username", postgres::getUsername)
			registry.add("spring.datasource.password", postgres::getPassword)
		}
	}
}
