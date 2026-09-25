package com.energetica.enron.application

import com.energetica.enron.domain.SearchResult
import org.springframework.stereotype.Service

@Service
class DefaultSearchService(
	private val searchQueryParser: SearchQueryParser,
	private val searchRepository: SearchRepository,
) : SearchService {
	override fun query(searchTerm: String): List<SearchResult> {
		val query = searchQueryParser.parse(searchTerm)
		return searchRepository.search(query, MAX_RESULTS)
	}

	private companion object {
		const val MAX_RESULTS = 100
	}
}
