package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A bare name in a subclass reading an inherited `@JvmField` is the superclass's field, even beside a method of the
 * getter's name: fernflower's Statement has `@JvmField var first` and `fun getFirst() { checkCanceled(); return first }`,
 * and `lst.add(first)` in DoStatement read as `this.getFirst()`, which made every getSequentialObjects() modifying
 * (maddi-mod#145).
 */
class InheritedJvmFieldTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("jf/Jf.kt", """
            package jf
            open class St {
                @JvmField var first: St? = null
                @JvmField var plain: St? = null
                @JvmField val stats: MutableList<St> = ArrayList()
                open fun getFirst(): St? { check(); return first }
                open fun getStats(): MutableList<St> { check(); return stats }
                fun check() { }
                fun own(): St? = first
            }
            class Do : St() {
                fun sub(): St? = first
                fun subPlain(): St? = plain
                fun subStats(): Int = stats.size
                fun other(o: St): St? = o.first
            }
            """.trimIndent())
    }

    @Test
    fun anInheritedJvmFieldIsReadAsTheField() {
        val sb = StringBuilder()
        types.flatMap { it.recursiveSubTypeStream().toList() }.forEach { t ->
            t.methods().filter { it.name() in setOf("own", "sub", "subPlain", "subStats", "other") }
                .forEach { sb.append(t.simpleName()).append('.').append(it.name()).append(": ").append(it.methodBody().statements().joinToString(" ")).append('\n') }
        }
        assertEquals("""
            St.own: return this.first;
            Do.sub: return this.first;
            Do.subPlain: return this.plain;
            Do.subStats: return this.stats.size;
            Do.other: return o.first;
            
        """.trimIndent(), sb.toString())
    }
}
