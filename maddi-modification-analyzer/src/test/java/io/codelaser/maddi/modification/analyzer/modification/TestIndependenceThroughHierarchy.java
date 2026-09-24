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

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A type's independence is not capped by the verdict of the interfaces it implements: what it inherits from an
 * interface are the abstract methods nothing on its own path overrides, and only those are judged -- the independence
 * twin of the immutability walk ({@code TestSiblingCapsThroughHierarchy}). The shape is vavr's {@code Option}:
 * {@code Val.get()} is abstract and exposes hidden content, so {@code Val} and {@code Opt} are {@code @Independent(hc
 * = true)}; {@code None} overrides {@code get()} with a method that exposes nothing. Under the old min over
 * supertypes {@code None} inherited {@code hc} from {@code Opt}, and with it the {@code hc} label on its
 * immutability, although it has no content at all.
 * <ul>
 * <li>{@code None}: every hc method of the hierarchy is overridden on its path -- {@code @Independent},
 *     {@code @Immutable}.</li>
 * <li>{@code Some}: the control; it holds a {@code T} and returns it -- {@code @Independent(hc = true)}.</li>
 * <li>{@code Base}: abstract, overrides nothing -- still inherits {@code get()} and stays hc.</li>
 * </ul>
 * vavr's {@code None} is also a singleton ({@code static final None<?> INSTANCE}); that shape is left out here. The
 * self field is skipped for the immutability verdict ({@code TestStaticAndSelfFields}), but the PUBLISHED independence
 * still counts it, and {@code AnalysisHelper.typeImmutable} grades {@code None<?>} by its wildcard argument even when
 * {@code None} itself is {@code @Immutable} -- so that field reads hc. A separate question from the walk.
 */
public class TestIndependenceThroughHierarchy extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.NoSuchElementException;

            public class X {
                interface Val<T> {
                    T get();
                    boolean isEmpty();
                    default boolean isDefined() {
                        return !isEmpty();
                    }
                }

                interface Opt<T> extends Val<T> {
                    default T orNull() {
                        return isEmpty() ? null : get();
                    }
                }

                static final class None<T> implements Opt<T> {
                    @Override
                    public T get() {
                        throw new NoSuchElementException();
                    }

                    @Override
                    public boolean isEmpty() {
                        return true;
                    }
                }

                static final class Some<T> implements Opt<T> {
                    private final T value;

                    Some(T value) {
                        this.value = value;
                    }

                    @Override
                    public T get() {
                        return value;
                    }

                    @Override
                    public boolean isEmpty() {
                        return false;
                    }
                }

                static abstract class Base<T> implements Opt<T> {
                    @Override
                    public boolean isEmpty() {
                        return false;
                    }
                }
            }
            """;

    @DisplayName("an overridden hc method of an interface does not cap the implementation's independence")
    @Test
    public void test() throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo x = bundle.javaInspector().parse("a.b.X", INPUT);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(x);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build());
        analyzer.analyze(analysisOrder);
        for (String name : List.of("Val", "Opt", "None", "Some", "Base")) {
            TypeInfo t = x.findSubType(name);
            System.out.println("### " + name + " " + immutable(t) + " " + independent(t));
        }

        assertSame(ValueImpl.IndependentImpl.INDEPENDENT_HC, independent(x.findSubType("Opt")), "abstract get()");
        assertSame(ValueImpl.IndependentImpl.INDEPENDENT, independent(x.findSubType("None")),
                "None overrides get(), the only hc method of its hierarchy");
        assertSame(ValueImpl.ImmutableImpl.IMMUTABLE, immutable(x.findSubType("None")));
        assertSame(ValueImpl.IndependentImpl.INDEPENDENT_HC, independent(x.findSubType("Some")), "returns its T");
        assertSame(ValueImpl.ImmutableImpl.IMMUTABLE_HC, immutable(x.findSubType("Some")));
        assertSame(ValueImpl.IndependentImpl.INDEPENDENT_HC, independent(x.findSubType("Base")),
                "get() is inherited, not overridden");
    }

    private static Value.Independent independent(TypeInfo t) {
        return t.analysis().getOrNull(PropertyImpl.INDEPENDENT_TYPE, ValueImpl.IndependentImpl.class);
    }

    private static Value.Immutable immutable(TypeInfo t) {
        return t.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
    }
}
