package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Two constructor shapes the analyzer tier found (2026-09-28):
 * - #85: a property initializer that reads a primary-constructor parameter runs IN the constructor, as kotlinc
 *   compiles it; as a field initializer it could not see the parameter, and the constructor then did not store it.
 * - #86: a `vararg val xs: String` constructor property's parameter is a `String[]` varargs parameter, as its
 *   field is.
 */
class ConstructorPropertyShapesTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("cp/Cp.kt", """
            package cp
            class Keep(xs: MutableList<StringBuilder>) {
                private val items = xs
                private val count = 0
                fun items(): MutableList<StringBuilder> = items
            }
            class Varg(vararg val xs: String) { fun first(): String = xs[0] }
            """.trimIndent() + "\n")
    }

    private fun type(name: String): TypeInfo = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    @Test
    fun initializerReadingAParameterRunsInTheConstructor() {
        assertEquals(0, PlaceholderCensus.of(types).total)
        val keep = type("Keep")
        val constructor = keep.findConstructor(1)
        assertEquals("this.items=xs;", constructor.methodBody().statements().joinToString(" "))
        assertTrue(keep.getFieldByName("items", true).initializer().isEmpty(), "the field keeps no initializer of its own")
        // an initializer that reads no parameter stays a field initializer
        assertEquals("0", keep.getFieldByName("count", true).initializer().toString())
    }

    @Test
    fun varargPropertyParameterIsAnArray() {
        val constructor = type("Varg").findConstructor(1)
        val parameter = constructor.parameters().single()
        assertEquals("Type String[]", parameter.parameterizedType().toString())
        assertTrue(parameter.isVarArgs)
        assertEquals("Type String[]", type("Varg").getFieldByName("xs", true).type().toString())
        assertEquals("this.xs=xs;", constructor.methodBody().statements().joinToString(" "))
    }
}
