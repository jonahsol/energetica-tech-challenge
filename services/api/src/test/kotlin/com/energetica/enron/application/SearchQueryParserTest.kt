package com.energetica.enron.application

import com.energetica.enron.domain.SearchExpression
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SearchQueryParserTest {
	private val parser = SearchQueryParser()

	@Test
	fun `treats separate keywords as an and of terms in the given order`() {
		val query = parser.parse("gas contract")

		assertEquals(
			SearchExpression.And(
				listOf(SearchExpression.Term("gas"), SearchExpression.Term("contract")),
			),
			query.expression,
		)
	}

	@Test
	fun `builds an explicit and or ast`() {
		assertEquals(
			SearchExpression.Or(
				listOf(
					SearchExpression.Term("gas"),
					SearchExpression.And(
						listOf(SearchExpression.Term("oil"), SearchExpression.Term("contract")),
					),
				),
			),
			parser.parse("gas OR oil AND contract").expression,
		)
	}
}
