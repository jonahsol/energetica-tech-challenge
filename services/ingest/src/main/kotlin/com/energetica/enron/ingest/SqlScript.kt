package com.energetica.enron.ingest

/**
 * Splits a SQL script on semicolons that are outside dollar-quoted function bodies.
 */
object SqlScript {
	fun split(sql: String): List<String> {
		val statements = mutableListOf<String>()
		val current = StringBuilder()
		var index = 0
		var inDollarQuote = false
		while (index < sql.length) {
			if (sql.startsWith("$$", index)) {
				inDollarQuote = !inDollarQuote
				current.append("$$")
				index += 2
				continue
			}
			val character = sql[index]
			if (character == ';' && !inDollarQuote) {
				addStatement(statements, current)
			} else {
				current.append(character)
			}
			index++
		}
		addStatement(statements, current)
		return statements
	}

	private fun addStatement(statements: MutableList<String>, current: StringBuilder) {
		val statement = current.toString().trim()
		current.clear()
		if (statement.isNotEmpty()) {
			statements.add(statement)
		}
	}
}
