package com.energetica.enron.ingest

import org.apache.james.mime4j.dom.Entity
import org.apache.james.mime4j.dom.Message
import org.apache.james.mime4j.dom.Multipart
import org.apache.james.mime4j.dom.TextBody
import org.apache.james.mime4j.dom.address.AddressList
import org.apache.james.mime4j.dom.address.Mailbox
import org.apache.james.mime4j.dom.address.MailboxList
import org.apache.james.mime4j.message.DefaultMessageBuilder
import org.apache.james.mime4j.stream.MimeConfig
import java.nio.charset.StandardCharsets

class MimeEmailParser {
	fun parse(record: CsvEmailRecord): ParsedEmail {
		val builder = DefaultMessageBuilder()
		builder.setMimeEntityConfig(MimeConfig.PERMISSIVE)
		val parsed = builder.parseMessage(record.rawMessage.byteInputStream(StandardCharsets.UTF_8))
		try {
			return ParsedEmail(
				id = record.id,
				messageId = parsed.messageId?.trim()?.takeIf { it.isNotEmpty() },
				sender = sender(parsed),
				recipientTo = addresses(parsed.to),
				recipientCc = addresses(parsed.cc),
				recipientBcc = addresses(parsed.bcc),
				xTo = header(parsed, "X-To"),
				xCc = header(parsed, "X-cc"),
				xBcc = header(parsed, "X-bcc"),
				date = parsed.date?.toInstant(),
				subject = parsed.subject?.trim().orEmpty(),
				body = body(parsed).trim(),
				rawMessage = record.rawMessage,
			)
		} finally {
			parsed.dispose()
		}
	}

	private fun header(message: Message, name: String): String? {
		return message.header?.getField(name)?.body?.trim()?.takeIf { it.isNotEmpty() }
	}

	private fun sender(message: Message): String {
		val mailbox = message.from?.firstOrNull()
		return mailbox?.address ?: mailbox?.name.orEmpty()
	}

	private fun addresses(mailboxes: MailboxList?): String? {
		return mailboxes
			?.mapNotNull { mailbox -> mailbox.address ?: mailbox.name }
			?.takeIf { it.isNotEmpty() }
			?.joinToString(", ")
	}

	private fun addresses(addresses: AddressList?): String? {
		if (addresses == null || addresses.isEmpty()) {
			return null
		}
		return addresses.mapNotNull { address ->
			when (address) {
				is Mailbox -> address.address ?: address.name
				else -> address.toString()
			}
		}.takeIf { it.isNotEmpty() }?.joinToString(", ")
	}

	private fun body(entity: Entity): String {
		val plain = StringBuilder()
		val html = StringBuilder()
		collect(entity, plain, html)
		val text = if (plain.isNotEmpty()) plain else html
		return text.toString()
	}

	private fun collect(entity: Entity, plain: StringBuilder, html: StringBuilder) {
		val body = entity.body
		if (body is Multipart) {
			for (part in body.bodyParts) {
				collect(part, plain, html)
			}
			return
		}
		if (body is TextBody) {
			val mimeType = entity.mimeType?.lowercase().orEmpty()
			val target = if (mimeType.startsWith("text/html")) html else plain
			body.reader.use { reader ->
				if (target.isNotEmpty()) {
					target.append("\n")
				}
				target.append(reader.readText())
			}
		}
	}
}
