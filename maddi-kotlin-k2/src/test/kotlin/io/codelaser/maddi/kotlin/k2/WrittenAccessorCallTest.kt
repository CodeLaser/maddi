package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * #82: a property with a WRITTEN accessor is read and written through it from every site but the accessor itself,
 * as kotlinc compiles it. A default accessor keeps the field access, which means the same.
 */
class WrittenAccessorCallTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("wa/Wa.kt", """
            package wa
            class Logged {
                var count = 0
                var sb: StringBuilder = StringBuilder()
                    set(v) { count++; field = v }
                val size: Int
                    get() = sb.length
                var plain: StringBuilder = StringBuilder()
                fun resetFromInside(s: StringBuilder) { sb = s; plain = s }
            }
            class X {
                fun customSet(l: Logged, s: StringBuilder) { l.sb = s }
                fun plainSet(l: Logged, s: StringBuilder) { l.plain = s }
                fun read(l: Logged): Int = l.count + l.size
            }
            """.trimIndent() + "\n")
    }

    private fun body(owner: String, name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == owner }.methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun writtenSetterIsCalled() {
        assertEquals(0, PlaceholderCensus.of(types).total)
        assertEquals("l.setSb(s);", body("X", "customSet"))
        // inside the class too, kotlinc calls the setter; the default one stays a field write
        assertEquals("setSb(s); this.plain=s;", body("Logged", "resetFromInside"))
        // the written setter's own body writes the backing field
        assertEquals("this.count++; this.sb=v;", body("Logged", "setSb"))
    }

    @Test
    fun defaultSetterStaysAFieldWrite() {
        assertEquals("l.plain=s;", body("X", "plainSet"))
    }

    @Test
    fun writtenGetterIsCalled() {
        assertEquals("return l.count+l.getSize();", body("X", "read"))
    }
}
