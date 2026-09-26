package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.MethodInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A `by`-delegation forwarder has the JVM signature kotlinc gives it: the delegated member as seen through the
 * supertype the class names (ExpressionTest has `Iterable<String> by values`), in parameters and in members the
 * interface inherits too, and BOXED where the interface declares a type parameter -- `Repo<Int> by r` forwards
 * `Integer find(int)` for `E find(int)`. An `int find(int)` does not override it: its Java stub did not compile.
 */
class DelegatedMemberTypesTest : KotlinScanTestBase() {

    private val source = """
        package org.example

        interface Store<K> {
            fun put(key: K, all: List<K>): K
        }
        interface Named<N> {
            fun name(): N
        }
        interface Repo<E> : Named<E> {
            fun find(id: Int): E
        }
        interface Sink<V> {
            fun accept(v: V, count: Int)
        }
        class IntSink(private val s: Sink<Int>) : Sink<Int> by s
        class Values(private val values: List<String>) : Iterable<String> by values
        class StringStore(private val d: Store<String>) : Store<String> by d
        class IntRepo(private val r: Repo<Int>) : Repo<Int> by r
        class Passing<X>(private val r: Repo<List<X>>) : Repo<List<X>> by r
    """.trimIndent()

    private fun signature(m: MethodInfo) = m.name() + "(" +
        m.parameters().joinToString(", ") { it.parameterizedType().detailedString() } + "): " +
        m.returnType().detailedString()

    @Test
    fun forwardersAreSubstituted() {
        val types = KotlinScan(runtime, sourceSet).parse("Delegation.kt", source).associateBy { it.simpleName() }
        fun of(type: String, method: String) = signature(types.getValue(type).methods().single { it.name() == method })

        assertEquals("iterator(): java.util.Iterator<String>", of("Values", "iterator"))
        assertEquals("put(String, java.util.List<String>): String", of("StringStore", "put"))
        // where the interface says a type parameter, kotlinc's forwarder is boxed; a declared Int stays int
        assertEquals("find(int): Integer", of("IntRepo", "find"))
        assertEquals("accept(Integer, int): void", of("IntSink", "accept"))
        // inherited by the delegated interface from its own supertype: Named<N> through Repo<E> through Repo<Int>
        assertEquals("name(): Integer", of("IntRepo", "name"))
        // the class's own type parameter survives
        assertEquals("find(int): java.util.List<X>", of("Passing", "find"))
    }
}
