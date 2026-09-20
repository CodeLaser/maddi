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

import io.codelaser.maddi.cst.api.element.JavaDoc
import io.codelaser.maddi.cst.api.info.Info
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A KDoc is its declaration's [JavaDoc], as a javadoc is a Java declaration's: every name in it that K2 resolves to a
 * project declaration is a tag whose resolved reference is that declaration and whose `sourceOfReference` is the
 * name as spelled -- each segment of a qualified link separately. Links are documentation, not code: they are not
 * in `DetailedSources.references`, which the call graph reads as calls.
 */
class KDocLinkTest : KotlinScanTestBase() {

    private val source = """
        package k

        open class Base {
            /** Greets; see [Base.greet] and [helper]. */
            open fun greet(): String = "b"
        }
        fun helper(): Int = 1
        fun path(s: String): String = s
        fun path(i: Int): String = i.toString()

        /**
         * Uses [Base.greet].
         * @see helper
         * @throws IllegalStateException never
         */
        class User {
            /** Returns [path]. */
            fun p(): String = path("x")
        }
        """.trimIndent() + "\n"

    private lateinit var types: List<TypeInfo>

    private fun parse() {
        types = KotlinScan(runtime, sourceSet).parse("k/K.kt", source)
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }
    private fun method(type: String, name: String) = type(type).methods().first { it.name() == name }

    /** `IDENTIFIER line:column -> target`, sorted. */
    private fun tags(info: Info): List<String> = info.javaDoc().tags().map {
        "${it.identifier()} ${it.sourceOfReference().beginLine()}:${it.sourceOfReference().beginPos()} -> " +
            ((it.resolvedReference() as? Info)?.fullyQualifiedName() ?: "null '${it.content()}'")
    }.sorted()

    @Test
    fun aLinkNamesEachOfItsSegments() {
        parse()
        assertEquals(listOf("LINK 4:22 -> k.Base", "LINK 4:27 -> k.Base.greet()", "LINK 4:39 -> k.KKt.helper()"),
            tags(method("Base", "greet")))
    }

    @Test
    fun aBlockTagsSubjectIsABlockTag() {
        parse()
        val user = type("User")
        assertEquals(listOf("LINK 12:10 -> k.Base", "LINK 12:15 -> k.Base.greet()", "SEE 13:9 -> k.KKt.helper()"),
            tags(user), "@throws names a library class: not a tag")
        assertTrue(user.javaDoc().tags().single { it.identifier() == JavaDoc.TagIdentifier.SEE }.blockTag())
        assertTrue(user.javaDoc().comment().contains("Uses"), user.javaDoc().comment())
    }

    @Test
    fun anOverloadedNameIsATagPerOverload() {
        parse()
        assertEquals(listOf("LINK 17:18 -> k.KKt.path(String)", "LINK 17:18 -> k.KKt.path(int)"),
            tags(method("User", "p")))
    }

    @Test
    fun aLinkIsNotACodeReference() {
        parse()
        val greet = method("Base", "greet")
        assertEquals(listOf<Any>(), type("User").source().detailedSources().references(greet))
        assertEquals(listOf<Any>(), greet.source().detailedSources().references(greet))
    }

    @Test
    fun aTypeLinkInAMultiLineKDoc() {
        // A one-line KDoc and a multi-line one, in the same file, linking the same type. Filed as #46 ("only the
        // one-liner is recorded"); it is not true here and never was -- the consumer read an even doc COUNT as none.
        val t = KotlinScan(runtime, sourceSet).parse(
            "m/M.kt",
            "package m\n\n"
                    + "class Widget\n\n"
                    + "/** Built on top of [Widget]. */\n"
                    + "class A\n\n"
                    + "/**\n"
                    + " * Interface to let the core know that this [Widget] is used.\n"
                    + " *\n"
                    + " * The core only runs it when [Widget] says so.\n"
                    + " */\n"
                    + "class B\n"
        )
        fun type(name: String) = t.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }
        val widget = type("Widget")
        fun links(of: Info) = (of.javaDoc()?.tags() ?: listOf()).filter { it.resolvedReference() == widget }
            .map { "${it.sourceOfReference()?.beginLine()}:${it.sourceOfReference()?.beginPos()}" }.sorted()
        assertEquals(listOf("5:22"), links(type("A")))
        assertEquals(listOf("11:32", "9:46"), links(type("B")))
    }

    @Test
    fun aTypeLinkInAMultiLineKDocInANOTHERFile() {
        // the same, with the linked type in a DIFFERENT file: the KDoc of a type is resolved against the whole
        // source set, not just its own file
        val t = KotlinScan(runtime, sourceSet).parse(
            mapOf(
                "m/Types.kt" to "package m\n\nclass Widget(val size: Int)\n",
                "m/Doc.kt" to "package m\n\n"
                        + "/**\n"
                        + " * Interface to let the core know that this [Widget] is used.\n"
                        + " *\n"
                        + " * The core only runs it when [Widget] says so.\n"
                        + " */\n"
                        + "class Doc\n"
            ), emptyMap()
        )
        fun type(name: String) = t.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }
        val widget = type("Widget")
        val tags = type("Doc").javaDoc()?.tags() ?: listOf()
        val links = tags.filter { it.resolvedReference() == widget }
            .map { "${it.sourceOfReference()?.beginLine()}:${it.sourceOfReference()?.beginPos()}" }.sorted()
        assertEquals(listOf("4:46", "6:32"), links)
    }
}
