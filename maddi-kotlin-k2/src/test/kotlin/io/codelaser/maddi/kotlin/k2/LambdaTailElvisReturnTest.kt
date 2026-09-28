package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * detekt's `UseDataClass.visitKlass` (placeholder census 2026-09-28, 8 -> 9): a lambda whose whole body is
 * `x ?: return`, the return leaving the enclosing FUNCTION. The elvis is the lambda's value, so it is neither a
 * statement nor a function's expression body, and the control-flow elvis lowering did not see it.
 */
class LambdaTailElvisReturnTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("lt/Lt.kt", """
            package lt
            class K {
                fun lengths(xs: List<String?>): List<String> {
                    val r = xs.map { it ?: return emptyList() }
                    return r
                }
                fun first(xs: List<String?>): String = xs.firstOrNull().let { it ?: return "" }
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "K" }.methods().first { it.name() == name }
        .methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n") + "\n" + body("lengths") + "\n" + body("first"))
    }

    @Test
    fun theShapes() {
        println("lengths: " + body("lengths"))
        println("first: " + body("first"))
    }
}
