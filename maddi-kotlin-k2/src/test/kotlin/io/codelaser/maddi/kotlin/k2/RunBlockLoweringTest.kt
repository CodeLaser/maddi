package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * `run sw@ { … return@sw … }` as a statement is J2K's translation of a `switch` whose cases `break`, and kotlinc inlines
 * it. As a call with a lambda, what the lambda did to a captured local did not reach where that local came from
 * (fernflower's ExprProcessor.processBlock, maddi-mod#145). It is now the labelled block `sw: { … break sw; … }`.
 */
class RunBlockLoweringTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("rb/Rb.kt", """
            package rb
            class Stk { var n: Int = 0; fun push(s: String) { n++ } }
            class C(private val st: Stk) {
                private var count = 0
                fun bump() { count++ }
                fun member(k: Int) { run sw@ { if (k == 0) return@sw; bump(); st.push("a"); this.count++ } }
            }
            object B {
                fun sw(s: Stk, k: Int) { run sw@ { when (k) { 0 -> { s.push("a"); return@sw } else -> s.push("b") }; s.push("c") } }
                fun plain(s: Stk, k: Int) { run { if (k == 0) return; s.push("a") } }
                fun counter(k: Int): Int { var c = 0; run sw@ { if (k == 0) return@sw; c++ }; return c }
                fun valued(s: Stk, k: Int): Int = run sw@ { if (k == 0) return@sw 1; 2 }
            }
            """.trimIndent())
    }

    private fun body(name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "B" }.methods().first { it.name() == name }.methodBody().statements()
        .joinToString(" ")

    @Test
    fun aRunStatementIsALabelledBlock() {
        val sw = body("sw")
        assert(sw.startsWith("sw: {") && "break sw;" in sw && "StandardKt.run" !in sw) { sw }
        val plain = body("plain")
        assert("StandardKt.run" !in plain && "return;" in plain) { plain }
    }

    @Test
    fun membersInTheBlockAreTheEnclosingThis() {
        val member = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == "C" }
            .methods().first { it.name() == "member" }.methodBody().statements().joinToString(" ")
        assertEquals("sw: {if(k==0){break sw;}bump();this.st.push(\"a\");this.count++;}", member)
    }

    @Test
    fun aVarAssignedInTheBlockIsNoRef() {
        val counter = body("counter")
        assert(counter.startsWith("int c=0;") && "break sw;" in counter) { counter }
    }

    @Test
    fun aValuedRunIsNotABlock() {
        val valued = body("valued")
        assert("break" !in valued) { valued }
    }
}
