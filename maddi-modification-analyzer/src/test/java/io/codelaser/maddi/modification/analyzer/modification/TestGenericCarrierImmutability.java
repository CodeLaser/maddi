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

package io.codelaser.maddi.modification.analyzer.modification;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⭐ A generic carrier is {@code @ImmutableHC} even when its final field is recorded as MODIFIED. That is the
 * point of hidden content: an unbound type parameter's object is not this type's to keep still, so
 * {@code UNMODIFIED_FIELD} on such a field does not cap the type, and neither does a method that is modifying
 * only because it hands the field to somebody else.
 * <p>
 * This is a CHARACTERIZATION test, written 2026-09-22 while chasing why vavr reaches zero immutable collections.
 * It exists because it refutes two plausible-sounding explanations, both of which I believed before measuring:
 * <ul>
 * <li>⛔ "the field is handed to an unknown function, so the carrier is capped" — REFUTED by {@code Carrier}:
 *     {@code apply} is {@code nonModifying=false} and the field {@code unmodified=false}, and the type is still
 *     {@code @Immutable(hc=true)}. (Related but separate: standard functional interfaces get an implicit
 *     {@code @IgnoreModifications} from {@code ShallowMethodAnalyzer.computeParameterIgnoreModifications}, which
 *     returns TRUE for any {@code java.util.function} type — but that rule lives in the SHALLOW analyzer, i.e.
 *     for bodiless methods, and measured on vavr ZERO of 1286 methods with a {@code java.util.function}
 *     parameter carry {@code ignoreModificationsParameter} in the output, bodiless ones included.)</li>
 * <li>⛔ "the new instance is built by a static factory on ANOTHER type, so the bootstrap goes pessimistic" —
 *     REFUTED by {@code Indirect}, which is the exact shape of {@code io.vavr.Tuple2.swap() -> Tuple.of(...)}
 *     and comes out {@code @Immutable(hc=true)} with both fields {@code unmodified=true}.</li>
 * </ul>
 * ⚠ So vavr's {@code io.vavr.Tuple2} — literally {@code public final T1 _1; public final T2 _2;} — landing on
 * FINAL_FIELDS with BOTH fields modified and 16 of its 26 methods modifying is NOT explained by any of these
 * shapes, and is not reproducible at unit scale. Whatever caps it is a corpus-scale effect; the tool for that is
 * {@code MODREACH_EXPLAIN=io.vavr.Tuple2} on a real run, which prints the BFS chain from the modification seed,
 * not another fixture. Measured facts that still hold and constrain the answer: {@code io.vavr.Tuple.of}'s
 * parameters carry no {@code unmodifiedParameter} (absent = modified), {@code Tuple2.containerType} is absent
 * (= false), and {@code Tuple2.toEntry()} — which constructs the JDK's {@code AbstractMap.SimpleEntry}, a type
 * whose hints already state its parameters are unmodified — is the one constructing method that stays clean.
 */
public class TestGenericCarrierImmutability extends CommonTest {

    @Language("java")
    private static final String GENERIC_CARRIER = """
            package a.b;
            import java.util.function.Function;

            public class X {

                // the Tuple2 shape, reduced: one final field of unbound type parameter, handed to a function
                static class Carrier<T> {
                    private final T t;

                    Carrier(T t) {
                        this.t = t;
                    }

                    T get() {
                        return t;
                    }

                    <U> U apply(Function<T, U> f) {
                        return f.apply(t);
                    }
                }

                // CONTROL: the same carrier without the function-applying method
                static class PlainCarrier<T> {
                    private final T t;

                    PlainCarrier(T t) {
                        this.t = t;
                    }

                    T get() {
                        return t;
                    }
                }

                // closer to the real io.vavr.Tuple2: two PUBLIC final fields, Comparable, a method taking
                // another instance of itself, and one that hands the fields to a mutable JDK carrier
                static class Pair<T1, T2> implements Comparable<Pair<T1, T2>>, java.io.Serializable {
                    public final T1 _1;
                    public final T2 _2;

                    Pair(T1 _1, T2 _2) {
                        this._1 = _1;
                        this._2 = _2;
                    }

                    <U> U apply(java.util.function.BiFunction<? super T1, ? super T2, ? extends U> f) {
                        return f.apply(_1, _2);
                    }

                    Pair<T1, T2> concat(Pair<T1, T2> that) {
                        return new Pair<>(that._1, that._2);
                    }

                    java.util.Map.Entry<T1, T2> toEntry() {
                        return new java.util.AbstractMap.SimpleEntry<>(_1, _2);
                    }

                    @Override
                    public int compareTo(Pair<T1, T2> o) {
                        return 0;
                    }
                }

                // the io.vavr.Tuple2 shape EXACTLY: the new instance is built by a static factory on ANOTHER
                // type (io.vavr.Tuple.of), not by a direct `new` in this class. Pair.concat above does the
                // direct `new` and comes out non-modifying; on vavr, Tuple2.swap()/update1()/concat() all go
                // through Tuple.of and come out MODIFYING, while toEntry() -- which constructs a JDK type whose
                // hints already state its parameters are unmodified -- stays clean.
                interface Factory {
                    static <A, B> Indirect<A, B> of(A a, B b) {
                        return new Indirect<>(a, b);
                    }
                }

                static class Indirect<T1, T2> {
                    public final T1 _1;
                    public final T2 _2;

                    Indirect(T1 _1, T2 _2) {
                        this._1 = _1;
                        this._2 = _2;
                    }

                    Indirect<T2, T1> swap() {
                        return Factory.of(_2, _1);
                    }

                    java.util.Map.Entry<T1, T2> toEntry() {
                        return new java.util.AbstractMap.SimpleEntry<>(_1, _2);
                    }
                }
            }
            """;

    @DisplayName("a generic carrier is @ImmutableHC even when its final field is recorded as modified")
    @Test
    public void genericCarrierIsImmutableHc() throws IOException {
        run(false);
    }

    @DisplayName("... and the same with the MODREACH cutover on, as every corpus run has it")
    @Test
    public void genericCarrierIsImmutableHcUnderModReach() throws IOException {
        run(true);
    }

    private void run(boolean modReach) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse("a.b.X", GENERIC_CARRIER);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10)
                        .setModificationViaReachability(modReach).build());
        analyzer.analyze(analysisOrder);
        System.out.println("### modReach=" + modReach);

        for (String name : new String[]{"Carrier", "PlainCarrier", "Pair", "Indirect"}) {
            TypeInfo sub = typeInfo.findSubType(name);
            Value.Immutable immutable = sub.analysis()
                    .getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
            assertNotNull(immutable, name + " has no immutableType");
            assertTrue(immutable.isAtLeastImmutableHC(), name + " must reach @ImmutableHC; have " + immutable);
        }

        // and the load-bearing half: Carrier gets there DESPITE a modified field and a modifying method, so the
        // assertion above is about hidden content and not about the carrier being trivially clean. PlainCarrier
        // is the control that shows the difference is real.
        TypeInfo carrier = typeInfo.findSubType("Carrier");
        assertEquals(ValueImpl.BoolImpl.FALSE, carrier.fields().getFirst().analysis()
                .getOrNull(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.class), "Carrier.t must be MODIFIED");
        assertEquals(ValueImpl.BoolImpl.TRUE, typeInfo.findSubType("PlainCarrier").fields().getFirst().analysis()
                .getOrNull(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.class),
                "the control's field must be UNMODIFIED, or the contrast proves nothing");
    }
}
