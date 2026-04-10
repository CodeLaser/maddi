package org.e2immu.analyzer.modification.link.impl.graph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestGraph {
    LinkNature IS_IDENTICAL_TO = LinkNatureImpl.makeIdenticalTo(null);

    final IncrementalFixpointEngine<String, LinkNature> engine = new IncrementalFixpointEngine<>(LinkNature::combine,
            LinkNature::best, LinkNature::valid, LinkNature::score, LinkNature::reverse);

    @Test
    public void test1() {
        int newFacts = engine.addSymmetricEdge("a", "b", IS_IDENTICAL_TO, "0");
        assertEquals(1, newFacts);
        assertEquals("a ≡ b", printEdges(engine));

        int newFacts1 = engine.addSymmetricEdge("b", "c", IS_IDENTICAL_TO, "0");
        assertEquals(2, newFacts1);
        assertEquals("a ≡ b / b ≡ c", printEdges(engine));
        assertEquals("""
                a ≡ b   0(a ≡ b)
                a ≡ c   [a ≡ b, b ≡ c]
                b ≡ c   0(b ≡ c)
                """, printClosure(engine));

        int newFacts2 = engine.addSymmetricEdge("a", "d", IS_ELEMENT_OF, "0");
        assertEquals(1, newFacts2);
        assertEquals("a ≡ b / a ∈ d / b ≡ c", printEdges(engine));
        assertEquals("""
                a ≡ b   0(a ≡ b)
                a ≡ c   [a ≡ b, b ≡ c]
                a ∈ d   0(a ∈ d)
                b ≡ c   0(b ≡ c)
                """, printClosure(engine));
    }
/*
    @Test
    public void testBidirectional() {
        engine.addEdge("a", "b", IS_IDENTICAL_TO, "0");
        engine.addEdge("b", "a", IS_IDENTICAL_TO, "0");

        assertEquals("a ≡ b / b ≡ a", printEdges(engine));

        engine.addEdge("b", "c", IS_IDENTICAL_TO, "1");
        engine.addEdge("c", "b", IS_IDENTICAL_TO, "1");
        assertEquals("a ≡ b / b ≡ a / b ≡ c / c ≡ b", printEdges(engine));
        assertEquals("""
                a ≡ b   0(a ≡ b)
                a ≡ c   [a ≡ b, b ≡ c]
                b ≡ a   0(b ≡ a)
                b ≡ c   1(b ≡ c)
                c ≡ a   [c ≡ b, b ≡ a]
                c ≡ b   1(c ≡ b)
                """, printClosure(engine));

        engine.addEdge("a", "d", IS_ELEMENT_OF, "2");
        engine.addEdge("d", "a", CONTAINS_AS_MEMBER, "2");
        assertEquals("a ≡ b / a ∈ d / b ≡ a / b ≡ c / c ≡ b / d ∋ a", printEdges(engine));
        String closure2 = """
                a ≡ b   0(a ≡ b)
                a ≡ c   [a ≡ b, b ≡ c]
                a ∈ d   2(a ∈ d)
                b ≡ a   0(b ≡ a)
                b ≡ c   1(b ≡ c)
                b ∈ d   [b ≡ a, a ∈ d]
                c ≡ a   [c ≡ b, b ≡ a]
                c ≡ b   1(c ≡ b)
                c ∈ d   [c ≡ a, a ∈ d]
                d ∋ a   2(d ∋ a)
                d ∋ b   [d ∋ a, a ≡ b]
                d ∋ c   [d ∋ b, b ≡ c]
                """;
        assertEquals(closure2, printClosure(engine));

        // adding this edge should not have any effect on the closure
        int newFacts = engine.addEdge("c", "d", IS_ELEMENT_OF, "3");
        assertEquals(0, newFacts);
        String edges3 = "a ≡ b / a ∈ d / b ≡ a / b ≡ c / c ≡ b / c ∈ d / d ∋ a";
        assertEquals(edges3, printEdges(engine));
        String closure3 = """
                a ≡ b   0(a ≡ b)
                a ≡ c   [a ≡ b, b ≡ c]
                a ∈ d   2(a ∈ d)
                b ≡ a   0(b ≡ a)
                b ≡ c   1(b ≡ c)
                b ∈ d   [b ≡ a, a ∈ d]
                c ≡ a   [c ≡ b, b ≡ a]
                c ≡ b   1(c ≡ b)
                c ∈ d   3(c ∈ d)
                d ∋ a   2(d ∋ a)
                d ∋ b   [d ∋ a, a ≡ b]
                d ∋ c   [d ∋ b, b ≡ c]
                """;
        assertEquals(closure3, printClosure(engine));
        assertNotEquals(closure2, closure3); // c ∈ d is now a direct witness

        assertFalse(engine.addVertex("d"));
        assertTrue(engine.addVertex("e"));
        assertEquals(edges3, printEdges(engine));
        assertEquals("""
                a ≡ b / a ∈ d
                b ≡ a / b ≡ c
                c ≡ b / c ∈ d
                d ∋ a
                e
                """, print(engine));

        // remove 'b'; important that 'c ∈ d' is kept
        engine.removeVertex("b");
        assertEquals("""
                a ∈ d
                c ∈ d
                d ∋ a
                e
                """, print(engine));
        assertEquals("""
                a ≡ c   [a ≡ b, b ≡ c]
                a ∈ d   2(a ∈ d)
                c ≡ a   [c ≡ b, b ≡ a]
                c ∈ d   3(c ∈ d)
                d ∋ a   2(d ∋ a)
                d ∋ c   [d ∋ b, b ≡ c]
                """, printClosure(engine));
    }


    @Test
    public void testUpdate1() {
        engine.addEdge("a", "b", IS_SUBSET_OF, "0");
        engine.addEdge("b", "a", IS_SUPERSET_OF, "0");

        assertEquals("""
                a ⊆ b   0(a ⊆ b)
                b ⊇ a   0(b ⊇ a)
                """, printClosure(engine));

        engine.addEdge("x", "a", IS_ELEMENT_OF, "1");
        engine.addEdge("a", "x", CONTAINS_AS_MEMBER, "1");

        assertEquals("""
                a ⊆ b / a ∋ x
                b ⊇ a
                x ∈ a
                """, print(engine));
        String closureBeforeReplace = """
                a ⊆ b   0(a ⊆ b)
                a ∋ x   1(a ∋ x)
                b ⊇ a   0(b ⊇ a)
                b ∋ x   [b ⊇ a, a ∋ x]
                x ∈ a   1(x ∈ a)
                x ∈ b   [x ∈ a, a ⊆ b]
                """;
        assertEquals(closureBeforeReplace, printClosure(engine));
        assertEquals("b=⊆, x=∋", engine.successorsInGraphStream("a")
                .map(Object::toString).collect(Collectors.joining(", ")));

        // start replacing

        Set<String> set1 = engine.replaceReturnAffected("a", "b", IS_SUBSET_OF, SHARES_ELEMENTS);
        assertEquals("a,b", set1.stream().sorted().collect(Collectors.joining(",")));

        assertEquals("""
                a ~ b / a ∋ x
                b ⊇ a
                x ∈ a
                """, print(engine));
        Set<String> set2 = engine.replaceReturnAffected("b", "a", IS_SUPERSET_OF, SHARES_ELEMENTS);
        assertEquals(set1, set2);

        assertEquals("""
                a ~ b / a ∋ x
                b ~ a
                x ∈ a
                """, print(engine));
        assertFalse(engine.allEdgesOfLabelGraphAreInClosure());
        assertEquals(closureBeforeReplace, printClosure(engine));

        engine.recompute(set1, "2", _ -> true);
        assertEquals("""
                a ~ b   2(a ~ b)
                a ∋ x   2(a ∋ x)
                b ~ a   2(b ~ a)
                x ∈ a   2(x ∈ a)
                """, printClosure(engine));
    }
*/
    private static String print(IncrementalFixpointEngine<String, LinkNature> engine) {
        return engine.print(String::compareTo);
    }

    private static String printEdges(IncrementalFixpointEngine<String, LinkNature> engine) {
        return engine.printEdges(String::compareTo);
    }

    private static String printClosure(IncrementalFixpointEngine<String, LinkNature> engine) {
        return engine.printClosure(String::compareTo);
    }

}
