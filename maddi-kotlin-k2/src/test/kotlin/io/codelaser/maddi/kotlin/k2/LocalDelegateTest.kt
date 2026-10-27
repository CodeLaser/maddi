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
 * #52: a LOCAL delegated property lost its initializer -- `val x by lazy { 5 }; return x` became `int x; return x;`,
 * a read of something never assigned, with the lambda and all it did gone from the tree. kotlinc compiles it as a
 * local `x$delegate` holding the delegate object, every read a read through it and every write a `setValue`.
 */
class LocalDelegateTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ld/Ld.kt", """
            package ld
            import kotlin.properties.Delegates
            class K {
                fun lazyLocal(): Int { val x by lazy { 5 }; return x }
                fun twice(): Int { val x by lazy { 5 }; return x + x }
                fun writable(): Int { var y by Delegates.notNull<Int>(); y = 3; return y }
                fun shadow(): Int { val x by lazy { 1 }; run { val x = 2; return x }; }
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "K" }.methods().first { it.name() == name }
        .methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    // `shadow`: the inner `x` is a plain local, not the delegate. Its bare `return` inside `run {}` leaves `shadow()`
    // itself (#58): the Java printer marks the non-local return in a comment rather than print a different program.
    @Test
    fun theShapes() {
        assertEquals("""
            lazyLocal: Lazy<Integer> x${'$'}delegate=LazyKt.lazy(()->5); return x${'$'}delegate.value;
            twice: Lazy<Integer> x${'$'}delegate=LazyKt.lazy(()->5); return x${'$'}delegate.value+x${'$'}delegate.value;
            writable: ReadWriteProperty<Object,Integer> y${'$'}delegate=Delegates.INSTANCE.notNull(); y${'$'}delegate.setValue(null,null,3); return y${'$'}delegate.getValue(null,null);
            shadow: Lazy<Integer> x${'$'}delegate=LazyKt.lazy(()->1); {int x=2;return x;}
            """.trimIndent(), listOf("lazyLocal", "twice", "writable", "shadow").joinToString("\n") { "$it: ${body(it)}" })
    }
}
