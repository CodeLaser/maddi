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

package org.e2immu.language.inspection.mixed

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Robustness coverage for the mixed-language driver beyond the single-type happy path: multiple types in one
 * batch, nested and generic cross-language references, enums, void returns, and inheritance across the boundary.
 * Each asserts the shared-core invariant (one `TypeInfo` instance per type) where it should hold.
 */
class TestMixedHardening {

    /** Two Kotlin types, one Java type referencing BOTH — every stub compiles together, both shares hold. */
    @Test
    fun javaReferencesTwoKotlinTypes() {
        val result = MixedInspector().parse(
            mapOf(
                "A.kt" to "package a.b\nclass A(val id: Int)\n",
                "B.kt" to "package a.b\nclass B(val name: String)\n"
            ),
            mapOf(
                "a.b.Use" to "package a.b;\n" +
                    "public class Use {\n" +
                    "    public A a;\n" +
                    "    public B b;\n" +
                    "}\n"
            )
        )
        val a = result.kotlinTypes.first { it.simpleName() == "A" }
        val b = result.kotlinTypes.first { it.simpleName() == "B" }
        val use = result.javaTypes.first { it.simpleName() == "Use" }
        assertSame(a, use.getFieldByName("a", true).type().typeInfo())
        assertSame(b, use.getFieldByName("b", true).type().typeInfo())
    }

    /** Java references a NESTED Kotlin type `Outer.Inner`. The stub emits nested types statically. */
    @Test
    fun javaReferencesNestedKotlinType() {
        val result = MixedInspector().parse(
            mapOf(
                "Outer.kt" to "package a.b\n" +
                    "class Outer {\n" +
                    "    class Inner(val v: Int)\n" +
                    "}\n"
            ),
            mapOf(
                "a.b.UseNested" to "package a.b;\n" +
                    "public class UseNested {\n" +
                    "    public a.b.Outer.Inner inner;\n" +
                    "}\n"
            )
        )
        val use = result.javaTypes.first { it.simpleName() == "UseNested" }
        val innerType = use.getFieldByName("inner", true).type().typeInfo()
        assertEquals("a.b.Outer.Inner", innerType.fullyQualifiedName())
        // the shared instance is the Kotlin-built Inner (registered as a subtype of Outer)
        val outer = result.kotlinTypes.first { it.simpleName() == "Outer" }
        val kotlinInner = outer.subTypes().first { it.simpleName() == "Inner" }
        assertSame(kotlinInner, innerType)
    }

    /** Java uses a GENERIC Kotlin type `Box<T>` — erased in the stub, still resolves. */
    @Test
    fun javaReferencesGenericKotlinType() {
        val result = MixedInspector().parse(
            mapOf(
                "Box.kt" to "package a.b\nclass Box<T>(val value: T)\n"
            ),
            mapOf(
                "a.b.UseBox" to "package a.b;\n" +
                    "public class UseBox {\n" +
                    "    public a.b.Box<String> box;\n" +
                    "}\n"
            )
        )
        val use = result.javaTypes.first { it.simpleName() == "UseBox" }
        val box = result.kotlinTypes.first { it.simpleName() == "Box" }
        assertSame(box, use.getFieldByName("box", true).type().typeInfo())
    }

    /** Java reads a Kotlin ENUM's constants — needs the stub to emit enum constants. */
    @Test
    fun javaReferencesKotlinEnumConstants() {
        val result = MixedInspector().parse(
            mapOf(
                "Color.kt" to "package a.b\nenum class Color { RED, GREEN, BLUE }\n"
            ),
            mapOf(
                "a.b.UseEnum" to "package a.b;\n" +
                    "public class UseEnum {\n" +
                    "    public a.b.Color c = a.b.Color.RED;\n" +
                    "}\n"
            )
        )
        val use = result.javaTypes.first { it.simpleName() == "UseEnum" }
        val color = result.kotlinTypes.first { it.simpleName() == "Color" }
        assertSame(color, use.getFieldByName("c", true).type().typeInfo())
    }

    /** Java calls a VOID (Unit) Kotlin function as a statement. */
    @Test
    fun javaCallsVoidKotlinFunction() {
        val result = MixedInspector().parse(
            mapOf(
                "Sink.kt" to "package a.b\nclass Sink { fun accept(x: Int) {} }\n"
            ),
            mapOf(
                "a.b.UseSink" to "package a.b;\n" +
                    "public class UseSink {\n" +
                    "    public void go(a.b.Sink s) { s.accept(1); }\n" +
                    "}\n"
            )
        )
        val use = result.javaTypes.first { it.simpleName() == "UseSink" }
        assertTrue(use.methods().any { it.name() == "go" })
    }

    /** Java reads a Kotlin `object` singleton via its `INSTANCE` field. */
    @Test
    fun javaReferencesKotlinObjectInstance() {
        val result = MixedInspector().parse(
            mapOf(
                "Config.kt" to "package a.b\nobject Config { fun name(): String = \"x\" }\n"
            ),
            mapOf(
                "a.b.UseObject" to "package a.b;\n" +
                    "public class UseObject {\n" +
                    "    public String go() { return a.b.Config.INSTANCE.name(); }\n" +
                    "}\n"
            )
        )
        val config = result.kotlinTypes.first { it.simpleName() == "Config" }
        val use = result.javaTypes.first { it.simpleName() == "UseObject" }
        assertTrue(use.methods().any { it.name() == "go" })
        assertTrue(config.fields().any { it.name() == "INSTANCE" }, "object should have an INSTANCE field")
    }

    /** Java calls a Kotlin `companion object` member via the nested `Companion` handle. */
    @Test
    fun javaReferencesKotlinCompanion() {
        val result = MixedInspector().parse(
            mapOf(
                "Factory.kt" to "package a.b\n" +
                    "class Factory private constructor(val id: Int) {\n" +
                    "    companion object {\n" +
                    "        fun create(): Factory = Factory(0)\n" +
                    "    }\n" +
                    "}\n"
            ),
            mapOf(
                "a.b.UseCompanion" to "package a.b;\n" +
                    "public class UseCompanion {\n" +
                    "    public a.b.Factory go() { return a.b.Factory.Companion.create(); }\n" +
                    "}\n"
            )
        )
        val factory = result.kotlinTypes.first { it.simpleName() == "Factory" }
        val use = result.javaTypes.first { it.simpleName() == "UseCompanion" }
        assertSame(factory, use.findUniqueMethod("go", 0).returnType().typeInfo())
    }

    /** Java uses a Kotlin `data class` — property getters (`getX`) must appear in the stub. */
    @Test
    fun javaReferencesKotlinDataClass() {
        val result = MixedInspector().parse(
            mapOf(
                "Point.kt" to "package a.b\ndata class Point(val x: Int, val y: Int)\n"
            ),
            mapOf(
                "a.b.UsePoint" to "package a.b;\n" +
                    "public class UsePoint {\n" +
                    "    public int sum(a.b.Point p) { return p.getX() + p.getY(); }\n" +
                    "}\n"
            )
        )
        val point = result.kotlinTypes.first { it.simpleName() == "Point" }
        val use = result.javaTypes.first { it.simpleName() == "UsePoint" }
        assertSame(point, use.findUniqueMethod("sum", 1).parameters().first().parameterizedType().typeInfo())
    }

    /** Java references a Kotlin `sealed` hierarchy — the sealed base and a subclass. */
    @Test
    fun javaReferencesKotlinSealedHierarchy() {
        val result = MixedInspector().parse(
            mapOf(
                "Shape.kt" to "package a.b\n" +
                    "sealed class Shape\n" +
                    "class Circle(val r: Int) : Shape()\n"
            ),
            mapOf(
                "a.b.UseSealed" to "package a.b;\n" +
                    "public class UseSealed {\n" +
                    "    public a.b.Shape shape;\n" +
                    "    public a.b.Circle circle;\n" +
                    "}\n"
            )
        )
        val shape = result.kotlinTypes.first { it.simpleName() == "Shape" }
        val circle = result.kotlinTypes.first { it.simpleName() == "Circle" }
        val use = result.javaTypes.first { it.simpleName() == "UseSealed" }
        assertSame(shape, use.getFieldByName("shape", true).type().typeInfo())
        assertSame(circle, use.getFieldByName("circle", true).type().typeInfo())
        assertSame(shape, circle.parentClass().typeInfo())
    }

    /** A Java class IMPLEMENTS a Kotlin interface, providing the abstract method. */
    @Test
    fun javaImplementsKotlinInterface() {
        val result = MixedInspector().parse(
            mapOf(
                "Greeter.kt" to "package a.b\ninterface Greeter { fun greet(): String }\n"
            ),
            mapOf(
                "a.b.MyGreeter" to "package a.b;\n" +
                    "public class MyGreeter implements a.b.Greeter {\n" +
                    "    public String greet() { return \"hi\"; }\n" +
                    "}\n"
            )
        )
        val greeter = result.kotlinTypes.first { it.simpleName() == "Greeter" }
        val my = result.javaTypes.first { it.simpleName() == "MyGreeter" }
        assertSame(greeter, my.interfacesImplemented().first().typeInfo())
    }

    /** A Java class implements a Kotlin interface but relies on its DEFAULT method — the stub must emit it as
     *  a `default` method (not abstract), else javac forces the Java class to implement it. */
    @Test
    fun javaReliesOnKotlinInterfaceDefaultMethod() {
        val result = MixedInspector().parse(
            mapOf(
                "Named.kt" to "package a.b\ninterface Named { fun name(): String = \"anon\" }\n"
            ),
            mapOf(
                "a.b.Thing" to "package a.b;\n" +
                    "public class Thing implements a.b.Named {\n" +
                    "    public String describe(a.b.Named n) { return n.name(); }\n" +
                    "}\n"
            )
        )
        val named = result.kotlinTypes.first { it.simpleName() == "Named" }
        val thing = result.javaTypes.first { it.simpleName() == "Thing" }
        assertSame(named, thing.interfacesImplemented().first().typeInfo())
    }

    /** Java calls a Kotlin top-level function via the file facade class (`UtilKt.helper()`). */
    @Test
    fun javaCallsKotlinTopLevelFunction() {
        val result = MixedInspector().parse(
            mapOf(
                "Util.kt" to "package a.b\nfun helper(): Int = 42\n"
            ),
            mapOf(
                "a.b.UseTopLevel" to "package a.b;\n" +
                    "public class UseTopLevel {\n" +
                    "    public int go() { return a.b.UtilKt.helper(); }\n" +
                    "}\n"
            )
        )
        val use = result.javaTypes.first { it.simpleName() == "UseTopLevel" }
        assertTrue(use.methods().any { it.name() == "go" })
        assertTrue(result.kotlinTypes.any { it.simpleName() == "UtilKt" }, "expected a UtilKt facade type")
    }

    /** Java calls a Kotlin extension function via the facade (`ExtKt.shout(receiver)`). */
    @Test
    fun javaCallsKotlinExtensionFunction() {
        val result = MixedInspector().parse(
            mapOf(
                "Ext.kt" to "package a.b\nfun String.shout(): String = this + \"!\"\n"
            ),
            mapOf(
                "a.b.UseExt" to "package a.b;\n" +
                    "public class UseExt {\n" +
                    "    public String go() { return a.b.ExtKt.shout(\"hi\"); }\n" +
                    "}\n"
            )
        )
        val use = result.javaTypes.first { it.simpleName() == "UseExt" }
        assertTrue(use.methods().any { it.name() == "go" })
    }

    /** Java calls a Kotlin `vararg` function — erased to an array parameter in the stub. */
    @Test
    fun javaCallsKotlinVarargFunction() {
        val result = MixedInspector().parse(
            mapOf(
                "Sum.kt" to "package a.b\nclass Sum { fun total(vararg xs: Int): Int = xs.sum() }\n"
            ),
            mapOf(
                "a.b.UseVararg" to "package a.b;\n" +
                    "public class UseVararg {\n" +
                    "    public int go(a.b.Sum s) { return s.total(new int[]{1, 2, 3}); }\n" +
                    "}\n"
            )
        )
        val use = result.javaTypes.first { it.simpleName() == "UseVararg" }
        assertTrue(use.methods().any { it.name() == "go" })
    }

    /** Java calls a Kotlin generic method with a bound (`<T extends Comparable> max`). */
    @Test
    fun javaCallsKotlinBoundedGenericMethod() {
        val result = MixedInspector().parse(
            mapOf(
                "Pick.kt" to "package a.b\n" +
                    "class Pick {\n" +
                    "    fun <T : Comparable<T>> max(a: T, b: T): T = if (a >= b) a else b\n" +
                    "}\n"
            ),
            mapOf(
                "a.b.UseGenericMethod" to "package a.b;\n" +
                    "public class UseGenericMethod {\n" +
                    "    public String go(a.b.Pick p) { return p.max(\"a\", \"b\"); }\n" +
                    "}\n"
            )
        )
        val use = result.javaTypes.first { it.simpleName() == "UseGenericMethod" }
        assertTrue(use.methods().any { it.name() == "go" })
    }

    /** A Java class EXTENDS a Kotlin (open) class across the boundary. */
    @Test
    fun javaExtendsKotlinClass() {
        val result = MixedInspector().parse(
            mapOf(
                "Base.kt" to "package a.b\nopen class Base(val id: Int)\n"
            ),
            mapOf(
                "a.b.Derived" to "package a.b;\n" +
                    "public class Derived extends a.b.Base {\n" +
                    "    public Derived() { super(0); }\n" +
                    "}\n"
            )
        )
        val base = result.kotlinTypes.first { it.simpleName() == "Base" }
        val derived = result.javaTypes.first { it.simpleName() == "Derived" }
        assertSame(base, derived.parentClass().typeInfo())
    }
}
