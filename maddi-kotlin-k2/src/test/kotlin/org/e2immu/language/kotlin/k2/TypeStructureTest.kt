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

    @Test
    fun companionObject() {
        val scan = KotlinScan(runtime, sourceSet)
        val foo = scan.parse(
            "WC.kt",
            "class WithCompanion { companion object { fun create(): Int = 1 } }\n"
        ).first()

        // a nested `Companion` type holds the companion's members (JVM: Outer$Companion)
        val companion = foo.subTypes().single { it.simpleName() == "Companion" }
        assertEquals(1, companion.methods().count { it.name() == "create" })

        // a public static final `Companion` field on the enclosing class, typed as the companion
        val field = foo.fields().single { it.name() == "Companion" }
        assertTrue(field.isStatic)
        assertEquals(companion, field.type().typeInfo())
    }

    @Test
    fun companionCallRouting() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "CC.kt",
            "class WithCompanion { companion object { fun create(): Int = 1 } }\n" +
                "class Caller { fun make(): Int = WithCompanion.create() }\n"
        ).associateBy { it.simpleName() }

        val create = types.getValue("WithCompanion").subTypes().single { it.simpleName() == "Companion" }
            .methods().single { it.name() == "create" }

        // `WithCompanion.create()` -> WithCompanion.Companion.create()
        val call = (types.getValue("Caller").findUniqueMethod("make", 0)
            .methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(call is MethodCall)
        assertEquals(create, (call as MethodCall).methodInfo())
        // the call's object is the `Companion` singleton field access
        val fieldRef = (call.`object`() as VariableExpression).variable() as FieldReference
        assertEquals("Companion", fieldRef.fieldInfo().name())
    }

    @Test
    fun namedObjectSingleton() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "Obj.kt",
            "object Registry { fun size(): Int = 0 }\n" +
                "class User { fun count(): Int = Registry.size() }\n"
        ).associateBy { it.simpleName() }

        // a named object gets a `public static final INSTANCE` field typed as itself
        val registry = types.getValue("Registry")
        val instance = registry.fields().single { it.name() == "INSTANCE" }
        assertTrue(instance.isStatic)
        assertEquals(registry, instance.type().typeInfo())

        // `Registry.size()` -> Registry.INSTANCE.size()
        val call = (types.getValue("User").findUniqueMethod("count", 0)
            .methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(call is MethodCall)
        assertEquals("size", (call as MethodCall).methodInfo().name())
        val fieldRef = (call.`object`() as VariableExpression).variable() as FieldReference
        assertEquals("INSTANCE", fieldRef.fieldInfo().name())
    }

    @Test
    fun companionJvmStaticAndConst() {
        val scan = KotlinScan(runtime, sourceSet)
        val config = scan.parse(
            "Config.kt",
            "class Config { companion object { const val MAX = 10\n" +
                "@JvmStatic fun reset(): Int = 0 } }\n"
        ).first()

        // `const val` is surfaced as a static final field on the enclosing class
        val max = config.fields().single { it.name() == "MAX" }
        assertTrue(max.isStatic)

        // `@JvmStatic fun` is surfaced as a static forwarder method on the enclosing class
        val reset = config.findUniqueMethod("reset", 0)
        assertTrue(reset.isStatic)
    }

    @Test
    fun declarationNameDetailedSources() {
        val scan = KotlinScan(runtime, sourceSet)
        val widget = scan.parse(
            "Widget.kt",
            "class Widget {\n" +       // `Widget` at line 1, cols 7..12
                "    fun render(): Int = 1\n" + // `render` at line 2, cols 9..14
                "}\n"
        ).first()

        // the type's name (keyed by its own simple-name String, mirroring Java: detail(typeInfo.simpleName()))
        val typeName = widget.source().detailedSources().detail(widget.simpleName())
        assertNotNull(typeName)
        assertEquals(1, typeName.beginLine())
        assertEquals(7, typeName.beginPos())
        assertEquals(12, typeName.endPos())

        // the `class` nature keyword, keyed by the shared TypeNature object (mirroring Java)
        val natureKeyword = widget.source().detailedSources().detail(widget.typeNature())
        assertNotNull(natureKeyword)
        assertEquals(1, natureKeyword.beginLine())
        assertEquals(1, natureKeyword.beginPos())
        assertEquals(5, natureKeyword.endPos()) // `class`

        // the method's name (keyed by the name String the Info holds, mirroring Java: detail(info.name()))
        val render = widget.findUniqueMethod("render", 0)
        val methodName = render.source().detailedSources().detail(render.name())
        assertNotNull(methodName)
        assertEquals(2, methodName.beginLine())
        assertEquals(9, methodName.beginPos())
        assertEquals(14, methodName.endPos())

        // the return-type reference `Int`, keyed by its TypeInfo (mirroring Java's pt.typeInfo())
        val returnType = render.source().detailedSources().detail(render.returnType().typeInfo())
        assertNotNull(returnType)
        assertEquals(2, returnType.beginLine())
        assertEquals(19, returnType.beginPos())
        assertEquals(21, returnType.endPos())
    }

    @Test
    fun supertypeDetailedSources() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "C.kt",
            "interface Iface\n" +
                "open class Base\n" +
                "class C : Base(), Iface\n" // Base at 11..14, Iface at 19..23
        ).associateBy { it.simpleName() }
        val ds = types.getValue("C").source().detailedSources()

        // each supertype reference keyed by its TypeInfo (mirroring Java)
        val base = ds.detail(types.getValue("Base"))
        assertNotNull(base)
        assertEquals(3, base.beginLine())
        assertEquals(11, base.beginPos())
        assertEquals(14, base.endPos())

        val iface = ds.detail(types.getValue("Iface"))
        assertNotNull(iface)
        assertEquals(19, iface.beginPos())
        assertEquals(23, iface.endPos())
    }

    @Test
    fun compilationUnitPackageDetailedSource() {
        val scan = KotlinScan(runtime, sourceSet)
        val a = scan.parse("A.kt", "package a.b\n\nclass A\n").first() // `a.b` at line 1, cols 9..11
        val cu = a.compilationUnit()

        // package name keyed by the package String (mirroring Java: detail(cu.packageName()))
        val pkg = cu.source().detailedSources().detail(cu.packageName())
        assertNotNull(pkg)
        assertEquals(1, pkg.beginLine())
        assertEquals(9, pkg.beginPos())
        assertEquals(11, pkg.endPos())
    }

    @Test
    fun typeParameterBounds() {
        val scan = KotlinScan(runtime, sourceSet)
        val types = scan.parse(
            "Box.kt",
            "class Box<T : Comparable<T>>(val v: T)\n" +
                "class Plain<X>(val x: X)\n"
        ).associateBy { it.simpleName() }

        // `T : Comparable<T>` -> one bound (Comparable), with a self-referential type argument
        val t = types.getValue("Box").typeParameters().first()
        assertEquals("T", t.simpleName())
        assertEquals(1, t.typeBounds().size)
        val bound = t.typeBounds().first()
        assertEquals("Comparable", bound.typeInfo().simpleName())
        assertEquals(t, bound.parameters().first().typeParameter()) // Comparable<T> -- the same T

        // an unbounded parameter has no bounds (the implicit Object upper bound is filtered out)
        assertEquals(0, types.getValue("Plain").typeParameters().first().typeBounds().size)
    }

    @Test
    fun nestedClass() {
        val types = KotlinScan(runtime, sourceSet).parse(
            "N.kt",
            "class Outer {\n" +
                "    class Nested { fun f(): Int = 1 }\n" +
                "    fun make(): Nested = Nested()\n" +
                "}\n"
        )
        val outer = types.first { it.simpleName() == "Outer" }

        // the nested class is registered as a subtype of Outer, with its members converted
        val nested = outer.subTypes().firstOrNull { it.simpleName() == "Nested" }
        assertNotNull(nested)
        assertEquals("f", nested!!.methods().first().name())

        // a reference to the nested type (return type + constructor call) resolves to that TypeInfo
        assertEquals(nested, outer.findUniqueMethod("make", 0).returnType().typeInfo())
    }

    @Test
    fun enumMembers() {
        val color = KotlinScan(runtime, sourceSet).parse("E.kt", "enum class Color { RED, GREEN, BLUE }\n")
            .first { it.simpleName() == "Color" }

        // entries become public static final fields
        assertTrue(color.fields().map { it.name() }.toSet().containsAll(setOf("RED", "GREEN", "BLUE")))
        // synthetic name()/values()/valueOf() (via the shared EnumSynthetics)
        assertTrue(color.methods().map { it.name() }.toSet().containsAll(setOf("name", "values", "valueOf")))
        // values() returns Color[]
        assertEquals(1, color.findUniqueMethod("values", 0).returnType().arrays())
    }
}
