package org.e2immu.analyzer.modification.link.impl.graph2;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.CONTAINS_AS_MEMBER;
import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.IS_ELEMENT_OF;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGraph {
    public static final String START = "a";
    LinkNature IS_IDENTICAL_TO = LinkNatureImpl.makeIdenticalTo(null);

    @Test
    public void test() {
        IncrementalFixpointEngine<String, LinkNature> engine = new IncrementalFixpointEngine<>(LinkNature::combine);
        UpdateResult<String> ur0 = engine.addEdge("a", "b", IS_IDENTICAL_TO);
        assertEquals("UpdateResult[affectedVertices=[a, b], newFacts=1, removedEdges=0]", ur0.toString());
        assertEquals("a ≡ b", printEdges(engine));

        UpdateResult<String> ur1 = engine.addEdge("b", "c", IS_IDENTICAL_TO);
        assertEquals("UpdateResult[affectedVertices=[a, b, c], newFacts=2, removedEdges=0]", ur1.toString());
        assertEquals("a ≡ b / b ≡ c", printEdges(engine));
        assertEquals("a ≡ b / a ≡ c / b ≡ c", printClosure(engine));

        UpdateResult<String> ur2 = engine.addEdge("a", "d", IS_ELEMENT_OF);
        assertEquals("UpdateResult[affectedVertices=[a, d], newFacts=1, removedEdges=0]", ur2.toString());
        assertEquals("a ≡ b / a ∈ d / b ≡ c", printEdges(engine));
        assertEquals("a ≡ b / a ≡ c / a ∈ d / b ≡ c", printClosure(engine));
    }

    @Test
    public void test2() {
        IncrementalFixpointEngine<String, LinkNature> engine = new IncrementalFixpointEngine<>(LinkNature::combine);
        engine.addEdge("a", "b", IS_IDENTICAL_TO);
        engine.addEdge("b", "a", IS_IDENTICAL_TO);

        assertEquals("a ≡ b / b ≡ a", printEdges(engine));

        engine.addEdge("b", "c", IS_IDENTICAL_TO);
        engine.addEdge("c", "b", IS_IDENTICAL_TO);
        assertEquals("a ≡ b / b ≡ a / b ≡ c / c ≡ b", printEdges(engine));
        assertEquals("a ≡ a / a ≡ b / a ≡ c / b ≡ a / b ≡ b / b ≡ c / c ≡ a / c ≡ b / c ≡ c",
                printClosure(engine));

        engine.addEdge("a", "d", IS_ELEMENT_OF);
        engine.addEdge("d", "a", CONTAINS_AS_MEMBER);
        assertEquals("a ≡ b / a ∈ d / b ≡ a / b ≡ c / c ≡ b / d ∋ a", printEdges(engine));
        assertEquals("a ≡ b / a ≡ c / a ∈ d / b ≡ c", printClosure(engine));
    }

    private static String printEdges(IncrementalFixpointEngine<String, LinkNature> engine) {
        return engine.print(String::compareTo);
    }

    private static String printClosure(IncrementalFixpointEngine<String, LinkNature> engine) {
        return engine.printClosure(String::compareTo, LinkNature::compareTo);
    }

}
