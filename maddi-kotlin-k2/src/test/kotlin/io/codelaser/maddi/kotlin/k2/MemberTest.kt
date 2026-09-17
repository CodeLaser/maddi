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

import io.codelaser.maddi.cst.api.element.DetailedSources
import io.codelaser.maddi.cst.api.element.SourceSet
import io.codelaser.maddi.cst.api.info.Variance
import io.codelaser.maddi.cst.api.expression.Assignment
import io.codelaser.maddi.cst.api.expression.BinaryOperator
import io.codelaser.maddi.cst.api.expression.EmptyExpression
import io.codelaser.maddi.cst.api.expression.InlineConditional
import io.codelaser.maddi.cst.api.expression.Lambda
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.expression.StringConcat
import io.codelaser.maddi.cst.api.expression.SwitchExpression
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.ParameterInfo
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.cst.api.statement.BreakStatement
import io.codelaser.maddi.cst.api.statement.DoStatement
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement
import io.codelaser.maddi.cst.api.statement.ForEachStatement
import io.codelaser.maddi.cst.api.statement.IfElseStatement
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import io.codelaser.maddi.cst.api.statement.SwitchStatementNewStyle
import io.codelaser.maddi.cst.api.statement.WhileStatement
import io.codelaser.maddi.cst.api.variable.FieldReference
import io.codelaser.maddi.cst.api.variable.LocalVariable
import io.codelaser.maddi.cst.api.variable.This
import io.codelaser.maddi.cst.api.type.NullableState
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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

        // the field's type reference `Int`, keyed by its TypeInfo (mirroring Java's pt.typeInfo())
        val typeSource = value.source().detailedSources().detail(value.type().typeInfo())
        assertNotNull(typeSource)
        assertEquals(2, typeSource.beginLine())
        assertEquals(16, typeSource.beginPos())
        assertEquals(18, typeSource.endPos())
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

        // the parameter name, keyed by parameterInfo.name() (mirroring the Java parser)
        val nameSource = parameter.source().detailedSources().detail(parameter.name())
        assertNotNull(nameSource)
        assertEquals(11, nameSource.beginPos())
        assertEquals(15, nameSource.endPos()) // `count`
    }

    @Test
    fun genericTypeArgumentDetailedSources() {
        val scan = KotlinScan(runtime, sourceSet)
        val c = scan.parse(
            "C.kt",
            "class Box<A, B>\n" +
                "class X\n" +
                "class Y\n" +
                "class C {\n" +
                "    fun m(p: Box<X, Y>) {}\n" + // Box 14..16, X at 18, comma 19, Y at 21
                "}\n"
        ).associateBy { it.simpleName() }.getValue("C")

        val type = c.findUniqueMethod("m", 1).parameters().first().parameterizedType()
        val ds = c.findUniqueMethod("m", 1).parameters().first().source().detailedSources()

        // outer Box keyed by its TypeInfo
        val outer = ds.detail(type.typeInfo())
        assertNotNull(outer)
        assertEquals(14, outer.beginPos())
        assertEquals(16, outer.endPos())

        // type-argument commas (the comma between X and Y), keyed by the shared marker
        val commas = ds.details(DetailedSources.TYPE_ARGUMENT_COMMAS)
        assertEquals(1, commas.size)
        assertEquals(19, commas.first().beginPos())

        // nested type arguments X and Y, each keyed by its own TypeInfo
        assertEquals(18, ds.detail(type.parameters()[0].typeInfo()).beginPos())
        assertEquals(21, ds.detail(type.parameters()[1].typeInfo()).beginPos())
    }

    @Test
    fun forwardConstructorReference() {
        // a method body that constructs a type declared LATER in the file (member structures for all types
        // are registered before any body is converted)
        val types = KotlinScan(runtime, sourceSet).parse(
            "F.kt",
            "class Factory { fun make(): Widget = Widget(1) }\n" +
                "class Widget(val id: Int)\n"
        ).associateBy { it.simpleName() }
        val expr = (types.getValue("Factory").findUniqueMethod("make", 0)
            .methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(expr is io.codelaser.maddi.cst.api.expression.ConstructorCall)
        assertEquals("Widget", (expr as io.codelaser.maddi.cst.api.expression.ConstructorCall).constructor().typeInfo().simpleName())
    }

    @Test
    fun innerClassAccessesOuterField() {
        // `inner class` body reads an outer field: `label` -> `Outer.this.label` (a field ref scoped by
        // the enclosing instance)
        val outer = KotlinScan(runtime, sourceSet).parse(
            "O.kt",
            "class Outer(private val label: String) {\n" +
                "    inner class Inner { fun show(): String = label }\n" +
                "}\n"
        ).first()
        val inner = outer.subTypes().first { it.simpleName() == "Inner" }
        val expr = (inner.findUniqueMethod("show", 0).methodBody().statements().first() as ReturnStatement).expression()
        val field = (expr as VariableExpression).variable() as FieldReference
        assertEquals("label", field.fieldInfo().name())
        val scope = (field.scope() as VariableExpression).variable() as This
        assertEquals(outer, scope.typeInfo()) // Outer.this
    }

    @Test
    fun modifierDetailedSources() {
        // each EXPLICIT modifier keyword's position is recorded, keyed by the runtime modifier singleton
        // (mirroring java-openjdk's attachModifiers). Implicit modifiers (default public/final) get no source.
        val scan = KotlinScan(runtime, sourceSet)
        val c = scan.parse(
            "C.kt",
            "package a.b\n" +
                "abstract class C {\n" +                       // line 2: `abstract` 1..8, `class` 10..14
                "    private val f: Int = 3\n" +               // line 3: `private` 5..11
                "    protected abstract fun m(p: Int)\n" +     // line 4: `protected` 5..13, `abstract` 15..22
                "}\n"
        ).first()

        // type modifier: `abstract`
        val tds = c.source().detailedSources()
        val abstractT = tds.detail(runtime.typeModifierAbstract())
        assertEquals(2, abstractT.beginLine()); assertEquals(1, abstractT.beginPos()); assertEquals(8, abstractT.endPos())

        // field modifier: `private`
        val fds = c.fields().first { it.name() == "f" }.source().detailedSources()
        val privateF = fds.detail(runtime.fieldModifierPrivate())
        assertEquals(3, privateF.beginLine()); assertEquals(5, privateF.beginPos()); assertEquals(11, privateF.endPos())

        // method modifiers: `protected` + `abstract`
        val mds = c.findUniqueMethod("m", 1).source().detailedSources()
        val protectedM = mds.detail(runtime.methodModifierProtected())
        assertEquals(4, protectedM.beginLine()); assertEquals(5, protectedM.beginPos()); assertEquals(13, protectedM.endPos())
        val abstractM = mds.detail(runtime.methodModifierAbstract())
        assertEquals(15, abstractM.beginPos()); assertEquals(22, abstractM.endPos())
    }

    @Test
    fun varargParameter() {
        // `vararg xs: Int` -> an int[] parameter flagged varargs (K2's element type is arrayified)
        val v = KotlinScan(runtime, sourceSet).parse("V.kt", "class V { fun f(vararg xs: Int) {} }\n").first()
        val p = v.findUniqueMethod("f", 1).parameters().first()
        assertTrue(p.isVarArgs)
        assertEquals(1, p.parameterizedType().arrays())
    }

    /**
     * A method calls a sibling declared after it, in a class and in a companion: every signature exists before any
     * body. K2's `declarations` is a lazy Sequence, and mapping it without `toList()` converted each body right after
     * its own signature, so `b()` below was a `k2-unresolved-call` placeholder.
     */
    @Test
    fun aBodyCallsASiblingDeclaredAfterIt() {
        val types = KotlinScan(runtime, sourceSet).parse("x/X.kt",
            "package x\nclass X {\n fun a() = b()\n fun b() = 1\n companion object {\n fun c() = d()\n fun d() = 2\n }\n}\n")
        val all = types.flatMap { it.recursiveSubTypeStream().toList() }
        for ((caller, callee) in listOf("a" to "b", "c" to "d")) {
            val method = all.flatMap { it.methods() }.single { it.name() == caller }
            val call = (method.methodBody().statements().single() as ReturnStatement).expression()
            assertTrue(call is MethodCall, "$caller: $call")
            assertEquals(callee, (call as MethodCall).methodInfo().name())
        }
    }

    /**
     * A `var` without a backing field has a setter too (#36): an interface's abstract one, and a computed one whose
     * written `set(value) { … }` is converted. Only the getter used to be built, so an assignment to such a property
     * had no target and a written setter's body was code nothing saw.
     */
    @Test
    fun aVarWithoutABackingFieldHasASetter() {
        val types = KotlinScan(runtime, sourceSet).parse("v/V.kt",
            "package v\n" +
                "interface I {\n" +
                "    var v: Int\n" +
                "    val r: Int\n" +
                "}\n" +
                "class C {\n" +
                "    private var store = 0\n" +
                "    var computed: Int\n" +
                "        get() = store\n" +
                "        set(value) { store = value + 1 }\n" +
                "}\n")
        val all = types.flatMap { it.recursiveSubTypeStream().toList() }
        val i = all.single { it.simpleName() == "I" }
        assertEquals(listOf("getR", "getV", "setV"), i.methods().map { it.name() }.sorted())
        assertTrue(i.methods().single { it.name() == "setV" }.isAbstract, "an interface's var")
        val setV = i.methods().single { it.name() == "setV" }
        assertEquals(listOf("value"), setV.parameters().map { it.name() })
        assertEquals(runtime.intParameterizedType(), setV.parameters().first().parameterizedType())

        val c = all.single { it.simpleName() == "C" }
        val setComputed = c.methods().single { it.name() == "setComputed" }
        assertFalse(setComputed.isAbstract)
        assertEquals(listOf("value"), setComputed.parameters().map { it.name() })
        // the written body is converted: `store = value + 1`
        val statement = setComputed.methodBody().statements().single()
        assertTrue(statement is ExpressionAsStatement, statement.toString())
        assertTrue((statement as ExpressionAsStatement).expression() is Assignment, statement.expression().toString())
    }

    /**
     * Assigning to a property without a backing field is a call of its setter, as kotlinc compiles it. The read is
     * a call of the getter, so the assignment used to have no target at all and became a placeholder (#36).
     */
    @Test
    fun anAssignmentToAPropertyWithoutABackingFieldCallsItsSetter() {
        val types = KotlinScan(runtime, sourceSet).parse("t/T.kt",
            "package t\n" +
                "class C {\n" +
                "    private var store = 0\n" +
                "    var computed: Int\n" +
                "        get() = store\n" +
                "        set(value) { store = value }\n" +
                "}\n" +
                "interface I { var v: Int }\n" +
                "fun use(c: C, i: I) {\n" +
                "    c.computed = 7\n" +
                "    i.v = 8\n" +
                "}\n")
        val use = types.flatMap { it.recursiveSubTypeStream().toList() }
            .flatMap { it.methods() }.single { it.name() == "use" }
        val calls = use.methodBody().statements().map { (it as ExpressionAsStatement).expression() }
        assertEquals(listOf("setComputed", "setV"), calls.map { (it as MethodCall).methodInfo().name() },
            calls.toString())
        assertEquals(listOf(1, 1), calls.map { (it as MethodCall).parameterExpressions().size })
    }

    /**
     * A declaration without a body is an ABSTRACT method, as in the Java front ends; an interface member with one is
     * a `default` method, which is what kotlinc compiles it to. Abstractness used to be the `abstract` modifier only,
     * so `isAbstract()` was false for every Kotlin interface member and prep registered no implementations (#35).
     */
    @Test
    fun anAbstractFunctionIsAnAbstractMethod() {
        val types = KotlinScan(runtime, sourceSet).parse("z/Z.kt",
            "package z\n" +
                "interface I {\n" +
                "    fun f(): Int\n" +                        // abstract
                "    fun withBody(): Int = 1\n" +             // default
                "    val v: Int\n" +                          // abstract getter
                "}\n" +
                "abstract class A {\n" +
                "    abstract fun g(): Int\n" +               // abstract
                "    fun h(): Int = 2\n" +                    // a plain method
                "}\n" +
                "class C : I {\n" +
                "    override fun f(): Int = 3\n" +
                "    override val v: Int = 4\n" +
                "}\n")
        val all = types.flatMap { it.recursiveSubTypeStream().toList() }
        fun method(type: String, name: String) =
            all.single { it.simpleName() == type }.methods().single { it.name() == name }

        assertTrue(method("I", "f").isAbstract, "an interface member without a body")
        assertTrue(method("I", "getV").isAbstract, "the getter of an abstract property")
        assertTrue(method("A", "g").isAbstract, "an abstract function of an abstract class")
        assertTrue(method("I", "withBody").isDefault, "an interface member with a body")
        assertFalse(method("A", "h").isAbstract, "a plain method")
        assertFalse(method("C", "f").isAbstract, "an implementation")

        // what prep reads to register an implementation: overrides(), filtered on isAbstract()
        assertEquals(listOf(method("I", "f")), method("C", "f").overrides().toList())
        assertTrue(method("C", "f").overrides().all { it.isAbstract })
    }

    /**
     * Kotlin's `override` is a modifier, where Java has an annotation, so the CST has no modifier object to key it
     * by: its position sits on the member's own source under [DetailedSources.OVERRIDE]. A rename that takes one
     * member out of its override family has to remove it.
     */
    @Test
    fun theOverrideKeywordHasAPosition() {
        val types = KotlinScan(runtime, sourceSet).parse("y/Y.kt",
            "package y\n" +                                    // 1
                "interface I {\n" +                            // 2
                "    fun f(): Int\n" +                         // 3
                "    val v: Int\n" +                           // 4
                "}\n" +                                        // 5
                "class C : I {\n" +                            // 6
                "    override fun f(): Int = 1\n" +            // 7, `override` at cols 5..12
                "    override val v: Int = 2\n" +              // 8, `override` at cols 5..12
                "    fun g(): Int = 3\n" +                     // 9, no override
                "}\n")
        val c = types.single { it.simpleName() == "C" }
        val f = c.methods().single { it.name() == "f" }
        val override = f.source().detailedSources().detail(DetailedSources.OVERRIDE)
        assertNotNull(override)
        assertEquals(7, override.beginLine())
        assertEquals(5, override.beginPos())
        assertEquals(12, override.endPos())

        val v = c.fields().single { it.name() == "v" }
        val propertyOverride = v.source().detailedSources().detail(DetailedSources.OVERRIDE)
        assertNotNull(propertyOverride)
        assertEquals(8, propertyOverride.beginLine())

        val g = c.methods().single { it.name() == "g" }
        assertNull(g.source().detailedSources().detail(DetailedSources.OVERRIDE))
    }
}
