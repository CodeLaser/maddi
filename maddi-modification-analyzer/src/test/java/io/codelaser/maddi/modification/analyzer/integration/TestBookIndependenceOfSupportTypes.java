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
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.codelaser.maddi.cst.impl.analysis.PropertyImpl.INDEPENDENT_FIELD;
import static io.codelaser.maddi.cst.impl.analysis.PropertyImpl.INDEPENDENT_METHOD;
import static io.codelaser.maddi.cst.impl.analysis.PropertyImpl.INDEPENDENT_PARAMETER;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Does the analyzer still compute {@code @Independent(hc=true)} where <em>The Road to Immutability</em> chapter 12
 * prints it?
 * <p>
 * The book's support-class listings carry {@code @Independent(hc=true)} at sixteen positions. The shipped
 * {@code maddi-support} sources carry it at none of them. The book's introduction says annotations in the text are
 * "a means of verification: the analyzer will check if it generates the same annotation at that location", so an
 * annotation the source omits is not automatically wrong — the analyzer may still compute it, and the source may
 * simply have stopped asserting it. This test settles which, position by position.
 * <p>
 * <b>The answer, as recorded in the expected blocks below: the book is right at thirteen of the sixteen, understates
 * one, and is wrong at two.</b>
 * <ul>
 *     <li>{@code EventuallyFinal} (3/3), {@code FirstThen} (4/4), {@code SetOnceMap} (3/3) and two of
 *     {@code SetOnce}'s three: computed {@code INDEPENDENT_HC}, exactly as printed. The sources dropped an
 *     annotation the analyzer still derives.</li>
 *     <li>{@code SetOnce.getOrDefault} returns fully {@code INDEPENDENT}, which is <em>stronger</em> than the book's
 *     {@code @Independent(hc=true)}. Note the shipped body differs from the book's here (finding 8).</li>
 *     <li>{@code Lazy}'s {@code supplier} field is {@code DEPENDENT}, and that is what the book's own section is
 *     arguing it should not be.</li>
 * </ul>
 * The fixtures are the <b>shipped</b> bodies, reduced to the members under test and stripped of annotations and
 * javadoc, so that nothing is contracted and every value below is computed. The positions are the book's.
 * {@code Lazy} gets a second fixture in the book's own shape — non-final {@code supplier}, cleared inside
 * {@code get()} — because that is the one listing whose <em>code</em> differs, and the book's argument for
 * extending rule 2 ("all fields are private, of immutable type, or equal to null") turns on exactly that
 * assignment. The two {@code Lazy} blocks are identical, so on the analyzer's own reading the assignment buys
 * nothing: see finding 2.
 * <p>
 * Each row carries the book's claim beside the computed value, and is tagged {@code agrees}, {@code STRONGER} or
 * {@code DIFFERS}, so the assertion diff is the finding rather than a lookup into another document.
 * <p>
 * See {@code docs/book-vs-support-divergence.md}, findings 2, 5 and 8.
 */
public class TestBookIndependenceOfSupportTypes extends CommonTest {

    private static final String HC = "INDEPENDENT_HC";

    /** One book-annotated position: what it is called, and what the book prints there. */
    private record Position(String label, String bookClaim, Value.Independent computed) {
        String render() {
            String actual = name(computed);
            String verdict = actual.equals(bookClaim) ? "agrees"
                    : "INDEPENDENT".equals(actual) && HC.equals(bookClaim) ? "STRONGER"
                    : "DIFFERS";
            return "%-42s %-14s book: %-14s %s".formatted(label, actual, bookClaim, verdict);
        }
    }

    /** Collects positions so one run reports every one of them, rather than stopping at the first surprise. */
    private static final class Positions {
        private final List<Position> list = new ArrayList<>();

        Positions method(TypeInfo typeInfo, String name, int n, String label, String bookClaim) {
            return add(label, bookClaim,
                    typeInfo.findUniqueMethod(name, n).analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        }

        Positions parameter(MethodInfo methodInfo, int index, String label, String bookClaim) {
            return add(label, bookClaim,
                    methodInfo.parameters().get(index).analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
        }

        Positions parameter(TypeInfo typeInfo, String name, int n, int index, String label, String bookClaim) {
            return parameter(typeInfo.findUniqueMethod(name, n), index, label, bookClaim);
        }

        Positions field(TypeInfo typeInfo, String name, String label, String bookClaim) {
            return add(label, bookClaim,
                    typeInfo.getFieldByName(name, true).analysis().getOrDefault(INDEPENDENT_FIELD, DEPENDENT));
        }

        private Positions add(String label, String bookClaim, Value.Independent computed) {
            list.add(new Position(label, bookClaim, computed));
            return this;
        }

        String render() {
            return list.stream().map(Position::render).reduce((a, b) -> a + "\n" + b).orElseThrow() + "\n";
        }
    }

    private static String name(Value.Independent independent) {
        if (independent.isIndependentHc()) return HC;
        if (independent.isDependent()) return "DEPENDENT";
        return "INDEPENDENT";
    }

    private TypeInfo analyze(String typeName, @Language("java") String input) {
        TypeInfo typeInfo = javaInspector.parse(typeName, input);
        List<Info> analysisOrder = prepWork(typeInfo);
        analyzer.go(analysisOrder);
        return typeInfo;
    }

    // ---------------------------------------------------------------------------------------------------------
    // SetOnce
    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String SET_ONCE = """
            import java.util.Objects;

            public class SetOnce<T> {
              private volatile T t;

              public void set(T t) {
                if (t == null) throw new NullPointerException("Null not allowed");
                synchronized (this) {
                  if (this.t != null) throw new IllegalStateException("Already set");
                  this.t = t;
                }
              }

              public T get() {
                if (t == null) throw new IllegalStateException("Not yet set");
                return t;
              }

              public boolean isSet() {
                return t != null;
              }

              public T getOrDefault(T alternative) {
                if (isSet()) return get();
                return Objects.requireNonNull(alternative);
              }
            }
            """;

    @DisplayName("SetOnce: the three positions the book annotates @Independent(hc=true)")
    @Test
    public void testSetOnce() {
        TypeInfo setOnce = analyze("SetOnce", SET_ONCE);
        // getOrDefault comes out fully INDEPENDENT, one step above what the book prints. The shipped body is not
        // the book's: it returns Objects.requireNonNull(alternative) where the book returns the parameter
        // unguarded, and it is @NotNull for that reason (finding 8). The book understating this is harmless in a
        // way the two DIFFERS rows are not -- @Independent(hc=true) is a weaker, still-true statement.
        assertEquals("""
                set(T t), parameter 0                      INDEPENDENT_HC book: INDEPENDENT_HC agrees
                get(), return                              INDEPENDENT_HC book: INDEPENDENT_HC agrees
                getOrDefault(T), return                    INDEPENDENT    book: INDEPENDENT_HC STRONGER
                """, new Positions()
                .parameter(setOnce, "set", 1, 0, "set(T t), parameter 0", HC)
                .method(setOnce, "get", 0, "get(), return", HC)
                .method(setOnce, "getOrDefault", 1, "getOrDefault(T), return", HC)
                .render());
    }

    // ---------------------------------------------------------------------------------------------------------
    // EventuallyFinal
    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String EVENTUALLY_FINAL = """
            public class EventuallyFinal<T> {
              private T value;
              private boolean isFinal;

              public T get() {
                return value;
              }

              public void setFinal(T value) {
                if (this.isFinal) throw new IllegalStateException("Trying to overwrite final value");
                this.isFinal = true;
                this.value = value;
              }

              public void setVariable(T value) {
                if (this.isFinal) throw new IllegalStateException("Value is already final");
                this.value = value;
              }

              public boolean isFinal() {
                return isFinal;
              }
            }
            """;

    @DisplayName("EventuallyFinal: the three positions the book annotates @Independent(hc=true)")
    @Test
    public void testEventuallyFinal() {
        TypeInfo eventuallyFinal = analyze("EventuallyFinal", EVENTUALLY_FINAL);
        assertEquals("""
                get(), return                              INDEPENDENT_HC book: INDEPENDENT_HC agrees
                setFinal(T), parameter 0                   INDEPENDENT_HC book: INDEPENDENT_HC agrees
                setVariable(T), parameter 0                INDEPENDENT_HC book: INDEPENDENT_HC agrees
                """, new Positions()
                .method(eventuallyFinal, "get", 0, "get(), return", HC)
                .parameter(eventuallyFinal, "setFinal", 1, 0, "setFinal(T), parameter 0", HC)
                .parameter(eventuallyFinal, "setVariable", 1, 0, "setVariable(T), parameter 0", HC)
                .render());
    }

    // ---------------------------------------------------------------------------------------------------------
    // SetOnceMap. Freezable comes along because SetOnceMap extends it, and the mark it carries is what makes the
    // type eventually immutable at all.
    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String SET_ONCE_MAP = """
            import java.util.HashMap;
            import java.util.Map;
            import java.util.Objects;

            public class SetOnceMap<K, V> extends Freezable {
              private final Map<K, V> map = new HashMap<>();

              public void put(K k, V v) {
                Objects.requireNonNull(k);
                Objects.requireNonNull(v);
                ensureNotFrozen();
                if (isSet(k)) throw new IllegalStateException("Already decided on " + k);
                map.put(k, v);
              }

              public V get(K k) {
                if (!isSet(k)) throw new IllegalStateException("Not yet decided on " + k);
                return Objects.requireNonNull(map.get(k));
              }

              public boolean isSet(K k) {
                return map.containsKey(k);
              }
            }

            abstract class Freezable {
              private volatile boolean frozen;

              public void freeze() {
                ensureNotFrozen();
                frozen = true;
              }

              public boolean isFrozen() {
                return frozen;
              }

              public void ensureNotFrozen() {
                if (frozen) throw new IllegalStateException("Already frozen!");
              }
            }
            """;

    @DisplayName("SetOnceMap: the three positions the book annotates @Independent(hc=true)")
    @Test
    public void testSetOnceMap() {
        TypeInfo setOnceMap = analyze("SetOnceMap", SET_ONCE_MAP);
        MethodInfo put = setOnceMap.findUniqueMethod("put", 2);
        assertEquals("""
                put(K k, V v), parameter k                 INDEPENDENT_HC book: INDEPENDENT_HC agrees
                put(K k, V v), parameter v                 INDEPENDENT_HC book: INDEPENDENT_HC agrees
                get(K), return                             INDEPENDENT_HC book: INDEPENDENT_HC agrees
                """, new Positions()
                .parameter(put, 0, "put(K k, V v), parameter k", HC)
                .parameter(put, 1, "put(K k, V v), parameter v", HC)
                .method(setOnceMap, "get", 1, "get(K), return", HC)
                .render());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Lazy, as SHIPPED: supplier is final and is never cleared.
    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String LAZY_SHIPPED = """
            import java.util.Objects;
            import java.util.function.Supplier;

            public class Lazy<T> {
              private final Supplier<T> supplier;
              private volatile T t;

              public Lazy(Supplier<T> supplierParam) {
                if (supplierParam == null) throw new NullPointerException("Null not allowed");
                this.supplier = supplierParam;
              }

              public T get() {
                if (t != null) return t;
                t = Objects.requireNonNull(supplier.get());
                return t;
              }

              public boolean hasBeenEvaluated() {
                return t != null;
              }
            }
            """;

    // ---------------------------------------------------------------------------------------------------------
    // Lazy, as the BOOK prints it: supplier is not final, and get() clears it at the transition.
    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String LAZY_BOOK = """
            import java.util.Objects;
            import java.util.function.Supplier;

            public class Lazy<T> {
              private Supplier<T> supplier;
              private volatile T t;

              public Lazy(Supplier<T> supplier) {
                this.supplier = supplier;
              }

              public T get() {
                if (t != null) return t;
                t = Objects.requireNonNull(supplier.get());
                supplier = null;
                return t;
              }

              public boolean hasBeenEvaluated() {
                return t != null;
              }
            }
            """;

    /**
     * The one block that is not a formality. The book annotates the {@code supplier} field
     * {@code @Independent(hc=true, after="t")} and spends fifteen lines explaining why it may be: the field is
     * blanked at the transition, so it is out of the picture afterwards, which is the case the book's extension of
     * rule 2 ("or equal to null") exists to license. The analyzer says {@code DEPENDENT} — and says it whether or
     * not the field is actually cleared, as {@link #testLazyBook()} shows.
     */
    @DisplayName("Lazy as shipped: the three positions the book annotates @Independent(hc=true)")
    @Test
    public void testLazyShipped() {
        TypeInfo lazy = analyze("Lazy", LAZY_SHIPPED);
        assertEquals("""
                field supplier                             DEPENDENT      book: INDEPENDENT_HC DIFFERS
                constructor, parameter 0                   DEPENDENT      book: INDEPENDENT_HC DIFFERS
                get(), return                              INDEPENDENT_HC book: INDEPENDENT_HC agrees
                """, new Positions()
                .field(lazy, "supplier", "field supplier", HC)
                .parameter(lazy.findConstructor(1), 0, "constructor, parameter 0", HC)
                .method(lazy, "get", 0, "get(), return", HC)
                .render());
    }

    /**
     * Identical to {@link #testLazyShipped()}, which is the finding: on the analyzer's reading,
     * {@code supplier = null} inside {@code get()} changes nothing. The book's argument for the rule 2 extension
     * does not survive being run.
     */
    @DisplayName("Lazy as the book prints it: clearing supplier changes none of the three")
    @Test
    public void testLazyBook() {
        TypeInfo lazy = analyze("Lazy", LAZY_BOOK);
        assertEquals("""
                field supplier                             DEPENDENT      book: INDEPENDENT_HC DIFFERS
                constructor, parameter 0                   DEPENDENT      book: INDEPENDENT_HC DIFFERS
                get(), return                              INDEPENDENT_HC book: INDEPENDENT_HC agrees
                """, new Positions()
                .field(lazy, "supplier", "field supplier", HC)
                .parameter(lazy.findConstructor(1), 0, "constructor, parameter 0", HC)
                .method(lazy, "get", 0, "get(), return", HC)
                .render());
    }

    // ---------------------------------------------------------------------------------------------------------
    // FirstThen
    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String FIRST_THEN = """
            import java.util.Objects;

            public class FirstThen<S, T> {
              private volatile S first;
              private volatile T then;

              public FirstThen(S first) {
                this.first = Objects.requireNonNull(first);
              }

              public boolean isFirst() {
                return first != null;
              }

              public void set(T then) {
                Objects.requireNonNull(then);
                synchronized (this) {
                  if (first == null) throw new IllegalStateException("Already set");
                  this.then = then;
                  first = null;
                }
              }

              public S getFirst() {
                if (first == null) throw new IllegalStateException();
                return first;
              }

              public T get() {
                if (first != null) throw new IllegalStateException("Not yet set");
                assert then != null;
                return then;
              }
            }
            """;

    @DisplayName("FirstThen: the four positions the book annotates @Independent(hc=true)")
    @Test
    public void testFirstThen() {
        TypeInfo firstThen = analyze("FirstThen", FIRST_THEN);
        assertEquals("""
                constructor, parameter 0                   INDEPENDENT_HC book: INDEPENDENT_HC agrees
                set(T), parameter 0                        INDEPENDENT_HC book: INDEPENDENT_HC agrees
                getFirst(), return                         INDEPENDENT_HC book: INDEPENDENT_HC agrees
                get(), return                              INDEPENDENT_HC book: INDEPENDENT_HC agrees
                """, new Positions()
                .parameter(firstThen.findConstructor(1), 0, "constructor, parameter 0", HC)
                .parameter(firstThen, "set", 1, 0, "set(T), parameter 0", HC)
                .method(firstThen, "getFirst", 0, "getFirst(), return", HC)
                .method(firstThen, "get", 0, "get(), return", HC)
                .render());
    }
}
