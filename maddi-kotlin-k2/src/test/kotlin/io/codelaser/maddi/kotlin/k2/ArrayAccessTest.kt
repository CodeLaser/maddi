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

import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.cst.api.expression.Assignment
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.variable.DependentVariable
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A Kotlin array's `get`/`set` is a JVM array load/store: `IntArray` is `int[]`, `Array<T>` is `T[]`. It was
 * converted as a `get`/`set` CALL looked up on the element type: a placeholder for every store and every
 * primitive-array load, and a bogus `String.get(int)` for `Array<String>`. It is now the element as a
 * DependentVariable, exactly as the Java parser builds `a[i]`. A user's EXTENSION `get`/`set` operator on an array
 * type stays a call, to its facade.
 */
class ArrayAccessTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("aa/Aa.kt", """
            package aa
            operator fun ByteArray.set(c: Char, v: Byte) { this[c.code] = v }
            class K {
                fun ints(a: IntArray): Int { a[0] = 1; return a[1] }
                fun nested(dp: Array<IntArray>): Int { dp[0][1] = 2; return dp[1][0] }
                fun objects(a: Array<String>): String { a[0] = "x"; return a[1] }
                fun augmented(a: IntArray) { a[0] += 2 }
                fun viaExtension(b: ByteArray) { b['c'] = 4 }
                fun list(l: MutableList<String>) { l[0] = "y" }
                fun chars(s: String): Char = s[0]
            }
            fun String.first(): Char = get(0)
            class Unused {
            }
            """.trimIndent() + "\n")
    }

    private fun method(name: String): MethodInfo = types.first { it.simpleName() == "K" }.findUniqueMethod(name, 1)

    private fun <T : Element> all(method: MethodInfo, type: Class<T>): List<T> {
        val found = mutableListOf<T>()
        method.methodBody().visit { e: Element -> if (type.isInstance(e)) found += type.cast(e); true }
        return found
    }

    @Test
    fun loadsAndStoresAreArrayElements() {
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
        for (name in listOf("ints", "nested", "objects")) {
            val m = method(name)
            assertEquals(listOf<String>(), all(m, MethodCall::class.java).map { it.methodInfo().name() }, "no call in $name")
            val store = all(m, Assignment::class.java).single()
            assertTrue(store.variableTarget() is DependentVariable, "$name: ${store.variableTarget()}")
        }
        assertEquals("{a[0]=1;return a[1];}", method("ints").methodBody().toString())
        assertEquals("{dp[0][1]=2;return dp[1][0];}", method("nested").methodBody().toString())
    }

    @Test
    fun anAugmentedStoreIsACompoundAssignment() {
        val store = all(method("augmented"), Assignment::class.java).single()
        assertTrue(store.variableTarget() is DependentVariable)
        assertEquals("{a[0]+=2;}", method("augmented").methodBody().toString())
    }

    @Test
    fun anExtensionOperatorStaysACallToItsFacade() {
        val call = all(method("viaExtension"), MethodCall::class.java).single()
        assertEquals("AaKt.set", call.methodInfo().typeInfo().simpleName() + "." + call.methodInfo().name())
        // and inside it, the built-in store on `this`
        val set = types.first { it.simpleName() == "AaKt" }.findUniqueMethod("set", 3)
        assertTrue(all(set, Assignment::class.java).single().variableTarget() is DependentVariable)
    }

    /**
     * `s[0]` and an implicit-receiver `get(0)` on a String both resolve (coil's `percentDecode`). ⚠ This fixture's
     * java.lang.String is built from K2's kotlin.String, which has `get` and no `charAt`; on a corpus, with the JDK's
     * String, the same lookup lands on `charAt`. So the NAME is not asserted here, only that each is one resolved call.
     */
    @Test
    fun aStringIndexResolves() {
        assertEquals("String", all(method("chars"), MethodCall::class.java).single().methodInfo().typeInfo().simpleName())
        val first = types.first { it.simpleName() == "AaKt" }.findUniqueMethod("first", 1)
        assertEquals("String", all(first, MethodCall::class.java).single().methodInfo().typeInfo().simpleName())
    }

    @Test
    fun aListIsStillACall() {
        assertEquals("List.set", all(method("list"), MethodCall::class.java).single()
            .let { it.methodInfo().typeInfo().simpleName() + "." + it.methodInfo().name() })
        assertTrue(all(method("list"), VariableExpression::class.java).none { it.variable() is DependentVariable })
    }
}
