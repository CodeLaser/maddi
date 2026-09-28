package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * #81: `d.copy(a = a)` passed `null` for the omitted `b`. kotlinc binds the call to `copy$default`, whose body
 * substitutes `this.b`; the data class now gets that synthetic, and the call binds to it.
 */
class DataClassCopyDefaultTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("dc/Dc.kt", """
            package dc
            data class D(val a: StringBuilder, val b: List<StringBuilder>)
            class X {
                fun copyD(d: D, a: StringBuilder): D = d.copy(a = a)
                fun copyBoth(d: D, a: StringBuilder, b: List<StringBuilder>): D = d.copy(a, b)
            }
            """.trimIndent() + "\n")
    }

    private fun type(name: String): TypeInfo = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }
    private fun body(owner: String, name: String): String =
        type(owner).methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun omittedArgumentBindsToCopyDefault() {
        assertEquals(0, PlaceholderCensus.of(types).total)
        assertEquals("return d.copy\$default(a,null,2);", body("X", "copyD"))
        // every argument written: the call binds to `copy` itself
        assertEquals("return d.copy(a,b);", body("X", "copyBoth"))
    }

    @Test
    fun copyDefaultSubstitutesTheProperty() {
        val copyDefault = type("D").methods().first { it.name() == "copy\$default" }
        assertTrue(copyDefault.isSynthetic)
        assertEquals(listOf("a", "b", "\$mask"), copyDefault.parameters().map { it.name() })
        val statements = copyDefault.methodBody().statements()
        assertEquals(listOf("0", "1", "2"), statements.map { it.source().index() })
        assertEquals("if((\$mask&1)!=0){a=this.a;}", statements[0].toString())
        assertEquals("if((\$mask&2)!=0){b=this.b;}", statements[1].toString())
        assertEquals("return copy(a,b);", statements[2].toString())
    }
}
