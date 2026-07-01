package org.e2immu.language.kotlin.k2

import org.e2immu.language.cst.api.expression.EmptyExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Second shakeout: a feature-heavy snippet (enums, sealed, data classes, nested types, operators,
 *  null-safety, generics, collections via member calls) -- collect every k2-* placeholder. */
class Shakeout2Test : KotlinScanTestBase() {

    private val source = """
        package org.example

        enum class Priority { LOW, HIGH;
            fun weight(): Int = if (this == HIGH) 10 else 1
        }

        sealed class Event {
            data class Click(val x: Int, val y: Int) : Event()
            data class Key(val code: Int) : Event()
            object Close : Event()
        }

        data class Point(val x: Int, val y: Int) {
            fun manhattan(): Int = x + y
            companion object { val ORIGIN = Point(0, 0) }
        }

        class Registry {
            private val items = HashMap<String, Point>()
            private val order = ArrayList<String>()

            fun put(key: String, p: Point) {
                items[key] = p
                order.add(key)
            }

            fun get(key: String): Point? = items[key]
            fun size(): Int = items.size
            fun has(key: String): Boolean = key in order

            fun describe(e: Event): String = when (e) {
                is Event.Click -> "click " + e.x
                is Event.Key -> "key " + e.code
                Event.Close -> "close"
            }

            fun best(a: Point, b: Point): Point = if (a.manhattan() > b.manhattan()) a else b
            fun firstOrDefault(key: String): Point = get(key) ?: Point.ORIGIN
            fun label(p: Point): String = "point(${'$'}{p.x}, ${'$'}{p.y})"
            fun run(action: () -> Unit) { action() }

            fun safe(key: String): Int {
                return try {
                    get(key)!!.manhattan()
                } catch (e: RuntimeException) {
                    -1
                }
            }
        }

        fun <T : Comparable<T>> maxOf3(a: T, b: T, c: T): T {
            val m = if (a > b) a else b
            return if (m > c) m else c
        }
    """.trimIndent()

    @Test
    fun shakeout() {
        val types = KotlinScan(runtime, sourceSet).parse("Shakeout2.kt", source)
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
