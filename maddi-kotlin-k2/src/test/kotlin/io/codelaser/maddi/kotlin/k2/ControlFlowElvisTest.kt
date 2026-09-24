/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.IfElseStatement
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import io.codelaser.maddi.cst.api.statement.ThrowStatement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * <b>`x ?: return v` — an elvis whose right-hand side leaves the method.</b> Kotlin writes it as an
 * expression; the CST has no expression that transfers control, so it was
 * `k2-unsupported-expr:KtReturnExpression` — 270 sites on detekt, and every one of eight sampled from the
 * corpus source was this idiom.
 *
 * It lowers to the pair kotlinc itself compiles: `if (s == null) return 0;` then `val t = s;`. No temporary
 * is needed in these positions, because the guard tests the value and the declaration names it.
 */
class ControlFlowElvisTest : KotlinScanTestBase() {

    private fun parse(body: String): TypeInfo =
        KotlinScan(runtime, sourceSet).parse("P.kt", "class P {\n    $body\n}\n").first()

    @Test
    fun aDeclarationBecomesAGuardAndADeclaration() {
        val p = parse("fun f(s: String?): Int { val t = s ?: return 0; return t.hashCode() }")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total, PlaceholderCensus.of(listOf(p)).byKind.toString())
        val statements = p.findUniqueMethod("f", 1).methodBody().statements()
        // ⚠ indexed `0.0`/`0.1` with nothing at `0`: the indexes only have to SORT, and renumbering the
        // siblings would move every statement after them
        assertEquals(listOf("0.0", "0.1", "1"), statements.map { it.source().index() })
        assertTrue(statements[0] is IfElseStatement, statements[0].javaClass.toString())
        assertTrue((statements[0] as IfElseStatement).block().statements().single() is ReturnStatement)
        assertTrue(statements[1] is LocalVariableCreation, statements[1].javaClass.toString())
    }

    @Test
    fun aThrowLowersTheSameWay() {
        val p = parse("fun f(s: String?): Int { val t = s ?: throw IllegalStateException(); return t.hashCode() }")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total)
        val statements = p.findUniqueMethod("f", 1).methodBody().statements()
        assertTrue((statements[0] as IfElseStatement).block().statements().single() is ThrowStatement)
    }

    /** As a statement of its own the elvis needs no second statement: the guard IS the lowering. */
    @Test
    fun anElvisStatementBecomesTheGuardAlone() {
        val p = parse("fun f(s: String?) { s ?: return; println(s) }")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total)
        val statements = p.findUniqueMethod("f", 1).methodBody().statements()
        assertEquals(listOf("0", "1"), statements.map { it.source().index() })
        assertTrue(statements[0] is IfElseStatement)
    }

    /**
     * ⛔ CONTROL. `return@mapNotNull` leaves a LAMBDA, not this method; lowering it to a plain `return` would
     * emit a return from the wrong method, silently. It must stay a placeholder — one of eight sites sampled
     * from detekt's source was exactly this.
     */
    @Test
    fun aLabelledReturnIsRefused() {
        val p = parse("fun f(xs: List<String?>) = xs.mapNotNull { it ?: return@mapNotNull null }")
        val census = PlaceholderCensus.of(listOf(p))
        assertEquals(setOf("k2-unsupported-expr:KtReturnExpression"), census.byKind.keys, census.byKind.toString())
    }

    /**
     * ⛔ CONTROL. `g(1, s ?: return 0)` cannot hoist the guard above the argument written before it: the
     * source evaluates `1` first. Refused, and left counted.
     */
    @Test
    fun anElvisInsideAnArgumentIsRefused() {
        val p = parse("""
                fun g(a: Int, b: String) = a
                fun f(s: String?): Int { return g(1, s ?: return 0) }
        """)
        val census = PlaceholderCensus.of(listOf(p))
        assertEquals(setOf("k2-unsupported-expr:KtReturnExpression"), census.byKind.keys, census.byKind.toString())
    }

    /** ⛔ CONTROL. An ordinary elvis is not control flow and keeps its InlineConditional, unsplit. */
    @Test
    fun anOrdinaryElvisIsUnchanged() {
        val p = parse("fun f(s: String?): String { val t = s ?: \"d\"; return t }")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total)
        assertEquals(listOf("0", "1"), p.findUniqueMethod("f", 1).methodBody().statements().map { it.source().index() })
    }

    /**
     * ⚠ <b>The lowering converts its left operand TWICE</b> (guard and value), so a left operand that is not
     * a stable reference is EVALUATED twice — `val t = f() ?: return` becomes `if (f() == null) return; val
     * t = f()`, two calls where the source has one. The CST is well formed and says something the source
     * does not, which no placeholder census can see. [KotlinScan.elvisReEvaluations] counts it so it is a
     * number rather than a worry.
     *
     * <p>⭐ This is the positive control for that counter. Measured with it: <b>0</b> on detekt and coil —
     * every lowered site in both corpora has a stable left operand — but "0 on the corpus" only means
     * something once the instrument is known to fire.
     */
    /**
     * ⭐ <b>A call on the left is evaluated ONCE, through a temporary.</b> The lowering needs its left
     * operand twice — to test for null and as the value — so converting it twice EVALUATED it twice:
     * `val t = g() ?: return 0` became `if (g() == null) return 0; val t = g()`, two calls where the source
     * has one. Well-formed CST saying something the source does not, and invisible to the placeholder
     * census. Measured before the fix: 190 such sites on detekt, 3 on coil.
     */
    @Test
    fun aCallOnTheLeftIsBoundToATemporaryAndEvaluatedOnce() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse("P.kt", """
            class P {
                fun g(): Int? = 1
                fun f(): Int { val t = g() ?: return 0; return t }
            }
            """.trimIndent() + "\n")
        val statements = types.first().findUniqueMethod("f", 0).methodBody().statements()
        // temporary, guard, declaration — then the original `return t`
        assertEquals(listOf("0.0", "0.1", "0.2", "1"), statements.map { it.source().index() })
        assertTrue(statements[0] is LocalVariableCreation, statements[0].javaClass.toString())
        assertEquals(1, scan.elvisTemporaries)
        assertEquals(0, scan.elvisReEvaluations, "the left operand must be evaluated exactly once")
    }

    /** …and a plain name needs no temporary: re-reading a stable reference evaluates nothing. */
    @Test
    fun aNameOnTheLeftNeedsNoTemporary() {
        val scan = KotlinScan(runtime, sourceSet)
        scan.parse("P.kt", """
            class P {
                fun f(s: String?): String { val t = s ?: return ""; return t }
            }
            """.trimIndent() + "\n")
        assertEquals(0, scan.elvisReEvaluations)
        assertEquals(0, scan.elvisTemporaries, "a plain name needs no temporary")
    }

    /** A dotted chain of names is stable too — and it is the common shape, which is why most sites need none. */
    @Test
    fun aDottedChainNeedsNoTemporary() {
        val scan = KotlinScan(runtime, sourceSet)
        scan.parse("P.kt", """
            class Q(val name: String?)
            class P(val q: Q) {
                fun f(): String { val t = q.name ?: return ""; return t }
            }
            """.trimIndent() + "\n")
        assertEquals(0, scan.elvisReEvaluations)
        assertEquals(0, scan.elvisTemporaries)
    }

    /** `?: continue` / `?: break` in a loop, and an ASSIGNMENT whose value is a control-flow elvis. */
    @Test
    fun continueBreakAndAssignment() {
        val p = parse("""fun f(xs: List<String?>): Int {
            var n = 0
            var last: String = ""
            for (x in xs) { val y = x ?: continue; n += y.hashCode() }
            for (x in xs) { last = x ?: break }
            return n + last.hashCode()
        }""")
        val census = PlaceholderCensus.of(listOf(p))
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        val printed = p.findUniqueMethod("f", 1).methodBody().toString()
        assertTrue(printed.contains("continue;"), printed)
        assertTrue(printed.contains("break;"), printed)
    }

    /** `?: return@label v` in a lambda returns from the lambda, as the lambda's own `return@label` does. */
    @Test
    fun aLabelledReturnInALambda() {
        val p = parse("""fun f(xs: List<String?>, g: ((String?) -> Int) -> Int): Int = g { x ->
            val y = x ?: return@g 0
            y.hashCode()
        }""")
        val census = PlaceholderCensus.of(listOf(p))
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    /** A `throw` as a lambda's last expression is a statement, not a value to return; `@Suppress` is dropped. */
    @Test
    fun throwAsTheLambdaResultAndAnAnnotatedExpression() {
        val p = parse("""fun f(e: Exception?, g: (Exception) -> Unit): Int {
            e?.let { throw it }
            @Suppress("UNCHECKED_CAST")
            val n = (e as Any?) as Int?
            @Suppress("MagicNumber")
            return n ?: 3
        }""")
        val census = PlaceholderCensus.of(listOf(p))
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }
}
