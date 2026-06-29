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

package org.e2immu.language.kotlin.k2

import org.e2immu.language.cst.api.element.SourceSet
import org.e2immu.language.cst.api.info.Variance
import org.e2immu.language.cst.api.expression.Assignment
import org.e2immu.language.cst.api.expression.BinaryOperator
import org.e2immu.language.cst.api.expression.EmptyExpression
import org.e2immu.language.cst.api.expression.InlineConditional
import org.e2immu.language.cst.api.expression.Lambda
import org.e2immu.language.cst.api.expression.MethodCall
import org.e2immu.language.cst.api.expression.StringConcat
import org.e2immu.language.cst.api.expression.SwitchExpression
import org.e2immu.language.cst.api.expression.VariableExpression
import org.e2immu.language.cst.api.info.ParameterInfo
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.cst.api.statement.BreakStatement
import org.e2immu.language.cst.api.statement.DoStatement
import org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation
import org.e2immu.language.cst.api.statement.ExpressionAsStatement
import org.e2immu.language.cst.api.statement.ForEachStatement
import org.e2immu.language.cst.api.statement.IfElseStatement
import org.e2immu.language.cst.api.statement.LocalVariableCreation
import org.e2immu.language.cst.api.statement.ReturnStatement
import org.e2immu.language.cst.api.statement.SwitchStatementNewStyle
import org.e2immu.language.cst.api.statement.WhileStatement
import org.e2immu.language.cst.api.variable.FieldReference
import org.e2immu.language.cst.api.variable.LocalVariable
import org.e2immu.language.cst.api.variable.This
import org.e2immu.language.cst.api.type.NullableState
import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

/** M1 acceptance: `class Foo { fun bar(): Int = 1 }` -> a CST TypeInfo with one MethodInfo. */

/** Type shape: nature, type parameters, nullability, variance, supertypes, visibility/modifiers, access. */
class TypeStructureTest : KotlinScanTestBase() {

    @Test
    fun walkingSkeleton() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse("Foo.kt", "class Foo { fun bar(): Int = 1 }\n")

        assertEquals(1, types.size)
        val foo = types.first()
        assertEquals("Foo", foo.simpleName())
        assertEquals(runtime.objectParameterizedType(), foo.parentClass())

        val bar = foo.findUniqueMethod("bar", 0)
        assertEquals("bar", bar.name())
        assertEquals(runtime.intParameterizedType(), bar.returnType())
        assertEquals("int", bar.returnType().fullyQualifiedName())
    }


    @Test
    fun parametersAndBuiltinTypes() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "Calc.kt",
            """
            class Calc {
                fun add(a: Int, b: Long): Long = a + b
                fun greet(name: String): Unit { }
                fun flag(): Boolean = true
            }
            """.trimIndent() + "\n"
        )
        val calc = types.first()

        val add = calc.findUniqueMethod("add", 2)
        assertEquals(runtime.longParameterizedType(), add.returnType())
        assertEquals("a", add.parameters()[0].name())
        assertEquals(runtime.intParameterizedType(), add.parameters()[0].parameterizedType())
        assertEquals(runtime.longParameterizedType(), add.parameters()[1].parameterizedType())

        val greet = calc.findUniqueMethod("greet", 1)
        assertEquals(runtime.voidParameterizedType(), greet.returnType())
        assertEquals(runtime.stringParameterizedType(), greet.parameters()[0].parameterizedType())

        val flag = calc.findUniqueMethod("flag", 0)
        assertEquals(runtime.booleanParameterizedType(), flag.returnType())
    }


    @Test
    fun nullability() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "Nn.kt",
            """
            class Nn {
                fun maybe(s: String?): String? = s
                fun never(s: String): String = s
                fun boxed(i: Int?): Int? = i
            }
            """.trimIndent() + "\n"
        )
        val nn = types.first()

        val maybe = nn.findUniqueMethod("maybe", 1)
        // String? -> the String type tagged NULLABLE: same erased type, distinct nullability
        assertEquals(NullableState.NULLABLE, maybe.returnType().nullable())
        assertTrue(maybe.returnType().equalsFQN(runtime.stringParameterizedType()))
        assertEquals(NullableState.NULLABLE, maybe.parameters()[0].parameterizedType().nullable())

        // non-null String stays UNSPECIFIED and equal to the predefined String type
        val never = nn.findUniqueMethod("never", 1)
        assertEquals(NullableState.UNSPECIFIED, never.returnType().nullable())
        assertEquals(runtime.stringParameterizedType(), never.returnType())

        // Int? boxes to Integer and is NULLABLE (a nullable primitive cannot stay `int`)
        val boxed = nn.findUniqueMethod("boxed", 1)
        assertEquals(NullableState.NULLABLE, boxed.returnType().nullable())
        assertTrue(boxed.returnType().isBoxedExcludingVoid)
    }


    @Test
    fun declarationSiteVariance() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "Variance.kt",
            """
            class Box<out T>
            class Sink<in T>
            class Pair<A, B>
            """.trimIndent() + "\n"
        ).associateBy { it.simpleName() }

        val box = types.getValue("Box").typeParameters()
        assertEquals(1, box.size)
        assertEquals("T", box[0].simpleName())
        assertEquals(Variance.COVARIANT, box[0].variance())

        assertEquals(Variance.CONTRAVARIANT, types.getValue("Sink").typeParameters()[0].variance())

        val pair = types.getValue("Pair").typeParameters()
        assertEquals(2, pair.size)
        assertEquals(Variance.INVARIANT, pair[0].variance())
        assertEquals(Variance.INVARIANT, pair[1].variance())
    }


    @Test
    fun typeNatureAndSupertypes() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            mapOf(
                "Shapes.kt" to """
                    interface Shape
                    abstract class Base
                    class Circle : Base(), Shape
                    enum class Color { RED, GREEN }
                    object Registry
                """.trimIndent() + "\n"
            )
        ).associateBy { it.simpleName() }

        // type natures classify correctly
        assertTrue(types.getValue("Shape").typeNature().isInterface)
        assertTrue(types.getValue("Color").typeNature().isEnum)
        assertTrue(types.getValue("Circle").typeNature().isClass)
        assertTrue(types.getValue("Registry").typeNature().isClass) // object -> class

        // source supertypes: Circle extends Base, implements Shape (both source types)
        val circle = types.getValue("Circle")
        assertEquals(types.getValue("Base"), circle.parentClass().typeInfo())
        val ifaces = circle.interfacesImplemented().map { it.typeInfo() }.toSet()
        assertTrue(ifaces.contains(types.getValue("Shape")), "interfaces were $ifaces")
    }


    @Test
    fun visibilityAndModifiers() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            mapOf(
                "V.kt" to """
                    abstract class Base {
                        abstract fun f(): Int
                        private fun secret(): Int = 1
                    }
                    open class Sub : Base() { override fun f(): Int = 0 }
                    class Final
                """.trimIndent() + "\n"
            )
        ).associateBy { it.simpleName() }

        val base = types.getValue("Base")
        // abstract class -> abstract type modifier; abstract fun -> abstract method modifier
        assertTrue(base.typeModifiers().contains(runtime.typeModifierAbstract()))
        assertTrue(base.findUniqueMethod("f", 0).methodModifiers().contains(runtime.methodModifierAbstract()))
        // private fun -> private (access + modifier)
        val secret = base.findUniqueMethod("secret", 0)
        assertEquals(runtime.accessPrivate(), secret.access())
        assertTrue(secret.methodModifiers().contains(runtime.methodModifierPrivate()))

        // Kotlin default is final; `open` removes it
        assertTrue(types.getValue("Final").typeModifiers().contains(runtime.typeModifierFinal()))
        assertFalse(types.getValue("Sub").typeModifiers().contains(runtime.typeModifierFinal()))
    }


    @Test
    fun accessIsComputed() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "Acc.kt",
            """
            interface Api { fun call(): Int }
            class Impl { private fun secret(): Int = 1 }
            """.trimIndent() + "\n"
        ).associateBy { it.simpleName() }

        // access() is the computed value (computeAccess), not a hand-mapped visibility:
        // an abstract interface method is public (interface special-casing in computeAccess)
        assertEquals(runtime.accessPublic(), types.getValue("Api").findUniqueMethod("call", 0).access())
        // a private method is private
        assertEquals(runtime.accessPrivate(), types.getValue("Impl").findUniqueMethod("secret", 0).access())
    }


    @Test
    fun internalVisibility() {
        val scan = KotlinScan(runtime, sourceSet)
        val mod = scan.parse("Mod.kt", "internal class Mod { internal fun work(): Int = 1 }\n").first()

        // Kotlin `internal` maps to the new CST internal modifier + internal (eventual) access
        assertEquals(runtime.accessInternal(), mod.access())
        val work = mod.findUniqueMethod("work", 0)
        assertEquals(runtime.accessInternal(), work.access())
        assertTrue(work.methodModifiers().contains(runtime.methodModifierInternal()))
    }
}
