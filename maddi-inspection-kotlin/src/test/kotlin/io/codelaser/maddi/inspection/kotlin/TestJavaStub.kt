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
import org.junit.jupiter.api.Assertions.assertEquals
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
     * What fernflower's Java tests call on its Kotlin translation: `@JvmStatic` companion functions (static methods of
     * the OUTER class on the JVM: `ConstExprent.trimFloat`, `InterpreterUtil.getBytes`) and a `fun interface`
     * implemented by a lambda (`IBytecodeProvider`), and a `@JvmRecord data class` (no Java class may extend
     * java.lang.Record, its parent). Missing from the stubs, javac dropped the four test types that use
     * them (maddi-mod#146), and with them the modifications they make (maddi-mod#145).
     */
    @Test
    fun jvmStaticCompanionFunctionsAndAFunInterfaceAreJava() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("k").setUri(URI.create("file:/")).build()
        val types = KotlinScan(runtime, sourceSet).parse(
            "Util.kt",
            "package a.b\n" +
                "fun interface Provider { fun get(path: String, inner: String?): ByteArray }\n" +
                "class Util {\n" +
                "    companion object {\n" +
                "        @JvmStatic fun trim(value: String, start: Float): String = value\n" +
                "        @JvmStatic fun put(key: String, value: Any?) {}\n" +
                "        fun notStatic(): Int = 1\n" +
                "    }\n" +
                "}\n" +
                "@JvmRecord data class Pair(val first: Int, val second: Int)\n"
        )
        assertEquals("java.lang.Record", types.first { it.simpleName() == "Pair" }.parentClass()?.typeInfo()?.fullyQualifiedName(),
            "the premise: @JvmRecord resolved")
        val stubs = types.filter { it.simpleName() in setOf("Provider", "Util", "Pair") }
            .associate { it.fullyQualifiedName() to JavaStubGenerator.stub(it) }
        val use = """
            package a.b;
            public class Use {
                public Provider p() { return (path, inner) -> new byte[0]; }
                public String t() { Util.put("k", "v"); return Util.trim("x", 1.0f) + Util.Companion.notStatic(); }
                public Object pair() { return new Pair(1, 2); }
            }
        """.trimIndent()

        val errors = attribute(stubs + ("a.b.Use" to use))
        assertTrue(errors.isEmpty(), stubs.values.joinToString("\n") + "\n--- errors ---\n" + errors.joinToString("\n"))
    }

    /**
     * An override of a Java method whose parameter is a TYPE VARIABLE of its class: erased as inherited, not as
     * declared. Quarkus's `override fun list(query: PanacheQuery<*>)` in a `MongoOperations<PanacheQuery<*>, …>` was
     * stubbed `list(Object)`, a name clash with the inherited `list(PanacheQuery<?>)`; `Consumer<List<*>>` is the
     * same shape with a JDK type.
     */
    @Test
    fun anOverrideOfATypeVariableParameterIsErasedAsInherited() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("k").setUri(URI.create("file:/")).build()
        val sink = KotlinScan(runtime, sourceSet).parse(
            "Sink.kt",
            "package a.b\n" +
                "class Sink : java.util.function.Consumer<List<*>> {\n" +
                "    override fun accept(t: List<*>) {}\n" +
                "}\n"
        ).first { it.simpleName() == "Sink" }
        val stub = JavaStubGenerator.stub(sink)
        assertTrue(stub.contains("accept(java.util.List p0)"), stub)
        val use = """
            package a.b;
            public class UseSink {
                public void use(Sink s, java.util.List<String> l) { s.accept(l); java.util.function.Consumer<java.util.List<?>> c = s; }
            }
        """.trimIndent()
        val errors = attribute(mapOf("a.b.Sink" to stub, "a.b.UseSink" to use))
        assertTrue(errors.isEmpty(), "--- stub ---\n$stub\n--- errors ---\n" + errors.joinToString("\n"))
    }

    /**
     * A GENERIC override of a Java or library method, whose parameters the stub erases: it must erase the whole method,
     * type parameters and return type included. `public <T> List<T> getRegisteredExtensions(KaModule,
     * ExtensionPointDescriptor)` (maddi-kotlin-k2's CompilerPlugins, against the Kotlin analysis API) neither overrides
     * the inherited `<T> ... (KaModule, ExtensionPointDescriptor<T>)` nor may sit beside it: javac reports "does not
     * override abstract method" and "name clash ... have the same erasure", and the mixed parse's stubs fail. The fully
     * erased method has the inherited one's erasure, so it overrides it (JLS 8.4.2; an unchecked return, 8.4.8.3).
     */
    @Test
    fun aGenericOverrideOfALibraryMethodIsErasedWhole() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("k").setUri(URI.create("file:/")).build()
        val elem = KotlinScan(runtime, sourceSet).parse(
            "Elem.kt",
            "package a.b\n" +
                "class Elem : java.lang.reflect.AnnotatedElement {\n" +
                "    override fun <T : Annotation> getAnnotation(annotationClass: Class<T>): T? = null\n" +
                "    override fun getAnnotations(): Array<Annotation> = arrayOf()\n" +
                "    override fun getDeclaredAnnotations(): Array<Annotation> = arrayOf()\n" +
                "}\n"
        ).first { it.simpleName() == "Elem" }
        val stub = JavaStubGenerator.stub(elem)
        assertTrue(stub.contains("getAnnotation(java.lang.Class p0)"), stub)
        val use = """
            package a.b;
            public class UseElem {
                public Object use(Elem e) { java.lang.reflect.AnnotatedElement a = e; return e.getAnnotation(Deprecated.class); }
            }
        """.trimIndent()
        val errors = attribute(mapOf("a.b.Elem" to stub, "a.b.UseElem" to use))
        assertTrue(errors.isEmpty(), "--- stub ---\n$stub\n--- errors ---\n" + errors.joinToString("\n"))
    }

    /**
     * An override of a Java collection method has the Java method's signature, as kotlinc compiles it:
     * `remove(element: E)` in an `ArrayList<E>` is `remove(Object)`, `addAll(c: Collection<E>)` is
     * `addAll(Collection<? extends E>)`. Stubbed with the Kotlin types, javac saw a second method of the same erasure
     * and refused the stub ("name clash"), and with it fernflower's whole mixed parse (VBStyleCollection).
     */
    @Test
    fun anOverrideOfAJavaCollectionMethodHasItsJavaSignature() {
        val runtime = RuntimeImpl()
        val sourceSet = SourceSetImpl.Builder().setName("k").setUri(URI.create("file:/")).build()
        val bag = KotlinScan(runtime, sourceSet).parse(
            "Bag.kt",
            "package a.b\n" +
                "open class Bag<E, K> : java.util.ArrayList<E> {\n" +
                "    constructor() : super()\n" +
                "    constructor(initialCapacity: Int) : super(initialCapacity)\n" +
                "    override fun remove(element: E): Boolean = super.remove(element)\n" +
                "    override fun removeAt(index: Int): E = super.removeAt(index)\n" +
                "    override fun addAll(elements: Collection<E>): Boolean = super.addAll(elements)\n" +
                "    fun keep(e: E): Boolean = true\n" +
                "}\n"
        ).first { it.simpleName() == "Bag" }

        val stub = JavaStubGenerator.stub(bag)
        val useBag = """
            package a.b;
            public class UseBag {
                public boolean use(Bag<String, Integer> b, java.util.List<String> l) { return b.remove("x") && b.addAll(l) && b.keep("y"); }
            }
        """.trimIndent()

        val errors = attribute(mapOf("a.b.Bag" to stub, "a.b.UseBag" to useBag))
        assertTrue(errors.isEmpty(), "--- stub ---\n$stub\n--- errors ---\n" + errors.joinToString("\n"))
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
