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
import org.e2immu.language.cst.impl.info.ImportComputerImpl
import org.e2immu.language.cst.print.FormattingOptionsImpl
import org.e2immu.language.cst.print.formatter2.Formatter2Impl
import org.e2immu.language.cst.print.kotlin.KotlinTypePrinter
import org.e2immu.language.inspection.api.integration.JavaInspector
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * Round-trip harness: parse real **Java** with the openjdk front-end, run prepwork, and print it back as Kotlin
 * — to shake out printer gaps at scale. Any un-translated Java-only surface (`new`, `instanceof`, `switch`,
 * `? :`, `;`) shows up as a leftover "Java-ism" and is reported (and, for the ones already covered, asserted).
 */
class TestKotlinPrinterRoundTrip {

    private val richJava = """
        package a;
        public class Rich<T> {
            private final int id;
            private String name;
            public Rich(int id, String name) { this.id = id; this.name = name; }
            public int getId() { return id; }
            public boolean check(Object x) { return x instanceof String && ((String) x).length() > 0; }
            public String pick(int n) { return n > 0 ? "pos" : "neg"; }
            public int sum(java.util.List<Integer> xs) {
                int total = 0;
                for (Integer x : xs) { total = total + x.intValue(); }
                return total;
            }
            public void risky() {
                try { throw new RuntimeException("x"); } catch (RuntimeException e) { getId(); }
            }
            public String classify(int n) {
                switch (n) {
                    case 1: return "one";
                    default: return "many";
                }
            }
            public int countdown(int n) {
                int c = 0;
                while (n > 0) { n = n - 1; c = c + 1; }
                return c;
            }
            public java.util.function.Function<Integer, Integer> adder(int k) {
                return x -> x + k;
            }
        }
        """.trimIndent()

    @Test
    fun javaToKotlin() {
        val javaInspector = JavaInspectorImpl()
        val sourceSet = SourceSetImpl.Builder().setName(JavaInspector.TEST_PROTOCOL).setUri(URI.create("file:/")).build()
        javaInspector.initialize(
            InputConfigurationImpl.Builder().addSourceSets(sourceSet).addClassPath("jmod:java.base").build()
        )
        javaInspector.onlyPreload()
        val runtime = javaInspector.runtime()
        val type = javaInspector.parse("a.Rich", richJava)
        PrepAnalyzer(runtime).doPrimaryTypes(setOf(type))

        val ob = KotlinTypePrinter(type, true)
            .print(ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType(), true)
        val kotlin = Formatter2Impl(runtime, FormattingOptionsImpl.Builder().build()).write(ob)

        // real Java -> idiomatic Kotlin, on a construct-rich class
        assertTrue(kotlin.contains("open class Rich<T>"), kotlin) // Java non-final class -> open
        assertTrue(kotlin.contains("(val id: Int, var name: String)"), kotlin) // final field -> val, mutable -> var
        assertTrue(kotlin.contains("fun check(x: Any): Boolean = x is String && (x as String)"), kotlin) // is + as + &&
        assertTrue(kotlin.contains("""fun pick(n: Int): String = if (n > 0) "pos" else "neg""""), kotlin) // ternary body
        assertTrue(kotlin.contains("for (x in xs)"), kotlin) // for-each -> for-in
        assertTrue(kotlin.contains("catch (e: RuntimeException)"), kotlin) // try/catch
        assertTrue(kotlin.contains("throw RuntimeException(\"x\")"), kotlin) // throw + new
        assertTrue(kotlin.contains("= { x -> x + k }"), kotlin) // lambda
        assertTrue(!kotlin.contains("instanceof") && !kotlin.contains(" new "), kotlin)

        // KNOWN gap: old-style (fall-through) `switch` is left as Java; arrow switches DO become `when`.
        val unexpected = listOf(" new ", "instanceof", " ? ", "<empty>", "k2-").filter { kotlin.contains(it) }
        assertTrue(unexpected.isEmpty(), "unexpected un-translated Java-isms $unexpected in:\n$kotlin")
    }
}
