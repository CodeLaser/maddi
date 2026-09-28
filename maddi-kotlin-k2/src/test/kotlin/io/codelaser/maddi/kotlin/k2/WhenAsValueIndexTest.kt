package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.expression.SwitchExpression
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * #75: the arms of a `when` used as a value are indexed under the statement that holds the switch expression, as
 * the Java parser indexes a switch expression's entries: `0.0.0`/`0.1.0` in `return when …` at statement 0, and
 * `1.0` (a block arm) / `1.1.0` in statement 1. Indexed from 0 they collided with the method's own statements.
 */
class WhenAsValueIndexTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("wv/Wv.kt", """
            package wv
            class X {
                fun a(i: Int): Int { return when (i) { 0 -> 1; else -> throw IllegalStateException() } }
                fun c(i: Int): Int { var x = 0; val r = when (i) { 0 -> { x = 5; 1 } else -> 2 }; return r + x }
            }
            """.trimIndent() + "\n")
    }

    private fun method(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "X" }.methods().first { it.name() == name }

    private fun armIndices(switch: SwitchExpression): List<String> = switch.entries().map { it.statement().source().index() }

    @Test
    fun armsOfAReturnedWhen() {
        assertEquals(0, PlaceholderCensus.of(types).total)
        val ret = method("a").methodBody().statements()[0] as ReturnStatement
        assertEquals(listOf("0.0.0", "0.1.0"), armIndices(ret.expression() as SwitchExpression))
    }

    @Test
    fun armsOfAnAssignedWhen() {
        val lvc = method("c").methodBody().statements()[1] as LocalVariableCreation
        val switch = lvc.localVariable().assignmentExpression() as SwitchExpression
        assertEquals(listOf("1.0", "1.1.0"), armIndices(switch))
        // the block arm's own statements sit under it
        assertEquals(listOf("1.0.0", "1.0.1"), switch.entries()[0].statement().let { (it as io.codelaser.maddi.cst.api.statement.Block).statements() }.map { it.source().index() })
    }
}
