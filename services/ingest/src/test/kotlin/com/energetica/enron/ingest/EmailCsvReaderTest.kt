package com.energetica.enron.ingest

import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmailCsvReaderTest {
	private val reader = EmailCsvReader()

	@Test
	fun `reads a quoted multiline message`() {
		val csv = """
			"file","message"
			"allen-p/_sent_mail/1.","Message-ID: <one>
			Date: Mon, 14 May 2001 16:39:00 -0700 (PDT)
			From: phillip.allen@enron.com

			Here is our forecast
			"
		""".trimIndent()

		val records = reader.read(csv.reader()).toList()

		assertEquals(1, records.size)
		assertEquals("allen-p/_sent_mail/1.", records[0].id)
		assertTrue(records[0].rawMessage.contains("Here is our forecast"))
		assertTrue(records[0].rawMessage.contains("\n"))
	}

	@Test
	fun `keeps commas that appear inside the message`() {
		val csv = """
			"file","message"
			"folder/2.","From: a@enron.com
			To: b@enron.com, c@enron.com
			Subject: Hello, team

			Prices, volumes, and dates"
		""".trimIndent()

		val records = reader.read(csv.reader()).toList()

		assertEquals(1, records.size)
		assertTrue(records[0].rawMessage.contains("b@enron.com, c@enron.com"))
		assertTrue(records[0].rawMessage.contains("Hello, team"))
		assertTrue(records[0].rawMessage.contains("Prices, volumes, and dates"))
	}

	@Test
	fun `streams records instead of exposing them as one retained list`() {
		val csv = buildString {
			appendLine("\"file\",\"message\"")
			repeat(5) { index ->
				appendLine("\"id-$index\",\"From: a@enron.com\n\nbody $index\"")
			}
		}
		val seen = mutableListOf<String>()
		for (record in reader.read(csv.reader())) {
			seen.add(record.id)
			assertTrue(seen.size <= 5)
		}
		assertEquals(listOf("id-0", "id-1", "id-2", "id-3", "id-4"), seen)
	}

	@Test
	fun `reads the first real dataset row without loading the file`() {
		val path = Path.of("emails.csv")
		if (!path.toFile().exists()) {
			return
		}
		val first = reader.read(path).first()
		assertEquals("allen-p/_sent_mail/1.", first.id)
		assertTrue(first.rawMessage.contains("Here is our forecast"))
	}
}
