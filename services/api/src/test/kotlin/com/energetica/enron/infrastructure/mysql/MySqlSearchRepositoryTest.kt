package com.energetica.enron.infrastructure.mysql

import com.energetica.enron.application.SearchRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class MySqlSearchRepositoryTest {
	@Autowired
	private lateinit var searchRepository: SearchRepository

	@Test
	fun `search returns ranked fulltext matches from mysql`() {
		val results = searchRepository.search("gas contract")

		assertTrue(results.isNotEmpty(), "expected fulltext matches for 'gas contract'")
		assertTrue(results.size <= 100)
		assertTrue(results.zipWithNext().all { (left, right) -> left.score >= right.score })
		assertTrue(results.all { it.score > 0.0 && it.sender.isNotEmpty() && it.body.isNotEmpty() })
		val known = results.first { it.id == 234679 }
		assertEquals(Instant.parse("2001-05-16T13:30:00Z"), known.date)
		assertEquals("IT Contract", known.subject)
	}
}
