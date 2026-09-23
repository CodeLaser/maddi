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

import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.cst.api.expression.AnnotationExpression
import io.codelaser.maddi.cst.api.expression.ClassExpression
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.variable.FieldReference
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Kotlin annotations reach the CST element Java would see them on, with values in the shapes the contract reader
 * casts to. ⭐ Placement is K2's, and was measured before this was written: `@get:`/`@set:`/`@field:`/`@setparam:`
 * sit on the accessor/field/parameter symbols, a Java annotation on a class-body property on its backing field, and
 * a Kotlin `PROPERTY`-only one on the property alone, which has no JVM element. Each test asserts the element it
 * lands on AND the neighbours it must not, so "annotations everywhere" cannot pass.
 */
class AnnotationPlacementTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("P.kt", """
            package p
            import io.codelaser.maddi.annotation.NotModified
            import io.codelaser.maddi.annotation.Modified
            import io.codelaser.maddi.annotation.Independent
            import io.codelaser.maddi.annotation.Immutable
            @Target(AnnotationTarget.PROPERTY) annotation class KProp
            enum class Level { LOW, HIGH }
            annotation class Tagged(val kind: java.lang.annotation.ElementType, val level: Level,
                                    val type: kotlin.reflect.KClass<*>, val names: Array<String>, val inner: Deprecated)
            @Immutable(hc = true)
            class P(@NotModified val ctorVal: MutableList<String>, @Independent(hcParameters = [0, 1]) plain: Int,
                    @field:NotModified val ctorField: Int, @get:NotModified val ctorGet: Int) {
                @NotModified val body: MutableList<String> = mutableListOf()
                @KProp val propertyOnly: Int = 0
                @get:NotModified @set:Modified var accessors: Int = 0
                @setparam:NotModified var setParam: Int = 0
                @Modified fun f(@NotModified a: MutableList<String>): Int = a.size
                @Tagged(java.lang.annotation.ElementType.FIELD, Level.HIGH, String::class, ["a", "b"], Deprecated("x")) fun g() {}
            }
            """.trimIndent() + "\n")
    }

    private val p by lazy { types.first { it.simpleName() == "P" } }

    private fun names(e: Element): List<String> = e.annotations().map { it.typeInfo().simpleName() }
    private fun method(name: String): MethodInfo = p.methods().first { it.name() == name }
    private fun annotation(e: Element, simpleName: String): AnnotationExpression =
        e.annotations().first { it.typeInfo().simpleName() == simpleName }

    @Test
    fun aTypeCarriesItsAnnotationWithItsValue() {
        assertEquals(listOf("Immutable"), names(p))
        assertEquals("io.codelaser.maddi.annotation.Immutable", annotation(p, "Immutable").typeInfo().fullyQualifiedName())
        assertTrue(annotation(p, "Immutable").extractBoolean("hc"))
    }

    @Test
    fun aFunctionAndItsParameterCarryTheirOwn() {
        assertEquals(listOf("Modified"), names(method("f")))
        assertEquals(listOf("NotModified"), names(method("f").parameters()[0]))
    }

    /** An int array is what `@Independent(hcParameters = …)` is read as: an `ArrayInitializer` of `IntConstant`s. */
    @Test
    fun aConstructorParameterCarriesAnIntArrayTheReaderCanExtract() {
        val ctor = p.constructors().single { it.parameters().size == 4 }
        val plain = ctor.parameters()[1]
        assertArrayEquals(intArrayOf(0, 1), annotation(plain, "Independent").extractIntArray("hcParameters"))
        assertEquals(listOf("NotModified"), names(ctor.parameters()[0]), "a no-target annotation on a ctor val")
        assertEquals(listOf<String>(), names(ctor.parameters()[2]), "@field: is not the parameter's")
        assertEquals(listOf<String>(), names(ctor.parameters()[3]), "@get: is not the parameter's")
    }

    @Test
    fun useSiteTargetsReachTheirJvmElement() {
        assertEquals(listOf("NotModified"), names(p.getFieldByName("ctorField", true)))
        assertEquals(listOf("NotModified"), names(method("getCtorGet")))
        assertEquals(listOf<String>(), names(p.getFieldByName("ctorGet", true)), "@get: is not the field's")
        assertEquals(listOf("NotModified"), names(method("getAccessors")))
        assertEquals(listOf("Modified"), names(method("setAccessors")))
        assertEquals(listOf<String>(), names(p.getFieldByName("accessors", true)))
        assertEquals(listOf("NotModified"), names(method("setSetParam").parameters()[0]))
        assertEquals(listOf<String>(), names(method("setSetParam")), "@setparam: is not the setter's")
    }

    /** A Java annotation on a class-body property has no PROPERTY target: Kotlin puts it on the field. */
    @Test
    fun aJavaAnnotationOnAPropertyIsOnItsField() {
        assertEquals(listOf("NotModified"), names(p.getFieldByName("body", true)))
        assertEquals(listOf<String>(), names(method("getBody")))
    }

    /** ⛔ The control: a `PROPERTY`-only annotation has no JVM element to sit on, and must appear on none. */
    @Test
    fun aPropertyOnlyAnnotationLandsNowhere() {
        assertEquals(listOf<String>(), names(p.getFieldByName("propertyOnly", true)))
        assertEquals(listOf<String>(), names(method("getPropertyOnly")))
    }

    /**
     * Enum entry, class literal, string array and nested annotation, each in the class-file reader's shape.
     * ⚠ The library enum is a JAVA one: this fixture has no class-file loader, and a Kotlin library enum built from
     * K2 symbols carries no entries as fields (`AnnotationTarget.FIELD` would drop its pair). The shipping pipeline
     * always has the Java front end's loader, where they are fields.
     */
    @Test
    fun everyValueKindHasTheClassFileReadersShape() {
        val tagged = annotation(method("g"), "Tagged")
        val kind = tagged.keyValuePairs().first { it.key() == "kind" }.value()
        assertTrue(kind is VariableExpression && kind.variable() is FieldReference, "enum entry: $kind")
        assertEquals("FIELD", ((kind as VariableExpression).variable() as FieldReference).fieldInfo().name())
        // a SOURCE enum: its entries must already be fields when the annotation naming one is converted
        val level = tagged.keyValuePairs().firstOrNull { it.key() == "level" }?.value()
        assertTrue(level is VariableExpression && (level.variable() as FieldReference).fieldInfo().name() == "HIGH",
            "source enum entry: $level in ${tagged.keyValuePairs().map { it.key() }}")
        val type = tagged.keyValuePairs().first { it.key() == "type" }.value()
        assertTrue(type is ClassExpression, "class literal: $type")
        assertEquals("java.lang.String", (type as ClassExpression).type().typeInfo().fullyQualifiedName())
        assertArrayEquals(arrayOf("a", "b"), tagged.extractStringArray("names"))
        val inner = tagged.keyValuePairs().first { it.key() == "inner" }.value()
        assertTrue(inner is AnnotationExpression && inner.extractString("message", "") == "x", "nested: $inner")
    }
}
