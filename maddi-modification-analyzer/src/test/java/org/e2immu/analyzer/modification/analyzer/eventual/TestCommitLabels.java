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

package org.e2immu.analyzer.modification.analyzer.eventual;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.impl.EventualCluster;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code commitLabels} reframe of "eventually non-modifying" (EVENTUALCLUSTER only; handoff spec in
 * {@code docs/handoff-eventual-interface-nonmodification.md} §5): a call is non-modifying-of-{@code this} after
 * mark M iff every {@code this}-derived value it touches -- receiver <em>and</em> arguments -- is committed by M.
 * These are the cross-reference accessor shapes ({@code returnType().typeInfo().isEnclosedIn(this.typeInfo)},
 * {@code compilationUnitOrEnclosingType.getRight().primaryType()}) that keep the {@code *Info} interfaces from
 * their eventual verdict; off the gate, the old receiver-only rooting must be untouched.
 */
public class TestCommitLabels extends CommonTest {

    private Set<String> nonModAfter(TypeInfo typeInfo, String methodName, int params) {
        return typeInfo.findUniqueMethod(methodName, params).analysis()
                .getOrDefault(PropertyImpl.EVENTUALLY_NON_MODIFYING_METHOD, ValueImpl.SetOfStringsImpl.EMPTY_SET)
                .set();
    }

    // the MethodInfoImpl.isFactoryMethod shape, reduced: a self-referencing cluster type whose accessors modify
    // (before the mark) through a cross-reference field 'other' of the candidate type itself
    @Language("java")
    private static final String INPUT_CROSS = """
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class B {
              static class T {
                private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
                private final T other;
                T(T other) { this.other = other; }
                public void commit(String s) { inspection.setFinal(s); }
                public int size() { return inspection.get().length(); }
                public int cross() { return other.size(); }
                public T friend() { inspection.get(); return other; }
                public int chained() { return friend().size(); }
                public boolean sameAs(T t) { return size() == t.size(); }
                public boolean encloses() { return other.sameAs(this.other); }
              }
            }
            """;

    @DisplayName("gate on: a modifying call through a committable cross-reference field is excused by its label")
    @Test
    public void testCrossReferenceReceiver() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = true;
        try {
            TypeInfo B = javaInspector.parse("B", INPUT_CROSS);
            analyzer.go(prepWork(B));
            TypeInfo T = B.findSubType("T");

            // the baseline read-through accessor, unchanged by the reframe
            assertEquals(Set.of("inspection"), nonModAfter(T, "size", 0));
            // other.size(): the receiver is this.other, a committable cross-reference field (cluster seed)
            assertEquals(Set.of("other"), nonModAfter(T, "cross", 0));
            // friend() forwards through this and returns the cross-reference: non-modifying after 'inspection'
            assertEquals(Set.of("inspection"), nonModAfter(T, "friend", 0));
            // friend().size(): the receiver chain roots in an *eventually*-non-modifying this-accessor whose
            // return type is a cluster candidate -- the exact returnType().typeInfo() shape that used to bail
            assertEquals(Set.of("inspection"), nonModAfter(T, "chained", 0));
            // size() == t.size(): a modifying call on a parameter is not a modification of this
            assertEquals(Set.of("inspection"), nonModAfter(T, "sameAs", 1));
            // other.sameAs(this.other): a committable this-field ARGUMENT is excused by its own label -- the
            // isEnclosedIn(this.typeInfo) shape the old all-or-nothing parameter guard bailed on
            assertEquals(Set.of("other"), nonModAfter(T, "encloses", 0));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    @DisplayName("gate off: the cross-reference shapes stay unexcused (old behaviour, Caveat 3)")
    @Test
    public void testGateOffUnchanged() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = false;
        try {
            TypeInfo B = javaInspector.parse("B", INPUT_CROSS);
            analyzer.go(prepWork(B));
            TypeInfo T = B.findSubType("T");

            // the plain read-through accessor works without the cluster
            assertEquals(Set.of("inspection"), nonModAfter(T, "size", 0));
            // but every cross-reference shape bails: T's own verdict is circular, so 'other' is not committable
            assertEquals(Set.of(), nonModAfter(T, "cross", 0));
            assertEquals(Set.of(), nonModAfter(T, "chained", 0));
            assertEquals(Set.of(), nonModAfter(T, "sameAs", 1));
            assertEquals(Set.of(), nonModAfter(T, "encloses", 0));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    // the TypeInfoImpl.primaryType shape: the committable content sits behind an immutable Either wrapper,
    // and behind a local variable holding an eventually-non-modifying accessor's result
    @Language("java")
    private static final String INPUT_EITHER = """
            import org.e2immu.support.Either;
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class D {
              static class T {
                private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
                private final Either<String, T> either;
                T(Either<String, T> e) { this.either = e; }
                public void commit(String s) { inspection.setFinal(s); }
                public int size() { return inspection.get().length(); }
                public T friend() { inspection.get(); return either.getRight(); }
                public int viaEither() { inspection.get(); return either.getRight().size(); }
                public int viaLocal() {
                  T t = friend();
                  return t.size();
                }
              }
            }
            """;

    @DisplayName("gate on: chains through an immutable wrapper and through a this-derived local resolve")
    @Test
    public void testEitherAndLocalChains() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = true;
        try {
            TypeInfo D = javaInspector.parse("D", INPUT_EITHER);
            analyzer.go(prepWork(D));
            TypeInfo T = D.findSubType("T");

            // either.getRight().size(): a non-modifying read on the immutable wrapper roots the chain in the
            // field; the inspection.get() makes the method modifying in the first place (a chain through the
            // wrapper's hidden content alone is not a modification of this)
            assertEquals(Set.of("either", "inspection"), nonModAfter(T, "viaEither", 0));
            // T t = friend(); t.size(): the local carries friend()'s commit labels (this-derived, committable).
            // friend() itself only needs 'inspection' -- its getRight() is @NotModified, so no receiver label
            assertEquals(Set.of("inspection"), nonModAfter(T, "viaLocal", 0));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    // every bail shape in one fixture: bare this as argument, a non-committable field as receiver, and a local
    // aliasing a non-committable field (the aliasing trap one hop removed -- NOT in the handoff spec, which
    // treats all locals as underived; buildLocalCommitMap closes that hole)
    @Language("java")
    private static final String INPUT_BAIL = """
            import java.util.ArrayList;
            import java.util.List;
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class C {
              static class T {
                private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
                private final List<String> items = new ArrayList<>();
                private final T peer;
                T(T peer) { this.peer = peer; }
                public void commit(String s) { inspection.setFinal(s); }
                public int len() { return inspection.get().length(); }
                public boolean same(T t) { return len() == t.len(); }
                public boolean bailBareThis() { return peer.same(this); }
                public void bailMutableField() { items.add("x"); }
                public void bailLocalAlias() {
                  List<String> l = items;
                  l.add("x");
                  inspection.get();
                }
                public int freshLocal() {
                  StringBuilder sb = new StringBuilder();
                  sb.append(len());
                  return sb.length();
                }
              }
            }
            """;

    @DisplayName("gate on: bare this, non-committable fields, and aliasing locals bail; fresh locals do not")
    @Test
    public void testBailShapes() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = true;
        try {
            TypeInfo C = javaInspector.parse("C", INPUT_BAIL);
            analyzer.go(prepWork(C));
            TypeInfo T = C.findSubType("T");

            assertEquals(Set.of("inspection"), nonModAfter(T, "len", 0));
            // peer.same(this): the receiver is committable, but bare this mid-transition never is
            assertEquals(Set.of(), nonModAfter(T, "bailBareThis", 0));
            // items.add: a plain mutable field is not committed by any mark
            assertEquals(Set.of(), nonModAfter(T, "bailMutableField", 0));
            // l = items; l.add(x): the local aliases the mutable field -- must NOT be excused even though the
            // body also contains an excusable inspection.get() that would otherwise supply a non-empty label set
            assertEquals(Set.of(), nonModAfter(T, "bailLocalAlias", 0));
            // sb = new StringBuilder(): a fresh object is not this-derived; only len()'s label remains
            assertEquals(Set.of("inspection"), nonModAfter(T, "freshLocal", 0));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }
}
