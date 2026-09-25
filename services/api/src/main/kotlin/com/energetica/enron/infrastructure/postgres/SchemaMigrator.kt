package com.energetica.enron.infrastructure.postgres

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class SchemaMigrator(
	private val dataSource: DataSource,
) : ApplicationRunner {
	override fun run(args: ApplicationArguments) {
		val sql = javaClass.getResource("/services/db/schema.sql")?.readText()
			?: error("Missing /services/db/schema.sql on the classpath")
		dataSource.connection.use { connection ->
			connection.autoCommit = true
			for (statement in SqlScript.split(sql)) {
				connection.createStatement().use { it.execute(statement) }
			}
		}
	}
}
