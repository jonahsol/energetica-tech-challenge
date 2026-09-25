package com.energetica.enron.domain

data class SearchQuery(
	val expression: SearchExpression,
)

sealed interface SearchExpression {
	data class Term(val text: String) : SearchExpression

	data class And(val children: List<SearchExpression>) : SearchExpression

	data class Or(val children: List<SearchExpression>) : SearchExpression
}
