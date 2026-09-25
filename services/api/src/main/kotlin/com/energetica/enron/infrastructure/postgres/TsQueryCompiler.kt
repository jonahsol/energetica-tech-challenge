package com.energetica.enron.infrastructure.postgres

import com.energetica.enron.domain.SearchExpression

data class CompiledTsQuery(
	val sql: String,
	val parameters: List<String>,
)

/**
 * Builds a tsquery expression from an already-resolved AST.
 * Operator text is fixed. Term values are bound as parameters.
 */
object TsQueryCompiler {
	fun compile(expression: SearchExpression): CompiledTsQuery {
		val parameters = mutableListOf<String>()
		val sql = compile(expression, parameters)
		return CompiledTsQuery(sql, parameters)
	}

	private fun compile(expression: SearchExpression, parameters: MutableList<String>): String {
		return when (expression) {
			is SearchExpression.Term -> {
				parameters.add(expression.text)
				"plainto_tsquery('simple', ?)"
			}
			is SearchExpression.And -> join("&&", expression.children, parameters)
			is SearchExpression.Or -> join("||", expression.children, parameters)
		}
	}

	private fun join(operator: String, children: List<SearchExpression>, parameters: MutableList<String>): String {
		require(children.isNotEmpty())
		if (children.size == 1) {
			return compile(children[0], parameters)
		}
		return children.joinToString(separator = " $operator ", prefix = "(", postfix = ")") {
			compile(it, parameters)
		}
	}
}
