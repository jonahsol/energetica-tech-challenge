package com.energetica.enron.ingest

data class IngestStats(
	val read: Int,
	val written: Int,
	val failed: Int,
)

class IngestPipeline(
	private val parser: MimeEmailParser,
	private val writer: EmailWriter,
) {
	fun run(records: Sequence<CsvEmailRecord>, batchSize: Int): IngestStats {
		require(batchSize > 0) { "batchSize must be positive" }
		val batch = ArrayList<ParsedEmail>(batchSize)
		var read = 0
		var written = 0
		var failed = 0
		for (record in records) {
			read++
			try {
				batch.add(parser.parse(record))
			} catch (exception: Exception) {
				failed++
				continue
			}
			if (batch.size >= batchSize) {
				written += writer.write(batch)
				batch.clear()
			}
		}
		if (batch.isNotEmpty()) {
			written += writer.write(batch)
			batch.clear()
		}
		return IngestStats(read = read, written = written, failed = failed)
	}
}
