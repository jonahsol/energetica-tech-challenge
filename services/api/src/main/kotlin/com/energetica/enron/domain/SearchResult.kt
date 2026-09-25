package com.energetica.enron.domain

import java.time.Instant

data class SearchResult(
	val id: String,
	val sender: String,
	val xTo: String?,
	val xCc: String?,
	val xBcc: String?,
	val date: Instant?,
	val subject: String,
	val body: String,
	val score: Double,
)
