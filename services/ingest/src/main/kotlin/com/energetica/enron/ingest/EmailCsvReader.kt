package com.energetica.enron.ingest

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class EmailCsvReader {
	fun read(path: Path): Sequence<CsvEmailRecord> {
		return read(Files.newBufferedReader(path, StandardCharsets.UTF_8))
	}

	fun read(reader: Reader): Sequence<CsvEmailRecord> = sequence {
		reader.use { source ->
			CSVParser.parse(source, FORMAT).use { parser ->
				for (record in parser) {
					val id = record.get("file").trim()
					if (id.isEmpty()) {
						continue
					}
					yield(CsvEmailRecord(id = id, rawMessage = record.get("message")))
				}
			}
		}
	}

	private companion object {
		val FORMAT: CSVFormat = CSVFormat.RFC4180.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.get()
	}
}
