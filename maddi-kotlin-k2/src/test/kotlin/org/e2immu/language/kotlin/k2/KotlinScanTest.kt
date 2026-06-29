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
class KotlinScanTest {

    private val runtime: Runtime = RuntimeImpl()
    private val sourceSet: SourceSet =
        SourceSetImpl.Builder().setName("source").setUri(URI.create("file:/")).build()

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
    fun controlFlow() {
        val scan = KotlinScan(runtime, sourceSet)
        val flow = scan.parse(
            "Flow.kt",
            """
            class Flow {
                fun classify(n: Int): Int { if (n > 0) { return 1 } else { return 0 } }
                fun countdown(n: Int): Int { var i = n; while (i > 0) { i = i - 1 }; return i }
                fun sign(n: Int): Int = if (n > 0) 1 else 0
            }
            """.trimIndent() + "\n"
        ).first()

        // if statement, with a relational-operator condition
        val ifStmt = flow.findUniqueMethod("classify", 1).methodBody().statements().first()
        assertTrue(ifStmt is IfElseStatement)
        assertTrue((ifStmt as IfElseStatement).expression() is BinaryOperator)

        // while statement (after the `var i = n` local)
        assertTrue(flow.findUniqueMethod("countdown", 1).methodBody().statements()[1] is WhileStatement)

        // if as an expression -> InlineConditional
        val sign = (flow.findUniqueMethod("sign", 1).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(sign is InlineConditional)
    }

    @Test
    fun forLoop() {
        val scan = KotlinScan(runtime, sourceSet)
        val loop = scan.parse(
            "Loop.kt",
            """
            class Loop {
                fun sum(items: List<Int>): Int {
                    var total = 0
                    for (x in items) { total = total + x }
                    return total
                }
            }
            """.trimIndent() + "\n"
        ).first()

        val forStmt = loop.findUniqueMethod("sum", 1).methodBody().statements()[1] // after `var total = 0`
        assertTrue(forStmt is ForEachStatement)
        // loop variable `x`, and the iterated expression is the parameter `items`
        assertEquals("x", (forStmt as ForEachStatement).initializer().localVariable().simpleName())
        assertTrue(forStmt.expression() is VariableExpression)

        // body `total = total + x`: x resolves to the loop variable
        val assign = (forStmt.block().statements().first() as ExpressionAsStatement).expression() as Assignment
        val x = (assign.value() as BinaryOperator).rhs()
        assertTrue((x as VariableExpression).variable() is LocalVariable)
    }

    @Test
    fun whenStatement() {
        val scan = KotlinScan(runtime, sourceSet)
        val w = scan.parse(
            "W.kt",
            """
            class W {
                fun describe(n: Int): String {
                    var r = "?"
                    when (n) {
                        0 -> r = "zero"
                        1, 2 -> r = "small"
                        else -> r = "other"
                    }
                    return r
                }
            }
            """.trimIndent() + "\n"
        ).first()

        val sw = w.findUniqueMethod("describe", 1).methodBody().statements()[1] // after `var r = "?"`
        assertTrue(sw is SwitchStatementNewStyle)
        assertTrue((sw as SwitchStatementNewStyle).expression() is VariableExpression) // selector is `n`
        assertEquals(3, sw.entries().size)
        assertEquals(2, sw.entries()[1].conditions().size)               // the `1, 2 ->` arm
        assertTrue(sw.entries()[2].conditions()[0] is EmptyExpression)   // the `else` arm
    }

    @Test
    fun whenExpression() {
        val scan = KotlinScan(runtime, sourceSet)
        val g = scan.parse(
            "G.kt",
            """
            class G {
                fun grade(n: Int): String = when (n) {
                    0 -> "zero"
                    1, 2 -> "small"
                    else -> "other"
                }
            }
            """.trimIndent() + "\n"
        ).first()

        // `= when (n) { … }` -> a SwitchExpression with 3 arms
        val grade = (g.findUniqueMethod("grade", 1).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(grade is SwitchExpression)
        assertEquals(3, (grade as SwitchExpression).entries().size)
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
    fun whenIsConditions() {
        val scan = KotlinScan(runtime, sourceSet)
        val m = scan.parse(
            "M.kt",
            """
            class M {
                fun describe(o: Any): String = when (o) {
                    is Int -> "int"
                    is String -> "string"
                    else -> "other"
                }
            }
            """.trimIndent() + "\n"
        ).first()

        val sw = (m.findUniqueMethod("describe", 1).methodBody().statements().first() as ReturnStatement)
            .expression() as SwitchExpression
        assertEquals(3, sw.entries().size)
        // `is Int ->` is a TYPE PATTERN on the entry (modern-Java compatible), not a condition expression
        val entry = sw.entries()[0]
        assertTrue(entry.conditions().isEmpty())
        val pattern = entry.patternVariable()
        assertEquals(runtime.intParameterizedType(), pattern.localVariable().parameterizedType())
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
