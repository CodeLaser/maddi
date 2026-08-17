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

package org.e2immu.language.inspection.kotlin

import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer
import org.e2immu.language.cst.api.info.MethodPrinter
import org.e2immu.language.cst.api.info.TypeInfo
import org.e2immu.language.cst.api.info.TypePrinter
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.cst.impl.info.CompilationUnitPrinterImpl
import org.e2immu.language.cst.impl.info.ImportComputerImpl
import org.e2immu.language.cst.impl.output.OutputBuilderImpl
import org.e2immu.language.cst.impl.output.TextImpl
import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.cst.print.FormattingOptionsImpl
import org.e2immu.language.cst.print.formatter2.Formatter2Impl
import org.e2immu.language.cst.print.kotlin.KotlinTypePrinter
import org.e2immu.language.inspection.resource.InfoByFqn
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.e2immu.language.kotlin.k2.KotlinScan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

class TestKotlinPrinter {

    private fun printKotlin(runtime: Runtime, type: TypeInfo): String {
        val ob = KotlinTypePrinter(type, true)
            .print(ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType(), true)
        return Formatter2Impl(runtime, FormattingOptionsImpl.Builder().build()).write(ob)
    }

    /** Parse Kotlin, run prepwork (populates getSetField), print back as Kotlin. */
    @Test
    fun kotlinRoundTrip() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val src = "package a\n" +
            "class Foo(val id: Int) {\n" +
            "    fun greet(name: String): String = \"hi \" + name\n" +
            "}\n"
        val types = KotlinScan(runtime, sourceSet, InfoByFqn()).parse("a/Foo.kt", src)
        PrepAnalyzer(runtime).doPrimaryTypes(types.toSet())
        val foo = types.first { it.simpleName() == "Foo" }

        val kotlin = printKotlin(runtime, foo)
        // primary constructor reconstructed, getId() collapsed into the `val` property, expression body
        assertTrue(kotlin.contains("class Foo(val id: Int)"), kotlin)
        assertTrue(kotlin.contains("fun greet(name: String): String = \"hi \" + name"), kotlin)
        assertTrue(!kotlin.contains("getId"), "getter must be collapsed into the property:\n$kotlin")
    }

    /** Expression/statement translation: `is`, `as`, `Foo()` (no `new`), `val`, expression bodies. */
    @Test
    fun expressionAndStatementTranslation() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val src = "package a\n" +
            "class Bar {\n" +
            "    fun check(x: Any): Boolean = x is String\n" +
            "    fun make(): Bar = Bar()\n" +
            "    fun pick(x: Any): String {\n" +
            "        val s = x as String\n" +
            "        return s\n" +
            "    }\n" +
            "}\n"
        val types = KotlinScan(runtime, sourceSet, InfoByFqn()).parse("a/Bar.kt", src)
        PrepAnalyzer(runtime).doPrimaryTypes(types.toSet())
        val bar = types.first { it.simpleName() == "Bar" }
        val kotlin = printKotlin(runtime, bar)

        assertTrue(kotlin.contains("fun check(x: Any): Boolean = x is String"), kotlin)
        assertTrue(kotlin.contains("fun make(): Bar = Bar()"), kotlin)
        assertTrue(kotlin.contains("s = x as String"), kotlin) // cast -> `as`, local -> val/var
        assertTrue(!kotlin.contains("new "), "no Java 'new':\n$kotlin")
        assertTrue(!kotlin.contains("instanceof"), "no Java 'instanceof':\n$kotlin")
        assertTrue(!kotlin.contains("constructor()"), "implicit default constructor suppressed:\n$kotlin")
    }

    /** Nested translation: a Java-only form inside an operator (was previously left as Java). */
    @Test
    fun nestedOperatorTranslation() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val src = "package a\n" +
            "class Baz {\n" +
            "    fun a(x: Any): Boolean = x is String && x.hashCode() > 3\n" +
            "    fun b(x: Any): Boolean = !x.equals(x)\n" +
            "}\n"
        val types = KotlinScan(runtime, sourceSet, InfoByFqn()).parse("a/Baz.kt", src)
        PrepAnalyzer(runtime).doPrimaryTypes(types.toSet())
        val baz = types.first { it.simpleName() == "Baz" }
        val kotlin = printKotlin(runtime, baz)

        assertTrue(kotlin.contains("x is String &&"), "instanceof nested in && must translate:\n$kotlin")
        assertTrue(kotlin.contains("!x.equals(x)"), "negation recurses into its operand:\n$kotlin")
        assertTrue(!kotlin.contains("instanceof"), "no Java 'instanceof':\n$kotlin")
    }

    /** Nullability (`?`), elvis (`?:` via DetailedSources), and control-flow (`while`). */
    @Test
    fun nullabilityElvisAndControlFlow() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val src = "package a\n" +
            "class Q {\n" +
            "    fun e(x: String?): String = x ?: \"d\"\n" +
            "    fun w(n: Int): Int {\n" +
            "        var i = 0\n" +
            "        while (i < n) { i = i + 1 }\n" +
            "        return i\n" +
            "    }\n" +
            "}\n"
        val types = KotlinScan(runtime, sourceSet, InfoByFqn()).parse("a/Q.kt", src)
        PrepAnalyzer(runtime).doPrimaryTypes(types.toSet())
        val q = types.first { it.simpleName() == "Q" }
        val kotlin = printKotlin(runtime, q)

        assertTrue(kotlin.contains("fun e(x: String?): String = x ?: \"d\""),
            "nullable param + return, elvis:\n$kotlin")
        assertTrue(kotlin.contains("while (i < n)"), "while loop:\n$kotlin")
        assertTrue(!kotlin.contains("?:") || kotlin.contains("x ?: "), kotlin)
    }

    /** when, try/catch, and the `!is` idiom. */
    @Test
    fun whenTryAndNotIs() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val src = "package a\n" +
            "class R {\n" +
            "    fun c(x: Any): Boolean = x !is String\n" +
            "    fun w(x: Int): String = when (x) { 1 -> \"a\"; else -> \"b\" }\n" +
            "    fun t(): Int {\n" +
            "        try { return 1 } catch (e: Exception) { return 2 }\n" +
            "    }\n" +
            "}\n"
        val types = KotlinScan(runtime, sourceSet, InfoByFqn()).parse("a/R.kt", src)
        PrepAnalyzer(runtime).doPrimaryTypes(types.toSet())
        val r = types.first { it.simpleName() == "R" }
        val kotlin = printKotlin(runtime, r)

        assertTrue(kotlin.contains("x !is String"), "not-instanceof idiom:\n$kotlin")
        assertTrue(kotlin.contains("when (x)"), "switch -> when:\n$kotlin")
        assertTrue(kotlin.contains("try {") && kotlin.contains("catch (e: Exception)"), "try/catch:\n$kotlin")
        assertTrue(!kotlin.contains("instanceof") && !kotlin.contains("switch"), kotlin)
    }

    /** A Kotlin data class round-trips to `data class`, and multi-statement blocks are newline-separated. */
    @Test
    fun dataClassAndBlockNewlines() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val src = "package a\n" +
            "data class Point(val x: Int, val y: Int) {\n" +
            "    fun shift(d: Int): Int {\n" +
            "        val s = x + d\n" +
            "        return s + y\n" +
            "    }\n" +
            "}\n"
        val types = KotlinScan(runtime, sourceSet, InfoByFqn()).parse("a/Point.kt", src)
        PrepAnalyzer(runtime).doPrimaryTypes(types.toSet())
        val point = types.first { it.simpleName() == "Point" }
        val kotlin = printKotlin(runtime, point)

        assertTrue(kotlin.contains("data class Point(val x: Int, val y: Int)"), kotlin)
        assertTrue(!kotlin.contains("component") && !kotlin.contains("fun copy"),
            "data-class generated methods must be suppressed:\n$kotlin")
        // the two body statements must be on separate lines (Kotlin has no ';')
        assertTrue(Regex("s = x \\+ d\\s*\\n\\s*return s \\+ y").containsMatchIn(kotlin),
            "block statements must be newline-separated:\n$kotlin")
    }

    /** The pluggable-printer seam: a custom MethodPrinter can be supplied to KotlinTypePrinter, as for Java. */
    @Test
    fun customMethodPrinterSeam() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val types = KotlinScan(runtime, sourceSet, InfoByFqn())
            .parse("a/Foo.kt", "package a\nclass Foo { fun greet(): String = \"hi\" }\n")
        PrepAnalyzer(runtime).doPrimaryTypes(types.toSet())
        val foo = types.first { it.simpleName() == "Foo" }

        // a custom method printer that replaces the method rendering entirely
        val customMethods = TypePrinter.MethodPrinterFactory { _, methodInfo, _ ->
            MethodPrinter { _ -> OutputBuilderImpl().add(TextImpl("/*custom:${methodInfo.name()}*/")) }
        }
        val importData = CompilationUnitPrinterImpl(foo.compilationUnit(), true)
            .computeImportData(ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType())
        val ob = KotlinTypePrinter(foo, true).print(importData, true, customMethods,
            { fieldInfo, f2 -> org.e2immu.language.cst.print.kotlin.KotlinFieldPrinter(fieldInfo, f2) },
            { ti, f2 -> KotlinTypePrinter(ti, f2) })
        val kotlin = Formatter2Impl(runtime, FormattingOptionsImpl.Builder().build()).write(ob)

        assertTrue(kotlin.contains("/*custom:greet*/"), kotlin)
        assertTrue(!kotlin.contains("fun greet"), "the custom printer must replace the default fun rendering:\n$kotlin")
    }
}
