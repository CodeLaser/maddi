package org.e2immu.analyzer.modification.link.impl.graph2;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.CONTAINS_AS_MEMBER;
import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.IS_ELEMENT_OF;
import static org.junit.jupiter.api.Assertions.*;

public class TestGraph {
    public static final String START = "a";
    LinkNature IS_IDENTICAL_TO = LinkNatureImpl.makeIdenticalTo(null);

    @Test
    public void testUniDirectional() {
        IncrementalFixpointEngine<String, LinkNature> engine = new IncrementalFixpointEngine<>(LinkNature::combine,
                LinkNature::best, LinkNature::valid);
        UpdateResult<String> ur0 = engine.addEdge("a", "b", IS_IDENTICAL_TO);
        assertEquals("UpdateResult[affectedVertices=[a, b], newFacts=1, removedEdges=0]", ur0.toString());
        assertEquals("a ≡ b", printEdges(engine));

        UpdateResult<String> ur1 = engine.addEdge("b", "c", IS_IDENTICAL_TO);
        assertEquals("UpdateResult[affectedVertices=[a, b, c], newFacts=2, removedEdges=0]", ur1.toString());
        assertEquals("a ≡ b / b ≡ c", printEdges(engine));
        assertEquals("""
                a ≡ b   0(a ≡ b)
                a ≡ c   [a ≡ b, b ≡ c]
                b ≡ c   0(b ≡ c)
                """, printClosure(engine));

        UpdateResult<String> ur2 = engine.addEdge("a", "d", IS_ELEMENT_OF);
        assertEquals("UpdateResult[affectedVertices=[a, d], newFacts=1, removedEdges=0]", ur2.toString());
        assertEquals("a ≡ b / a ∈ d / b ≡ c", printEdges(engine));
        assertEquals("""
                a ≡ b   0(a ≡ b)
                a ≡ c   [a ≡ b, b ≡ c]
                a ∈ d   0(a ∈ d)
                b ≡ c   0(b ≡ c)
                """, printClosure(engine));
    }

    @Test
    public void testBidirectional() {
        IncrementalFixpointEngine<String, LinkNature> engine = new IncrementalFixpointEngine<>(LinkNature::combine,
                LinkNature::best, LinkNature::valid);
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
        UpdateResult<String> ur = engine.addEdge("c", "d", IS_ELEMENT_OF, "3");
        assertEquals("UpdateResult[affectedVertices=[], newFacts=0, removedEdges=0]", ur.toString());
        String edges3 = "a ≡ b / a ∈ d / b ≡ a / b ≡ c / c ≡ b / c ∈ d / d ∋ a";
        assertEquals(edges3, printEdges(engine));
        assertEquals(closure2, printClosure(engine));

        assertFalse(engine.addVertex("d"));
        assertTrue(engine.addVertex("e"));
        assertEquals(edges3, printEdges(engine));
        assertEquals("""
                a:  ≡ b,  ∈ d
                b:  ≡ a,  ≡ c
                c:  ≡ b,  ∈ d
                d:  ∋ a
                e:
                """, print(engine));

        // remove 'b'; important that 'c ∈ d' is kept
        engine.removeVertex("b");
        assertEquals("""
                a:  ∈ d
                c:  ∈ d
                d:  ∋ a
                e:
                """, print(engine));
        assertEquals("""
                a ≡ c   [a ≡ b, b ≡ c]
                a ∈ d   2(a ∈ d)
                c ≡ a   [c ≡ b, b ≡ a]
                c ∈ d   [c ≡ a, a ∈ d]
                d ∋ a   2(d ∋ a)
                d ∋ c   [d ∋ b, b ≡ c]
                """, printClosure(engine));
    }

    private static String print(IncrementalFixpointEngine<String, LinkNature> engine) {
        return engine.print(String::compareTo) + "\n";
    }

    private static String printEdges(IncrementalFixpointEngine<String, LinkNature> engine) {
        return engine.printEdges(String::compareTo);
    }

    private static String printClosure(IncrementalFixpointEngine<String, LinkNature> engine) {
        return engine.printClosure(String::compareTo) + "\n";
    }

}
