package org.e2immu.analyzer.modification.link.impl.graph;

import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLabeledGraph {

    @Test
    public void test() {
        LabeledGraph<String, LinkNature> graph = new LabeledGraph<>();
        graph.addSymmetricEdge("a", "b", IS_ELEMENT_OF, CONTAINS_AS_MEMBER);
        assertEquals("""
                a ∈ b
                b ∋ a
                """, graph.print(String::compareTo));
        graph.addSymmetricEdge("b", "c", IS_SUBSET_OF, IS_SUPERSET_OF);
        assertEquals("""
                a ∈ b
                b ∋ a / b ⊆ c
                c ⊇ b
                """, graph.print(String::compareTo));
        graph.replace("b", "c", IS_FIELD_OF, CONTAINS_AS_FIELD);
        assertEquals("""
                a ∈ b
                b ∋ a / b ≺ c
                c ≻ b
                """, graph.print(String::compareTo));
        assertEquals("{b=∈}", graph.successors("a").toString());
        assertEquals("a=∋, c=≺", graph.successors("b").entrySet()
                .stream().map(Object::toString).sorted().collect(Collectors.joining(", ")));

        graph.removeVertices(Set.of("a"));
        assertEquals("{}", graph.successors("a").toString());

        assertEquals("""
                b ≺ c
                c ≻ b
                """, graph.print(String::compareTo));
    }
}
