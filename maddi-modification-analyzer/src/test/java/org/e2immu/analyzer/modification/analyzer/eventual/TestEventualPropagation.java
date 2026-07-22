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
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 2 of eventual immutability (road to immutability §060; plan in {@code docs/eventual-immutability.md}):
 * a type holding a field of eventually immutable type inherits the mark, method by method. No preconditions are
 * involved — the callee's contract says which side of the transition it belongs to.
 */
public class TestEventualPropagation extends CommonTest {

    private Value.Eventual eventual(MethodInfo methodInfo) {
        return methodInfo.analysis().getOrDefault(PropertyImpl.EVENTUAL_METHOD,
                ValueImpl.EventualImpl.NOT_EVENTUAL);
    }

    /**
     * The shape of {@code TypeInfoImpl}: one final field of eventually immutable type, one method committing it,
     * accessors reading it. This is the case that must work for maddi to certify its own CST.
     */
    @Language("java")
    private static final String INPUT1 = """
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class B {
              private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();

              public void commit(String s) {
                inspection.setFinal(s);
              }

              public void setVariable(String s) {
                inspection.setVariable(s);
              }

              public boolean isCommitted() {
                return inspection.isFinal();
              }

              public boolean isNotCommitted() {
                return inspection.isVariable();
              }

              public int length() {
                return inspection.get().length();
              }
            }
            """;

    @DisplayName("@Mark/@Only/@TestMark propagate from a field of eventually immutable type")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse("B", INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        // commit() calls the @Mark method setFinal on the field: it effects the transition
        Value.Eventual commit = eventual(B.findUniqueMethod("commit", 1));
        assertTrue(commit.isMark(), "commit() should be @Mark, is " + commit);
        // the label is OUR field, not EventuallyFinalOnDemand's internal 'isFinal'
        assertEquals(Set.of("inspection"), commit.fields());

        // setVariable() calls an @Only(before=) method: it can only run before the mark
        Value.Eventual setVariable = eventual(B.findUniqueMethod("setVariable", 1));
        assertTrue(setVariable.isOnly());
        assertEquals(Boolean.FALSE, setVariable.after());
        assertEquals(Set.of("inspection"), setVariable.fields());

        // 'return inspection.isFinal()' is a state test, in the same sense as the callee
        Value.Eventual isCommitted = eventual(B.findUniqueMethod("isCommitted", 0));
        assertTrue(isCommitted.isTestMark());
        assertEquals(Boolean.TRUE, isCommitted.test());

        // isVariable() is the inverted test; forwarding it keeps the inverted sense
        assertEquals(Boolean.FALSE, eventual(B.findUniqueMethod("isNotCommitted", 0)).test());

        // get() carries no contract: it works on both sides, so length() is not eventual
        assertFalse(eventual(B.findUniqueMethod("length", 0)).isEventual());
    }

    @DisplayName("the type itself becomes @Immutable(after=...)")
    @Test
    public void test1TypeLevel() {
        TypeInfo B = javaInspector.parse("B", INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        Value.EventuallyImmutable ev = B.analysis().getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE,
                ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
        assertTrue(ev.isEventual(), "B should be eventually immutable, is " + ev);
        assertEquals("inspection", ev.markLabel());
        // the field holds hidden content (a String inside the support class), so hc=true is the right level
        assertTrue(ev.immutableAfterMark().isAtLeastImmutableHC(), "after the mark: " + ev.immutableAfterMark());

        // and the unconditional verdict stays conservative: before the mark, B really is not immutable
        Value.Immutable unconditional = B.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.MUTABLE);
        assertFalse(unconditional.isAtLeastImmutableHC(), "unconditional: " + unconditional);
    }

    @Language("java")
    private static final String INPUT2 = """
            import org.e2immu.support.SetOnce;

            public class B {
              private final SetOnce<String> one = new SetOnce<>();
              private final SetOnce<String> two = new SetOnce<>();

              public void setBoth(String s, String t) {
                one.set(s);
                two.set(t);
              }

              public String get() {
                return one.get() + two.get();
              }
            }
            """;

    @DisplayName("one method marking two fields carries both labels")
    @Test
    public void test2() {
        TypeInfo B = javaInspector.parse("B", INPUT2);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        Value.Eventual setBoth = eventual(B.findUniqueMethod("setBoth", 2));
        assertTrue(setBoth.isMark());
        assertEquals(Set.of("one", "two"), setBoth.fields());
        assertEquals("one,two", setBoth.markLabel());
    }

    @Language("java")
    private static final String INPUT3 = """
            import org.e2immu.annotation.eventual.Mark;
            import org.e2immu.support.SetOnce;

            public class B {
              private final SetOnce<String> t = new SetOnce<>();

              @Mark("t")
              public void contracted(String s) {
                t.set(s);
              }

              public void indirect(String s) {
                contracted(s);
              }

              public boolean touchesStateInAnAssert() {
                assert t.isSet();
                return true;
              }
            }
            """;

    @DisplayName("a hand-written contract is not overwritten; a state test in an assert is not a @TestMark")
    @Test
    public void test3() {
        TypeInfo B = javaInspector.parse("B", INPUT3);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        Value.Eventual contracted = eventual(B.findUniqueMethod("contracted", 1));
        assertTrue(contracted.isMark());
        assertEquals(Set.of("t"), contracted.fields());

        // calling a @Mark method on 'this' inherits the mark
        Value.Eventual indirect = eventual(B.findUniqueMethod("indirect", 1));
        assertTrue(indirect.isMark(), "indirect() should inherit the mark, is " + indirect);
        assertEquals(Set.of("t"), indirect.fields());

        // consulting the state in an assert says nothing about the method itself
        assertFalse(eventual(B.findUniqueMethod("touchesStateInAnAssert", 0)).isEventual());
    }

    @Language("java")
    private static final String INPUT4 = """
            import org.e2immu.support.Freezable;

            public class B extends Freezable {
              public void add() {
                ensureNotFrozen();
              }

              public int read() {
                ensureFrozen();
                return 1;
              }
            }
            """;

    @DisplayName("an inherited mark (Freezable) propagates through calls on this")
    @Test
    public void test4() {
        TypeInfo B = javaInspector.parse("B", INPUT4);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        Value.Eventual add = eventual(B.findUniqueMethod("add", 0));
        assertTrue(add.isOnly(), "add() should be @Only(before), is " + add);
        assertEquals(Boolean.FALSE, add.after());
        assertEquals(Set.of("frozen"), add.fields());

        Value.Eventual read = eventual(B.findUniqueMethod("read", 0));
        assertTrue(read.isOnly());
        assertEquals(Boolean.TRUE, read.after());
    }

    // the shape of TypeInfo / TypeInfoImpl: the interface declares the marking method, but only the
    // implementation holds the eventually immutable field
    @Language("java")
    private static final String INPUT5 = """
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class B {
              interface I {
                void commit(String s);
                boolean hasBeenCommitted();
              }
              static class Impl implements I {
                private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
                @Override public void commit(String s) { inspection.setFinal(s); }
                @Override public boolean hasBeenCommitted() { return inspection.isFinal(); }
              }
            }
            """;

    @DisplayName("eventuality travels from implementation to interface, and back down the hierarchy")
    @Test
    public void test5() {
        TypeInfo B = javaInspector.parse("B", INPUT5);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        TypeInfo I = B.findSubType("I");
        TypeInfo impl = B.findSubType("Impl");

        // the abstract method inherits the mark of its single implementation
        Value.Eventual commit = eventual(I.findUniqueMethod("commit", 1));
        assertTrue(commit.isMark(), "I.commit should be @Mark, is " + commit);
        assertEquals(Set.of("inspection"), commit.fields());
        assertEquals(Boolean.TRUE, eventual(I.findUniqueMethod("hasBeenCommitted", 0)).test());

        // and the interface itself becomes eventually immutable, which is what unblocks the implementation:
        // the hierarchy rule would otherwise make a mutable interface force Impl to MUTABLE
        Value.EventuallyImmutable evI = I.analysis().getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE,
                ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
        assertTrue(evI.isEventual(), "I should be eventually immutable, is " + evI);
        assertEquals("inspection", evI.markLabel());

        Value.EventuallyImmutable evImpl = impl.analysis().getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE,
                ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
        assertTrue(evImpl.isEventual(), "Impl should be eventually immutable, is " + evImpl);
    }

    /*
     Independence after the mark. I leaks its eventually immutable state through builder(), which can only be
     called before the mark -- exactly the TypeInfo.builder() shape. The leak makes Impl.builder(), and hence the
     abstract I.builder(), @Dependent; without independence-after-mark the dependence cap in computeImmutableType
     fires before the AfterMark relaxation is ever consulted, and I stops at FINAL_FIELDS.
    */
    @Language("java")
    private static final String INPUT6 = """
            import org.e2immu.annotation.eventual.Mark;
            import org.e2immu.annotation.eventual.Only;
            import org.e2immu.support.SetOnce;

            public class B {
              interface I {
                @Mark("t") void commit(String s);
                @Only(before = "t") SetOnce<String> builder();
              }
              static class Impl implements I {
                private final SetOnce<String> t = new SetOnce<>();
                @Override public void commit(String s) { t.set(s); }
                @Override public SetOnce<String> builder() { return t; }
              }
            }
            """;

    @DisplayName("a dependent accessor that can only run before the mark stops capping the type")
    @Test
    public void test6() {
        TypeInfo B = javaInspector.parse("B", INPUT6);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        TypeInfo I = B.findSubType("I");

        // the leak is real: the accessor is dependent, which is what used to cap the type
        assertTrue(I.findUniqueMethod("builder", 0).analysis()
                .getOrDefault(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.DEPENDENT).isDependent());

        Value.EventuallyImmutable ev = I.analysis().getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE,
                ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
        assertTrue(ev.isEventual(), "I should be eventually immutable, is " + ev);
        assertEquals("t", ev.markLabel());
        assertTrue(ev.immutableAfterMark().isAtLeastImmutableHC(),
                "after the mark I should be at least immutable-HC, is " + ev.immutableAfterMark());
    }

    /*
     The soundness constraint, and the more important of the two tests. Same shape as INPUT6, but leak() hands
     out a plain ArrayList. "Cannot be called after the mark" is NOT enough on its own: a reference handed out
     before the mark survives it, and that list stays mutable forever. Only an escaped object that is itself
     frozen by a mark of its own may be discounted -- so I must NOT be promoted here.
    */
    @Language("java")
    private static final String INPUT7 = """
            import java.util.ArrayList;
            import java.util.List;
            import org.e2immu.annotation.eventual.Mark;
            import org.e2immu.annotation.eventual.Only;
            import org.e2immu.support.SetOnce;

            public class B {
              interface I {
                @Mark("t") void commit(String s);
                @Only(before = "t") List<String> leak();
              }
              static class Impl implements I {
                private final SetOnce<String> t = new SetOnce<>();
                private final List<String> list = new ArrayList<>();
                @Override public void commit(String s) { t.set(s); }
                @Override public List<String> leak() { return list; }
              }
            }
            """;

    @DisplayName("leaking a NOT eventually immutable object keeps the type dependent, mark or no mark")
    @Test
    public void test7() {
        TypeInfo B = javaInspector.parse("B", INPUT7);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        TypeInfo I = B.findSubType("I");

        // the mark itself is still detected -- this is about independence, not about the transition
        assertTrue(eventual(I.findUniqueMethod("commit", 1)).isMark());
        assertTrue(I.findUniqueMethod("leak", 0).analysis()
                .getOrDefault(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.DEPENDENT).isDependent());

        Value.EventuallyImmutable ev = I.analysis().getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE,
                ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
        assertFalse(ev.isEventual() && ev.immutableAfterMark().isAtLeastImmutableHC(),
                "the escaped ArrayList stays mutable after the mark; I must not be promoted to immutable-HC"
                + " (got " + ev + ")");
    }

    /*
     The TypeInspection shape, reduced: ONE interface implemented by BOTH a mutable builder and an immutable
     product. Road to immutability §060 names this exact pattern -- "an interface that is implemented both by
     the builder and the immutable type" -- as the way to span both stages, and warns it "becomes difficult
     ... with an eye on immutability". These two tests measure how difficult.
    */
    @Language("java")
    private static final String INPUT9_SHARED = """
            import java.util.*;
            public class B {
              interface I {
                List<String> items();
              }
              static class Product implements I {
                private final List<String> items;
                Product(List<String> items) { this.items = List.copyOf(items); }
                @Override public List<String> items() { return items; }
              }
              static class Builder implements I {
                private final List<String> items = new ArrayList<>();
                public void add(String s) { items.add(s); }
                @Override public List<String> items() { return items; }
                public Product build() { return new Product(items); }
              }
            }
            """;

    // identical, except the builder no longer implements the shared interface
    @Language("java")
    private static final String INPUT9_SPLIT = """
            import java.util.*;
            public class C {
              interface I {
                List<String> items();
              }
              static class Product implements I {
                private final List<String> items;
                Product(List<String> items) { this.items = List.copyOf(items); }
                @Override public List<String> items() { return items; }
              }
              static class Builder {
                private final List<String> items = new ArrayList<>();
                public void add(String s) { items.add(s); }
                public List<String> items() { return items; }
                public Product build() { return new Product(items); }
              }
            }
            """;

    private Value.Immutable immutable(TypeInfo typeInfo) {
        return typeInfo.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE);
    }

    /*
     Scope note (2026-07): this fixture's builder accessor is a plain getter, hence non-modifying, so the
     meet over implementations costs the shared interface nothing and the split is genuinely a no-op HERE.
     Do not generalise it -- the real CST is the opposite case. TypeInspection.superTypesExcludingJavaLangObject
     is non-modifying in the product but MODIFYING in the Builder (it reaches TypeInfo.parentClass(), which runs
     EventuallyFinalOnDemand's on-demand loader), so the meet caps the interface at FINAL_FIELDS and the
     hierarchy rule then makes every implementation mutable. There, splitting the builder off IS the blocker.
     See docs/dynamic-immutability-feasibility.md.
     */
    @DisplayName("splitting the builder off the shared interface is a no-op when its accessors do not modify")
    @Test
    public void test9BuilderSplitIsNoOpForNonModifyingBuilder() {
        TypeInfo shared = javaInspector.parse("B", INPUT9_SHARED);
        analyzer.go(prepWork(shared));
        Value.Immutable sharedProduct = immutable(shared.findSubType("Product"));

        TypeInfo split = javaInspector.parse("C", INPUT9_SPLIT);
        analyzer.go(prepWork(split));
        Value.Immutable splitProduct = immutable(split.findSubType("Product"));

        /*
         Measured, and it refutes the natural reading of §060: having the mutable builder implement the same
         interface as the immutable product costs the product NOTHING. The two land in exactly the same place,
         and wherever that is, it has nothing to do with the builder.

         Recorded as a pin because the "separate the builder from the interface" refactor is an obvious thing to
         reach for on TypeInspection, and this says it would buy nothing.

         The absolute level moved once DynamicImmutabilityInference landed: Product's constructor does
         `this.items = List.copyOf(items)`, so the field is now provably immutable and the type reaches
         @Immutable(hc=true) instead of being capped at FINAL_FIELDS by the independence gate. That is the
         limitation this test used to pin being removed -- the equality above, which is what the test is
         actually about, is unaffected.
        */
        assertEquals(sharedProduct, splitProduct);
        assertTrue(sharedProduct.isAtLeastImmutableHC(), "the defensive copy is now understood: " + sharedProduct);
    }

    private Set<String> nonModAfter(MethodInfo methodInfo) {
        return methodInfo.analysis().getOrDefault(PropertyImpl.EVENTUALLY_NON_MODIFYING_METHOD,
                ValueImpl.SetOfStringsImpl.EMPTY_SET).set();
    }

    // The real TypeInfo/Info shape the whole exercise is for: the interface declares a read-through accessor
    // (length) whose ONLY modification is the lazy loader inside inspection.get(). Without method-level eventual
    // non-modification the accessor is @Modified, the interface caps at FINAL_FIELDS, and the hierarchy rule then
    // drags every implementation to MUTABLE.
    @Language("java")
    private static final String INPUT10 = """
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class B {
              interface I {
                void commit(String s);
                boolean hasBeenCommitted();
                int length();
              }
              static class Impl implements I {
                private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
                @Override public void commit(String s) { inspection.setFinal(s); }
                @Override public boolean hasBeenCommitted() { return inspection.isFinal(); }
                @Override public int length() { return inspection.get().length(); }
              }
            }
            """;

    @DisplayName("a read-through accessor is @NotModified(after=), so it stops capping the interface")
    @Test
    public void test10ReadThroughAccessor() {
        TypeInfo B = javaInspector.parse("B", INPUT10);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        TypeInfo I = B.findSubType("I");
        TypeInfo impl = B.findSubType("Impl");

        // the implementation's accessor really does modify before the mark (get() runs the loader) ...
        MethodInfo implLength = impl.findUniqueMethod("length", 0);
        assertFalse(implLength.analysis().getOrDefault(PropertyImpl.NON_MODIFYING_METHOD,
                ValueImpl.BoolImpl.FALSE).isTrue(), "Impl.length modifies before the mark");
        // ... but is non-modifying after 'inspection' has been committed
        assertEquals(Set.of("inspection"), nonModAfter(implLength));
        // and the abstract accessor inherits that
        assertEquals(Set.of("inspection"), nonModAfter(I.findUniqueMethod("length", 0)));

        // so the interface still reaches an eventual verdict despite the @Modified accessor
        Value.EventuallyImmutable evI = I.analysis().getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE,
                ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
        assertTrue(evI.isEventual(), "I should be eventually immutable, is " + evI);
        assertEquals("inspection", evI.markLabel());

        assertTrue(impl.analysis().getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE,
                ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL).isEventual(), "Impl should be eventually immutable");
    }

    // The Info interface exactly: it declares NO @Mark method (commit lives elsewhere), only a read-through
    // accessor. The type's mark label must then be derived from that accessor's after-label -- there is no other
    // source. This is the case that needs computeTypeLevel to read EVENTUALLY_NON_MODIFYING_METHOD.
    @Language("java")
    private static final String INPUT11 = """
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class B {
              interface Reader {
                int length();
              }
              static class Impl implements Reader {
                private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
                public void commit(String s) { inspection.setFinal(s); }
                @Override public int length() { return inspection.get().length(); }
              }
            }
            """;

    @DisplayName("an interface with only a read-through accessor derives its mark from the after-label")
    @Test
    public void test11MarkFromAccessorOnly() {
        TypeInfo B = javaInspector.parse("B", INPUT11);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        TypeInfo reader = B.findSubType("Reader");
        assertEquals(Set.of("inspection"), nonModAfter(reader.findUniqueMethod("length", 0)));

        Value.EventuallyImmutable ev = reader.analysis().getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE,
                ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
        assertTrue(ev.isEventual(), "Reader should be eventually immutable, is " + ev);
        assertEquals("inspection", ev.markLabel());
    }

    @DisplayName("a @Mark method is NOT recorded as non-modifying-after-the-mark (its call IS the transition)")
    @Test
    public void test12MarkIsNotEventuallyNonModifying() {
        TypeInfo B = javaInspector.parse("B", INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        MethodInfo commit = B.findUniqueMethod("commit", 1);
        assertTrue(eventual(commit).isMark(), "commit is @Mark");
        // it calls inspection.setFinal (the @Mark on the field): that is the transition, not a post-mark read
        assertTrue(nonModAfter(commit).isEmpty(), "commit must not be eventually-non-modifying, is " + nonModAfter(commit));
        // setVariable is @Only(before=): also excluded
        assertTrue(nonModAfter(B.findUniqueMethod("setVariable", 1)).isEmpty());
        // the read-through accessor, by contrast, is
        assertEquals(Set.of("inspection"), nonModAfter(B.findUniqueMethod("length", 0)));
    }

    // a modification through a field that is NOT eventually immutable cannot be excused by any mark
    @Language("java")
    private static final String INPUT13 = """
            import java.util.*;
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class B {
              private final List<String> items = new ArrayList<>();
              private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
              public void commit(String s) { inspection.setFinal(s); }
              public void touch(String s) { items.add(s); }
              public int length() { return inspection.get().length(); }
            }
            """;

    @DisplayName("modifying a plain mutable field is not excusable, and does not over-conclude eventuality")
    @Test
    public void test13PlainFieldNotExcused() {
        TypeInfo B = javaInspector.parse("B", INPUT13);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        // touch() modifies items.add(...), a field that is not eventually immutable: no after-label
        assertTrue(nonModAfter(B.findUniqueMethod("touch", 1)).isEmpty(),
                "touch must not be eventually-non-modifying, is " + nonModAfter(B.findUniqueMethod("touch", 1)));
        // length() still is (it reads through the eventually immutable field)
        assertEquals(Set.of("inspection"), nonModAfter(B.findUniqueMethod("length", 0)));

        // and the type is NOT eventually immutable: 'items' stays mutable, marked by nothing
        assertFalse(B.analysis().getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE,
                ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL).isEventual(),
                "B must not be eventually immutable: it has an unmarked mutable field");
    }

    // one accessor forwarding to another (this.length()) rides along with its after-label
    @Language("java")
    private static final String INPUT14 = """
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class B {
              private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
              public void commit(String s) { inspection.setFinal(s); }
              public int length() { return inspection.get().length(); }
              public int twiceLength() { return length() + length(); }
            }
            """;

    @DisplayName("a this-forward to an eventually-non-modifying method inherits its after-label")
    @Test
    public void test14ThisForward() {
        TypeInfo B = javaInspector.parse("B", INPUT14);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        assertEquals(Set.of("inspection"), nonModAfter(B.findUniqueMethod("length", 0)));
        // twiceLength() only modifies via this.length(), which is non-modifying after 'inspection'
        assertEquals(Set.of("inspection"), nonModAfter(B.findUniqueMethod("twiceLength", 0)));
    }
}
