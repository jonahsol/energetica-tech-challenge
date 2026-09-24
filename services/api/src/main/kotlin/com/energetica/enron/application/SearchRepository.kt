package com.energetica.enron.application

import com.energetica.enron.domain.SearchResult

interface SearchRepository {
	fun search(searchTerm: String): List<SearchResult>
}
