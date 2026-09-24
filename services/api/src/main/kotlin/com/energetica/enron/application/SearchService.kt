package com.energetica.enron.application

import com.energetica.enron.domain.SearchResult

interface SearchService {
	fun query(searchTerm: String): List<SearchResult>
}
