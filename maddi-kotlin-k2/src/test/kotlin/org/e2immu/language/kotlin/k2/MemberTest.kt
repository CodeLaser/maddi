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

/** Members: properties (backing field + tagged accessors, computed) and constructors (+ delegation). */
class MemberTest : KotlinScanTestBase() {

    @Test
    fun propertiesBecomeFieldsAndTaggedAccessors() {
        val scan = KotlinScan(runtime, sourceSet)
        // primary-constructor val + a mutable var
        val point = scan.parse("Point.kt", "class Point(val x: Int, var name: String)\n").first()

        // backing fields, with val -> final
        val fields = point.fields().associateBy { it.name() }
        assertTrue(fields.getValue("x").isFinal)
        assertFalse(fields.getValue("name").isFinal)
        assertEquals(runtime.intParameterizedType(), fields.getValue("x").type())

        // JavaBean-named accessors: getX/getName always, setName only for the var
        val methodNames = point.methods().map { it.name() }.toSet()
        assertTrue(methodNames.containsAll(listOf("getX", "getName", "setName")), "methods were $methodNames")
        assertFalse(methodNames.contains("setX")) // val: no setter

        // harmonization: the accessors are tagged as getter/setter for their field (maddi normalization)
        val getX = point.findUniqueMethod("getX", 0)
        assertEquals(fields.getValue("x"), getX.getSetField().field())
        assertEquals(runtime.intParameterizedType(), getX.returnType())
        val setName = point.findUniqueMethod("setName", 1)
        assertEquals(fields.getValue("name"), setName.getSetField().field())
    }


    @Test
    fun constructors() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "C.kt",
            """
            class Point(val x: Int, var name: String)
            class Multi(val a: Int) { constructor() : this(0) }
            """.trimIndent() + "\n"
        ).associateBy { it.simpleName() }

        // primary constructor: parameters, and a body that assigns the property backing fields
        val pc = types.getValue("Point").findConstructor(2)
        assertEquals(listOf("x", "name"), pc.parameters().map { it.name() })
        assertEquals(runtime.intParameterizedType(), pc.parameters()[0].parameterizedType())
        assertEquals(2, pc.methodBody().statements().size) // this.x = x; this.name = name;

        // primary + secondary constructor both present
        assertEquals(2, types.getValue("Multi").constructors().size)
    }


    @Test
    fun constructorDelegation() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "D.kt",
            """
            open class Base(val x: Int)
            class Sub : Base(5)
            class Multi(val a: Int) { constructor() : this(0) }
            """.trimIndent() + "\n"
        ).associateBy { it.simpleName() }

        // primary super-type call `Sub : Base(5)` -> super(...) targeting Base's constructor
        val subEci = types.getValue("Sub").findConstructor(0).methodBody().statements().first()
        assertTrue(subEci is ExplicitConstructorInvocation)
        assertTrue((subEci as ExplicitConstructorInvocation).isSuper)
        assertEquals(types.getValue("Base"), subEci.methodInfo().typeInfo())

        // secondary `constructor() : this(0)` -> this(...) targeting another Multi constructor
        val multi = types.getValue("Multi")
        val secEci = multi.findConstructor(0).methodBody().statements().first()
        assertTrue(secEci is ExplicitConstructorInvocation)
        assertFalse((secEci as ExplicitConstructorInvocation).isSuper)
        assertEquals(multi, secEci.methodInfo().typeInfo())
    }


    @Test
    fun computedProperty() {
        val scan = KotlinScan(runtime, sourceSet)
        val point = scan.parse(
            "Point.kt",
            """
            class Point(val x: Int, val y: Int) {
                val sum: Int get() = x + y
            }
            """.trimIndent() + "\n"
        ).first()

        // x, y have backing fields; the computed `sum` does NOT
        val fieldNames = point.fields().map { it.name() }.toSet()
        assertTrue(fieldNames.contains("x") && fieldNames.contains("y"), "fields were $fieldNames")
        assertFalse(fieldNames.contains("sum"))

        // getSum() carries the real computed body `return x + y`
        val ret = (point.findUniqueMethod("getSum", 0).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(ret is BinaryOperator)
        assertEquals(runtime.plusOperatorInt(), (ret as BinaryOperator).operator())
    }

    @Test
    fun fieldNameDetailedSource() {
        val scan = KotlinScan(runtime, sourceSet)
        val box = scan.parse(
            "Box.kt",
            "class Box {\n" +
                "    val value: Int = 0\n" + // `value` at line 2, cols 9..13
                "}\n"
        ).first()

        // the backing field's name, keyed by its own name String (mirroring Java's field-decl dsb.put(name))
        val value = box.fields().first { it.name() == "value" }
        val nameSource = value.source().detailedSources().detail(value.name())
        assertNotNull(nameSource)
        assertEquals(2, nameSource.beginLine())
        assertEquals(9, nameSource.beginPos())
        assertEquals(13, nameSource.endPos())
    }

    @Test
    fun parameterTypeDetailedSource() {
        val scan = KotlinScan(runtime, sourceSet)
        val c = scan.parse(
            "C.kt",
            "class C {\n" +
                "    fun m(count: Int) {}\n" + // `Int` at line 2, cols 18..20
                "}\n"
        ).first()

        // the parameter's type reference, keyed by the type's TypeInfo (mirroring Java's pt.typeInfo())
        val parameter = c.findUniqueMethod("m", 1).parameters().first()
        val typeSource = parameter.source().detailedSources().detail(parameter.parameterizedType().typeInfo())
        assertNotNull(typeSource)
        assertEquals(2, typeSource.beginLine())
        assertEquals(18, typeSource.beginPos())
        assertEquals(20, typeSource.endPos())
    }

}
