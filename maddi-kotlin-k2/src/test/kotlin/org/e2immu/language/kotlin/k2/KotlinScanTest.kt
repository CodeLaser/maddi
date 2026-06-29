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
import org.e2immu.language.cst.api.expression.InlineConditional
import org.e2immu.language.cst.api.expression.MethodCall
import org.e2immu.language.cst.api.expression.VariableExpression
import org.e2immu.language.cst.api.info.ParameterInfo
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation
import org.e2immu.language.cst.api.statement.ExpressionAsStatement
import org.e2immu.language.cst.api.statement.IfElseStatement
import org.e2immu.language.cst.api.statement.LocalVariableCreation
import org.e2immu.language.cst.api.statement.ReturnStatement
import org.e2immu.language.cst.api.statement.WhileStatement
import org.e2immu.language.cst.api.variable.FieldReference
import org.e2immu.language.cst.api.variable.LocalVariable
import org.e2immu.language.cst.api.variable.This
import org.e2immu.language.cst.api.type.NullableState
import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
}
