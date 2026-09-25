package com.energetica.enron.infrastructure.postgres

import com.energetica.enron.application.SearchRepository
import com.energetica.enron.domain.SearchExpression
import com.energetica.enron.domain.SearchQuery
import com.energetica.enron.domain.SearchResult
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.OffsetDateTime

@Repository
class PostgresSearchRepository(
	private val jdbcTemplate: JdbcTemplate,
) : SearchRepository {
	override fun search(query: SearchQuery, limit: Int): List<SearchResult> {
        
        // First resolve the term against the search_terms table
		val resolved = resolve(query.expression) ?: return emptyList()
        // Then compile the resolved expression to a tsquery
		val compiled = TsQueryCompiler.compile(resolved)

		val sql = """
			SELECT id, sender, x_to, x_cc, x_bcc, date, subject, body, score
			FROM (
			    SELECT DISTINCT ON (COALESCE(emails.message_id, emails.id))
			        emails.id,
			        emails.sender,
			        emails.x_to,
			        emails.x_cc,
			        emails.x_bcc,
			        emails.date,
			        emails.subject,
			        emails.body,
			        ts_rank_cd(emails.search_vector, parsed.query) AS score
			    FROM emails
			    CROSS JOIN LATERAL (
			        SELECT (${compiled.sql}) AS query
			    ) AS parsed
			    WHERE parsed.query <> ''::tsquery
			      AND emails.search_vector @@ parsed.query
			    ORDER BY COALESCE(emails.message_id, emails.id), score DESC, emails.id
			) AS matches
			ORDER BY score DESC
			LIMIT ?
		""".trimIndent()
		val arguments = arrayOf<Any>(*compiled.parameters.toTypedArray(), limit)
		return jdbcTemplate.query(sql, { rs, _ -> mapRow(rs) }, *arguments)
	}

	private fun resolve(expression: SearchExpression): SearchExpression? {
		return when (expression) {
			is SearchExpression.Term -> resolveTerm(expression.text)?.let { SearchExpression.Term(it) }
			is SearchExpression.And -> combine(expression.children) { SearchExpression.And(it) }
			is SearchExpression.Or -> combine(expression.children) { SearchExpression.Or(it) }
		}
	}

	private fun combine(
		children: List<SearchExpression>,
		wrap: (List<SearchExpression>) -> SearchExpression,
	): SearchExpression? {
		val resolved = children.mapNotNull { resolve(it) }
		return when (resolved.size) {
			0 -> null
			1 -> resolved[0]
			else -> wrap(resolved)
		}
	}

	private fun resolveTerm(term: String): String? {
		return jdbcTemplate.query(RESOLVE_SQL, { rs, _ -> rs.getString("term") }, term, term, term, term, term)
			.firstOrNull()
	}

	private fun mapRow(rs: ResultSet): SearchResult {
		val date = rs.getObject("date", OffsetDateTime::class.java)?.toInstant()
		return SearchResult(
			id = rs.getString("id"),
			sender = rs.getString("sender").orEmpty(),
			xTo = rs.getString("x_to"),
			xCc = rs.getString("x_cc"),
			xBcc = rs.getString("x_bcc"),
			date = date,
			subject = rs.getString("subject").orEmpty(),
			body = rs.getString("body").orEmpty(),
			score = rs.getDouble("score"),
		)
	}

	private companion object {
		val RESOLVE_SQL = """
			SELECT term
			FROM search_terms
			WHERE term = ?
			   OR (char_length(?) >= 4 AND term % ?)
			ORDER BY (term = ?) DESC, similarity(term, ?) DESC
			LIMIT 1
		""".trimIndent()
	}
}
