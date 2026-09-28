package com.energetica.enron.ingest

import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals

@Testcontainers
class EmailBatchWriterTest {
	@Test
	fun `stores an oversized body without building a tsvector from all of it`() {
		withDatabase { connection ->
			val body = "A".repeat(1_100_000)
			EmailBatchWriter(connection).write(listOf(MimeEmailParser().parse(record("huge", body))))

			val stored = connection.createStatement().use { statement ->
				statement.executeQuery("SELECT body, octet_length(body::text) FROM emails WHERE id = 'huge'").use { rs ->
					rs.next()
					rs.getString(1).length to rs.getInt(2)
				}
			}
			assertEquals(1_100_000, stored.first)
		}
	}

	@Test
	fun `duplicate vocabulary terms do not create duplicate rows`() {
		withDatabase { connection ->
			val writer = EmailBatchWriter(connection)
			val parser = MimeEmailParser()
			val first = parser.parse(record("a", "The gas contract is ready"))
			val second = parser.parse(record("b", "Another gas contract arrived"))
			writer.write(listOf(first))
			writer.write(listOf(second))

			assertEquals(1, count(connection, "SELECT count(*) FROM search_terms WHERE term = 'contract'"))
			assertEquals(2, count(connection, "SELECT count(*) FROM emails"))
		}
	}

	@Test
	fun `keeps the first folder copy in a mailbox and skips later ones`() {
		withDatabase { connection ->
			val writer = EmailBatchWriter(connection)
			val parser = MimeEmailParser()
			val body = "The gas contract is ready"
			val written = writer.write(
				listOf(
					parser.parse(record("dasovich-j/all_documents/1.", body)),
					parser.parse(record("dasovich-j/notes_inbox/2.", body)),
					parser.parse(record("dasovich-j/inbox/3.", "A different note about gas")),
				),
			)
			val writtenAgain = writer.write(
				listOf(parser.parse(record("dasovich-j/sent/4.", body))),
			)
			val otherMailbox = writer.write(
				listOf(parser.parse(record("skilling-j/inbox/1.", body))),
			)
			val repeated = writer.write(
				(1..50).map { index -> parser.parse(record("dasovich-j/copy/$index.", body)) },
			)

			assertEquals(2, written)
			assertEquals(0, writtenAgain)
			assertEquals(1, otherMailbox)
			assertEquals(0, repeated)
			assertEquals(
				listOf("dasovich-j/all_documents/1.", "dasovich-j/inbox/3.", "skilling-j/inbox/1."),
				ids(connection),
			)
			assertEquals(1, count(connection, "SELECT count(*) FROM search_terms WHERE term = 'contract'"))
			assertEquals(1, count(connection, "SELECT count(*) FROM search_terms WHERE term = 'different'"))
		}
	}

	private fun withDatabase(block: (Connection) -> Unit) {
		val jdbcUrl = postgres.jdbcUrl + (if (postgres.jdbcUrl.contains("?")) "&" else "?") + "reWriteBatchedInserts=true"
		DriverManager.getConnection(jdbcUrl, postgres.username, postgres.password).use { connection ->
			val schema = EmailBatchWriterTest::class.java.getResource("/services/db/schema.sql")!!.readText()
			SchemaMigrator().apply(connection, schema)
			connection.autoCommit = false
			connection.createStatement().use { it.execute("DELETE FROM search_terms") }
			connection.createStatement().use { it.execute("DELETE FROM emails") }
			block(connection)
		}
	}

	private fun count(connection: Connection, sql: String): Int {
		return connection.createStatement().use { statement ->
			statement.executeQuery(sql).use { rs ->
				rs.next()
				rs.getInt(1)
			}
		}
	}

	private fun ids(connection: Connection): List<String> {
		return connection.createStatement().use { statement ->
			statement.executeQuery("SELECT id FROM emails ORDER BY id").use { rs ->
				val ids = mutableListOf<String>()
				while (rs.next()) {
					ids.add(rs.getString(1))
				}
				ids
			}
		}
	}

	private fun record(id: String, body: String) = CsvEmailRecord(
		id = id,
		rawMessage = """
			From: legal@enron.com
			Subject: Gas Contract
			Content-Type: text/plain; charset=us-ascii

			$body
		""".trimIndent(),
	)

	companion object {
		@Container
		@JvmStatic
		val postgres = PostgreSQLContainer("postgres:17")
	}
}
