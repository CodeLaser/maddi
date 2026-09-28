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

    // `shadow`: the inner `x` is a plain local, not the delegate. (Its `return` inside `run {}` returning from the
    // lambda is #58, not this.)
    @Test
    fun theShapes() {
        assertEquals("""
            lazyLocal: Lazy<Integer> x${'$'}delegate=LazyKt__LazyJVMKt.lazy(()->5); return x${'$'}delegate.value;
            twice: Lazy<Integer> x${'$'}delegate=LazyKt__LazyJVMKt.lazy(()->5); return x${'$'}delegate.value+x${'$'}delegate.value;
            writable: ReadWriteProperty<Object,Integer> y${'$'}delegate=Delegates.INSTANCE.notNull(); y${'$'}delegate.setValue(null,null,3); return y${'$'}delegate.getValue(null,null);
            shadow: Lazy<Integer> x${'$'}delegate=LazyKt__LazyJVMKt.lazy(()->1); StandardKt__StandardKt.run(this,${'$'}receiver->{int x=2;return x;});
            """.trimIndent(), listOf("lazyLocal", "twice", "writable", "shadow").joinToString("\n") { "$it: ${body(it)}" })
    }
}
