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
import io.codelaser.maddi.cst.api.element.JavaDoc
import io.codelaser.maddi.cst.api.info.FieldInfo
import io.codelaser.maddi.cst.api.info.Info
import io.codelaser.maddi.cst.api.info.ParameterInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A parameter is a reference target like a function or a property: where a member's text names it, the member's
 * source records it -- in the body, in another parameter's default value, as a named argument at a call site, and in
 * KDoc. A default value's position is a detail of the parameter. A constructor's `val`/`var` parameter is the
 * property: a reference to it names the field.
 */
class ParameterReferenceTest : KotlinScanTestBase() {

    private val source = """
        package r

        /**
         * Adds.
         * @param a the first
         */
        fun add(a: Int, b: Int = a): Int = a + b
        fun use() = add(a = 1) + add(1, b = 2)

        open class C(x: Int, val y: Int) {
            val z = x + 1
            init { println(x) }
        }
        fun make() = C(x = 1, y = 2)
        class D(w: Int) : C(x = w, y = 0)
        """.trimIndent() + "\n"

    private lateinit var types: List<TypeInfo>

    private fun parse() {
        types = KotlinScan(runtime, sourceSet).parse("r/R.kt", source)
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }
    private fun facadeMethod(name: String) = type("RKt").methods().first { it.name() == name }

    /** `line:column` of every reference [host] records to [target]. */
    private fun references(host: Info, target: Info): List<String> =
        host.source().detailedSources()?.references(target).orEmpty().map { "${it.beginLine()}:${it.beginPos()}" }.sorted()

    @Test
    fun aFunctionParameterIsNamedInItsBodyItsDefaultsAndItsCalls() {
        parse()
        val add = facadeMethod("add")
        val (a, b) = add.parameters()
        assertEquals(listOf("7:26", "7:36"), references(add, a), "`= a` and `a + b`")
        assertEquals(listOf("7:40"), references(add, b))
        val use = facadeMethod("use")
        assertEquals(listOf("8:17"), references(use, a), "the named argument `a = 1`")
        assertEquals(listOf("8:33"), references(use, b))
    }

    @Test
    fun aDefaultValueIsADetailOfItsParameter() {
        parse()
        val (a, b) = facadeMethod("add").parameters()
        assertNull(a.source().detailedSources().detail(DetailedSources.DEFAULT_VALUE))
        val value = b.source().detailedSources().detail(DetailedSources.DEFAULT_VALUE)
        assertEquals(listOf(7, 26, 7, 26), listOf(value.beginLine(), value.beginPos(), value.endLine(), value.endPos()))
    }

    @Test
    fun aParamTagNamesTheParameter() {
        parse()
        val add = facadeMethod("add")
        val tag = add.javaDoc().tags().single()
        assertEquals(JavaDoc.TagIdentifier.PARAM, tag.identifier())
        assertTrue(tag.resolvedReference() === add.parameters()[0], tag.toString())
    }

    @Test
    fun aConstructorParameterIsNamedInInitializersAndCalls() {
        parse()
        val c = type("C")
        val ctor = c.constructors().single()
        val x = ctor.parameters()[0]
        val z = c.fields().single { it.name() == "z" }
        assertEquals(listOf("11:13"), references(z, x), "the property initializer")
        assertEquals(listOf("12:20"), references(ctor, x), "the init block")
        val make = facadeMethod("make")
        assertEquals(listOf("14:16"), references(make, x), "the named argument `x = 1`")
        // `y = 2` names the property y: the val parameter is its declaration
        val y = c.fields().single { it.name() == "y" }
        assertEquals(listOf("14:23"), references(make, y))
        assertTrue(make.source().detailedSources().references(ctor.parameters()[1]).isNullOrEmpty())
        assertTrue(x is ParameterInfo && y is FieldInfo)
    }

    /** A superclass constructor call in a class header is code of the primary constructor: recorded there. */
    @Test
    fun aSuperclassCallInTheHeaderIsThePrimaryConstructors() {
        parse()
        val x = type("C").constructors().single().parameters()[0]
        val d = type("D")
        val dCtor = d.constructors().single()
        assertEquals(listOf("15:21"), references(dCtor, x), "`x = w` in `: C(x = w, y = 0)`")
        assertEquals(listOf("15:25"), references(dCtor, dCtor.parameters()[0]))
        assertEquals(listOf<String>(), references(d, x))
    }
}
