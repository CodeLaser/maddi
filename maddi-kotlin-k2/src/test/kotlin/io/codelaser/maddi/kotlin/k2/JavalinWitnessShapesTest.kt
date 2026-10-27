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
            fun Handler.unwrap(): Handler = this
            class K {
                fun run(a: Runnable, b: Runnable) {
                    fun runConcurrently(vararg tasks: Runnable) { tasks.forEach { it.run() } }
                    runConcurrently({ a.run() }, { b.run() })
                }
                fun connect(names: List<String>): Server = Server().apply { names.forEach(this::addConnector) }
                fun attach(h: Handler, inner: Handler) { (h as? Wrapper)?.handler = inner }
                fun attachUnwrapped(h: Handler, inner: Handler) { (h.unwrap() as? Wrapper)?.handler = inner }
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
            run: Function1<Runnable[],Object> runConcurrently=tasks->ArraysKt.forEach(tasks,it->it.run()); runConcurrently.invoke(new Runnable[]{()->a.run(),()->b.run()});
            connect: Server ${'$'}this${'$'}apply=new Server(); {CollectionsKt.forEach(names,${'$'}this${'$'}apply::addConnector);} return ${'$'}this${'$'}apply;
            attach: if(h instanceof Wrapper){((Wrapper)h).handler=inner;}
            attachUnwrapped: {Handler ${'$'}safe0=JwKt.unwrap(h);if(${'$'}safe0 instanceof Wrapper){((Wrapper)${'$'}safe0).handler=inner;}}
            self: Server ${'$'}this${'$'}apply=new Server(); {${'$'}this${'$'}apply.addConnector("x");} return ${'$'}this${'$'}apply;
            """.trimIndent(), listOf("run", "connect", "attach", "attachUnwrapped", "self").joinToString("\n") { "$it: ${body(it)}" })
    }
}
