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
 * javalin's remaining shapes: a Java varargs CONSTRUCTOR (jetty's `ServerConnector(server, f1, f2)`,
 * `ALPNServerConnectionFactory()`), a Java setter through a receiver lambda (`SslContextFactory.Server().apply {
 * keyStorePath = … }`), `::p.isInitialized` on a `lateinit` property, an imported `@JvmField` of a library object
 * (`import kotlin.text.Charsets.UTF_8`). (`Array(n) { … }` in EXPRESSION position stays open: §7.72.)
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
            date: return StandardKt__StandardKt.apply(new Date(),${'$'}receiver->${'$'}receiver.setTime(5L));
            calendar: return StandardKt__StandardKt.apply(new GregorianCalendar(),${'$'}receiver->${'$'}receiver.setTimeInMillis(5L));
            ready: return this.driver!=null;
            readyThis: return this.driver!=null;
            hiddenReady: return this.hidden!=null;
            text: return ArraysKt__ArraysJVMKt.toString(b,Charsets.UTF_8);
            """.trimIndent(), listOf("none", "two", "date", "calendar", "ready", "readyThis", "hiddenReady", "text").joinToString("\n") { "$it: ${body(it)}" })
    }
}
