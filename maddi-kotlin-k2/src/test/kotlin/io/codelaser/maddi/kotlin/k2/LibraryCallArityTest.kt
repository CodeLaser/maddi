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

import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * <b>A library callee was looked up by the number of arguments the SOURCE WRITES.</b> Kotlin's two features
 * that break that identity — an omitted defaulted parameter and a vararg — were therefore exactly the calls
 * that became placeholders: `x.joinToString(",")` is `joinToString/7` on the JVM, and `listOf("a","b")` is
 * one array parameter. On detekt this family was 2,793 placeholders, the largest by far.
 *
 * ⭐ The omitted parameters are filled with their zero value and the call binds the REAL method, not a
 * synthesized `$default`: the annotated API's contracts are keyed to the real signature, so this keeps them
 * reachable. A source callee still binds its `$default` with the mask, as kotlinc compiles it.
 */
class LibraryCallArityTest : KotlinScanTestBase() {

    private fun parse(body: String): TypeInfo =
        KotlinScan(runtime, sourceSet).parse("P.kt", "class P {\n$body\n}\n").first()

    private fun callees(type: TypeInfo, method: String, parameters: Int): List<String> {
        val found = mutableListOf<String>()
        type.findUniqueMethod(method, parameters).methodBody().visit { e ->
            if (e is MethodCall) found.add(e.methodInfo().fullyQualifiedName())
            true
        }
        return found
    }

    @Test
    fun anOmittedDefaultBindsTheRealLibraryMethod() {
        val p = parse("    fun f(x: List<String>) = x.joinToString(\",\")")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total, PlaceholderCensus.of(listOf(p)).byKind.toString())
        val callee = callees(p, "f", 1).single()
        assertTrue(callee.startsWith("kotlin.collections.CollectionsKt"), callee)
        assertTrue(callee.contains("joinToString(Iterable,CharSequence,CharSequence,CharSequence,int,"), callee)
    }

    @Test
    fun aVarargCallBindsTheArrayParameter() {
        val p = parse("""
                fun f() = listOf("a", "b")
                fun g() = listOf<String>()
        """)
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total)
        // written out, as the Java front end represents a varargs call (MethodInfo.typeOfParameterHandleVarargs)
        assertTrue(callees(p, "f", 0).single().endsWith("listOf(Object[])"), callees(p, "f", 0).toString())
        assertTrue(callees(p, "g", 0).single().endsWith("listOf(Object[])"), callees(p, "g", 0).toString())
    }

    /** An operator/infix function declared as an EXTENSION is not a member of the left operand's type. */
    @Test
    fun anInfixLibraryExtensionRoutesThroughItsFacade() {
        val p = parse("    fun f() = mapOf(\"a\" to 1, \"b\" to 2)")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total, PlaceholderCensus.of(listOf(p)).byKind.toString())
        val callees = callees(p, "f", 0)
        assertTrue(callees.any { it.endsWith("mapOf(kotlin.Pair[])") }, callees.toString())
        assertEquals(2, callees.count { it == "kotlin.TuplesKt.to(Object,Object)" }, callees.toString())
    }

    /**
     * ⭐ The control. Widening the arity rule must not make everything "resolve": a name that exists nowhere
     * is still a placeholder, and a run whose census could no longer go up would be measuring nothing.
     */
    @Test
    fun aNameThatExistsNowhereIsStillAPlaceholder() {
        val p = parse("    fun f(x: List<String>) = x.thisMethodDoesNotExist(1, 2)")
        val census = PlaceholderCensus.of(listOf(p))
        assertEquals(1, census.total, census.byKind.toString())
        assertEquals(setOf("k2-unresolved-call:thisMethodDoesNotExist"), census.byKind.keys)
    }
}
