package com.energetica.enron.ingest

import java.nio.file.Files
import java.nio.file.Path

data class IngestConfig(
	val jdbcUrl: String,
	val user: String,
	val password: String,
	val csvPath: Path,
	val batchSize: Int,
) {
	companion object {
		fun fromEnvironment(env: Map<String, String> = System.getenv()): IngestConfig {
			val host = env["DB_HOST"] ?: "localhost"
			val port = env["DB_PORT"] ?: "5432"
			val name = env["DB_NAME"] ?: "enron"
			val user = env["DB_USER"] ?: "enron"
			val password = env["DB_PASSWORD"] ?: "enron"
			val batchSize = env["BATCH_SIZE"]?.toIntOrNull()?.takeIf { it > 0 } ?: 50
			return IngestConfig(
				jdbcUrl = "jdbc:postgresql://$host:$port/$name",
				user = user,
				password = password,
				csvPath = resolveCsv(env),
				batchSize = batchSize,
			)
		}

		private fun resolveCsv(env: Map<String, String>): Path {
			env["INGEST_CSV"]?.let { return Path.of(it) }
			val candidates = listOf(
				Path.of("emails.csv"),
				Path.of("services/ingest/emails.csv"),
			)
			return candidates.firstOrNull { Files.exists(it) } ?: candidates.last()
		}
	}
}
