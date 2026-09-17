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

import io.codelaser.maddi.cst.api.info.Info
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * What the front end records for a PROPERTY: the surface `rename.field` edits.
 *
 * A property with a backing field is a [io.codelaser.maddi.cst.api.info.FieldInfo], and **every** place a
 * member's text spells it records a reference to that field -- a bare name in the class, `obj.x` on a receiver,
 * an assignment target, a named argument for a constructor `val`. Its accessors are synthesized and Kotlin text
 * never spells them, so `getA`/`setB` are referenced nowhere: in pure Kotlin a property rename is the
 * declaration plus its field references, and nothing else.
 *
 * A property WITHOUT a backing field has no field at all. Its references are recorded against the getter
 * instead, spelled as the property name (`c`, `p.c`) rather than as `getC`.
 */
class PropertyReferenceTest : KotlinScanTestBase() {

    private val source = """
        package r

        class P(val a: Int) {
            var b: Int = 1
            val c: Int get() = b + 1
            fun read(): Int = a + b + c
            fun write() { b = 2 }
        }
        fun use(p: P): Int = p.a + p.b + p.c
        fun set(p: P) { p.b = 3 }
        fun make() = P(a = 1)

        class Q {
            var value: String = ""
                get() = field
                set(v) { field = v }
        }
        class T(val who: String) {
            fun greet() = "hello ${'$'}who and ${'$'}{who.length}"
        }
        """.trimIndent() + "\n"

    private lateinit var types: List<TypeInfo>

    private fun parse() {
        types = KotlinScan(runtime, sourceSet).parse("r/R.kt", source)
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == name }

    /** `line:column` of every reference [host] records to [target]. */
    private fun references(host: Info, target: Info): List<String> =
        host.source()?.detailedSources()?.references(target).orEmpty()
            .map { "${it.beginLine()}:${it.beginPos()}" }.sorted()

    @Test
    fun onlyAPropertyWithABackingFieldIsAField() {
        parse()
        val p = type("P")
        assertEquals(listOf("a", "b"), p.fields().map { it.name() }, "`c` is computed: no backing field")
        assertEquals(listOf("<init>", "getA", "getB", "setB", "getC", "read", "write"),
                p.constructorAndMethodStream().toList().map { it.name() })
    }

    @Test
    fun everyPlaceTheTextSpellsAPropertyNamesItsField() {
        parse()
        val p = type("P")
        val facade = type("RKt")
        val a = p.fields().single { it.name() == "a" }
        val b = p.fields().single { it.name() == "b" }
        fun method(owner: TypeInfo, name: String) = owner.constructorAndMethodStream().toList()
            .single { it.name() == name }

        // the key is the stored String INSTANCE, not an equal one: `detail("a")` misses where
        // `detail(a.name())` hits. A literal here would look like an unrecorded position.
        assertNotNull(a.source().detailedSources().detail(a.name()), "the `val a` declaration's own name")
        assertEquals("3:13", a.source().detailedSources().detail(a.name()).let { "${it.beginLine()}:${it.beginPos()}" })
        assertEquals("4:9", b.source().detailedSources().detail(b.name()).let { "${it.beginLine()}:${it.beginPos()}" })

        assertEquals(listOf("6:23"), references(method(p, "read"), a), "the bare `a`")
        assertEquals(listOf("9:24"), references(method(facade, "use"), a), "`p.a`")
        assertEquals(listOf("11:16"), references(method(facade, "make"), a), "the named argument `a = 1`")

        assertEquals(listOf("5:24"), references(method(p, "getC"), b), "`b + 1` in the computed getter")
        assertEquals(listOf("6:27"), references(method(p, "read"), b))
        assertEquals(listOf("7:19"), references(method(p, "write"), b), "the assignment target `b = 2`")
        assertEquals(listOf("9:30"), references(method(facade, "use"), b), "`p.b`")
        assertEquals(listOf("10:19"), references(method(facade, "set"), b), "`p.b = 3`")
    }

    /** Kotlin text never spells a synthesized accessor, so nothing refers to one: only a Java caller would. */
    @Test
    fun aSynthesizedAccessorIsReferencedNowhere() {
        parse()
        val p = type("P")
        val hosts: List<Info> = listOf(p) + p.fields() + p.constructorAndMethodStream().toList() +
                type("RKt").methods().toList()
        for (name in listOf("getA", "getB", "setB")) {
            val accessor = p.constructorAndMethodStream().toList().single { it.name() == name }
            assertEquals(listOf<String>(), hosts.flatMap { references(it, accessor) }, name)
        }
    }

    /**
     * `field` inside a written accessor is the KEYWORD for the backing field, not a spelling of the property
     * name. If it recorded a reference, a rename would rewrite the keyword -- and a length check would not
     * catch it for a property whose name is five characters, like this one.
     */
    @Test
    fun theFieldKeywordIsNotAReferenceToTheProperty() {
        parse()
        val q = type("Q")
        val value = q.fields().single { it.name() == "value" }
        val hosts: List<Info> = listOf(q) + q.fields() + q.constructorAndMethodStream().toList()
        val refs = hosts.flatMap { h -> references(h, value).map { "$h@$it" } }
        assertEquals(listOf<String>(), refs, "`field` is the keyword, not a spelling of `value`")
        assertNotNull(value.source().detailedSources().detail(value.name()), "its declaration is still recorded")
    }

    /** A string template's `${'$'}who` is a real reference: a rename that missed it would break the string. */
    @Test
    fun aStringTemplateNamesTheProperty() {
        parse()
        val t = type("T")
        val who = t.fields().single { it.name() == "who" }
        val greet = t.constructorAndMethodStream().toList().single { it.name() == "greet" }
        // both the short form and the braced one, each the bare identifier and nothing around it, so the
        // planner's "spelled as the plain name" length check passes and rewrites exactly the name
        assertEquals(listOf("19:27", "19:37"), references(greet, who))
    }

    /** A property with no backing field: the getter carries the references, spelled as the property name. */
    @Test
    fun aComputedPropertyIsReferencedThroughItsGetter() {
        parse()
        val p = type("P")
        val getC = p.constructorAndMethodStream().toList().single { it.name() == "getC" }
        assertEquals(listOf("6:31"), references(
                p.constructorAndMethodStream().toList().single { it.name() == "read" }, getC), "the bare `c`")
        assertEquals(listOf("9:36"), references(
                type("RKt").methods().toList().single { it.name() == "use" }, getC), "`p.c`")
    }
}
