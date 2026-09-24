package com.energetica.enron.application

import com.energetica.enron.domain.SearchResult
import org.springframework.stereotype.Service

@Service
class DefaultSearchService(
	private val searchRepository: SearchRepository,
) : SearchService {
	override fun query(searchTerm: String): List<SearchResult> {
		require(searchTerm.isNotBlank()) { "searchTerm must not be blank" }
		return searchRepository.search(searchTerm)
	}
}
