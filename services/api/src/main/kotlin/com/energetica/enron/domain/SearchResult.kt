package com.energetica.enron.domain

import java.time.Instant

data class SearchResult(
	val id: Int,
	val sender: String,
	val date: Instant,
	val subject: String,
	val body: String,
	val score: Double,
)
