package org.e2immu.analyzer.modification.link.impl.graph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.IS_ELEMENT_OF;
import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.SHARES_ELEMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEngine {
    LinkNature IS_IDENTICAL_TO = LinkNatureImpl.makeIdenticalTo(null);

    final IncrementalFixpointEngine<String, LinkNature> engine = new IncrementalFixpointEngine<>(LinkNature::combine,
            LinkNature::best, LinkNature::valid, LinkNature::score, LinkNature::reverse,
            Object::toString, String::compareTo);

    @Test
    public void test1() {
        int newFacts = engine.addSymmetricEdge("a", "b", IS_IDENTICAL_TO, "0");
        assertEquals(2, newFacts);
        assertEquals("a ≡ b / b ≡ a", engine.printEdges());

        int newFacts1 = engine.addSymmetricEdge("b", "c", IS_IDENTICAL_TO, "0");
        assertEquals(2, newFacts1);
        assertEquals("a ≡ b / b ≡ a / b ≡ c / c ≡ b", engine.printEdges());
        assertEquals("""
                a ≡ b   0(a ≡ b)
                a ≡ c   *[a ≡ b, b ≡ c]
                b ≡ a   0(b ≡ a)
                b ≡ c   0(b ≡ c)
                c ≡ a   *[c ≡ b, b ≡ a]
                c ≡ b   0(c ≡ b)
                """, engine.printClosure());

        int newFacts2 = engine.addSymmetricEdge("a", "d", IS_ELEMENT_OF, "0");
        assertEquals(2, newFacts2);
        assertEquals("a ≡ b / a ∈ d / b ≡ a / b ≡ c / c ≡ b / d ∋ a", engine.printEdges());
        assertEquals("""
                a ≡ b   0(a ≡ b)
                a ≡ c   *[a ≡ b, b ≡ c]
                a ∈ d   0(a ∈ d)
                b ≡ a   0(b ≡ a)
                b ≡ c   0(b ≡ c)
                b ∈ d   *[b ≡ a, a ∈ d]
                c ≡ a   *[c ≡ b, b ≡ a]
                c ≡ b   0(c ≡ b)
                c ∈ d   *[c ≡ a, a ∈ d]
                d ∋ a   0(d ∋ a)
                d ∋ b   *[d ∋ a, a ≡ b]
                d ∋ c   *[d ∋ b, b ≡ c]
                """, engine.printClosure());
    }

    @DisplayName("test best link")
    @Test
    public void test2() {
        // copy ~ input
        // a ∈ copy, a ∈ input
        // ∋? is overwritten to ∋ because of the direct fact

        int newFacts = engine.addSymmetricEdge("copy", "input", SHARES_ELEMENTS, "0");
        assertEquals(2, newFacts);

        int newFacts1 = engine.addSymmetricEdge("a", "copy", IS_ELEMENT_OF, "1");
        assertEquals(2, newFacts1);
        assertEquals("""
                a ∈ copy   1(a ∈ copy)
                a ∈? input   *[a ∈ copy, copy ~ input]
                copy ∋ a   1(copy ∋ a)
                copy ~ input   0(copy ~ input)
                input ∋? a   *[input ~ copy, copy ∋ a]
                input ~ copy   0(input ~ copy)
                """, engine.printClosure());

        // overwrite a ∈? input by adding a direct edge
        int newFacts2 = engine.addSymmetricEdge("a", "input", IS_ELEMENT_OF, "2");
        assertEquals(2, newFacts2);
        assertEquals("a ∈ copy / a ∈ input / copy ∋ a / copy ~ input / input ∋ a / input ~ copy",
                engine.printEdges());
        assertEquals("""
                a ∈ copy   1(a ∈ copy)
                a ∈ input   2(a ∈ input)
                copy ∋ a   1(copy ∋ a)
                copy ~ input   0(copy ~ input)
                input ∋ a   2(input ∋ a)
                input ~ copy   0(input ~ copy)
                """, engine.printClosure());

        engine.recompute(Set.of("a", "input"), "3",
                fact -> SHARES_ELEMENTS == fact.label());
        assertEquals("""
                a ∈ copy   1(a ∈ copy)
                a ∈ input   2(a ∈ input)
                copy ∋ a   1(copy ∋ a)
                copy ~ input   0(copy ~ input)
                input ∋ a   2(input ∋ a)
                input ~ copy   3(input ~ copy)
                """, engine.printClosure());
    }

    

}
