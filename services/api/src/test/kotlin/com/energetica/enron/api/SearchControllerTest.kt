package com.energetica.enron.api

import com.energetica.enron.application.SearchService
import com.energetica.enron.domain.SearchResult
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant

@WebMvcTest(SearchController::class)
@Import(ApiExceptionHandler::class)
class SearchControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockitoBean
	private lateinit var searchService: SearchService

	@Test
	fun `returns matching emails as json`() {
		`when`(searchService.query("gas contract")).thenReturn(
			listOf(
				SearchResult(
					id = "allen-p/_sent_mail/1.",
					sender = "gerald.nemec@enron.com",
					xTo = "Tim Belden",
					xCc = null,
					xBcc = null,
					date = Instant.parse("2001-05-16T13:30:00Z"),
					subject = "IT Contract",
					body = "Please review the gas contract.",
					score = 5.2679,
				),
			),
		)

		mockMvc.post("/enron-data/search") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"searchTerm":"gas contract"}"""
		}.andExpect {
			status { isOk() }
			content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].id") { value("allen-p/_sent_mail/1.") }
			jsonPath("$[0].sender") { value("gerald.nemec@enron.com") }
			jsonPath("$[0].xTo") { value("Tim Belden") }
			jsonPath("$[0].xCc") { value(null) }
			jsonPath("$[0].xBcc") { value(null) }
			jsonPath("$[0].date") { value("2001-05-16T13:30:00Z") }
			jsonPath("$[0].subject") { value("IT Contract") }
			jsonPath("$[0].body") { value("Please review the gas contract.") }
			jsonPath("$[0].score") { value(5.2679) }
		}
	}

	@Test
	fun `rejects a blank search term`() {
		mockMvc.post("/enron-data/search") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"searchTerm":"   "}"""
		}.andExpect {
			status { isBadRequest() }
			jsonPath("$.message") { value("searchTerm must not be blank") }
		}

		verifyNoInteractions(searchService)
	}

	@Test
	fun `rejects a missing search term`() {
		mockMvc.post("/enron-data/search") {
			contentType = MediaType.APPLICATION_JSON
			content = """{}"""
		}.andExpect {
			status { isBadRequest() }
		}

		verifyNoInteractions(searchService)
	}

	@Test
	fun `hides unexpected failures from the client`() {
		`when`(searchService.query("gas contract")).thenThrow(
			RuntimeException("SELECT password FROM mysql.user"),
		)

		mockMvc.post("/enron-data/search") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"searchTerm":"gas contract"}"""
		}.andExpect {
			status { isInternalServerError() }
			jsonPath("$.message") { value("An unexpected error occurred") }
			content { string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SELECT"))) }
		}
	}
}
