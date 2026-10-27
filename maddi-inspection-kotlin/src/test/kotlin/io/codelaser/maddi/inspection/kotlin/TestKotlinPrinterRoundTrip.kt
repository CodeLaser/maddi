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

import io.codelaser.maddi.cst.impl.info.ImportComputerImpl
import io.codelaser.maddi.cst.print.FormattingOptionsImpl
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl
import io.codelaser.maddi.cst.print.kotlin.KotlinTypePrinter
import io.codelaser.maddi.inspection.api.integration.JavaInspector
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * Round-trip harness: parse real **Java** with the openjdk front-end and print it back as Kotlin
 * — to shake out printer gaps at scale. Any un-translated Java-only surface (`new`, `instanceof`, `switch`,
 * `? :`, `;`) shows up as a leftover "Java-ism" and is reported (and, for the ones already covered, asserted).
 */
class TestKotlinPrinterRoundTrip {

    /** Parse one `.java` primary type with the openjdk front-end and print it back as Kotlin. */
    private fun printAsKotlin(fqn: String, source: String): String {
        val javaInspector = JavaInspectorImpl()
        val sourceSet = SourceSetImpl.Builder().setName(JavaInspector.TEST_PROTOCOL).setUri(URI.create("file:/")).build()
        javaInspector.initialize(
            InputConfigurationImpl.Builder().addSourceSets(sourceSet).addClassPath("jmod:java.base").build()
        )
        javaInspector.onlyPreload()
        val runtime = javaInspector.runtime()
        val type = javaInspector.parse(fqn, source)

        val ob = KotlinTypePrinter(type, true)
            .print(ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType(), true)
        return Formatter2Impl(runtime, FormattingOptionsImpl.Builder().build()).write(ob)
    }

    /** Java-only surface that must never survive translation of a fully-covered sample. */
    private fun javaIsms(kotlin: String) =
        listOf(" new ", "instanceof", " ? ", "switch", "<empty>", "k2-").filter { kotlin.contains(it) }

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

    private val calcJava = """
        package a;
        public class Calc {
            public String describe(int n) {
                return switch (n) {
                    case 0 -> "zero";
                    case 1 -> "one";
                    default -> "many";
                };
            }
            public boolean notString(Object x) { return !(x instanceof String); }
            public int rounds(int n) {
                int c = 0;
                do { c = c + 1; n = n - 1; } while (n > 0);
                return c;
            }
            public String grade(int score) {
                if (score >= 90) return "A";
                else if (score >= 80) return "B";
                else return "C";
            }
            public <U> U identity(U u) { return u; }
            public java.util.Map<String, Integer> counts() { return new java.util.HashMap<>(); }
        }
        """.trimIndent()

    private val apiJava = """
        package a;
        public interface Api {
            int size();
            boolean isEmpty();
            default boolean nonEmpty() { return !isEmpty(); }
        }
        """.trimIndent()

    private val colorJava = """
        package a;
        public enum Color {
            RED, GREEN, BLUE;
            public boolean isRed() { return this == RED; }
        }
        """.trimIndent()

    private val opsJava = """
        package a;
        public class Ops {
            public int sumAll(int[] xs) {
                int total = 0;
                for (int x : xs) { total = total + x; }
                return total;
            }
            public boolean either(boolean a, boolean b) { return a || !b; }
            public int mod(int a, int b) { return a % b; }
            public String join(String a, String b) { return a + "-" + b; }
        }
        """.trimIndent()

    @Test
    fun javaToKotlin() {
        val kotlin = printAsKotlin("a.Rich", richJava)

        // real Java -> idiomatic Kotlin, on a construct-rich class
        assertTrue(kotlin.contains("open class Rich<T>"), kotlin) // Java non-final class -> open
        // final field -> val, mutable -> var; private in Java, private properties, which have no accessors: getId()
        // stays the public method it is (85758feb8)
        assertTrue(kotlin.contains("(private val id: Int, private var name: String)"), kotlin)
        assertTrue(kotlin.contains("fun getId(): Int = id"), kotlin)
        // is + &&: Kotlin smart-casts x after the test, so Java's (String) x needs no cast
        assertTrue(kotlin.contains("fun check(x: Any): Boolean = x is String && x.length > 0"), kotlin)
        assertTrue(kotlin.contains("""fun pick(n: Int): String = if (n > 0) "pos" else "neg""""), kotlin) // ternary body
        assertTrue(kotlin.contains("for (x in xs)"), kotlin) // for-each -> for-in
        assertTrue(kotlin.contains("catch (e: RuntimeException)"), kotlin) // try/catch
        assertTrue(kotlin.contains("throw RuntimeException(\"x\")"), kotlin) // throw + new
        // lambda; outside argument position it names its interface (a SAM constructor)
        assertTrue(kotlin.contains("= java.util.function.Function<Int, Int> { x -> x + k }"), kotlin)
        assertTrue(!kotlin.contains("instanceof") && !kotlin.contains(" new "), kotlin)

    }

    @Test
    fun javaToKotlin2() {
        val kotlin = printAsKotlin("a.Calc", calcJava)

        // arrow `switch` -> `when` (expression body); the default arm -> `else`
        assertTrue(kotlin.contains("fun describe(n: Int): String = when (n) {"), kotlin)
        assertTrue(kotlin.contains("else -> \"many\""), kotlin)
        assertTrue(kotlin.contains("fun notString(x: Any): Boolean = x !is String"), kotlin) // !(x instanceof T) -> x !is T
        assertTrue(kotlin.contains("} while (n > 0)"), kotlin) // do-while
        assertTrue(kotlin.contains("else if (score >= 80)"), kotlin) // else-if chain flattens (no `else { if … }`)
        assertTrue(kotlin.contains("fun <U> identity(u: U): U = u"), kotlin) // generic method
        // diamond + JDK type map; the constructed type is qualified as the Java printer would (no import computed here)
        // Java's Map is mutable: MutableMap
        assertTrue(kotlin.contains("fun counts(): MutableMap<String, Int> = java.util.HashMap<String, Int>()"), kotlin)

        assertTrue(javaIsms(kotlin).isEmpty(), "unexpected un-translated Java-isms ${javaIsms(kotlin)} in:\n$kotlin")
    }

    @Test
    fun javaToKotlin3_interface() {
        val kotlin = printAsKotlin("a.Api", apiJava)

        assertTrue(kotlin.contains("interface Api {"), kotlin) // Java interface -> Kotlin interface
        assertTrue(kotlin.contains("fun size(): Int"), kotlin) // abstract method: no body
        assertTrue(kotlin.contains("fun isEmpty(): Boolean"), kotlin)
        assertTrue(kotlin.contains("fun nonEmpty(): Boolean = !isEmpty()"), kotlin) // default method -> expression body
        assertTrue(javaIsms(kotlin).isEmpty(), "unexpected un-translated Java-isms ${javaIsms(kotlin)} in:\n$kotlin")
    }

    @Test
    fun javaToKotlin4_enum() {
        val kotlin = printAsKotlin("a.Color", colorJava)

        assertTrue(kotlin.contains("enum class Color {"), kotlin)
        assertTrue(kotlin.contains("RED, GREEN, BLUE;"), kotlin) // enum constants -> entries (not `val RED = Color()`)
        assertTrue(kotlin.contains("fun isRed(): Boolean = this === RED"), kotlin) // Java's == on references is identity
        // the enum-constant `new Color()` initializers must NOT leak through as `new`
        assertTrue(javaIsms(kotlin).isEmpty(), "unexpected un-translated Java-isms ${javaIsms(kotlin)} in:\n$kotlin")
    }

    @Test
    fun javaToKotlin5_operators() {
        val kotlin = printAsKotlin("a.Ops", opsJava)

        assertTrue(kotlin.contains("fun sumAll(xs: IntArray): Int"), kotlin) // int[] -> IntArray
        assertTrue(kotlin.contains("for (x in xs)"), kotlin)
        assertTrue(kotlin.contains("fun either(a: Boolean, b: Boolean): Boolean = a || !b"), kotlin) // ||, unary !
        assertTrue(kotlin.contains("fun mod(a: Int, b: Int): Int = a % b"), kotlin) // %
        assertTrue(kotlin.contains("""fun join(a: String, b: String): String = a + "-" + b"""), kotlin) // string concat
        assertTrue(javaIsms(kotlin).isEmpty(), "unexpected un-translated Java-isms ${javaIsms(kotlin)} in:\n$kotlin")
    }
}
