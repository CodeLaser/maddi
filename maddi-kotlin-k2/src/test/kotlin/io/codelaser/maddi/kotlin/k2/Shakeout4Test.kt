package org.e2immu.language.kotlin.k2

import org.e2immu.language.cst.api.expression.EmptyExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Fourth shakeout: typealias, class delegation (`by`), value classes, extension properties, suspend,
 *  inline, tailrec, sealed interfaces, when-with-subject-binding -- collect every k2-* placeholder. */
class Shakeout4Test : KotlinScanTestBase() {

    private val source = """
        package org.example

        typealias Handler = (Int) -> Int

        interface Repo {
            fun find(id: Int): String
        }

        class CachingRepo(private val backing: Repo) : Repo by backing {
            fun findOrDefault(id: Int): String = find(id)
        }

        @JvmInline
        value class UserId(val raw: Int) {
            fun next(): UserId = UserId(raw + 1)
        }

        sealed interface Shape {
            fun area(): Double
        }
        class Circle(val r: Double) : Shape {
            override fun area(): Double = 3.14 * r * r
        }

        val Int.doubled: Int get() = this * 2

        inline fun applyTwice(x: Int, f: Handler): Int = f(f(x))

        tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

        suspend fun computeAsync(x: Int): Int = x * 2

        class Service {
            private val repo: Repo = CachingRepo(object : Repo { override fun find(id: Int): String = "x" })

            fun describe(id: Int): String {
                val u = UserId(id)
                val n = u.next().raw
                return when (val v = n.doubled) {
                    0 -> "zero"
                    else -> "v=" + v
                }
            }

            fun sum(): Int = applyTwice(3) { it + 1 }
            fun divisor(): Int = gcd(48, 36)
            fun lookup(id: Int): String = repo.find(id)
        }
    """.trimIndent()

    @Test
    fun shakeout() {
        val types = KotlinScan(runtime, sourceSet).parse("Shakeout4.kt", source)
        val placeholders = sortedMapOf<String, Int>()
        types.forEach { type ->
            (type.methods() + type.constructors()).forEach { m ->
                m.methodBody().visit { e ->
                    if (e is EmptyExpression && e.msg().startsWith("k2-")) placeholders.merge(e.msg(), 1) { a, b -> a + b }
                    true
                }
            }
        }
        assertEquals("", placeholders.entries.joinToString("\n") { "${it.value}x ${it.key}" })
    }
}
