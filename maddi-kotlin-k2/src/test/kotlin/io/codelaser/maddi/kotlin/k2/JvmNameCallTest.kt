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
 * A function renamed for the JVM by `@JvmName` is called by its JVM name: the stdlib's `Iterable<Int>.sum()` is
 * `sumOfInt`, okio's `operator fun Path.div(String)` is `resolve` (coil's `directory / "journal"`), a source function
 * in the same compilation carries its `@JvmName` too.
 */
class JvmNameCallTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("jn/Jn.kt", """
            package jn
            class Seg(val s: String) {
                @JvmName("resolve") operator fun div(child: String): Seg = Seg(s + "/" + child)
            }
            @JvmName("twiceOf") fun Int.twice(): Int = this * 2
            class K {
                fun total(l: List<Int>): Int = l.sum()
                fun totalLong(l: List<Long>): Long = l.sum()
                fun javaClass(k: kotlin.reflect.KClass<String>): Class<String> = k.java
                fun ofInstance(s: String): Class<String> = s.javaClass
                fun implicitThis(): Class<K> = javaClass
                fun safeCall(s: String?): Class<String>? = s?.javaClass
                fun inLambda(l: List<Any>): List<Class<Any>> = l.map { it.javaClass }
                fun path(p: Seg): Seg = p / "journal"
                fun named(p: Seg): Seg = p.div("x")
                fun ext(i: Int): Int = i.twice()
            }
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    private fun body(name: String): String =
        type("K").methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    /** `T.javaClass` and `KClass<T>.java` share the JVM name `getJavaClass`: the receiver tells them apart. */
    @Test
    fun twoGettersOfOneName() {
        fun callee(name: String) = (type("K").methods().first { it.name() == name }.methodBody().statements().first()
            .expression() as io.codelaser.maddi.cst.api.expression.MethodCall).methodInfo().fullyQualifiedName()
        assertEquals("kotlin.jvm.JvmClassMappingKt.getJavaClass(kotlin.reflect.KClass)", callee("javaClass"))
        assertEquals("kotlin.jvm.JvmClassMappingKt.getJavaClass(Object)", callee("ofInstance"))
        // the other shapes detekt writes `javaClass` in, a lambda's body included: each binds the Object getter
        fun callees(name: String): List<String> {
            val found = mutableListOf<String>()
            type("K").methods().first { it.name() == name }.methodBody().visit { e ->
                if (e is io.codelaser.maddi.cst.api.expression.MethodCall) found.add(e.methodInfo().fullyQualifiedName())
                true
            }
            return found
        }
        listOf("implicitThis", "safeCall", "inLambda").forEach { name ->
            assertEquals(listOf("kotlin.jvm.JvmClassMappingKt.getJavaClass(Object)"),
                callees(name).filter { it.contains("getJavaClass") }, name)
        }
    }

    @Test
    fun theJvmNames() {
        // the unit world's stdlib is BUILT from K2, under the JVM names its class file has: the seven `sum`
        // overloads over an Iterable erase alike, and only their @JvmName tells them apart (#15)
        assertEquals("""
            total: return CollectionsKt.sumOfInt(l);
            totalLong: return CollectionsKt.sumOfLong(l);
            javaClass: return JvmClassMappingKt.getJavaClass(k);
            ofInstance: return JvmClassMappingKt.getJavaClass(s);
            path: return p.resolve("journal");
            named: return p.resolve("x");
            ext: return JnKt.twiceOf(i);
            """.trimIndent(), listOf("total", "totalLong", "javaClass", "ofInstance", "path", "named", "ext")
            .joinToString("\n") { "$it: ${body(it)}" })
    }
}
