package com.energetica.enron.application

import com.energetica.enron.domain.SearchQuery
import com.energetica.enron.domain.SearchResult

interface SearchRepository {
	fun search(query: SearchQuery, limit: Int): List<SearchResult>
}
