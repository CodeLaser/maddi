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
import io.codelaser.maddi.cst.api.info.MethodInfo;
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
 * ⭐ A stateful SIBLING must not cap an immutable implementation through a shared interface.
 * <p>
 * The vavr shape, reduced (measured on vavr 1.0.1, 2026-09-23, with {@code EC_TYPE_DEBUG}): {@code Option.Some}
 * has every method non-modifying and a single {@code private final T value}, yet lands on FINAL_FIELDS, only
 * through its hierarchy {@code Some -> Option -> Value}. {@code io.vavr.Value}'s abstract methods fold over ALL
 * their implementations ({@code AbstractMethodAnalyzerImpl}), and those include {@code io.vavr.collection.Iterator}
 * — a genuinely stateful {@code Value}. So {@code Value} is honestly FINAL_FIELDS; the defect is that
 * {@code TypeImmutableAnalyzerImpl} takes the MIN over supertype verdicts, which hands the sibling's mutability
 * to every other implementation.
 * <ul>
 * <li>{@code V} is {@code io.vavr.Value}: abstract {@code get}/{@code isEmpty}/{@code prefix}, and a default
 *     {@code getOrNull} (which, like every own concrete method, does NOT enter a type verdict — only fields and
 *     abstract methods do).</li>
 * <li>{@code Cursor} is {@code io.vavr.collection.Iterator}: implements {@code V} directly, and its {@code get}
 *     and {@code prefix} modify it. CONTROL: must stay MUTABLE, and must keep {@code V} at FINAL_FIELDS.</li>
 * <li>{@code O} is {@code io.vavr.control.Option}: re-declares {@code get}/{@code isEmpty} (so those fold over
 *     {@code Some}/{@code None} only and come out non-modifying) but NOT {@code prefix} — as {@code Option} does
 *     not re-declare {@code stringPrefix()}. So a hierarchy walk alone does not free {@code O}: the inherited
 *     {@code V.prefix()} must be folded over {@code O}'s cone ({@code Some}, {@code None}) only.</li>
 * <li>{@code Some}/{@code None} are {@code Option.Some}/{@code None}. {@code None} deliberately has no static
 *     {@code INSTANCE} field: on vavr that field is recorded modified and would cap {@code None} by itself (the
 *     type rule's field loop does not skip static fields) — a separate defect, kept out of this fixture.</li>
 * </ul>
 * Premises are asserted separately from the target, so that a failure says WHICH link of the explanation broke.
 */
public class TestSiblingCapsThroughHierarchy extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            import java.util.NoSuchElementException;

            public class X {

                interface V<T> {
                    T get();

                    boolean isEmpty();

                    String prefix();

                    default T getOrNull() {
                        return isEmpty() ? null : get();
                    }
                }

                static class Cursor<T> implements V<T> {
                    private final List<T> list;
                    private int i;
                    private int prefixCalls;

                    Cursor(List<T> list) {
                        this.list = list;
                    }

                    @Override
                    public T get() {
                        return list.get(i++);
                    }

                    @Override
                    public boolean isEmpty() {
                        return i >= list.size();
                    }

                    @Override
                    public String prefix() {
                        prefixCalls++;
                        return "Cursor";
                    }
                }

                interface O<T> extends V<T> {
                    @Override
                    T get();

                    @Override
                    boolean isEmpty();
                }

                static final class Some<T> implements O<T> {
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

                    @Override
                    public String prefix() {
                        return "Some";
                    }
                }

                static final class None<T> implements O<T> {
                    @Override
                    public T get() {
                        throw new NoSuchElementException();
                    }

                    @Override
                    public boolean isEmpty() {
                        return true;
                    }

                    @Override
                    public String prefix() {
                        return "None";
                    }
                }
            }
            """;

    @DisplayName("a stateful sibling does not cap an immutable implementation through a shared interface")
    @Test
    public void siblingDoesNotCap() throws IOException {
        run(false);
    }

    @DisplayName("... and the same with the MODREACH cutover on, as every corpus run has it")
    @Test
    public void siblingDoesNotCapUnderModReach() throws IOException {
        run(true);
    }

    private void run(boolean modReach) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo x = bundle.javaInspector().parse("a.b.X", INPUT);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(x);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10)
                        .setModificationViaReachability(modReach).build());
        analyzer.analyze(analysisOrder);
        TypeInfo v = x.findSubType("V");
        TypeInfo cursor = x.findSubType("Cursor");
        TypeInfo o = x.findSubType("O");
        TypeInfo some = x.findSubType("Some");
        TypeInfo none = x.findSubType("None");
        System.out.println("### modReach=" + modReach);
        for (TypeInfo t : List.of(v, cursor, o, some, none)) {
            System.out.println("### " + t.simpleName() + " immutable=" + immutable(t)
                               + " independent=" + t.analysis().getOrNull(PropertyImpl.INDEPENDENT_TYPE,
                    ValueImpl.IndependentImpl.class));
        }

        // premises: each is one link of the explanation; they hold BEFORE the fix and must keep holding after it
        assertEquals(ValueImpl.BoolImpl.FALSE, nonModifying(method(cursor, "get")), "Cursor.get modifies");
        assertEquals(ValueImpl.BoolImpl.FALSE, nonModifying(method(v, "get")),
                "V.get folds over Cursor.get, so it is modifying");
        assertEquals(ValueImpl.BoolImpl.FALSE, nonModifying(method(v, "prefix")),
                "V.prefix folds over Cursor.prefix, so it is modifying");
        assertEquals(ValueImpl.BoolImpl.TRUE, nonModifying(method(o, "get")),
                "O re-declares get: its fold covers Some/None only");
        assertEquals(ValueImpl.BoolImpl.TRUE, nonModifying(method(o, "isEmpty")),
                "O re-declares isEmpty: its fold covers Some/None only");
        for (TypeInfo t : List.of(some, none)) {
            for (MethodInfo mi : t.methods()) {
                assertEquals(ValueImpl.BoolImpl.TRUE, nonModifying(mi), mi + " is non-modifying");
            }
        }

        // controls: the stateful sibling, and the interface it really does make non-immutable
        assertTrue(immutable(cursor).isMutable(), "Cursor is stateful; have " + immutable(cursor));
        assertFalse(immutable(v).isAtLeastImmutableHC(), "V is honestly not immutable; have " + immutable(v));

        // target
        for (TypeInfo t : List.of(o, some, none)) {
            Value.Immutable imm = immutable(t);
            assertNotNull(imm, t.simpleName() + " has no immutableType");
            assertTrue(imm.isAtLeastImmutableHC(), t.simpleName() + " must reach @ImmutableHC; have " + imm);
        }
    }

    private static Value.Immutable immutable(TypeInfo t) {
        return t.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
    }

    private static Value.Bool nonModifying(MethodInfo mi) {
        return mi.analysis().getOrDefault(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE);
    }

    private static MethodInfo method(TypeInfo t, String name) {
        return t.methods().stream().filter(m -> m.name().equals(name)).findFirst().orElseThrow();
    }
}
