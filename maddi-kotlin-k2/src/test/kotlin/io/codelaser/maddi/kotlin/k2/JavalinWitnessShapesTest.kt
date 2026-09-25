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
 * javalin's remaining witness-file shapes: a LOCAL vararg function called with lambdas (TestSse's
 * `runConcurrently({ … }, { … })`), a bound reference through the lambda receiver's `this` (JettyServer's
 * `forEach(this::addConnector)` inside `Server().apply { }`), and an assignment through a safe call
 * (`(unwrap() as? Wrapper)?.handler = h`).
 */
class JavalinWitnessShapesTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("jw/Jw.kt", """
            package jw
            class Server { fun addConnector(c: String) {} }
            open class Handler
            class Wrapper : Handler() { var handler: Handler? = null }
            class K {
                fun run(a: Runnable, b: Runnable) {
                    fun runConcurrently(vararg tasks: Runnable) { tasks.forEach { it.run() } }
                    runConcurrently({ a.run() }, { b.run() })
                }
                fun connect(names: List<String>): Server = Server().apply { names.forEach(this::addConnector) }
                fun attach(h: Handler, inner: Handler) { (h as? Wrapper)?.handler = inner }
                fun self(): Server = Server().apply { this.addConnector("x") }
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
        // `this` in a receiver lambda is the lambda's receiver (it was K's `this`: a read of the wrong object)
        assertEquals("""
            run: Function1<Runnable[],Object> runConcurrently=tasks->ArraysKt___ArraysKt.forEach(tasks,it->it.run()); runConcurrently.invoke(new Runnable[]{()->a.run(),()->b.run()});
            connect: return StandardKt__StandardKt.apply(new Server(),${'$'}receiver->CollectionsKt___CollectionsKt.forEach(names,${'$'}receiver::addConnector));
            attach: if(h instanceof Wrapper){((Wrapper)h).handler=inner;}
            self: return StandardKt__StandardKt.apply(new Server(),${'$'}receiver->${'$'}receiver.addConnector("x"));
            """.trimIndent(), listOf("run", "connect", "attach", "self").joinToString("\n") { "$it: ${body(it)}" })
    }
}
