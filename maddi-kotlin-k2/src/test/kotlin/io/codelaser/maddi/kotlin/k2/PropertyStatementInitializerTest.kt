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
 * A property initializer only a statement can hold: an `if` whose branch declares (detekt AbsentOrWrongFileLicense's
 * `val matchesLicense = if (regex) { val r = …; fun matcher(…) = …; ::matcher } else { … }`), or a `try`. The field
 * keeps no initializer. An instance property is assigned in each branch in the constructor, in source order with the
 * `init` blocks and reading the constructor's parameters, where kotlinc puts it; a top-level one in the facade's static
 * initializer.
 */
class PropertyStatementInitializerTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ps/Ps.kt", """
            package ps
            class K(private val regex: Boolean, private val template: String) {
                private val matches: (String) -> Boolean = if (regex) {
                    val prefix = template + "!"
                    fun matcher(text: String): Boolean = text == prefix
                    ::matcher
                } else {
                    { it == template }
                }
                private val parsed: Int = try { template.toInt() } catch (_: NumberFormatException) { 0 }
                private val plain: Int = 3
                init { check(plain > 0) }
                private val late: Int = if (regex) { val x = plain; x * 2 } else plain
                fun check(s: String): Boolean = matches(s)
            }
            val top: Int = if (System.nanoTime() > 0L) { val a = 1; a + 1 } else 0
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun assignedInTheInitializerBlock() {
        fun shape(name: String) = type(name).let { t ->
            t.fields().joinToString(" ") { "${it.name()}=${it.initializer()}" } + " | " +
                (t.constructors() + t.methods().filter { it.isInstanceInitializer || it.isStaticInitializer })
                    .joinToString(" ") { "${it.name()}: ${it.methodBody().statements().joinToString(" ")}" }
        }
        assertEquals("""
            regex=<empty> template=<empty> matches=<empty> parsed=<empty> plain=3 late=<empty> | <init>: this.regex=regex; this.template=template; if(regex){String prefix=template+"!";Function1<String,Boolean> matcher=text->text.equals(prefix);this.matches=matcher;}else{this.matches=(it->it.equals(template));} try{this.parsed=StringsKt__StringNumberConversionsJVMKt.toInt(template);}catch(NumberFormatException _){this.parsed=0;} {PreconditionsKt__PreconditionsKt.check(this.plain>0);} if(regex){int x=this.plain;this.late=x*2;}else{this.late=this.plain;}
            top=<empty> | <static_0>: if(System.nanoTime()>0L){int a=1;PsKt.top=a+1;}else{PsKt.top=0;}
            """.trimIndent(), shape("K") + "\n" + shape("PsKt"))
    }
}
