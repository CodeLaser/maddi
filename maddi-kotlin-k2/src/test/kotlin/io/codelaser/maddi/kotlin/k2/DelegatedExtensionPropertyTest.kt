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

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A top-level EXTENSION property with a delegate: detekt's `var KtFile.modifiedText: String? by
 * UserDataProperty(Key("modifiedText"))`. On the JVM: a static `modifiedText$delegate` on the file facade, and the
 * accessors `getModifiedText(KtFile)` / `setModifiedText(KtFile, String)` that call its `getValue`/`setValue` with the
 * receiver as `thisRef`. Read, written, and read from inside another extension on the same receiver.
 */
class DelegatedExtensionPropertyTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("dx/Dx.kt", """
            package dx
            class Holder
            class Slot<T>(var v: T?) {
                operator fun getValue(r: Holder, p: Any?): T? = v
                operator fun setValue(r: Holder, p: Any?, x: T?) { v = x }
            }
            var Holder.note: String? by Slot(null)
            fun Holder.echo(): String? = note
            class K {
                fun read(h: Holder): String? = h.note
                fun write(h: Holder) { h.note = "x" }
            }
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    private fun body(type: String, name: String): String =
        type(type).methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theShapes() {
        assertEquals("""
            read: return DxKt.getNote(h);
            write: DxKt.setNote(h,"x");
            echo: return DxKt.getNote(${'$'}receiver);
            getNote: return DxKt.note${'$'}delegate.getValue(${'$'}receiver,null);
            setNote: DxKt.note${'$'}delegate.setValue(${'$'}receiver,null,value);
            """.trimIndent(), listOf("read", "write").joinToString("\n") { "$it: ${body("K", it)}" } +
            "\necho: " + body("DxKt", "echo") + "\ngetNote: " + body("DxKt", "getNote") + "\nsetNote: " + body("DxKt", "setNote"))
    }
}
