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

import io.codelaser.maddi.cst.api.expression.ConstructorCall
import io.codelaser.maddi.cst.api.info.Info
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Every project reference written in a Kotlin member is recorded on that member's source, under the CST [Info] it
 * names ([io.codelaser.maddi.cst.api.element.DetailedSources.references]) -- including the ones the desugared CST
 * has no element for. Positions are `line:column` of the identifier.
 */
class RecordedReferencesTest : KotlinScanTestBase() {

    private val base = """
        package a

        open class Base {
            open fun greet(): String = "b"
        }
        class Sub : Base() {
            override fun greet(): String = "s" + super.greet()
        }
        fun String.path(): String = this
        fun Int.path(): String = toString()
        class Task {
            override fun toString(): String = "t"
        }
        fun helper(): Int = 1
        interface Greeter {
            fun hi(): String
        }
        """.trimIndent() + "\n"

    private val user = """
        package b

        import a.Base
        import a.Sub
        import a.path

        fun use(b: Base, s: Sub): String = b.greet() + s.greet() + "x".path() + 1.path()
        fun all(items: List<Base>): List<String> = items.map { it.greet() }
        """.trimIndent() + "\n"

    // the places a reference used to fall through to the class, where the call graph does not look for a caller
    private val mixed = """
        package c

        import a.Greeter
        import a.helper

        class Holder(v: Int) {
            val lazyValue by lazy { helper() }
            init { require(v == helper()) }
        }
        fun make(): Greeter = object : Greeter {
            override fun hi(): String = "m"
        }
        class Forward(g: Greeter) : Greeter by g
        private fun top(): Int = helper()
        fun callForward(f: Forward, g: Greeter): String = f.hi() + g.hi()
        """.trimIndent() + "\n"

    private lateinit var types: List<TypeInfo>

    private fun parse() {
        types = KotlinScan(runtime, sourceSet)
            .parse(mapOf("a/Base.kt" to base, "b/User.kt" to user, "c/Mixed.kt" to mixed), emptyMap())
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }
    private fun method(type: String, name: String) = type(type).methods().first { it.name() == name }
    private fun path(receiver: String) = type("BaseKt").methods().single {
        it.name() == "path" && it.parameters().first().parameterizedType().typeInfo()?.fullyQualifiedName() == receiver
    }

    /** Where [host]'s text names [target], as `line:column`. */
    private fun names(host: Info, target: Info): List<String> =
        host.source().detailedSources().references(target).map { "${it.beginLine()}:${it.beginPos()}" }.sorted()

    @Test
    fun aCallIsRecordedOnTheMemberItIsWrittenIn() {
        parse()
        val use = method("UserKt", "use")
        assertEquals(listOf("7:38"), names(use, method("Base", "greet")))
        assertEquals(listOf("7:50"), names(use, method("Sub", "greet")))
        assertEquals(listOf("7:12"), names(use, type("Base")), "a type in the signature is a reference too")
        // Base has no written constructor: nothing may be recorded under the implicit one, whose psi is the class
        assertEquals(listOf<String>(), names(use, type("Base").constructors().single()))
    }

    @Test
    fun overloadsAreDistinctTargets() {
        parse()
        val use = method("UserKt", "use")
        assertEquals(listOf("7:64"), names(use, path("java.lang.String")))
        assertEquals(listOf("7:75"), names(use, path("int")))
    }

    @Test
    fun aCallTheCstDroppedIsStillRecorded() {
        parse()
        // `items.map { ... }` is an unresolved library extension call: the CST keeps a placeholder, lambda and all
        val all = method("UserKt", "all")
        assertEquals(listOf("8:59"), names(all, method("Base", "greet")))
    }

    @Test
    fun aLambdaLabelIsRecordedAsANameOfTheFunctionItBorrowsItFrom() {
        // `before { return@before }`: the label is the called function's name, so renaming the function must rename
        // it -- and K2 resolves the label to the LAMBDA, so only the call around it says which function that is.
        val types = KotlinScan(runtime, sourceSet).parse(
            mapOf(
                "a/Api.kt" to "package a\n\nfun before(action: () -> Unit) { action() }\n"
                        + "fun after(action: () -> Unit) { action() }\n",
                "b/Use.kt" to "package b\n\nimport a.before\nimport a.after\n\n"
                        + "fun use() {\n"
                        + "    before { return@before }\n"
                        + "    before { after { return@before } }\n"
                        + "    after(fun() { })\n"
                        + "    before outer@{ return@outer }\n"
                        + "}\n"
            ), emptyMap()
        )
        fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }
        val before = type("ApiKt").methods().first { it.name() == "before" }
        val use = type("UseKt").methods().first { it.name() == "use" }
        val at = use.source().detailedSources().references(before)
            .map { "${it.beginLine()}:${it.beginPos()}" }.sorted()
        // three calls spell `before`, and two of them are labelled; the `return@outer` is the author's own label,
        // and `before outer@{ … }` no longer labels anything
        assertEquals(listOf("10:5", "7:21", "7:5", "8:29", "8:5"), at)
    }

    @Test
    fun aSuperCallIsRecordedOnTheOverride() {
        parse()
        assertEquals(listOf("7:48"), names(method("Sub", "greet"), method("Base", "greet")))
    }

    @Test
    fun anImportIsRecordedOnTheFileAndNamesEveryOverload() {
        parse()
        val file = type("UserKt") // the facade holds what is written outside any member
        assertEquals(listOf("5:10"), names(file, path("java.lang.String")))
        assertEquals(listOf("5:10"), names(file, path("int")))
        assertEquals(listOf("3:10"), names(file, type("Base")))
    }

    @Test
    fun anOverrideKnowsWhatItOverrides() {
        parse()
        assertEquals(setOf(method("Base", "greet")), method("Sub", "greet").overrides())
        // a library one too; standalone, a library supertype loaded past the member depth is a shell with nothing to
        // override (Runnable.run would not be found here), but Object is bootstrapped with its members
        assertEquals(listOf("java.lang.Object.toString()"),
            method("Task", "toString").overrides().map { it.fullyQualifiedName() })
        assertEquals(setOf<MethodInfo>(), method("Base", "greet").overrides())
    }

    @Test
    fun anImportIsRecordedOnTheFacadeOfAFileWithClassesToo() {
        parse()
        assertEquals(listOf("4:10"), names(type("MixedKt"), method("BaseKt", "helper")))
    }

    @Test
    fun aDelegateExpressionIsRecordedOnTheDelegateField() {
        parse()
        val field = type("Holder").fields().single { it.name() == "lazyValue\$delegate" }
        assertEquals(listOf("7:29"), names(field, method("BaseKt", "helper")))
    }

    @Test
    fun anInitBlockIsRecordedOnThePrimaryConstructor() {
        parse()
        assertEquals(listOf("8:25"), names(type("Holder").constructors().single(), method("BaseKt", "helper")))
        assertEquals(listOf<String>(), names(type("Holder"), method("BaseKt", "helper")))
    }

    @Test
    fun anObjectExpressionsMethodKnowsWhatItOverrides() {
        parse()
        val anonymous = ArrayList<TypeInfo>()
        method("MixedKt", "make").methodBody().visit { e ->
            (e as? ConstructorCall)?.anonymousClass()?.let { anonymous += it }
            true
        }
        val hi = anonymous.single().methods().single { it.name() == "hi" }
        assertEquals(setOf(method("Greeter", "hi")), hi.overrides())
        assertEquals(11, hi.source().detailedSources().detail(hi.name()).beginLine()) // identity-keyed by the name
    }

    @Test
    fun aDelegationForwarderIsSyntheticAndNamesNothing() {
        parse()
        val forwarder = method("Forward", "hi")
        assertTrue(forwarder.isSynthetic)
        assertEquals(setOf(method("Greeter", "hi")), forwarder.overrides())
        // it has no text of its own: it used to carry Greeter.hi's source, a position in another file
        assertEquals(null, forwarder.source()?.detailedSources()?.detail(forwarder.name()))
        // ...and it used to take Greeter.hi's place as the target of everything naming it
        assertEquals(listOf("15:53", "15:62"), names(method("MixedKt", "callForward"), method("Greeter", "hi")))
    }

    @Test
    fun everyHostIsCommitted() {
        parse()
        val members = types.flatMap { it.recursiveSubTypeStream().toList() }
            .flatMap { t -> listOf<Info>(t) + t.methods() + t.fields() + t.constructors() }
        assertTrue(members.all { it.hasBeenInspected() }, "uncommitted: ${members.filterNot { it.hasBeenInspected() }}")
        assertTrue(members.filterIsInstance<MethodInfo>().isNotEmpty())
    }

    @Test
    fun aCompanionReachedThroughItsClassNameRecordsTheCLASS() {
        // ⛔ THE EXPRESSION DENOTES THE COMPANION, THE NAME SPELLS THE CLASS. Kotlin inserts the companion
        // implicitly, so K2 resolves the name `Widget` in `Widget.create(1)` to `Widget.Companion` -- but the text
        // there is the class's simple name, and it is the class's rename that must change it.
        val types = KotlinScan(runtime, sourceSet).parse(
            mapOf(
                "a/Widget.kt" to "package a\n\n"
                        + "class Widget private constructor(val size: Int) {\n"
                        + "    companion object Factory {\n"
                        + "        fun create(size: Int) = Widget(size)\n"
                        + "    }\n"
                        + "}\n",
                "b/Use.kt" to "package b\n\nimport a.Widget\n\n"
                        + "fun make() = Widget.create(1)\n"
                        + "fun spelled() = Widget.Factory.create(2)\n"
            ), emptyMap()
        )
        fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }
        fun at(host: Info, target: Info) =
            host.source().detailedSources().references(target).map { "${it.beginLine()}:${it.beginPos()}" }.sorted()

        val widget = type("Widget")
        val factory = type("Factory")
        val make = type("UseKt").methods().first { it.name() == "make" }
        val spelled = type("UseKt").methods().first { it.name() == "spelled" }

        assertEquals(listOf("5:14"), at(make, widget), "the written name is the class's")
        assertEquals(listOf<String>(), at(make, factory), "the companion is spelled nowhere in `Widget.create(1)`")
        // written out, both names are real: `Widget` is the class, `Factory` the companion in its own right
        assertEquals(listOf("6:17"), at(spelled, widget))
        assertEquals(listOf("6:24"), at(spelled, factory))
    }
}
