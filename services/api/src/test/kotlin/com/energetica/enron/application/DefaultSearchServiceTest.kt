package com.energetica.enron.application

import com.energetica.enron.domain.SearchExpression
import com.energetica.enron.domain.SearchQuery
import com.energetica.enron.domain.SearchResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant
import kotlin.test.assertEquals

class DefaultSearchServiceTest {
	private val repository = mock(SearchRepository::class.java)
	private val service = DefaultSearchService(SearchQueryParser(), repository)

	@Test
	fun `parses keywords and delegates to the repository`() {
		val expected = listOf(sampleResult())
		val query = SearchQuery(
			SearchExpression.And(
				listOf(SearchExpression.Term("gas"), SearchExpression.Term("contract")),
			),
		)
		`when`(repository.search(query, 100)).thenReturn(expected)

		val actual = service.query("gas contract")

		assertEquals(expected, actual)
		verify(repository).search(query, 100)
	}

	@Test
	fun `rejects a blank search term`() {
		assertThrows<IllegalArgumentException> { service.query("") }
		verifyNoInteractions(repository)
	}

	@Test
	fun `rejects a whitespace-only search term`() {
		assertThrows<IllegalArgumentException> { service.query("   \t") }
		verifyNoInteractions(repository)
	}

	@Test
	fun `rejects a query that contains no keywords`() {
		assertThrows<IllegalArgumentException> { service.query("@@@") }
		verifyNoInteractions(repository)
	}

	private fun sampleResult() = SearchResult(
		id = "allen-p/_sent_mail/1.",
		sender = "gerald.nemec@enron.com",
		xTo = "Tim Belden",
		xCc = null,
		xBcc = null,
		date = Instant.parse("2001-05-16T13:30:00Z"),
		subject = "IT Contract",
		body = "Please review the gas contract.",
		score = 5.2679,
	)
}
