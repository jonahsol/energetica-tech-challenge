package com.energetica.enron.ingest

import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import kotlin.test.assertEquals

@Testcontainers
class EmailBatchWriterTest {
	@Test
	fun `stores an oversized body without building a tsvector from all of it`() {
		DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
			val schema = EmailBatchWriterTest::class.java.getResource("/services/db/schema.sql")!!.readText()
			SchemaMigrator().apply(connection, schema)
			connection.autoCommit = false
			connection.createStatement().use { it.execute("DELETE FROM search_terms") }
			connection.createStatement().use { it.execute("DELETE FROM emails") }
			val body = "A".repeat(1_100_000)
			EmailBatchWriter(connection).write(listOf(MimeEmailParser().parse(record("huge", body))))

			val stored = connection.createStatement().use { statement ->
				statement.executeQuery("SELECT body, octet_length(body::text) FROM emails WHERE id = 'huge'").use { rs ->
					rs.next()
					rs.getString(1).length to rs.getInt(2)
				}
			}
			assertEquals(1_100_000, stored.first)
		}
	}

	@Test
	fun `duplicate vocabulary terms do not create duplicate rows`() {
		DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
			val schema = EmailBatchWriterTest::class.java.getResource("/services/db/schema.sql")!!.readText()
			SchemaMigrator().apply(connection, schema)
			connection.autoCommit = false
			connection.createStatement().use { it.execute("DELETE FROM search_terms") }
			connection.createStatement().use { it.execute("DELETE FROM emails") }
			val writer = EmailBatchWriter(connection)
			val parser = MimeEmailParser()
			val first = parser.parse(record("a", "The gas contract is ready"))
			val second = parser.parse(record("b", "Another gas contract arrived"))
			writer.write(listOf(first))
			writer.write(listOf(second))

			val terms = connection.createStatement().use { statement ->
				statement.executeQuery("SELECT count(*) FROM search_terms WHERE term = 'contract'").use { rs ->
					rs.next()
					rs.getInt(1)
				}
			}
			val emails = connection.createStatement().use { statement ->
				statement.executeQuery("SELECT count(*) FROM emails").use { rs ->
					rs.next()
					rs.getInt(1)
				}
			}
			assertEquals(1, terms)
			assertEquals(2, emails)
		}
	}

	private fun record(id: String, body: String) = CsvEmailRecord(
		id = id,
		rawMessage = """
			From: legal@enron.com
			Subject: Gas Contract
			Content-Type: text/plain; charset=us-ascii

			$body
		""".trimIndent(),
	)

	companion object {
		@Container
		@JvmStatic
		val postgres = PostgreSQLContainer("postgres:17")
	}
}
