package com.energetica.enron.application

import com.energetica.enron.domain.SearchExpression
import com.energetica.enron.domain.SearchQuery
import org.springframework.stereotype.Component

@Component
class SearchQueryParser {
	fun parse(raw: String): SearchQuery {
		if (raw.isBlank()) {
			throw IllegalArgumentException("searchTerm must not be blank")
		}
		val tokens = TOKEN.findAll(raw).map { match ->
			val text = match.value.lowercase()
			when (text) {
				"and" -> Token.And
				"or" -> Token.Or
				else -> Token.Word(text)
			}
		}.toList()
		val expression = parseOr(tokens)
			?: throw IllegalArgumentException("searchTerm must contain at least one keyword")
		return SearchQuery(expression)
	}

	private fun parseOr(tokens: List<Token>): SearchExpression? {
		val children = split(tokens, Token.Or).mapNotNull { parseAnd(it) }
		return when (children.size) {
			0 -> null
			1 -> children[0]
			else -> SearchExpression.Or(children)
		}
	}

	private fun parseAnd(tokens: List<Token>): SearchExpression? {
		val words = tokens.filterIsInstance<Token.Word>().filter { it.text.length >= 2 }
		return when (words.size) {
			0 -> null
			1 -> SearchExpression.Term(words[0].text)
			else -> SearchExpression.And(words.map { SearchExpression.Term(it.text) })
		}
	}

	private fun split(tokens: List<Token>, separator: Token): List<List<Token>> {
		val parts = mutableListOf<List<Token>>()
		val current = mutableListOf<Token>()
		for (token in tokens) {
			if (token == separator) {
				parts.add(current.toList())
				current.clear()
			} else {
				current.add(token)
			}
		}
		parts.add(current.toList())
		return parts
	}

	private sealed interface Token {
		data object And : Token

		data object Or : Token

		data class Word(val text: String) : Token
	}

	private companion object {
		val TOKEN = Regex("[A-Za-z0-9]+")
	}
}
