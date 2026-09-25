package com.energetica.enron.ingest

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MimeEmailParserTest {
	private val parser = MimeEmailParser()

	@Test
	fun `extracts headers subject and body`() {
		val raw = """
			Message-ID: <18782981.1075855378110.JavaMail.evans@thyme>
			Date: Mon, 14 May 2001 16:39:00 -0700 (PDT)
			From: phillip.allen@enron.com
			To: tim.belden@enron.com
			Cc: legal@enron.com
			Bcc: audit@enron.com
			X-To: Tim Belden <Tim Belden/Enron@EnronXGate>
			X-cc: Legal Team
			X-bcc: Audit
			Subject: Gas Contract
			Mime-Version: 1.0
			Content-Type: text/plain; charset=us-ascii
			Content-Transfer-Encoding: 7bit

			Please review the gas contract.
		""".trimIndent()

		val email = parser.parse(CsvEmailRecord("allen-p/_sent_mail/1.", raw))

		assertEquals("allen-p/_sent_mail/1.", email.id)
		assertEquals("<18782981.1075855378110.JavaMail.evans@thyme>", email.messageId)
		assertEquals("phillip.allen@enron.com", email.sender)
		assertEquals("tim.belden@enron.com", email.recipientTo)
		assertEquals("legal@enron.com", email.recipientCc)
		assertEquals("audit@enron.com", email.recipientBcc)
		assertEquals("Tim Belden <Tim Belden/Enron@EnronXGate>", email.xTo)
		assertEquals("Legal Team", email.xCc)
		assertEquals("Audit", email.xBcc)
		assertEquals(Instant.parse("2001-05-14T23:39:00Z"), email.date)
		assertEquals("Gas Contract", email.subject)
		assertEquals("Please review the gas contract.", email.body)
		assertTrue(email.rawMessage.contains("Message-ID:"))
	}

	@Test
	fun `decodes a quoted-printable body`() {
		val raw = """
			From: phillip.allen@enron.com
			Subject: Forecast
			MIME-Version: 1.0
			Content-Type: text/plain; charset=utf-8
			Content-Transfer-Encoding: quoted-printable

			Here is our foreca=
			st
		""".trimIndent()

		val email = parser.parse(CsvEmailRecord("id", raw))

		assertEquals("Forecast", email.subject)
		assertTrue(email.body.contains("forecast"))
	}
}
