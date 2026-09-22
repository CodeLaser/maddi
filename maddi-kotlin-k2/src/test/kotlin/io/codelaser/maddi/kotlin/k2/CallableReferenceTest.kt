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

import io.codelaser.maddi.cst.api.expression.MethodReference
import io.codelaser.maddi.cst.api.expression.TypeExpression
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.variable.This
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `::f` is Java's `::f`, and the CST already has a [MethodReference] for it. ⭐ The field that decides what the
 * link engine concludes is not `methodInfo()` but **`scope()`**: `ExpressionVisitor.methodReference` treats a
 * scope with no links-primary (a [TypeExpression]) as an INTERNAL receiver and drops its self-modifications,
 * and a scope that is a value as the caller's own object. So `Q::len` and `this::len` must not produce the
 * same tree, and asserting "no placeholder" would not have noticed if they did.
 */
class CallableReferenceTest : KotlinScanTestBase() {

    private fun parse(src: String): List<TypeInfo> =
        KotlinScan(runtime, sourceSet).parse("P.kt", src.trimIndent() + "\n")

    /** The single expression of `P.f`'s expression body, as the argument of the call it is written in. */
    private fun referenceIn(types: List<TypeInfo>, arity: Int = 1): MethodReference {
        val body = types.first { it.simpleName() == "P" }.findUniqueMethod("f", arity).methodBody()
        val found = ArrayList<MethodReference>()
        body.visit { e -> if (e is MethodReference) found.add(e); true }
        assertEquals(1, found.size, "expected exactly one method reference, got $found")
        return found.first()
    }

    @Test
    fun aTopLevelFunctionReferenceIsScopedToTheFileFacade() {
        val types = parse("""
            class P { fun f(l: List<Int>): List<Int> = l.map(::two) }
            fun two(i: Int): Int = i + i
            """)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val mr = referenceIn(types)
        assertEquals("two", mr.methodInfo().name())
        assertTrue(mr.scope() is TypeExpression, "a top-level function has no instance: got ${mr.scope()}")
        assertEquals("PKt", mr.methodInfo().typeInfo().simpleName())
    }

    @Test
    fun aBoundMemberReferenceCarriesThisAsItsScope() {
        val types = parse("""
            class P {
                fun twice(i: Int): Int = i * 2
                fun f(l: List<Int>): List<Int> = l.map(this::twice)
            }
            """)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val mr = referenceIn(types)
        assertEquals("twice", mr.methodInfo().name())
        val scope = mr.scope()
        assertTrue(scope is VariableExpression && scope.variable() is This,
            "a bound reference's receiver is the caller's own object: got $scope")
    }

    /** ⭐ Written without `this::`, it means the same thing and must produce the same tree. */
    @Test
    fun animplicitMemberReferenceIsAlsoBound() {
        val types = parse("""
            class P {
                fun twice(i: Int): Int = i * 2
                fun f(l: List<Int>): List<Int> = l.map(::twice)
            }
            """)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val scope = referenceIn(types).scope()
        assertTrue(scope is VariableExpression && scope.variable() is This, "got $scope")
    }

    /** ⛔ The control that the two are NOT the same: an unbound reference's scope is the TYPE. */
    @Test
    fun anUnboundMemberReferenceIsScopedToTheType() {
        val types = parse("""
            class Q { fun len(): Int = 3 }
            class P { fun f(l: List<Q>): List<Int> = l.map(Q::len) }
            """)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val mr = referenceIn(types)
        assertEquals("len", mr.methodInfo().name())
        assertEquals("Q", mr.methodInfo().typeInfo().simpleName())
        assertTrue(mr.scope() is TypeExpression, "an unbound reference names a type, not a value: got ${mr.scope()}")
    }

    @Test
    fun aConstructorReferenceResolvesToTheConstructor() {
        val types = parse("""
            class Q(val i: Int)
            class P { fun f(l: List<Int>): List<Q> = l.map(::Q) }
            """)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val mr = referenceIn(types)
        assertTrue(mr.methodInfo().isConstructor, "expected a constructor, got ${mr.methodInfo()}")
        assertEquals("Q", mr.methodInfo().typeInfo().simpleName())
        assertTrue(mr.scope() is TypeExpression, "got ${mr.scope()}")
    }

    /**
     * ⛔ Not yet converted, and deliberately: a Kotlin property is not a `MethodInfo` in this front end, so
     * `Q::i` has nothing to reference. It keeps a placeholder that NAMES the shape rather than hiding among
     * the unsupported expressions — the census is the disclosure instrument, so the name is the deliverable.
     */
    @Test
    fun aPropertyReferenceKeepsANamedPlaceholder() {
        val types = parse("""
            class Q(val i: Int)
            class P { fun f(l: List<Q>): List<Int> = l.map(Q::i) }
            """)
        val census = PlaceholderCensus.of(types)
        assertEquals(1, census.total, census.byKind.toString())
        assertEquals(setOf("k2-callable-ref-property"), census.byKind.keys)
    }

    /**
     * ⚠ The reference converts; INVOKING the value it was stored in does not. `g(2)` on a `KFunction1`-typed
     * local is Kotlin's invoke-operator sugar over a type this front end knows only shallowly. Pinned so the
     * remaining hole is a stated one, and so that closing it shows up here.
     */
    @Test
    fun aReferenceStoredInALocalConvertsButCallingItDoesNot() {
        val types = parse("""
            class P {
                fun twice(i: Int): Int = i * 2
                fun f(): Int { val g = ::twice; return g(2) }
            }
            """)
        val census = PlaceholderCensus.of(types)
        assertEquals(setOf("k2-unresolved-call:g"), census.byKind.keys, census.byKind.toString())
        assertEquals("twice", referenceIn(types, 0).methodInfo().name())
    }
}
