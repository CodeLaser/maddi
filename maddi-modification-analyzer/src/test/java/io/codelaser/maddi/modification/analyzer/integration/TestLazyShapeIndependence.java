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

package io.codelaser.maddi.modification.analyzer.integration;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.codelaser.maddi.cst.impl.analysis.PropertyImpl.INDEPENDENT_METHOD;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Seven ways to write "return the lazily initialised value", and two of them get a different answer.
 * <p>
 * All seven hand the caller the object the field holds. Five are computed {@code INDEPENDENT_HC} — independent
 * of the object's fields <em>except through hidden content</em>, which is the honest answer for a method that
 * returns a {@code T} held in a field. Two are computed fully {@code INDEPENDENT}, which says the caller cannot
 * reach anything the object holds, and that is not true of any of them.
 * <p>
 * ⛔ <b>THIS TEST ASSERTS THE WRONG ANSWER ON PURPOSE.</b> D and E are pinned as {@code INDEPENDENT} so that the
 * discrepancy is recorded rather than rediscovered. When the analyzer improves, this test fails and points at
 * itself. It is not a specification.
 * <p>
 * <b>What separates them.</b> D and E end in {@code return value}, where {@code value} is a local that first read
 * the field and was then REASSIGNED from the supplier before {@code t = value} published it. F is D with its last
 * statement written {@code return t} instead — the same object, at the same instant — and lands back on
 * {@code INDEPENDENT_HC}. So the link survives {@code local = field} (B, G), and survives being returned through
 * the field (A, C, F), and is lost through {@code field = local} once the local has been reassigned. E is the
 * sharper case: it contains a literal {@code return t} on one branch and is still called {@code INDEPENDENT}, so
 * the method's verdict is not the join over its return statements either.
 * <p>
 * <b>Why it is not academic.</b> D/E is the natural way to write the idiom, and E is exactly what
 * {@code io.codelaser.maddi.support.Lazy} was rewritten to when it was aligned with {@code kotlin.Lazy}. That
 * rewrite would have shipped a support type whose own body makes the analyzer state something too strong about
 * it — which is how this was found, and only because the rewrite was checked against the analyzer rather than
 * assumed. {@code Lazy} now uses shape F, with a comment saying why. G is {@code Memo}'s shape and is
 * <em>unaffected</em>: it never reassigns the local after the field read.
 * <p>
 * See {@code docs/book-vs-support-divergence.md}, finding 12.
 */
public class TestLazyShapeIndependence extends CommonTest {

    /** The same fields and constructor under every shape; only {@code get()} differs. */
    private static String source(String name, String body) {
        return """
                import java.util.Objects;
                import java.util.function.Supplier;

                public class NAME<T> {
                  private volatile Supplier<T> supplier;
                  private volatile T t;

                  public NAME(Supplier<T> supplierParam) {
                    if (supplierParam == null) throw new NullPointerException("Null not allowed");
                    this.supplier = supplierParam;
                  }

                """.replace("NAME", name) + body + """

                  public boolean hasBeenEvaluated() {
                    return t != null;
                  }
                }
                """;
    }

    /** The shape before the alignment with {@code kotlin.Lazy}: the supplier is never dropped. */
    @Language("java")
    private static final String A_ORIGINAL = """
              public T get() {
                if (t != null) return t;
                t = Objects.requireNonNull(supplier.get());
                return t;
              }
            """;

    /** A: with the guard reading the field into a local first. */
    @Language("java")
    private static final String B_LOCAL_READ = """
              public T get() {
                T value = t;
                if (value != null) return value;
                t = Objects.requireNonNull(supplier.get());
                return t;
              }
            """;

    /** A: with the supplier dropped, exactly as the book's listing does it. */
    @Language("java")
    private static final String C_CLEAR_ONLY = """
              public T get() {
                if (t != null) return t;
                t = Objects.requireNonNull(supplier.get());
                supplier = null;
                return t;
              }
            """;

    /** Publish through the local, then return the local. The first shape that loses the link. */
    @Language("java")
    private static final String D_LOCAL_RETURN = """
              public T get() {
                T value = t;
                if (value != null) return value;
                value = Objects.requireNonNull(supplier.get());
                t = value;
                return value;
              }
            """;

    /** D plus the supplier read once and dropped: the NPE-free alignment, and still {@code INDEPENDENT}. */
    @Language("java")
    private static final String E_LOCAL_RETURN_SAFE = """
              public T get() {
                T value = t;
                if (value != null) return value;
                Supplier<T> localSupplier = supplier;
                if (localSupplier == null) return t;
                value = Objects.requireNonNull(localSupplier.get());
                t = value;
                supplier = null;
                return value;
              }
            """;

    /** E with its last statement written {@code return t}. What {@code maddi-support.Lazy} ships. */
    @Language("java")
    private static final String F_PUBLISH_THEN_READ = """
              public T get() {
                T value = t;
                if (value != null) return value;
                Supplier<T> localSupplier = supplier;
                if (localSupplier == null) return t;
                t = Objects.requireNonNull(localSupplier.get());
                supplier = null;
                return t;
              }
            """;

    /** {@code io.codelaser.maddi.support.Memo}: the supplier is a parameter, and the local is not reassigned. */
    @Language("java")
    private static final String G_MEMO = """
            import java.util.Objects;
            import java.util.function.Supplier;

            public final class MemoG<T> {
              private volatile T value;

              public T get(Supplier<? extends T> compute) {
                T v = value;
                if (v == null) {
                  v = Objects.requireNonNull(compute.get(), "A Memo cannot cache null");
                  value = v;
                }
                return v;
              }
            }
            """;

    private String independent(TypeInfo typeInfo, String methodName, int parameters) {
        Value.Independent v = typeInfo.findUniqueMethod(methodName, parameters).analysis()
                .getOrDefault(INDEPENDENT_METHOD, DEPENDENT);
        return v.isIndependentHc() ? "INDEPENDENT_HC" : v.isDependent() ? "DEPENDENT" : "INDEPENDENT";
    }

    /** Each shape is parsed under its own type name: one inspector cannot inspect the same name twice. */
    private String row(String label, String body) {
        String name = "Lazy" + label.charAt(0);
        TypeInfo typeInfo = javaInspector.parse(name, source(name, body));
        analyzer.go(prepWork(typeInfo));
        return String.format("%-22s %s%n", label, independent(typeInfo, "get", 0));
    }

    @DisplayName("seven ways to return a lazily initialised value; two are called INDEPENDENT and should not be")
    @Test
    public void shapes() {
        TypeInfo memo = javaInspector.parse("MemoG", G_MEMO);
        analyzer.go(prepWork(memo));

        assertEquals("""
                A original             INDEPENDENT_HC
                B local read           INDEPENDENT_HC
                C clear only           INDEPENDENT_HC
                D local return         INDEPENDENT
                E local return safe    INDEPENDENT
                F publish then read    INDEPENDENT_HC
                G Memo shape           INDEPENDENT_HC
                """,
                row("A original", A_ORIGINAL)
                + row("B local read", B_LOCAL_READ)
                + row("C clear only", C_CLEAR_ONLY)
                + row("D local return", D_LOCAL_RETURN)
                + row("E local return safe", E_LOCAL_RETURN_SAFE)
                + row("F publish then read", F_PUBLISH_THEN_READ)
                + String.format("%-22s %s%n", "G Memo shape", independent(memo, "get", 1)));
    }
}
