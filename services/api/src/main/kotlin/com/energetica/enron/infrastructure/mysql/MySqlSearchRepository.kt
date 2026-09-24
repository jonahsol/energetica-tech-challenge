package com.energetica.enron.infrastructure.mysql

import com.energetica.enron.application.SearchRepository
import com.energetica.enron.domain.SearchResult
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class MySqlSearchRepository(
	private val jdbcTemplate: JdbcTemplate,
) : SearchRepository {
	override fun search(searchTerm: String): List<SearchResult> {
		return jdbcTemplate.query(
			SEARCH_SQL,
			{ rs, _ -> mapRow(rs) },
			searchTerm,
			searchTerm,
			MAX_RESULTS,
		)
	}

	private fun mapRow(rs: ResultSet): SearchResult {
		return SearchResult(
			id = rs.getInt("mid"),
			sender = rs.getString("sender"),
			date = rs.getObject("date", LocalDateTime::class.java).toInstant(ZoneOffset.UTC),
			subject = rs.getString("subject"),
			body = rs.getString("body"),
			score = rs.getDouble("score"),
		)
	}

	private companion object {
		const val MAX_RESULTS = 100

		val SEARCH_SQL = """
			SELECT
			    mid,
			    sender,
			    date,
			    subject,
			    body,
			    MATCH(subject, body) AGAINST (? IN NATURAL LANGUAGE MODE) AS score
			FROM message
			WHERE MATCH(subject, body) AGAINST (? IN NATURAL LANGUAGE MODE)
			ORDER BY score DESC
			LIMIT ?
		""".trimIndent()
	}
}
