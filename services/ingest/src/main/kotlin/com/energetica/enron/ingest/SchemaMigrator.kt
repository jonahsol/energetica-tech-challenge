package com.energetica.enron.ingest

import java.sql.Connection

class SchemaMigrator {
	fun apply(connection: Connection, sql: String) {
		val originalAutoCommit = connection.autoCommit
		connection.autoCommit = true
		try {
			for (statement in SqlScript.split(sql)) {
				connection.createStatement().use { it.execute(statement) }
			}
		} finally {
			connection.autoCommit = originalAutoCommit
		}
	}
}
