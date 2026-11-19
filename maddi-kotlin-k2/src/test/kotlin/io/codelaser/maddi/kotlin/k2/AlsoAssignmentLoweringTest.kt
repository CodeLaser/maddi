package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * `e.also { v = it }` is J2K's translation of Java's assignment expression `(v = e)`, and kotlinc inlines it. As a call
 * with a lambda, `v` was a `Ref` holder, and what `e`'s call did to v's earlier value did not reach where that value
 * came from (fernflower's `getMaxBreakLift`, maddi-mod#145). It is now that assignment, `v` a plain local.
 */
class AlsoAssignmentLoweringTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("al/Al.kt", """
            package al
            class S { var parent: S? = null; fun up(): S? = parent }
            object B {
                fun next(s: S?): S? = s!!.up()
                fun loop(stat: S?): S? { var closure = stat; var n = stat; while ((next(n).also { n = it }) != null) { closure = n }; return closure }
                fun named(stat: S?): S? { var n = stat; while ((next(n).also { x -> n = x }) != null) { }; return n }
                fun more(stat: S?): S? { var n = stat; next(n).also { n = it; println(it) }; return n }
            }
            """.trimIndent())
    }

    private fun body(name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "B" }.methods().first { it.name() == name }.methodBody().statements()
        .joinToString(" ")

    @Test
    fun anAlsoThatOnlyAssignsIsTheAssignment() {
        assertEquals("S closure=stat; S n=stat; while(!((n=next(n))==null)){closure=n;} return closure;", body("loop"))
        assertEquals("S n=stat; while(!((n=next(n))==null)){} return n;", body("named"))
    }

    @Test
    fun anAlsoThatDoesMoreStaysACall() {
        assert(body("more").startsWith("ObjectRef<S> n=new ObjectRef();")) { body("more") }
    }
}
