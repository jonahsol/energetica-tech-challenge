package com.energetica.enron.infrastructure.postgres

import com.energetica.enron.domain.SearchExpression
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TsQueryCompilerTest {
	@Test
	fun `binds terms as parameters instead of concatenating them into sql`() {
		val hostile = "gas'; DROP TABLE emails;--"
		val compiled = TsQueryCompiler.compile(
			SearchExpression.And(
				listOf(SearchExpression.Term("contract"), SearchExpression.Term(hostile)),
			),
		)

		assertEquals(
			"(plainto_tsquery('simple', ?) && plainto_tsquery('simple', ?))",
			compiled.sql,
		)
		assertEquals(listOf("contract", hostile), compiled.parameters)
		assertFalse(compiled.sql.contains(hostile))
		assertFalse(compiled.sql.contains("DROP"))
	}
}
