package com.energetica.enron.application

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
	private val service = DefaultSearchService(repository)

	@Test
	fun `delegates a valid search term to the repository`() {
		val expected = listOf(sampleResult())
		`when`(repository.search("gas contract")).thenReturn(expected)

		val actual = service.query("gas contract")

		assertEquals(expected, actual)
		verify(repository).search("gas contract")
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

	private fun sampleResult() = SearchResult(
		id = 234679,
		sender = "gerald.nemec@enron.com",
		date = Instant.parse("2001-05-16T13:30:00Z"),
		subject = "IT Contract",
		body = "Please review the gas contract.",
		score = 5.2679,
	)
}
