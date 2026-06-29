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

/** The <FileName>Kt facade: top-level functions/properties and extension functions. */
class FacadeAndExtensionTest : KotlinScanTestBase() {

    @Test
    fun fileFacade() {
        val scan = KotlinScan(runtime, sourceSet)
        // top-level function + property live on the JVM file facade `GreetKt` as static members
        val types = scan.parse(
            "Greet.kt",
            "val version: Int = 1\nfun greet(name: String): String = \"hi\"\n"
        ).associateBy { it.simpleName() }

        val facade = types["GreetKt"]
        assertNotNull(facade, "facade types were ${types.keys}")
        val greet = facade!!.findUniqueMethod("greet", 1)
        assertTrue(greet.isStatic)
        assertTrue(greet.returnType().isJavaLangString)

        // top-level property -> a static backing field + a static getter
        val version = facade.fields().single { it.name() == "version" }
        assertTrue(version.isStatic)
        assertTrue(facade.findUniqueMethod("getVersion", 0).isStatic)
    }


    @Test
    fun topLevelExtensionFunction() {
        val scan = KotlinScan(runtime, sourceSet)
        val ext = scan.parse(
            "Ext.kt",
            "fun String.tag(suffix: String): String = suffix\nfun use(s: String): String = s.tag(\"x\")\n"
        ).associateBy { it.simpleName() }.getValue("ExtKt")

        // declaration: `fun String.tag(...)` -> static facade method with the receiver as the first parameter
        val tag = ext.findUniqueMethod("tag", 2)
        assertTrue(tag.isStatic)
        assertTrue(tag.parameters()[0].parameterizedType().isJavaLangString)
        assertEquals("suffix", tag.parameters()[1].name())

        // call site: `s.tag("x")` -> ExtKt.tag(s, "x") with the receiver as argument 0
        val call = (ext.findUniqueMethod("use", 1).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(call is MethodCall)
        assertEquals(tag, (call as MethodCall).methodInfo())
        assertEquals(2, call.parameterExpressions().size)
        assertTrue((call.parameterExpressions()[0] as VariableExpression).variable() is ParameterInfo) // s
    }


    @Test
    fun extensionUnqualifiedReceiverMember() {
        val scan = KotlinScan(runtime, sourceSet)
        val facade = scan.parse("Box.kt", "class Box(val v: Int)\nfun Box.unwrap(): Int = v\n")
            .associateBy { it.simpleName() }.getValue("BoxKt")

        // `v` (unqualified) in `Box.unwrap` is access to the receiver's field -> $receiver.v
        val ret = (facade.findUniqueMethod("unwrap", 1).methodBody().statements().first() as ReturnStatement).expression()
        val fieldRef = (ret as VariableExpression).variable() as FieldReference
        assertEquals("v", fieldRef.fieldInfo().name())
        assertEquals("\$receiver", (fieldRef.scope() as VariableExpression).variable().simpleName())
    }


    @Test
    fun facadeJvmName() {
        val scan = KotlinScan(runtime, sourceSet)
        // `@file:JvmName` overrides the default `<File>Kt` facade name
        val types = scan.parse("Custom.kt", "@file:JvmName(\"CustomFacade\")\nfun hello(): String = \"hi\"\n")
            .associateBy { it.simpleName() }
        assertNotNull(types["CustomFacade"], "types were ${types.keys}")
        assertTrue(types["CustomFacade"]!!.findUniqueMethod("hello", 0).isStatic)
    }


    @Test
    fun extensionThisIsReceiver() {
        val scan = KotlinScan(runtime, sourceSet)
        val ext = scan.parse("E2.kt", "fun String.echo(): String = this\n")
            .associateBy { it.simpleName() }.getValue("E2Kt")

        // `this` in an extension body resolves to the synthetic receiver parameter
        val ret = (ext.findUniqueMethod("echo", 1).methodBody().statements().first() as ReturnStatement).expression()
        val variable = (ret as VariableExpression).variable() as ParameterInfo
        assertEquals("\$receiver", variable.name())
    }

}
