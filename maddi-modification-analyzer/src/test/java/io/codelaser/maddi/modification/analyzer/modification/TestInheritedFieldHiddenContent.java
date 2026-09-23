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

import static org.junit.jupiter.api.Assertions.*;

/**
 * The hc-free level ({@code @Immutable}) requires every instance field to be of a deeply immutable type — and an
 * INHERITED instance field is as much this object's content as an own one. Until 2026-09-23 only own fields were
 * checked, so a field-less final subclass of an {@code @Immutable(hc=true)} class came out hc-free.
 * <p>
 * Found on Guava ({@code com.google.common.collect.Synchronized.SynchronizedTable}, which inherits
 * {@code final Object delegate} from {@code SynchronizedObject}) when the interface walk
 * ({@code TestSiblingCapsThroughHierarchy}) removed the {@code Table} cap that had been hiding it.
 * {@code WithString}/{@code SubString} is the control: an inherited {@code String} is not hidden content, so that
 * subclass must stay hc-free. {@code SubObject} is saved by independence already (it inherits {@code WithObject}'s
 * hc independence); {@code SyncTab} is not ({@code SyncObject} is {@code @Independent}), which is why the Guava
 * shape is spelled out.
 */
public class TestInheritedFieldHiddenContent extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;

            public class X {

                static class WithObject {
                    private final Object delegate;

                    WithObject(Object delegate) {
                        this.delegate = delegate;
                    }

                    Object delegate() {
                        return delegate;
                    }
                }

                static final class SubObject extends WithObject {
                    SubObject(Object delegate) {
                        super(delegate);
                    }

                    int size() {
                        return 3;
                    }
                }

                static class WithString {
                    private final String s;

                    WithString(String s) {
                        this.s = s;
                    }

                    String s() {
                        return s;
                    }
                }

                static final class SubString extends WithString {
                    SubString(String s) {
                        super(s);
                    }

                    int size() {
                        return 3;
                    }
                }

                // the Guava shape: com.google.common.collect.Synchronized.SynchronizedObject / SynchronizedTable
                interface Tab<E> {
                    void clear();

                    int size();
                }

                static class TabImpl<E> implements Tab<E> {
                    private int n;

                    public void clear() {
                        n = 0;
                    }

                    public int size() {
                        return n;
                    }
                }

                static class SyncObject implements java.io.Serializable {
                    final Object delegate;
                    final Object mutex;

                    SyncObject(Object delegate, Object mutex) {
                        this.delegate = delegate;
                        this.mutex = mutex == null ? this : mutex;
                    }

                    Object delegate() {
                        return delegate;
                    }

                    @Override
                    public String toString() {
                        synchronized (mutex) {
                            return delegate.toString();
                        }
                    }
                }

                static final class SyncTab<E> extends SyncObject implements Tab<E> {
                    SyncTab(Tab<E> delegate, Object mutex) {
                        super(delegate, mutex);
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    Tab<E> delegate() {
                        return (Tab<E>) super.delegate();
                    }

                    @Override
                    public void clear() {
                        synchronized (mutex) {
                            delegate().clear();
                        }
                    }

                    @Override
                    public int size() {
                        synchronized (mutex) {
                            return delegate().size();
                        }
                    }
                }
            }
            """;

    @DisplayName("an inherited field of a hidden-content type keeps the subclass at @Immutable(hc=true)")
    @Test
    public void test() throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo x = bundle.javaInspector().parse("a.b.X", INPUT);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(x);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build());
        analyzer.analyze(analysisOrder);
        for (String name : List.of("WithObject", "SubObject", "WithString", "SubString", "Tab", "SyncObject",
                "SyncTab")) {
            TypeInfo t = x.findSubType(name);
            System.out.println("### " + name + " immutable=" + immutable(t) + " independent="
                               + t.analysis().getOrNull(PropertyImpl.INDEPENDENT_TYPE, ValueImpl.IndependentImpl.class));
        }

        // premises
        assertSame(ValueImpl.ImmutableImpl.IMMUTABLE_HC, immutable(x.findSubType("WithObject")),
                "an Object field is hidden content");
        // control: an inherited String is not hidden content
        assertSame(ValueImpl.ImmutableImpl.IMMUTABLE, immutable(x.findSubType("SubString")));
        // target
        assertSame(ValueImpl.ImmutableImpl.IMMUTABLE_HC, immutable(x.findSubType("SubObject")),
                "SubObject inherits the Object field");
        // the Guava shape. Whatever else holds, SyncTab inherits 'Object delegate', so it is never hc-free. Under
        // the interface walk (no Tab cap) it lands on @Immutable(hc=true). ⚠ The RIGHT answer is FINAL_FIELDS or
        // lower: SyncTab.clear() modifies the inherited delegate through its down-casting delegate(). That is a
        // separate gap -- a type's rule reads only its OWN fields' UNMODIFIED_FIELD, and SyncObject may treat the
        // Object-typed field as hidden content -- and it is deliberately not pinned here.
        assertFalse(immutable(x.findSubType("SyncTab")).isImmutable(),
                "SyncTab inherits an Object field; have " + immutable(x.findSubType("SyncTab")));
    }

    private static Value.Immutable immutable(TypeInfo t) {
        return t.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
    }
}
