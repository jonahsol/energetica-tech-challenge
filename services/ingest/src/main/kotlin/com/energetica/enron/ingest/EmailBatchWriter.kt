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
			insertEmails(batch)
			insertTerms(batch.map { it.id })
			connection.commit()
		} catch (exception: Exception) {
			connection.rollback()
			throw exception
		}
		return batch.size
	}

	private fun insertEmails(batch: List<ParsedEmail>) {
		connection.prepareStatement(INSERT_EMAIL).use { statement ->
			for (email in batch) {
				statement.setString(1, email.id)
				statement.setString(2, email.messageId)
				statement.setString(3, email.sender)
				statement.setString(4, email.recipientTo)
				statement.setString(5, email.recipientCc)
				statement.setString(6, email.recipientBcc)
				statement.setString(7, email.xTo)
				statement.setString(8, email.xCc)
				statement.setString(9, email.xBcc)
				if (email.date == null) {
					statement.setObject(10, null)
				} else {
					statement.setObject(10, java.time.OffsetDateTime.ofInstant(email.date, java.time.ZoneOffset.UTC))
				}
				statement.setString(11, email.subject)
				statement.setString(12, email.body)
				statement.setString(13, email.rawMessage)
				statement.addBatch()
			}
			statement.executeBatch()
		}
	}

	private fun insertTerms(ids: List<String>) {
		connection.prepareStatement(INSERT_TERMS).use { statement ->
			statement.setArray(1, connection.createArrayOf("text", ids.toTypedArray()))
			statement.executeUpdate()
		}
	}

	private companion object {
		val INSERT_EMAIL = """
			INSERT INTO emails (
			    id, message_id, sender, recipient_to, recipient_cc, recipient_bcc,
			    x_to, x_cc, x_bcc, date, subject, body, raw_message
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			ON CONFLICT (id) DO UPDATE SET
			    message_id = EXCLUDED.message_id,
			    sender = EXCLUDED.sender,
			    recipient_to = EXCLUDED.recipient_to,
			    recipient_cc = EXCLUDED.recipient_cc,
			    recipient_bcc = EXCLUDED.recipient_bcc,
			    x_to = EXCLUDED.x_to,
			    x_cc = EXCLUDED.x_cc,
			    x_bcc = EXCLUDED.x_bcc,
			    date = EXCLUDED.date,
			    subject = EXCLUDED.subject,
			    body = EXCLUDED.body,
			    raw_message = EXCLUDED.raw_message
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
