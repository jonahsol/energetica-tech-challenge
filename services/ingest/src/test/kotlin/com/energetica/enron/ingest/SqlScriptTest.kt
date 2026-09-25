package com.energetica.enron.ingest

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlScriptTest {
	@Test
	fun `keeps semicolons inside dollar-quoted function bodies together`() {
		val sql = """
			CREATE TABLE example (id INT);
			CREATE FUNCTION demo() RETURNS void AS ${'$'}$
			BEGIN
			    PERFORM 1;
			END;
			${'$'}$ LANGUAGE plpgsql;
		""".trimIndent()

		val statements = SqlScript.split(sql)

		assertEquals(2, statements.size)
		assertTrue(statements[1].contains("PERFORM 1;"))
	}
}
