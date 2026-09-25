package com.energetica.enron.ingest

import java.time.Instant

data class ParsedEmail(
	val id: String,
	val messageId: String?,
	val sender: String,
	val recipientTo: String?,
	val recipientCc: String?,
	val recipientBcc: String?,
	val xTo: String?,
	val xCc: String?,
	val xBcc: String?,
	val date: Instant?,
	val subject: String,
	val body: String,
	val rawMessage: String,
)
