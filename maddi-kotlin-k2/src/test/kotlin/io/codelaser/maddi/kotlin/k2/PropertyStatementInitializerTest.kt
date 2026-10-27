/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            regex=<empty> template=<empty> matches=<empty> parsed=<empty> plain=3 late=<empty> | <init>: this.regex=regex; this.template=template; if(regex){String prefix=template+"!";Function1<String,Boolean> matcher=text->text.equals(prefix);this.matches=matcher;}else{this.matches=(it->it.equals(template));} try{this.parsed=StringsKt.toInt(template);}catch(NumberFormatException _){this.parsed=0;} {if(!(this.plain>0)){throw new IllegalStateException("Check failed.");}} if(regex){int x=this.plain;this.late=x*2;}else{this.late=this.plain;}
            top=<empty> | <static_0>: if(System.nanoTime()>0L){int a=1;PsKt.top=a+1;}else{PsKt.top=0;}
            """.trimIndent(), shape("K") + "\n" + shape("PsKt"))
    }
}
