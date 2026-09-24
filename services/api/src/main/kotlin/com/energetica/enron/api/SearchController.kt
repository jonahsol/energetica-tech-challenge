package com.energetica.enron.api

import com.energetica.enron.application.SearchService
import com.energetica.enron.domain.SearchResult
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SearchController(
	private val searchService: SearchService,
) {
	@PostMapping("/enron-data/search")
	fun search(@RequestBody request: SearchRequest): List<SearchResult> {
		val searchTerm = request.searchTerm?.takeIf { it.isNotBlank() }
			?: throw IllegalArgumentException("searchTerm must not be blank")
		return searchService.query(searchTerm)
	}
}
