package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A bare call inside a companion is a call on the companion's own `this`, as kotlinc compiles it and as a named
 * `object`'s is. It was `Outer.Companion.f()`, a call on a static field of the outer type: a modifying `f` then
 * modified the outer type's own static state (fernflower's IfHelper, maddi-mod#145). From the outer class, and
 * written qualified, the call still goes through the field.
 */
class CompanionSelfCallTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("cs/Cs.kt", """
            package cs
            class H {
                fun outer(s: String): Int = mid(s)
                companion object {
                    @JvmStatic fun top(s: String): Int = mid(s)
                    fun mid(s: String): Int = s.length
                    fun qualified(s: String): Int = H.mid(s)
                }
            }
            """.trimIndent())
    }

    private fun body(type: String, method: String): String =
        types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == type }
            .methods().first { it.name() == method }.methodBody().statements().joinToString(" ")

    @Test
    fun aBareCallInsideTheCompanionIsOnItsThis() {
        assertEquals("return mid(s);", body("Companion", "top"))
        assertEquals("return H.Companion.mid(s);", body("Companion", "qualified"))
        assertEquals("return H.Companion.mid(s);", body("H", "outer"))
        assertEquals("return H.Companion.top(s);", body("H", "top"))
    }
}
