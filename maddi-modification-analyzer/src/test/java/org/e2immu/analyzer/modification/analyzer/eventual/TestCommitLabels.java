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

    // the ParameterizedTypeImpl.typesReferenced shape (spec-eventually-unmodified-parameter §8.3 item 1):
    // a final, never-modified List field of candidate content is committable one indirection deeper -- but a
    // List of plain mutable content, or a List the owner mutates, is not
    @Language("java")
    private static final String INPUT_CONTAINER = """
            import java.util.ArrayList;
            import java.util.List;
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class F {
              static class T {
                private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
                private final List<T> children;
                private final List<StringBuilder> raw = new ArrayList<>();
                private final List<T> pool = new ArrayList<>();
                T(List<T> children) { this.children = List.copyOf(children); }
                public void commit(String s) { inspection.setFinal(s); }
                public int size() { return inspection.get().length(); }
                public int firstSize() { return children.get(0).size(); }
                public int rawAppend() { return raw.get(0).append('x').length(); }
                public void grow(T t) { pool.add(t); }
                public int poolFirstSize() { return pool.get(0).size(); }
                public T copyish() { inspection.get(); return new T(children); }
              }
            }
            """;

    @DisplayName("gate on: the container ride-along commits a final unmodified List of candidate content")
    @Test
    public void testContainerRideAlong() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = true;
        try {
            TypeInfo F = javaInspector.parse("F", INPUT_CONTAINER);
            analyzer.go(prepWork(F));
            TypeInfo T = F.findSubType("T");

            // children.get(0).size(): the modifying element call is excused because the receiver chain roots
            // in a final, owner-unmodified List of candidate content -- the ride-along one indirection deeper
            assertEquals(Set.of("children"), nonModAfter(T, "firstSize", 0));
            // raw: same wrapper shape, but StringBuilder content is not committable by any mark
            assertEquals(Set.of(), nonModAfter(T, "rawAppend", 0));
            // pool: candidate content, but the owner mutates the list (grow) -- wrapper stability fails
            assertEquals(Set.of(), nonModAfter(T, "poolFirstSize", 0));
            // the argument position at a constructor site: the wrapper handed to a ctor whose body provably
            // handles it safely (List.copyOf -- a defensive copy, no capture)
            assertEquals(Set.of("children", "inspection"), nonModAfter(T, "copyish", 0));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    @DisplayName("gate off: the container ride-along is dormant")
    @Test
    public void testContainerRideAlongGateOff() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = false;
        try {
            TypeInfo F = javaInspector.parse("F", INPUT_CONTAINER);
            analyzer.go(prepWork(F));
            TypeInfo T = F.findSubType("T");
            assertEquals(Set.of("inspection"), nonModAfter(T, "size", 0));
            assertEquals(Set.of(), nonModAfter(T, "firstSize", 0));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    // the Element/Statement breadth round: the downward interface closure -- a markless sub-interface
    // of a candidate interface (Block/Comment under Element) is a cluster member, so a carrier field of
    // that type excuses; a setter-bearing sub-interface (a Builder) is refused on both ends
    @Language("java")
    private static final String INPUT_BREADTH = """
            import java.util.ArrayList;
            import java.util.List;
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class J {
              interface E { int size(); }
              interface Sub extends E { int size2(); }
              interface BuilderLike extends E { String foo(); void setFoo(String foo); }
              static class T implements E {
                private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
                public void commit(String s) { inspection.setFinal(s); }
                @Override public int size() { return inspection.get().length(); }
              }
              static class Carrier {
                private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
                private final Sub sub;
                private final BuilderLike builderLike;
                Carrier(Sub sub, BuilderLike builderLike) {
                  this.sub = sub;
                  this.builderLike = builderLike;
                }
                public void commit(String s) { inspection.setFinal(s); }
                public int viaSub() { inspection.get(); return sub.size2(); }
                public int viaBuilder() { inspection.get(); return builderLike.foo().length(); }
              }
              // the UnaryOperatorImpl.operator shape: a subclass method reading a SUPERCLASS field -- the
              // walk owns inherited fields too; the label names the super's field, tolerated at type level
              // exactly like an inherited mark
              static class Base {
                protected final T item;
                Base(T item) { this.item = item; }
              }
              static class SubClass extends Base {
                SubClass(T item) { super(item); }
                public int subSize() { return item.size(); }
              }
            }
            """;
    // NOTE: the round's other two mechanisms -- the accessor spelling comments() of this.comments in the
    // container ride-along, and the primitive-stream (mapToInt) handed-on admit -- are corpus-validated
    // only: the unit harness runs without annotated APIs, and the shallow defaults it gives the JDK
    // container/stream types make any fixture non-discriminating (the walk resolves through them with or
    // without the mechanism under test).

    @DisplayName("gate on: downward interface closure admits markless sub-interfaces, refuses setter-bearing ones")
    @Test
    public void testBreadthShapes() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = true;
        try {
            TypeInfo J = javaInspector.parse("J", INPUT_BREADTH);
            // the full pipeline marks abstract Builder setters via the synthetic-fields step; the unit
            // harness does not run it, so arrange the same GET_SET_FIELD property by hand
            MethodInfo setFoo = J.findSubType("BuilderLike").findUniqueMethod("setFoo", 1);
            setFoo.analysis().set(PropertyImpl.GET_SET_FIELD,
                    new ValueImpl.GetSetValueImpl(null, true, -1, false));
            analyzer.go(prepWork(J));
            TypeInfo carrier = J.findSubType("Carrier");

            // sub.size2(): Sub carries no eventual intent of its own, but extends the candidate interface E
            // -- the downward closure admits it, and the field read excuses after [sub]
            assertEquals(Set.of("inspection", "sub"), nonModAfter(carrier, "viaSub", 0));
            // builderLike is a setter-bearing sub-interface: refused (haveSetters can never prove)
            assertEquals(Set.of(), nonModAfter(carrier, "viaBuilder", 0));
            // item is Base's field, read from SubClass: the inherited-field read is excusable by its label
            assertEquals(Set.of("item"), nonModAfter(J.findSubType("SubClass"), "subSize", 0));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    @DisplayName("gate off: the breadth shapes stay unexcused")
    @Test
    public void testBreadthGateOff() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = false;
        try {
            TypeInfo J = javaInspector.parse("J", INPUT_BREADTH);
            analyzer.go(prepWork(J));
            TypeInfo carrier = J.findSubType("Carrier");
            assertEquals(Set.of(), nonModAfter(carrier, "viaSub", 0));
            assertEquals(Set.of(), nonModAfter(carrier, "viaBuilder", 0));
            assertEquals(Set.of(), nonModAfter(J.findSubType("SubClass"), "subSize", 0));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    // the builder() accessor shape (handoff-builder-leans §4A): a body guarded by a @TestMark observation
    // is @Only on the side the guard asserts; and a transition on ANOTHER object's lifecycle (a parameter)
    // no longer bails the walk -- only the arguments carry root-derived content
    @Language("java")
    private static final String INPUT_GUARD = """
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class G {
              static class T {
                private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
                public void commit(String s) { inspection.setFinal(s); }
                public String builderStyle() {
                  assert inspection.isVariable();
                  return inspection.get();
                }
                public String guarded() {
                  if (inspection.isVariable()) return inspection.get();
                  throw new UnsupportedOperationException();
                }
                public String afterGuard() {
                  assert inspection.isFinal();
                  return inspection.get();
                }
                public void fillOther(StringBuilder sb, T other) {
                  inspection.get();
                  other.commit(sb.toString());
                }
              }
            }
            """;

    @DisplayName("gate on: precondition guards classify @Only, and another object's transition is excusable")
    @Test
    public void testGuardsAndForeignTransitions() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = true;
        try {
            TypeInfo G = javaInspector.parse("G", INPUT_GUARD);
            analyzer.go(prepWork(G));
            TypeInfo T = G.findSubType("T");

            var builderStyle = T.findUniqueMethod("builderStyle", 0).analysis()
                    .getOrDefault(PropertyImpl.EVENTUAL_METHOD, ValueImpl.EventualImpl.NOT_EVENTUAL);
            assertTrue(builderStyle.isOnly());
            assertEquals(Boolean.FALSE, builderStyle.after());
            assertEquals(Set.of("inspection"), builderStyle.fields());
            var guarded = T.findUniqueMethod("guarded", 0).analysis()
                    .getOrDefault(PropertyImpl.EVENTUAL_METHOD, ValueImpl.EventualImpl.NOT_EVENTUAL);
            assertTrue(guarded.isOnly());
            assertEquals(Boolean.FALSE, guarded.after());
            var afterGuard = T.findUniqueMethod("afterGuard", 0).analysis()
                    .getOrDefault(PropertyImpl.EVENTUAL_METHOD, ValueImpl.EventualImpl.NOT_EVENTUAL);
            assertTrue(afterGuard.isOnly());
            assertEquals(Boolean.TRUE, afterGuard.after());
            // other.commit(...) is the PARAMETER's transition, not this's: the walk continues, and the
            // method's own pre-mark read supplies the label
            assertEquals(Set.of("inspection"), nonModAfter(T, "fillOther", 2));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    @DisplayName("gate off: precondition guards are not read, foreign transitions still bail")
    @Test
    public void testGuardsGateOff() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = false;
        try {
            TypeInfo G = javaInspector.parse("G", INPUT_GUARD);
            analyzer.go(prepWork(G));
            TypeInfo T = G.findSubType("T");
            var builderStyle = T.findUniqueMethod("builderStyle", 0).analysis()
                    .getOrDefault(PropertyImpl.EVENTUAL_METHOD, ValueImpl.EventualImpl.NOT_EVENTUAL);
            assertFalse(builderStyle.isEventual());
            assertEquals(Set.of(), nonModAfter(T, "fillOther", 2));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    // the rewirePhase1/3 residue (handoff-builder-leans §4b resolution): a MODIFYING fluent chain on another
    // object -- a parameter's copy, or a fresh local read through a chained local -- whose arguments carry
    // root-derived committed content. The handed-on judgment must settle from the not-root (resp. fresh) base
    // plus the accumulated labels, never from a candidacy lean on the wrapper's return type: Wr is
    // deliberately NOT a cluster candidate, so without the excuse these methods bail to ∅
    @Language("java")
    private static final String INPUT_FLUENT = """
            import java.util.ArrayList;
            import java.util.List;
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class H {
              static class Wr {
                private final List<String> parts = new ArrayList<>();
                Wr() { }
                Wr(String first) { parts.add(first); }
                Wr add(String s) { parts.add(s); return this; }
                Wr self() { return this; }
              }
              static class T {
                private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
                public void commit(String s) { inspection.setFinal(s); }
                public String data() { return inspection.get(); }
                public void rewire(Wr copy) { copy.add(data()).add("x"); }
                public void fillFresh() {
                  Wr w = new Wr(data());
                  Wr b = w.self();
                  b.add("x");
                }
              }
            }
            """;

    @DisplayName("gate on: a modifying fluent chain on another object's graph needs no lean on the wrapper type")
    @Test
    public void testFluentChainOnForeignGraph() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = true;
        try {
            TypeInfo H = javaInspector.parse("H", INPUT_FLUENT);
            analyzer.go(prepWork(H));
            TypeInfo T = H.findSubType("T");

            // copy.add(data()).add("x"): the outer add's receiver carries [inspection] (folded from the
            // argument), but the chain's BASE is the copy parameter -- another object's graph; the
            // modification cannot reach this except through content already committed by the labels
            assertEquals(Set.of("inspection"), nonModAfter(T, "rewire", 1));
            // Wr b = w.self(): the local-variable spelling of a fluent chain off a fresh local -- freshness
            // must chase through the assignment graph, exactly as rootedInFresh chases the inline chain
            assertEquals(Set.of("inspection"), nonModAfter(T, "fillFresh", 0));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    @DisplayName("gate off: the fluent-chain shapes stay unexcused")
    @Test
    public void testFluentChainGateOff() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = false;
        try {
            TypeInfo H = javaInspector.parse("H", INPUT_FLUENT);
            analyzer.go(prepWork(H));
            TypeInfo T = H.findSubType("T");
            assertEquals(Set.of(), nonModAfter(T, "rewire", 1));
            assertEquals(Set.of(), nonModAfter(T, "fillFresh", 0));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

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
            // peer.same(this): bare this handed out IS excusable for a cluster-candidate owner (post-marks it
            // is committed; the self-assumption is witnessed and the contraction validates) -- the receiver
            // field contributes its label
            assertEquals(Set.of("peer"), nonModAfter(T, "bailBareThis", 0));
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
