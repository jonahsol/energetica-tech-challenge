package com.energetica.enron.api

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
	private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

	@ExceptionHandler(IllegalArgumentException::class)
	fun badRequest(exception: IllegalArgumentException): ResponseEntity<ApiError> {
		return ResponseEntity.badRequest().body(ApiError(exception.message ?: "Invalid request"))
	}

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun malformedRequest(exception: HttpMessageNotReadableException): ResponseEntity<ApiError> {
		log.debug("Malformed search request", exception)
		return ResponseEntity.badRequest().body(ApiError("Request body must be JSON with a searchTerm"))
	}

	@ExceptionHandler(Exception::class)
	fun unexpected(exception: Exception): ResponseEntity<ApiError> {
		log.error("Search request failed", exception)
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiError("An unexpected error occurred"))
	}
}

data class ApiError(
	val message: String,
)
