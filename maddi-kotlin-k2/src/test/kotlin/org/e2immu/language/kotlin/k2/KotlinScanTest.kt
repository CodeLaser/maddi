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
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.cst.api.statement.ReturnStatement
import org.e2immu.language.cst.api.type.NullableState
import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
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
    }
}
