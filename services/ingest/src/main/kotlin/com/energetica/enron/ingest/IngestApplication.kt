package com.energetica.enron.ingest

import org.slf4j.LoggerFactory
import java.sql.DriverManager
import kotlin.system.exitProcess

class IngestApplication(
	private val config: IngestConfig,
) {
	private val log = LoggerFactory.getLogger(IngestApplication::class.java)

	fun run() {
		val schema = SchemaMigrator::class.java.getResource("/services/db/schema.sql")?.readText()
			?: error("Missing /services/db/schema.sql on the classpath")
		DriverManager.getConnection(config.jdbcUrl, config.user, config.password).use { connection ->

            // Apply the schema to the database
			SchemaMigrator().apply(connection, schema)
			connection.autoCommit = false

            log.info("Running ingest pipeline")
   
            // Run the ingest pipeline
			val stats = IngestPipeline(
                // Parse the email
				parser = MimeEmailParser(),
                // Write to the database
				writer = EmailBatchWriter(connection),
			).run(
                // Read the CSV file
                EmailCsvReader().read(config.csvPath), 
                config.batchSize
            )


			log.info(
				"Ingest finished. read={} written={} failed={}",
				stats.read,
				stats.written,
				stats.failed,
			)
			if (stats.failed == stats.read) {
				throw IllegalStateException("Ingest wrote no emails")
			}
		}
	}
}

fun main() {
	val log = LoggerFactory.getLogger("IngestApplication")
	try {
		IngestApplication(IngestConfig.fromEnvironment()).run()
	} catch (exception: Exception) {
		log.error("Ingest failed", exception)
		exitProcess(1)
	}
}
