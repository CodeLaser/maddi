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
 * <b>An inherited `var`, assigned by its simple name in a subclass</b>: `hasNext = result.next()` in Exposed's
 * `ResultIterator : StatementIterator`, `duration += delta` in `JdbcTransaction : Transaction`. Each was a
 * `k2-assign-target` placeholder that took its value's reads with it (maddi#220).
 */
class InheritedPropertyAssignmentTest : KotlinScanTestBase() {

    private fun parse(source: String): List<TypeInfo> = KotlinScan(runtime, sourceSet).parse("a/P.kt", source)

    @Test
    fun anInheritedVarIsAssigned() {
        val types = parse("""
            package a
            abstract class Base {
                protected var hasNext = false
                var duration: Long = 0
            }
            class Sub(private val n: Int) : Base() {
                init { hasNext = n > 0 }
                fun add(d: Long) { duration += d }
                fun reset() { this.duration = 0 }
            }
        """.trimIndent())
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val sub = types.first { it.simpleName() == "Sub" }
        assertEquals("{this.setDuration(this.getDuration()+d);}", sub.findUniqueMethod("add", 1).methodBody().toString())
        assertEquals("{super();this.n=n;{this.setHasNext(n>0);}}", sub.constructors().single().methodBody().toString())
    }
    /**
     * ...also when a METHOD has the property's name: Exposed's `StatementIterator` has `protected var hasNext` (with a
     * written setter) and `override fun hasNext()`; `Query` has `count` and `fun count()`. The property's accessors
     * are `getHasNext`/`setHasNext`, and the method is not one of them.
     */
    @Test
    fun aVarNamedLikeAMethodIsAssignedThroughItsSetter() {
        val types = parse("""
            package a
            abstract class It(private val more: () -> Boolean) : Iterator<Int> {
                protected var hasNext = false
                    set(value) { field = value }
                override fun hasNext(): Boolean = hasNext
                override fun next(): Int { hasNext = more(); return 1 }
            }
            abstract class Q { protected var count: Boolean = false }
            class Sub(more: () -> Boolean) : It(more) {
                init { hasNext = more() }
            }
            class Query : Q() {
                fun count(): Long { count = true; return 0 }
            }
        """.trimIndent())
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        assertEquals("{super(more);{setHasNext(more.invoke());}}",
            types.first { it.simpleName() == "Sub" }.constructors().single().methodBody().toString())
        assertEquals("{setCount(true);return 0L;}",
            types.first { it.simpleName() == "Query" }.findUniqueMethod("count", 0).methodBody().toString())
    }
    /**
     * A Java bean property written in a lambda WITH a receiver: Gradle's `filter { isFailOnNoMatchingTests = false }`
     * and `classpath += files(…)` in Exposed's build logic; `Thread` has the same shape (`isDaemon`, `name`).
     */
    @Test
    fun aJavaPropertyIsAssignedOnTheLambdaReceiver() {
        val types = parse("""
            package a
            class P {
                fun flag(t: Thread) { t.apply { isDaemon = true } }
                fun append(t: Thread) { t.apply { name += "-x" } }
                fun qualified(t: Thread) { t.isDaemon = false }
            }
        """.trimIndent())
        val p = types.first { it.simpleName() == "P" }
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        assertEquals("{{t.setDaemon(true);}}", p.findUniqueMethod("flag", 1).methodBody().toString())
        assertEquals("{{t.setName(t.getName()+\"-x\");}}", p.findUniqueMethod("append", 1).methodBody().toString())
        assertEquals("{t.setDaemon(false);}", p.findUniqueMethod("qualified", 1).methodBody().toString())
    }
}
