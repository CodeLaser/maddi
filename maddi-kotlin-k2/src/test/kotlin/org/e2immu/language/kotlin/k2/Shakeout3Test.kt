package org.e2immu.language.kotlin.k2

import org.e2immu.language.cst.api.expression.EmptyExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Third shakeout: generics-in-use, inheritance/override/super, interfaces with defaults, inner classes,
 *  operator overloading, labeled jumps, named/spread args -- collect every k2-* placeholder. */
class Shakeout3Test : KotlinScanTestBase() {

    private val source = """
        package org.example

        interface Named {
            val name: String
            fun greeting(): String = "hello, " + name
        }

        abstract class Animal(override val name: String) : Named {
            abstract fun sound(): String
            open fun describe(): String = name + " says " + sound()
        }

        class Dog(name: String) : Animal(name) {
            override fun sound(): String = "woof"
            override fun describe(): String = "dog: " + super.describe()
        }

        class Box<T>(private val value: T) {
            fun get(): T = value
            fun <R> map(f: (T) -> R): Box<R> = Box(f(value))
        }

        class Counter(var n: Int) {
            operator fun plus(other: Counter): Counter = Counter(n + other.n)
            operator fun get(index: Int): Int = n + index
            operator fun compareTo(other: Counter): Int = n - other.n
        }

        class Outer(private val label: String) {
            inner class Inner {
                fun show(): String = "inner of " + label
            }
            fun make(): Inner = Inner()
        }

        fun useCounter(a: Counter, b: Counter): Boolean {
            val c = a + b
            val first = c[0]
            return a < b && first > 0
        }

        fun labeled(xs: List<Int>): Int {
            var sum = 0
            loop@ for (x in xs) {
                if (x < 0) continue@loop
                if (x > 100) break@loop
                sum += x
            }
            return sum
        }

        fun named(a: Int, b: Int = 10, c: Int = 20): Int = a + b + c
        fun callNamed(): Int = named(1, c = 5)

        fun variance(src: List<out Number>, dst: MutableList<in Int>) {
            dst.add(1)
        }

        fun <T> firstOr(xs: List<T>, default: T): T = if (xs.isEmpty()) default else xs[0]
    """.trimIndent()

    @Test
    fun shakeout() {
        val types = KotlinScan(runtime, sourceSet).parse("Shakeout3.kt", source)
        val placeholders = sortedMapOf<String, Int>()
        types.forEach { type ->
            (type.methods() + type.constructors()).forEach { m ->
                m.methodBody().visit { e ->
                    if (e is EmptyExpression && e.msg().startsWith("k2-")) placeholders.merge(e.msg(), 1) { a, b -> a + b }
                    true
                }
            }
        }
        // every feature in the snippet must resolve: no `k2-*` placeholders survive
        assertEquals("", placeholders.entries.joinToString("\n") { "${it.value}x ${it.key}" })
    }
}
