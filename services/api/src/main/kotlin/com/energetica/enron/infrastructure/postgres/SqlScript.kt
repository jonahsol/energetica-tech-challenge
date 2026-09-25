package com.energetica.enron.infrastructure.postgres

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
				val statement = current.toString().trim()
				current.clear()
				if (statement.isNotEmpty()) {
					statements.add(statement)
				}
			} else {
				current.append(character)
			}
			index++
		}
		val tail = current.toString().trim()
		if (tail.isNotEmpty()) {
			statements.add(tail)
		}
		return statements
	}
}
