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

/** Type registry / resolution: source types, mapped library types, cross-file and JDK references. */
class TypeResolutionTest : KotlinScanTestBase() {

    @Test
    fun sourceTypeResolution() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "Types.kt",
            """
            class A {
                fun self(): A = this
                fun other(): B = B()
            }
            class B
            class Box<T> {
                fun id(p: T): T = p
                fun wrap(): Box<A> = Box<A>()
            }
            """.trimIndent() + "\n"
        ).associateBy { it.simpleName() }
        val a = types.getValue("A")
        val b = types.getValue("B")
        val box = types.getValue("Box")

        // references to sibling source types resolve to their actual TypeInfo
        assertEquals(a, a.findUniqueMethod("self", 0).returnType().typeInfo())
        assertEquals(b, a.findUniqueMethod("other", 0).returnType().typeInfo())

        // a bare T resolves to the class's type parameter (param and return)
        val id = box.findUniqueMethod("id", 1)
        assertEquals(box.typeParameters()[0], id.returnType().typeParameter())
        assertEquals(box.typeParameters()[0], id.parameters()[0].parameterizedType().typeParameter())

        // generic argument: Box<A> -> Box with one parameter A
        val wrap = box.findUniqueMethod("wrap", 0).returnType()
        assertEquals(box, wrap.typeInfo())
        assertEquals(1, wrap.parameters().size)
        assertEquals(a, wrap.parameters()[0].typeInfo())
    }


    @Test
    fun libraryTypesMapToJvm() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "Lib.kt",
            """
            class Lib {
                fun strings(): List<String> = listOf()
                fun ints(): MutableList<Int> = mutableListOf()
                fun any(): Any = this
            }
            """.trimIndent() + "\n"
        )
        val lib = types.first()

        // List<String> -> java.util.List<java.lang.String>
        val strings = lib.findUniqueMethod("strings", 0).returnType()
        assertEquals("java.util.List", strings.typeInfo().fullyQualifiedName())
        assertEquals(1, strings.parameters().size)
        assertEquals(runtime.stringParameterizedType(), strings.parameters()[0])

        // MutableList collapses to the same java.util.List; Int boxes to Integer in a generic argument
        val ints = lib.findUniqueMethod("ints", 0).returnType()
        assertEquals("java.util.List", ints.typeInfo().fullyQualifiedName())
        assertTrue(ints.parameters()[0].isBoxedExcludingVoid)

        // Any -> java.lang.Object (the predefined instance)
        assertEquals(runtime.objectParameterizedType(), lib.findUniqueMethod("any", 0).returnType())
    }


    @Test
    fun crossFileReferences() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            mapOf(
                "Service.kt" to "class Service { fun make(): Model = Model() }\n",
                "Model.kt" to "class Model { fun owner(): Service = Service() }\n",
            )
        ).associateBy { it.simpleName() }
        val service = types.getValue("Service")
        val model = types.getValue("Model")

        // a method in one file resolves to the TypeInfo declared in the other (single shared registry)
        assertEquals(model, service.findUniqueMethod("make", 0).returnType().typeInfo())
        assertEquals(service, model.findUniqueMethod("owner", 0).returnType().typeInfo())
    }


    @Test
    fun resolvesNonBuiltinJdkType() {
        // UUID is not a Kotlin builtin; it only resolves if the session has the real JDK on its classpath
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "U.kt",
            "import java.util.UUID\nclass U { fun id(): UUID = UUID.randomUUID() }\n"
        )
        val id = types.first().findUniqueMethod("id", 0).returnType()
        assertEquals("java.util.UUID", id.typeInfo().fullyQualifiedName())
    }


    @Test
    fun deepensLibraryTypeHierarchy() {
        // a non-mapped library type is loaded from its real symbol: nature + supertype hierarchy
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "U.kt",
            "import java.util.UUID\nclass U { fun id(): UUID = UUID.randomUUID() }\n"
        )
        val uuid = types.first().findUniqueMethod("id", 0).returnType().typeInfo()

        assertEquals(runtime.objectParameterizedType(), uuid.parentClass())
        val interfaceFqns = uuid.interfacesImplemented().map { it.typeInfo().fullyQualifiedName() }.toSet()
        assertTrue(interfaceFqns.contains("java.io.Serializable"), "interfaces were $interfaceFqns")
        assertTrue(interfaceFqns.contains("java.lang.Comparable"), "interfaces were $interfaceFqns")

        // members are loaded (signatures): the directly-referenced library type has its real methods
        val methodNames = uuid.methods().map { it.name() }.toSet()
        assertTrue(methodNames.contains("toString"), "methods were $methodNames")
        assertTrue(methodNames.contains("compareTo"), "methods were $methodNames")
        // a method with a simple return type resolves it: getMostSignificantBits(): long
        assertEquals(
            runtime.longParameterizedType(),
            uuid.findUniqueMethod("getMostSignificantBits", 0).returnType()
        )
    }

    @Test
    fun libraryTypeLoadsInheritedMembers() {
        // the full member scope is flattened: Random does not declare equals/hashCode (Object does), yet they
        // must sit on Random so calls resolve -- this is what memberScope (vs declaredMemberScope) buys us
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse("R.kt", "import java.util.Random\nclass R { fun r(): Random = Random() }\n")
        val random = types.first().findUniqueMethod("r", 0).returnType().typeInfo()
        val methodNames = random.methods().map { it.name() }.toSet()

        assertTrue(methodNames.contains("nextInt"), "declared method missing; methods were $methodNames")
        assertTrue(methodNames.contains("equals"), "inherited method missing; methods were $methodNames")
        assertTrue(methodNames.contains("hashCode"), "inherited method missing; methods were $methodNames")
    }

    @Test
    fun libraryTypeLoadsPropertiesAsFields() {
        // library properties are loaded as fields, so `obj.x` resolves; IntRange inherits `first`/`last`
        // from IntProgression (so this also exercises inherited members)
        val scan = KotlinScan(runtime, sourceSet)
        val c = scan.parse("C.kt", "class C { fun m(r: IntRange): Int { return r.first } }\n").first()

        val intRange = c.findUniqueMethod("m", 1).parameters().first().parameterizedType().typeInfo()
        val fieldNames = intRange.fields().map { it.name() }.toSet()
        assertTrue(fieldNames.contains("first"), "fields were $fieldNames")
        assertTrue(fieldNames.contains("last"), "fields were $fieldNames")

        // and `r.first` resolves to a field access (not a placeholder)
        val ret = (c.findUniqueMethod("m", 1).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(ret is VariableExpression && ret.variable() is FieldReference)
    }

}
