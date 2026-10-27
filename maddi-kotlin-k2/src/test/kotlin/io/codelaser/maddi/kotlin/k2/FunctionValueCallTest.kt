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
 * Two calls whose callee is not what the source names: a PROPERTY of function type called like a method
 * (`d.provider("x")` is `d.getProvider().invoke("x")`), and a parameter of a function type WITH a receiver invoked
 * with that receiver implicit (`init()` is `init.invoke($receiver)`). And `AutoCloseable.use`, whose facade,
 * `kotlin.jdk7.AutoCloseableKt`, lives in a JVM package that is not its Kotlin one (`@file:JvmPackageName`); called
 * with a function value, since a literal is inlined as a try-with-resources (#88).
 */
class FunctionValueCallTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("fv/Fv.kt", """
            package fv
            class Desc(val provider: (String) -> Int)
            class W { fun write(s: String) {} }
            fun W.tag(name: String, init: W.() -> Unit) { write(name); init() }
            class K {
                fun viaProperty(d: Desc): Int = d.provider("x")
                fun viaUse(c: AutoCloseable, f: (AutoCloseable) -> Int): Int = c.use(f)
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
        val actual = "viaProperty: " + body("K", "viaProperty") + "\nviaUse: " + body("K", "viaUse") +
            "\ntag: " + body("FvKt", "tag")
        assertEquals("""
            viaProperty: return d.getProvider().invoke("x");
            viaUse: return AutoCloseableKt.use(c,f);
            tag: ${'$'}receiver.write(name); init.invoke(${'$'}receiver);
            """.trimIndent(), actual)
    }
}
