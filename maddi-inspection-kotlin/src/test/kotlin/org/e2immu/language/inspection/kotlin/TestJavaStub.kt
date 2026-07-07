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

import com.sun.source.util.JavacTask
import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.e2immu.language.kotlin.k2.KotlinScan
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider

/**
 * Phase 3 de-risk (Java → Kotlin): the make-or-break question is whether a **signature-only Java stub**
 * generated from a Kotlin [org.e2immu.language.cst.api.info.TypeInfo] actually lets javac resolve a Java
 * source that references the Kotlin type. Here a Kotlin `Widget` is parsed, [JavaStubGenerator] emits its
 * stub, and javac attributes a Java `UseWidget` that constructs `Widget`, reads its field/getter and calls
 * its methods — against the stub only. No errors ⇒ the stub approach is viable.
 */
class TestJavaStub {

    @Test
    fun javacResolvesAJavaSourceAgainstAKotlinDerivedStub() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("k").setUri(URI.create("file:/")).build()
        val widget = KotlinScan(runtime, sourceSet).parse(
            "Widget.kt",
            "package a.b\n" +
                "class Widget(val id: Int) {\n" +
                "    fun label(): String = \"w\"\n" +
                "    fun combine(other: Widget): Widget = other\n" +
                "}\n"
        ).first { it.simpleName() == "Widget" }

        val stub = JavaStubGenerator.stub(widget)

        val useWidget = """
            package a.b;
            public class UseWidget {
                public Widget field;
                public Widget make() { return new Widget(1); }
                public String label(Widget w) { return w.label(); }
                public Widget combine(Widget a, Widget b) { return a.combine(b); }
                public int id(Widget w) { return w.getId(); }
            }
        """.trimIndent()

        val errors = attribute(mapOf("a.b.Widget" to stub, "a.b.UseWidget" to useWidget))
        assertTrue(errors.isEmpty(),
            "javac must resolve UseWidget against the Kotlin-derived stub.\n--- stub ---\n$stub\n--- errors ---\n" +
                errors.joinToString("\n"))
    }

    /** Parse + attribute (no code generation) the given sources together; return ERROR diagnostics. */
    private fun attribute(sourcesByFqn: Map<String, String>): List<String> {
        val compiler = ToolProvider.getSystemJavaCompiler()
        val collector = DiagnosticCollector<JavaFileObject>()
        val files = sourcesByFqn.map { (fqn, code) -> inMemorySource(fqn, code) }
        val task = compiler.getTask(null, null, collector, listOf("-proc:none"), null, files) as JavacTask
        task.analyze() // parse + attribute; surfaces unresolved references as ERROR diagnostics
        return collector.diagnostics.filter { it.kind == Diagnostic.Kind.ERROR }.map { it.toString() }
    }

    private fun inMemorySource(fqn: String, code: String): JavaFileObject =
        object : SimpleJavaFileObject(
            URI.create("string:///" + fqn.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE
        ) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = code
        }
}
