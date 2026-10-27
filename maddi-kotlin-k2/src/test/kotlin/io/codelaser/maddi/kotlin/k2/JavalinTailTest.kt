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
 * javalin's remaining shapes: a Java varargs CONSTRUCTOR (jetty's `ServerConnector(server, f1, f2)`,
 * `ALPNServerConnectionFactory()`), a Java setter through a receiver lambda (`SslContextFactory.Server().apply {
 * keyStorePath = … }`), `::p.isInitialized` on a `lateinit` property, an imported `@JvmField` of a library object
 * (`import kotlin.text.Charsets.UTF_8`), a receiver-typed function value called on a
 * written receiver (`url?.openConnection()?.getter()`). (`Array(n) { … }` in EXPRESSION position: ArrayInitTest.)
 */
class JavalinTailTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("jt/Jt.kt", """
            package jt
            import kotlin.text.Charsets.UTF_8
            class K {
                lateinit var driver: String
                private lateinit var hidden: String
                fun hiddenReady(): Boolean = ::hidden.isInitialized
                fun calendar(): java.util.GregorianCalendar = java.util.GregorianCalendar().apply { timeInMillis = 5L }
                fun none(): ProcessBuilder = ProcessBuilder()
                fun two(): ProcessBuilder = ProcessBuilder("a", "b")
                fun date(): java.util.Date = java.util.Date().apply { time = 5L }
                fun ready(): Boolean = ::driver.isInitialized
                fun readyThis(): Boolean = this::driver.isInitialized
                fun text(b: ByteArray): String = b.toString(UTF_8)
                fun <T> conn(s: StringBuilder?, getter: StringBuilder.() -> T): T? = s?.getter()
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "K" }.methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theShapes() {
        assertEquals("""
            none: return new ProcessBuilder();
            two: return new ProcessBuilder("a","b");
            date: Date ${'$'}this${'$'}apply=new Date(); {${'$'}this${'$'}apply.setTime(5L);} return ${'$'}this${'$'}apply;
            calendar: GregorianCalendar ${'$'}this${'$'}apply=new GregorianCalendar(); {${'$'}this${'$'}apply.setTimeInMillis(5L);} return ${'$'}this${'$'}apply;
            ready: return this.driver!=null;
            readyThis: return this.driver!=null;
            hiddenReady: return this.hidden!=null;
            text: return new String(b,Charsets.UTF_8);
            conn: return s==null?null:getter.invoke(s);
            """.trimIndent(), listOf("none", "two", "date", "calendar", "ready", "readyThis", "hiddenReady", "text", "conn").joinToString("\n") { "$it: ${body(it)}" })
    }
}
