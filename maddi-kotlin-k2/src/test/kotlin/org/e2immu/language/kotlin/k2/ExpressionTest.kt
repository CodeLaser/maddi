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

import org.e2immu.language.cst.api.element.DetailedSources
import org.e2immu.language.cst.api.element.SourceSet
import org.e2immu.language.cst.api.info.Variance
import org.e2immu.language.cst.api.expression.Assignment
import org.e2immu.language.cst.api.expression.BinaryOperator
import org.e2immu.language.cst.api.expression.ConstructorCall
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

/** Expression/statement bodies: references, operators, calls, locals, lambdas, overloads, operator/infix functions. */
class ExpressionTest : KotlinScanTestBase() {

    @Test
    fun bodies() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "Bodies.kt",
            """
            class Bodies {
                fun one(): Int = 1
                fun two(): Int { return 2 }
                fun hi(): String = "hi"
                fun yes(): Boolean = true
            }
            """.trimIndent() + "\n"
        )
        val b = types.first()

        // expression body `= 1` -> { return 1; }
        val one = b.findUniqueMethod("one", 0).methodBody()
        assertEquals(1, one.statements().size)
        assertEquals(runtime.newInt(1), (one.statements()[0] as ReturnStatement).expression())

        // block body `{ return 2 }`
        val two = b.findUniqueMethod("two", 0).methodBody()
        assertEquals(runtime.newInt(2), (two.statements()[0] as ReturnStatement).expression())

        // string + boolean literals
        val hi = b.findUniqueMethod("hi", 0).methodBody()
        assertEquals(runtime.newStringConstant("hi"), (hi.statements()[0] as ReturnStatement).expression())
        val yes = b.findUniqueMethod("yes", 0).methodBody()
        assertEquals(runtime.newBoolean(true), (yes.statements()[0] as ReturnStatement).expression())
    }


    @Test
    fun expressionBodyReferences() {
        val scan = KotlinScan(runtime, sourceSet)
        val holder = scan.parse(
            "R.kt",
            """
            class Holder(val v: Int) {
                fun id(p: Int): Int = p
                fun self(): Holder = this
                fun read(): Int = v
            }
            """.trimIndent() + "\n"
        ).first()

        fun returnedExpr(name: String, n: Int) =
            (holder.findUniqueMethod(name, n).methodBody().statements().first() as ReturnStatement).expression()

        // parameter reference: `= p`
        val p = (returnedExpr("id", 1) as VariableExpression).variable()
        assertTrue(p is ParameterInfo)
        assertEquals("p", (p as ParameterInfo).name())

        // `this`
        assertTrue((returnedExpr("self", 0) as VariableExpression).variable() is This)

        // field/property reference: `= v`
        val v = (returnedExpr("read", 0) as VariableExpression).variable()
        assertTrue(v is FieldReference)
        assertEquals("v", (v as FieldReference).fieldInfo().name())
    }


    @Test
    fun binaryOperators() {
        val scan = KotlinScan(runtime, sourceSet)
        val op = scan.parse(
            "Op.kt",
            """
            class Op {
                fun add(a: Int, b: Int): Int = a + b
                fun lt(a: Int, b: Int): Boolean = a < b
            }
            """.trimIndent() + "\n"
        ).first()

        fun returnedExpr(name: String) =
            (op.findUniqueMethod(name, 2).methodBody().statements().first() as ReturnStatement).expression()

        // a + b -> BinaryOperator whose operator is the Runtime's plusOperatorInt, result int
        val add = returnedExpr("add")
        assertTrue(add is BinaryOperator)
        assertEquals(runtime.plusOperatorInt(), (add as BinaryOperator).operator())
        assertEquals(runtime.intParameterizedType(), add.parameterizedType())
        assertTrue(add.lhs() is VariableExpression && add.rhs() is VariableExpression)

        // a < b -> lessOperatorInt, result boolean
        val lt = returnedExpr("lt") as BinaryOperator
        assertEquals(runtime.lessOperatorInt(), lt.operator())
        assertEquals(runtime.booleanParameterizedType(), lt.parameterizedType())
    }


    @Test
    fun methodCallsAndQualifiedAccess() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "Calls.kt",
            """
            class Calc {
                fun base(): Int = 1
                fun viaThis(): Int = base()
            }
            class Client { fun use(c: Calc): Int = c.base() }
            class Box(val v: Int) { fun peek(other: Box): Int = other.v }
            """.trimIndent() + "\n"
        ).associateBy { it.simpleName() }

        fun returned(t: String, name: String, n: Int) =
            (types.getValue(t).findUniqueMethod(name, n).methodBody().statements().first() as ReturnStatement).expression()

        // implicit-this call: base()
        val viaThis = returned("Calc", "viaThis", 0)
        assertTrue(viaThis is MethodCall)
        assertTrue((viaThis as MethodCall).objectIsImplicit())
        assertEquals("base", viaThis.methodInfo().name())

        // receiver call: c.base()
        val use = returned("Client", "use", 1) as MethodCall
        assertFalse(use.objectIsImplicit())
        assertEquals(types.getValue("Calc"), use.methodInfo().typeInfo())

        // qualified property access: other.v -> field reference
        val peek = returned("Box", "peek", 1)
        assertTrue((peek as VariableExpression).variable() is FieldReference)
        assertEquals("v", (peek.variable() as FieldReference).fieldInfo().name())
    }


    @Test
    fun localsAndAssignment() {
        val scan = KotlinScan(runtime, sourceSet)
        val counter = scan.parse(
            "Counter.kt",
            """
            class Counter {
                var count: Int = 0
                fun bump(): Int {
                    val step = 1
                    count = count + step
                    return count
                }
            }
            """.trimIndent() + "\n"
        ).first()
        val body = counter.findUniqueMethod("bump", 0).methodBody()
        assertEquals(3, body.statements().size)

        // local `val step = 1`
        val lvc = body.statements()[0]
        assertTrue(lvc is LocalVariableCreation)
        assertEquals("step", (lvc as LocalVariableCreation).localVariable().simpleName())
        assertEquals(runtime.intParameterizedType(), lvc.localVariable().parameterizedType())

        // `count = count + step` -> assignment to the field, rhs references the local
        val assign = ((body.statements()[1] as ExpressionAsStatement).expression()) as Assignment
        assertTrue(assign.variableTarget() is FieldReference) // count
        val rhs = assign.value() as BinaryOperator             // count + step
        assertTrue((rhs.rhs() as VariableExpression).variable() is LocalVariable) // step resolves to the local
    }


    @Test
    fun residueBatch() {
        val scan = KotlinScan(runtime, sourceSet)
        val more = scan.parse(
            "More.kt",
            """
            class More {
                fun add(a: Int): Int { var s = 0; s += a; return s }
                fun greet(name: String): String = "Hi ${'$'}name"
                fun stop() { while (true) { break } }
                fun spin() { do { } while (false) }
            }
            """.trimIndent() + "\n"
        ).first()

        // augmented assignment `s += a` -> Assignment with the assign-plus operator method
        val sPlus = (more.findUniqueMethod("add", 1).methodBody().statements()[1] as ExpressionAsStatement)
            .expression() as Assignment
        assertEquals(runtime.assignPlusOperatorInt(), sPlus.assignmentOperator())

        // string template "Hi $name" -> StringConcat
        val greet = (more.findUniqueMethod("greet", 1).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(greet is StringConcat)

        // break inside a while loop
        val whileStmt = more.findUniqueMethod("stop", 0).methodBody().statements().first() as WhileStatement
        assertTrue(whileStmt.block().statements().first() is BreakStatement)

        // do-while
        assertTrue(more.findUniqueMethod("spin", 0).methodBody().statements().first() is DoStatement)
    }


    @Test
    fun lambda() {
        val scan = KotlinScan(runtime, sourceSet)
        // the lambda captures the enclosing method parameter `factor`
        val lam = scan.parse(
            "Lam.kt",
            "class Lam { fun scale(factor: Int): (Int) -> Int = { x -> x * factor } }\n"
        ).first()

        val ret = (lam.findUniqueMethod("scale", 1).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(ret is Lambda)
        // the SAM `invoke(x: Int): Int`
        val sam = (ret as Lambda).methodInfo()
        assertEquals("invoke", sam.name())
        assertEquals("x", sam.parameters().single().name())
        assertEquals(runtime.intParameterizedType(), sam.returnType())

        // body `return x * factor`: x is the lambda parameter, factor is captured from the enclosing method
        val mul = (sam.methodBody().statements().first() as ReturnStatement).expression() as BinaryOperator
        assertEquals("x", ((mul.lhs() as VariableExpression).variable() as ParameterInfo).name())
        assertEquals("factor", ((mul.rhs() as VariableExpression).variable() as ParameterInfo).name())
    }


    @Test
    fun inheritedCall() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "Inh.kt",
            """
            open class Base { fun greet(): String = "hi" }
            class Sub : Base() { fun delegate(): String = greet() }
            """.trimIndent() + "\n"
        ).associateBy { it.simpleName() }

        // greet() is inherited from Base; the call resolves up the hierarchy to Base.greet
        val call = (types.getValue("Sub").findUniqueMethod("delegate", 0)
            .methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(call is MethodCall)
        assertEquals("greet", (call as MethodCall).methodInfo().name())
        assertEquals(types.getValue("Base"), call.methodInfo().typeInfo())
    }


    @Test
    fun operatorFunctionAndInfix() {
        val scan = KotlinScan(runtime, sourceSet)
        val v = scan.parse(
            "V.kt",
            """
            class V(val x: Int) {
                operator fun plus(o: V): V = V(x + o.x)
                infix fun upTo(o: V): Int = o.x - x
                fun addOp(a: V, b: V): V = a + b
                fun infixCall(a: V, b: V): Int = a upTo b
            }
            """.trimIndent() + "\n"
        ).first()

        // `a + b` on objects -> a.plus(b)
        val plus = (v.findUniqueMethod("addOp", 2).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(plus is MethodCall)
        assertEquals("plus", (plus as MethodCall).methodInfo().name())

        // `a upTo b` (infix) -> a.upTo(b)
        val infix = (v.findUniqueMethod("infixCall", 2).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(infix is MethodCall)
        assertEquals("upTo", (infix as MethodCall).methodInfo().name())
    }


    @Test
    fun overloadDisambiguation() {
        val scan = KotlinScan(runtime, sourceSet)
        val o = scan.parse(
            "O.kt",
            """
            class O {
                fun handle(x: Int): Int = x
                fun handle(x: String): Int = 0
                fun callInt(): Int = handle(1)
                fun callStr(): Int = handle("a")
            }
            """.trimIndent() + "\n"
        ).first()

        // handle(1) resolves to the Int overload, handle("a") to the String overload
        val intCall = (o.findUniqueMethod("callInt", 0).methodBody().statements().first() as ReturnStatement)
            .expression() as MethodCall
        assertEquals(runtime.intParameterizedType(), intCall.methodInfo().parameters().single().parameterizedType())

        val strCall = (o.findUniqueMethod("callStr", 0).methodBody().statements().first() as ReturnStatement)
            .expression() as MethodCall
        assertTrue(strCall.methodInfo().parameters().single().parameterizedType().isJavaLangString)
    }

    @Test
    fun anonymousObjectExpression() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "Anon.kt",
            "interface Greeter { fun greet(): String }\n" +
                "class Factory { fun make(): Greeter = object : Greeter { override fun greet(): String = \"hi\" } }\n"
        ).associateBy { it.simpleName() }

        // `object : Greeter { … }` -> a ConstructorCall of a synthetic anonymous type implementing Greeter
        val expr = (types.getValue("Factory").findUniqueMethod("make", 0)
            .methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(expr is ConstructorCall)
        val anon = (expr as ConstructorCall).anonymousClass()
        assertNotNull(anon)
        assertEquals(1, anon!!.methods().count { it.name() == "greet" })
        assertTrue(anon.interfacesImplemented().any { it.typeInfo() == types.getValue("Greeter") })
    }

    @Test
    fun destructuringDeclaration() {
        val scan = KotlinScan(runtime, sourceSet)
        val d = scan.parse(
            "D.kt",
            "data class Pair2(val first: Int, val second: Int)\n" +
                "class D { fun use(p: Pair2): Int { val (a, b) = p; return a } }\n"
        ).associateBy { it.simpleName() }.getValue("D")

        // `val (a, b) = p` -> one LocalVariableCreation declaring both a and b
        val lvc = d.findUniqueMethod("use", 1).methodBody().statements().first() as LocalVariableCreation
        assertEquals("a", lvc.localVariable().simpleName())
        assertEquals(1, lvc.otherLocalVariables().size)
        assertEquals("b", lvc.otherLocalVariables().first().simpleName())
    }

    @Test
    fun detailedSourcesForCallArguments() {
        val scan = KotlinScan(runtime, sourceSet)
        val c = scan.parse(
            "C.kt",
            "class C {\n" +
                "    fun sum(a: Int, b: Int): Int = a + b\n" +
                "    fun caller(): Int {\n" +
                "        return sum(1, 2)\n" + // comma at col 21, `)` at col 24, whole call at 16..24
                "    }\n" +
                "}\n"
        ).first()

        val call = (c.findUniqueMethod("caller", 0).methodBody().statements().first() as ReturnStatement)
            .expression() as MethodCall
        val ds = call.source().detailedSources()

        // mirrors the Java parser's method-call entries (shared marker singletons -> language-unaware)
        val commas = ds.details(DetailedSources.ARGUMENT_COMMAS)
        assertEquals(1, commas.size)
        assertEquals(4, commas.first().beginLine())
        assertEquals(21, commas.first().beginPos())

        val rightParenthesis = ds.detail(DetailedSources.END_OF_ARGUMENT_LIST)
        assertNotNull(rightParenthesis)
        assertEquals(24, rightParenthesis.beginPos())

        // the call-site method name, keyed by methodInfo.name() (mirroring the Java parser)
        val nameSource = ds.detail(call.methodInfo().name())
        assertNotNull(nameSource)
        assertEquals(16, nameSource.beginPos())
        assertEquals(18, nameSource.endPos()) // `sum`
    }

    @Test
    fun localVariableNameDetailedSource() {
        val scan = KotlinScan(runtime, sourceSet)
        val c = scan.parse(
            "C.kt",
            "class C {\n" +
                "    fun m(): Int {\n" +
                "        val total = 42\n" + // `total` at line 3, cols 13..17
                "        return total\n" +
                "    }\n" +
                "}\n"
        ).first()

        val lvc = c.findUniqueMethod("m", 0).methodBody().statements().first() as LocalVariableCreation
        val local = lvc.localVariable()
        val ds = lvc.source().detailedSources()

        // keyed by both the name String and the LocalVariable element (mirroring the Java parser)
        val byName = ds.detail(local.simpleName())
        assertNotNull(byName)
        assertEquals(3, byName.beginLine())
        assertEquals(13, byName.beginPos())
        assertEquals(17, byName.endPos())
        assertEquals(byName, ds.detail(local))
    }

    @Test
    fun localVariableTypeDetailedSource() {
        val scan = KotlinScan(runtime, sourceSet)
        val c = scan.parse(
            "C.kt",
            "class C {\n" +
                "    fun m() {\n" +
                "        val x: C = this\n" + // `C` at line 3, col 16
                "    }\n" +
                "}\n"
        ).first()

        // an explicit local type reference is detailed too, keyed by its TypeInfo
        val lvc = c.findUniqueMethod("m", 0).methodBody().statements().first() as LocalVariableCreation
        val typeSource = lvc.source().detailedSources().detail(lvc.localVariable().parameterizedType().typeInfo())
        assertNotNull(typeSource)
        assertEquals(3, typeSource.beginLine())
        assertEquals(16, typeSource.beginPos())
        assertEquals(16, typeSource.endPos())
    }

    @Test
    fun sourcePositions() {
        val scan = KotlinScan(runtime, sourceSet)
        val p = scan.parse(
            "P.kt",
            "class P {\n" +
                "    fun m(): Int {\n" +
                "        return 42\n" + // 8 spaces, then `return 42`
                "    }\n" +
                "}\n"
        ).first()

        // the `return 42` statement carries real positions + its statement index
        val ret = p.findUniqueMethod("m", 0).methodBody().statements().first() as ReturnStatement
        assertEquals(3, ret.source().beginLine())
        assertEquals(9, ret.source().beginPos())
        assertEquals("0", ret.source().index())

        // the `42` expression has its own (narrower) position
        val fortyTwo = ret.expression().source()
        assertEquals(3, fortyTwo.beginLine())
        assertEquals(16, fortyTwo.beginPos())
        assertEquals(17, fortyTwo.endPos()) // end column inclusive
    }

    @Test
    fun incrementDecrement() {
        val scan = KotlinScan(runtime, sourceSet)
        val inc = scan.parse("Inc.kt", "class Inc { fun m(): Int { var x = 0; x++; --x; return x } }\n").first()
        val statements = inc.findUniqueMethod("m", 0).methodBody().statements()

        // x++ -> a postfix-increment Assignment
        val postInc = (statements[1] as ExpressionAsStatement).expression() as Assignment
        assertEquals(false, postInc.prefixPrimitiveOperator())
        assertEquals(runtime.assignPlusOperatorInt(), postInc.assignmentOperator())

        // --x -> a prefix-decrement Assignment
        val preDec = (statements[2] as ExpressionAsStatement).expression() as Assignment
        assertEquals(true, preDec.prefixPrimitiveOperator())
        assertEquals(runtime.assignMinusOperatorInt(), preDec.assignmentOperator())
    }

    @Test
    fun constructorCall() {
        val scan = KotlinScan(runtime, sourceSet)
        val c = scan.parse(
            "C.kt",
            "class Box2(val v: Int)\n" +
                "class C { fun m(): Box2 { return Box2(3) } }\n"
        ).first { it.simpleName() == "C" }

        // `Box2(3)` -> ConstructorCall of the source type's 1-arg constructor
        val expr = (c.findUniqueMethod("m", 0).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(expr is ConstructorCall)
        assertEquals("Box2", (expr as ConstructorCall).constructor().typeInfo().simpleName())
        assertEquals(1, expr.parameterExpressions().size)
    }

    @Test
    fun libraryConstructorCall() {
        val scan = KotlinScan(runtime, sourceSet)
        val c = scan.parse(
            "C.kt",
            "class C { fun m(): StringBuilder { return StringBuilder() } }\n"
        ).first()

        // `StringBuilder()` -> ConstructorCall of the library type's no-arg constructor (constructors loaded)
        val expr = (c.findUniqueMethod("m", 0).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(expr is ConstructorCall)
        assertEquals("StringBuilder", (expr as ConstructorCall).constructor().typeInfo().simpleName())
        assertEquals(0, expr.parameterExpressions().size)
    }

    @Test
    fun superCallTargetsParent() {
        // `super.describe()` -> a MethodCall on `this` (writeSuper) whose callee is the PARENT's method
        val types = KotlinScan(runtime, sourceSet).parse(
            "S.kt",
            "open class Base { open fun describe(): String = \"base\" }\n" +
                "class Sub : Base() { override fun describe(): String = super.describe() }\n"
        ).associateBy { it.simpleName() }
        val body = (types.getValue("Sub").findUniqueMethod("describe", 0)
            .methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(body is MethodCall)
        val call = body as MethodCall
        assertEquals(types.getValue("Base"), call.methodInfo().typeInfo()) // resolves to Base.describe
        val self = (call.`object`() as VariableExpression).variable() as This
        assertTrue(self.writeSuper())
    }

    @Test
    fun namedAndDefaultArguments() {
        // `named(1, c = 5)` with `named(a, b = 10, c = 20)` -> arguments reordered + defaults filled:
        // a=1 (positional), b=10 (default), c=5 (named)
        val types = KotlinScan(runtime, sourceSet).parse(
            "N.kt",
            "fun named(a: Int, b: Int = 10, c: Int = 20): Int = a + b + c\n" +
                "fun call(): Int = named(1, c = 5)\n"
        )
        val call = (types.flatMap { it.methods() }.first { it.name() == "call" }
            .methodBody().statements().first() as ReturnStatement).expression() as MethodCall
        assertEquals(3, call.parameterExpressions().size)
        // a=1, b=10 (default), c=5 -- in declaration order
        assertEquals(listOf("1", "10", "5"), call.parameterExpressions().map { it.toString() })
    }

}
