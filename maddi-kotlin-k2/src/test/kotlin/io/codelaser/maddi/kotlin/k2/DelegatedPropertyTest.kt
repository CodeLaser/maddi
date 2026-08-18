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

import io.codelaser.maddi.cst.api.expression.EmptyExpression
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import io.codelaser.maddi.cst.api.variable.FieldReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Delegated properties (`by`): a private final `<name>$delegate` field of the DELEGATE's type, plus accessors
 * that read/write through it — the JVM's own model. Before this, a delegated property produced no field and an
 * accessor with an empty body, which made its owner look like it had no state at all.
 */
class DelegatedPropertyTest : KotlinScanTestBase() {

    private fun parse(src: String) =
        KotlinScan(runtime, sourceSet).parse("D.kt", src.trimIndent() + "\n").associateBy { it.simpleName() }

    /** `by lazy`: the delegate is a `kotlin.Lazy<T>`, whose value is a FIELD once loaded from bytecode. */
    @Test
    fun byLazyBecomesALazyFieldAndAReadingGetter() {
        val holder = parse(
            """
            class Holder(private val n: Int) {
                val expensive: String by lazy { "v" + n }
            }
            """
        ).getValue("Holder")

        val delegate = holder.getFieldByName("expensive\$delegate", true)
        assertEquals("kotlin.Lazy", delegate.type().typeInfo()!!.fullyQualifiedName())
        assertEquals(listOf(runtime.stringParameterizedType()), delegate.type().parameters())
        assertTrue(delegate.isFinal)
        // the property itself is NOT a field: on the JVM only the delegate is
        assertNull(holder.fields().firstOrNull { it.name() == "expensive" })

        // getExpensive() { return this.expensive$delegate.value; }
        val getter = holder.findUniqueMethod("getExpensive", 0)
        assertEquals(runtime.stringParameterizedType(), getter.returnType())
        val returned = (getter.methodBody().statements().single() as ReturnStatement).expression()
        val read = (returned as VariableExpression).variable() as FieldReference
        assertEquals("value", read.fieldInfo().name())
        assertEquals(delegate, ((read.scope() as VariableExpression).variable() as FieldReference).fieldInfo())
        // not tagged as a plain field access -- it is a call into the delegate
        assertNull(getter.getSetField().field())
    }

    /**
     * The delegate EXPRESSION becomes the field's initializer, so the `lazy { … }` call and its lambda body —
     * and every call edge inside it — are visible instead of dropped.
     */
    @Test
    fun theDelegateExpressionBecomesTheFieldInitializer() {
        val holder = parse(
            """
            class Holder(private val n: Int) {
                val expensive: String by lazy { tag() + n }
                private fun tag(): String = "v"
            }
            """
        ).getValue("Holder")

        val initializer = holder.getFieldByName("expensive\$delegate", true).initializer()
        println("INITIALIZER = ${initializer.javaClass.simpleName} | $initializer")
        assertFalse(initializer is EmptyExpression, "initializer was empty: $initializer")
        // the lambda body survived: the call to tag() and the read of n are both in there
        assertTrue(initializer.toString().contains("tag()"), "no call to tag() in $initializer")
        assertTrue(initializer.toString().contains("n"), "no read of n in $initializer")
    }

    /** The claim the modelling rests on: `by lazy` and the explicit `Lazy` field now read the same way. */
    @Test
    fun byLazyReadsLikeTheExplicitForm() {
        val types = parse(
            """
            class Delegated(private val n: Int) {
                val v: String by lazy { "v" + n }
            }
            class Explicit(private val n: Int) {
                private val slot: Lazy<String> = lazy { "v" + n }
                fun getV(): String = slot.value
            }
            """
        )
        fun readOf(name: String) =
            (types.getValue(name).findUniqueMethod("getV", 0).methodBody().statements().single()
                    as ReturnStatement).expression().toString()
        assertEquals(readOf("Explicit").replace("slot", "v\$delegate"), readOf("Delegated"))
    }

    /** A hand-written delegate declares the `getValue`/`setValue` operators; a `var` keeps its setter. */
    @Test
    fun operatorDelegateUsesGetValueAndSetValue() {
        val holder = parse(
            """
            import kotlin.reflect.KProperty

            class Slot {
                private var v: Int = 0
                operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = v
                operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) { v = value }
            }
            class Holder {
                var counter: Int by Slot()
            }
            """
        ).getValue("Holder")

        assertEquals("Slot", holder.getFieldByName("counter\$delegate", true).type().typeInfo()!!.simpleName())

        val get = (holder.findUniqueMethod("getCounter", 0).methodBody().statements().single()
                as ReturnStatement).expression()
        assertEquals("getValue", (get as MethodCall).methodInfo().name())

        // a `var` delegated property had NO setter at all before this
        val setter = holder.findUniqueMethod("setCounter", 1)
        assertEquals(runtime.voidParameterizedType(), setter.returnType())
        assertTrue(setter.methodBody().statements().single().toString().contains("setValue"))
    }

    /**
     * `by map` — the delegate is a `Map`, whose `getValue(thisRef, property)` is an **extension** in
     * `kotlin.MapsKt`, not a member, so neither spelling resolves. The point is that it lands on a marked
     * placeholder rather than on silence: the round-trip harness already greps for `k2-`.
     */
    @Test
    fun aDelegateWithNoMemberAccessorIsMarked() {
        val holder = parse(
            """
            class Holder(private val backing: Map<String, Any?>) {
                val name: String by backing
            }
            """
        ).getValue("Holder")
        val read = (holder.findUniqueMethod("getName", 0).methodBody().statements().single()
                as ReturnStatement).expression()
        assertTrue(read is EmptyExpression, "expected a placeholder, got ${read.javaClass.simpleName}: $read")
        assertEquals("k2-delegate-read:name\$delegate", (read as EmptyExpression).msg())
    }

    /**
     * A local `val x by lazy { … }` is a SECOND site (`KotlinBodyConverter`, the local-variable path) and is
     * still dropped — and, unlike the member case, still dropped without a marker. Pinned so the day that
     * changes, this test says so.
     */
    @Test
    fun aLocalDelegatedPropertyIsStillDropped() {
        val holder = parse(
            """
            class Holder(private val n: Int) {
                fun run(): String {
                    val local: String by lazy { "v" + n }
                    return local
                }
            }
            """
        ).getValue("Holder")
        val first = holder.findUniqueMethod("run", 0).methodBody().statements().first()
        // the local is created with no initializer at all: neither the `lazy` call nor the lambda survives
        assertEquals("String local;", first.toString())
    }

    /**
     * The same operator delegate, declared AFTER its user. `convertMembers` converts a type's properties before
     * any method signatures exist, so a delegate type not yet converted has no `getValue` to find — this pins
     * whether that ordering reaches the delegate read. (`by lazy` is immune: `kotlin.Lazy` is a library type.)
     */
    @Test
    fun operatorDelegateDeclaredAfterItsUser() {
        val holder = parse(
            """
            import kotlin.reflect.KProperty

            class Holder {
                var counter: Int by Slot()
            }
            class Slot {
                private var v: Int = 0
                operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = v
                operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) { v = value }
            }
            """
        ).getValue("Holder")
        val get = (holder.findUniqueMethod("getCounter", 0).methodBody().statements().single()
                as ReturnStatement).expression()
        println("REVERSE-ORDER read = ${get.javaClass.simpleName} | $get")
        assertEquals("getValue", (get as MethodCall).methodInfo().name())
    }
}
