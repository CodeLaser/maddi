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
     * A property reference is its GETTER: as a function, `Q::i` is `(Q) -> Int`, which is what a Java author writes
     * as `Q::getI`. The same two fields decide the answer as for a function reference: the getter, and whether the
     * scope is the type (unbound) or a value (bound).
     */
    @Test
    fun anUnboundPropertyReferenceIsItsGetterScopedToTheType() {
        val types = parse("""
            class Q(val i: Int)
            class P { fun f(l: List<Q>): List<Int> = l.map(Q::i) }
            """)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val mr = referenceIn(types)
        assertEquals("getI", mr.methodInfo().name())
        assertEquals("Q", mr.methodInfo().typeInfo().simpleName())
        assertTrue(mr.scope() is TypeExpression, "an unbound reference names a type, not a value: got ${mr.scope()}")
    }

    /** ⛔ The control: bound to a value, the scope is that value, not its type. */
    @Test
    fun aBoundPropertyReferenceCarriesItsReceiverAsItsScope() {
        val types = parse("""
            class Q(val i: Int)
            class P { fun f(q: Q): () -> Int = q::i }
            """)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val mr = referenceIn(types)
        assertEquals("getI", mr.methodInfo().name())
        val scope = mr.scope()
        assertTrue(scope is VariableExpression && scope.variable().simpleName() == "q", "got $scope")
    }

    /** Written without `this::`, a member property is bound to the enclosing object, as a member function is. */
    @Test
    fun anImplicitMemberPropertyReferenceIsBound() {
        val types = parse("""
            class P {
                val i: Int = 3
                fun f(): () -> Int = ::i
            }
            """)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val mr = referenceIn(types, 0)
        assertEquals("getI", mr.methodInfo().name())
        val scope = mr.scope()
        assertTrue(scope is VariableExpression && scope.variable() is This, "got $scope")
    }

    /**
     * ⚠ A library property has no getter to reference in THIS fixture: with no `CompiledTypesManager` a library
     * type is built from K2 symbols, and `loadLibraryMembers` turns its properties into FIELDS (so `sb.length`
     * reads a field). A method reference cannot name a field, so the shape keeps its named placeholder here. The
     * shipping pipeline loads library types from class files, where `length()` is a method; that path is covered
     * by `TestLoweredShapesVsJava` in maddi-run-kotlin. Pinned so a change to the standalone loader shows up.
     */
    @Test
    fun aLibraryPropertyReferenceHasNoGetterInAStandaloneScan() {
        val types = parse("""
            class P { fun f(l: List<StringBuilder>): List<Int> = l.map(StringBuilder::length) }
            """)
        assertEquals(setOf("k2-callable-ref-property-no-getter"), PlaceholderCensus.of(types).byKind.keys)
    }

    /** An extension property is a static getter on its facade, the receiver its first parameter. */
    @Test
    fun anExtensionPropertyReferenceIsTheFacadeGetter() {
        val types = parse("""
            val Q.doubled: Int get() = i * 2
            class Q(val i: Int)
            class P { fun f(l: List<Q>): List<Int> = l.map(Q::doubled) }
            """)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val mr = referenceIn(types)
        assertEquals("getDoubled", mr.methodInfo().name())
        assertEquals("PKt", mr.methodInfo().typeInfo().simpleName())
        assertTrue(mr.methodInfo().isStatic, "an extension getter is static: ${mr.methodInfo()}")
        assertTrue(mr.scope() is TypeExpression, "got ${mr.scope()}")
    }

    /** A top-level property's getter lives on the file facade, like a top-level function. */
    @Test
    fun aTopLevelPropertyReferenceIsTheFacadeGetter() {
        val types = parse("""
            val top: Int = 3
            class P { fun f(): () -> Int = ::top }
            """)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val mr = referenceIn(types, 0)
        assertEquals("getTop", mr.methodInfo().name())
        assertEquals("PKt", mr.methodInfo().typeInfo().simpleName())
        assertTrue(mr.scope() is TypeExpression, "got ${mr.scope()}")
    }

    /**
     * An extension FUNCTION referenced through its receiver type is the facade's static method with the receiver as
     * parameter 0 -- `Q::twice` is `PKt::twice`, `(Q) -> Int` -- so the scope is the facade type and the callee has one
     * parameter more than the Kotlin declaration.
     */
    @Test
    fun anExtensionFunctionReferenceIsTheFacadeStatic() {
        val types = parse("""
            class Q(val i: Int)
            fun Q.twice(): Int = i * 2
            class P { fun f(l: List<Q>): List<Int> = l.map(Q::twice) }
            """)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val mr = referenceIn(types)
        assertEquals("twice", mr.methodInfo().name())
        assertEquals("PKt", mr.methodInfo().typeInfo().simpleName())
        assertEquals(1, mr.methodInfo().parameters().size, "the receiver is parameter 0")
        assertTrue(mr.scope() is TypeExpression, "got ${mr.scope()}")
    }

    /** The same for a LIBRARY extension: `String::toRegex` was 18 of detekt's unresolved references. */
    @Test
    fun aLibraryExtensionFunctionReferenceIsItsFacadeStatic() {
        val types = parse("""
            class P { fun f(l: List<String>): List<Regex> = l.map(String::toRegex) }
            """)
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val mr = referenceIn(types)
        assertEquals("toRegex", mr.methodInfo().name())
        assertEquals(1, mr.methodInfo().parameters().size, "the receiver is parameter 0")
        assertTrue(mr.methodInfo().isStatic, "${mr.methodInfo()}")
        assertTrue(mr.scope() is TypeExpression, "got ${mr.scope()}")
    }

    /**
     * ⛔ Kept as NAMED placeholders, each for its own reason: a BOUND extension reference binds the facade method's
     * first argument, which no Java method reference spells; a LOCAL function is not modelled by this front end.
     */
    @Test
    fun boundExtensionsAndLocalFunctionsKeepNamedPlaceholders() {
        val types = parse("""
            class Q(val i: Int)
            fun Q.twice(): Int = i * 2
            class P {
                fun f(q: Q): () -> Int = q::twice
                fun g(l: List<Int>): List<Int> { fun inc(i: Int) = i + 1; return l.map(::inc) }
            }
            """)
        // the third is the local function's own DECLARATION, the same unmodelled construct
        assertEquals(setOf("k2-callable-ref-bound-extension", "k2-callable-ref-local-function",
            "k2-unsupported-expr:KtNamedFunction"), PlaceholderCensus.of(types).byKind.keys)
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
