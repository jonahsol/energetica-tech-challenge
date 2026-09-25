package com.energetica.enron.ingest

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IngestPipelineTest {
	@Test
	fun `writes bounded batches and discards them`() {
		val writer = RecordingWriter()
		val pipeline = IngestPipeline(MimeEmailParser(), writer)
		val records = (1..5).asSequence().map { index ->
			CsvEmailRecord(
				id = "id-$index",
				rawMessage = """
					From: sender-$index@enron.com
					Subject: Subject $index
					Content-Type: text/plain; charset=us-ascii

					Body $index mentions gas
				""".trimIndent(),
			)
		}

		val stats = pipeline.run(records, batchSize = 2)

		assertEquals(IngestStats(read = 5, written = 5, failed = 0), stats)
		assertEquals(listOf(2, 2, 1), writer.batchSizes)
		assertTrue(writer.maxBuffered <= 2)
	}

	private class RecordingWriter : EmailWriter {
		val batchSizes = mutableListOf<Int>()
		var maxBuffered = 0

		override fun write(batch: List<ParsedEmail>): Int {
			maxBuffered = maxOf(maxBuffered, batch.size)
			batchSizes.add(batch.size)
			return batch.size
		}
	}
}
