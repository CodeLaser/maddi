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

package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The printer's side of nullability: verdicts (here by hand, in production maddi-mod's NullabilityPass) put the
 * {@code ?} on declarations, and the {@link KotlinPrintOptions.NullCheck} policy writes {@code !!} or {@code ?.}
 * where a value Kotlin types nullable is used as non-null.
 */
public class TestJavaToKotlinNullability extends CommonJavaToKotlin {

    private static void contains(String kotlin, String expected) {
        assertTrue(strip(kotlin).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + kotlin);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(Collectors.joining("\n"));
    }

    /** NULLABLE for the declarations named here: fields and parameters by name, returns by method, locals by name. */
    private record ByName(Set<String> nullable) implements NullabilityVerdicts {
        private ParameterizedType verdict(String name, ParameterizedType type) {
            return nullable.contains(name) ? type.withNullable(NullableState.NULLABLE) : null;
        }

        @Override
        public ParameterizedType field(FieldInfo fieldInfo) {
            return verdict(fieldInfo.name(), fieldInfo.type());
        }

        @Override
        public ParameterizedType parameter(ParameterInfo parameterInfo) {
            return verdict(parameterInfo.name(), parameterInfo.parameterizedType());
        }

        @Override
        public ParameterizedType returnType(MethodInfo methodInfo) {
            return verdict(methodInfo.name() + "()", methodInfo.returnType());
        }

        @Override
        public ParameterizedType local(MethodInfo method, Element declaration, LocalVariable variable) {
            return verdict(variable.simpleName(), variable.parameterizedType());
        }
    }

    @Language("java")
    private static final String NULLS = """
            package a;
            import java.util.Map;
            class C {
                String name;
                void use(String s) { }
                String find(Map<String, String> m, String k) { return m.get(k); }
                int len(Map<String, String> m, String k) { return m.get(k).length(); }
                void m(Map<String, String> m) {
                    use(name);
                    String x = null;
                    x = "a";
                    String y = m.get("k");
                    use(y);
                    for (String s : names()) use(s);
                }
                java.util.List<String> names() { return null; }
            }
            """;

    private static final KotlinPrintOptions VERDICTS = new KotlinPrintOptions(
            new ByName(Set.of("name", "find()", "x", "names()")), KotlinPrintOptions.NullCheck.ASSERT);

    @Test
    public void declarationsAndAssertions() {
        String kotlin = kotlin(NULLS, VERDICTS);
        contains(kotlin, "var name: String? = null");
        contains(kotlin, "open fun find(m: MutableMap<String, String>, k: String): String? = m.get(k)");
        contains(kotlin, "open fun len(m: MutableMap<String, String>, k: String): Int = m.get(k)!!.length");
        contains(kotlin, "use(name!!)");
        contains(kotlin, "var x: String? = null");
        // no verdict for y: Kotlin infers String? from Map.get, so its use as a non-null argument is asserted
        contains(kotlin, "var y = m.get(\"k\")");
        contains(kotlin, "for (s in names()!!) {"); // Kotlin does not loop over a nullable collection
    }

    @Test
    public void safeCall() {
        String kotlin = kotlin(NULLS, new KotlinPrintOptions(VERDICTS.verdicts(), KotlinPrintOptions.NullCheck.SAFE_CALL));
        contains(kotlin, "m.get(k)?.length");
        contains(kotlin, "use(name!!)"); // not a receiver: still asserted
    }

    @Language("java")
    private static final String BOXED = """
            package a;
            class V {
                final int a;
                V(int a, int b) { this.a = a + b; }
                V(Integer a, Integer b) { this(a.intValue(), b.intValue()); }
            }
            """;

    /** Java's int and Integer overloads are both Int in Kotlin: the boxed one takes Int?. */
    @Test
    public void boxedOverloads() {
        String kotlin = kotlin(BOXED);
        contains(kotlin, "constructor(a: Int, b: Int)");
        contains(kotlin, "constructor(a: Int?, b: Int?) : this(a!!.toInt(), b!!.toInt())");
        assertFalse(kotlin.contains("constructor(a: Int, b: Int) : this"), kotlin);
    }
}
