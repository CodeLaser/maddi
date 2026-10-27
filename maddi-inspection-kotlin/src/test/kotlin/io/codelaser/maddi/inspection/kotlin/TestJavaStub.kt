/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.kotlin

import com.sun.source.util.JavacTask
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import io.codelaser.maddi.kotlin.k2.KotlinScan
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
 * generated from a Kotlin [io.codelaser.maddi.cst.api.info.TypeInfo] actually lets javac resolve a Java
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

    /**
     * A `vararg` that is NOT the last parameter is a plain array on the JVM (kotlinc marks only a trailing one
     * ACC_VARARGS), and `T...` anywhere but last does not compile: Exposed's
     * `fun create(vararg tables: Table, inBatch: Boolean = false)` failed the mixed parse's stub compilation 108 times
     * with "varargs parameter must be the last parameter". A trailing one stays `T...`, so Java can pass elements.
     */
    @Test
    fun onlyATrailingVarargIsJavaVarargs() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("k").setUri(URI.create("file:/")).build()
        val schema = KotlinScan(runtime, sourceSet).parse(
            "Schema.kt",
            "package a.b\n" +
                "class Schema {\n" +
                "    fun create(vararg tables: String, inBatch: Boolean): Int = tables.size\n" +
                "    fun drop(vararg tables: String): Int = tables.size\n" +
                "}\n"
        ).first { it.simpleName() == "Schema" }

        val stub = JavaStubGenerator.stub(schema)

        val use = """
            package a.b;
            public class UseSchema {
                public int create(Schema s) { return s.create(new String[]{"a", "b"}, true); }
                public int drop(Schema s) { return s.drop("a", "b"); }
            }
        """.trimIndent()

        val errors = attribute(mapOf("a.b.Schema" to stub, "a.b.UseSchema" to use))
        assertTrue(errors.isEmpty(), "--- stub ---\n$stub\n--- errors ---\n" + errors.joinToString("\n"))
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
