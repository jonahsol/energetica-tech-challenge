package com.energetica.enron.ingest

import java.sql.Connection

interface EmailWriter {
	fun write(batch: List<ParsedEmail>): Int
}

class EmailBatchWriter(
	private val connection: Connection,
) : EmailWriter {
	override fun write(batch: List<ParsedEmail>): Int {
		if (batch.isEmpty()) {
			return 0
		}
		try {
			val insertedIds = insertEmails(batch)
			if (insertedIds.isNotEmpty()) {
				insertTerms(insertedIds)
			}
			connection.commit()
			return insertedIds.size
		} catch (exception: Exception) {
			connection.rollback()
			throw exception
		}
	}

	private fun insertEmails(batch: List<ParsedEmail>): List<String> {
		connection.prepareStatement(INSERT_EMAIL).use { statement ->
			val arrays = arrayOf(
				textArray(batch.map { it.id }),
				textArray(batch.map { it.messageId }),
				textArray(batch.map { it.sender }),
				textArray(batch.map { it.recipientTo }),
				textArray(batch.map { it.recipientCc }),
				textArray(batch.map { it.recipientBcc }),
				textArray(batch.map { it.xTo }),
				textArray(batch.map { it.xCc }),
				textArray(batch.map { it.xBcc }),
				timestampArray(batch.map { email ->
					email.date?.let { java.time.OffsetDateTime.ofInstant(it, java.time.ZoneOffset.UTC) }
				}),
				textArray(batch.map { it.subject }),
				textArray(batch.map { it.body }),
				textArray(batch.map { it.rawMessage }),
			)
			try {
				arrays.forEachIndexed { index, array -> statement.setArray(index + 1, array) }
				statement.executeQuery().use { rs ->
					val insertedIds = mutableListOf<String>()
					while (rs.next()) {
						insertedIds.add(rs.getString(1))
					}
					return insertedIds
				}
			} finally {
				arrays.forEach { it.free() }
			}
		}
	}

	private fun textArray(values: List<String?>): java.sql.Array {
		val elements = arrayOfNulls<Any>(values.size)
		values.forEachIndexed { index, value -> elements[index] = value }
		return connection.createArrayOf("text", elements)
	}

	private fun timestampArray(values: List<java.time.OffsetDateTime?>): java.sql.Array {
		val elements = arrayOfNulls<Any>(values.size)
		values.forEachIndexed { index, value -> elements[index] = value }
		return connection.createArrayOf("timestamptz", elements)
	}

	private fun insertTerms(ids: List<String>) {
		connection.prepareStatement(INSERT_TERMS).use { statement ->
			statement.setArray(1, connection.createArrayOf("text", ids.toTypedArray()))
			statement.executeUpdate()
		}
	}

	private companion object {
		// One statement for the whole batch. JDBC's reWriteBatchedInserts turns addBatch()
		// into a multi-row INSERT whose generated content_key collisions abort the batch.
		val INSERT_EMAIL = """
			INSERT INTO emails (
			    id, message_id, sender, recipient_to, recipient_cc, recipient_bcc,
			    x_to, x_cc, x_bcc, date, subject, body, raw_message
			)
			SELECT DISTINCT ON (email_content_key(id, sender, sent_at, subject, body))
			    id, message_id, sender, recipient_to, recipient_cc, recipient_bcc,
			    x_to, x_cc, x_bcc, sent_at, subject, body, raw_message
			FROM unnest(
			    ?::text[],
			    ?::text[],
			    ?::text[],
			    ?::text[],
			    ?::text[],
			    ?::text[],
			    ?::text[],
			    ?::text[],
			    ?::text[],
			    ?::timestamptz[],
			    ?::text[],
			    ?::text[],
			    ?::text[]
			) WITH ORDINALITY AS incoming(
			    id,
			    message_id,
			    sender,
			    recipient_to,
			    recipient_cc,
			    recipient_bcc,
			    x_to,
			    x_cc,
			    x_bcc,
			    sent_at,
			    subject,
			    body,
			    raw_message,
			    ordinality
			)
			ORDER BY email_content_key(id, sender, sent_at, subject, body), ordinality
			ON CONFLICT (content_key) DO NOTHING
			RETURNING id
		""".trimIndent()

		val INSERT_TERMS = """
			INSERT INTO search_terms (term)
			SELECT DISTINCT lexeme
			FROM emails AS source
			CROSS JOIN LATERAL unnest(source.search_vector) AS lex(lexeme, positions, weights)
			WHERE source.id = ANY (?)
			  AND char_length(lexeme) >= 2
			ON CONFLICT DO NOTHING
		""".trimIndent()
	}
}
