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
        // primary constructor reconstructed, getId() collapsed into the `val` property
        assertTrue(kotlin.contains("class Foo(val id: Int)"), kotlin)
        assertTrue(kotlin.contains("fun greet(name: String): String"), kotlin)
        assertTrue(!kotlin.contains("getId"), "getter must be collapsed into the property:\n$kotlin")
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
