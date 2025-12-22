package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFixedpointPropagationAlgorithm {

    public static final String START = "a";

    @Test
    public void test() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", IS_IDENTICAL_TO));
        assertEquals("b: ≡", compute(graph));
    }

    // a == b ∋ c --> a ∋ c
    @Test
    public void test2() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", IS_IDENTICAL_TO));
        graph.put("b", Map.of("c", CONTAINS_AS_MEMBER));

        assertEquals("b: ≡, c: ∋", compute(graph));
    }

    // a and d share an element
    // a ∋ b == c ∈ d --> a ~ d
    @Test
    public void test3() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", CONTAINS_AS_MEMBER));
        graph.put("b", Map.of("c", IS_IDENTICAL_TO));
        graph.put("c", Map.of("d", IS_ELEMENT_OF));
        assertEquals("b: ∋, c: ∋, d: ~", compute(graph));
    }

    // a ∈ b == c ∋ d --> no relation between a and d
    @Test
    public void test3b() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", IS_ELEMENT_OF));
        graph.put("b", Map.of("c", IS_IDENTICAL_TO));
        graph.put("c", Map.of("d", CONTAINS_AS_MEMBER));
        assertEquals("b: ∈, c: ∈, d: X", compute(graph));
    }

    // a ~ b == c ∈ d --> a ∈ d
    @Test
    public void test6() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", SHARES_ELEMENTS));
        graph.put("b", Map.of("c", IS_IDENTICAL_TO));
        graph.put("c", Map.of("d", IS_ELEMENT_OF));
        assertEquals("b: ~, c: ~, d: X", compute(graph));
    }

    // a ~ b == c ∋ d --> a ∋ d
    @Test
    public void test6b() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", SHARES_ELEMENTS));
        graph.put("b", Map.of("c", IS_IDENTICAL_TO));
        graph.put("c", Map.of("d", CONTAINS_AS_MEMBER));
        assertEquals("b: ~, c: ~, d: X", compute(graph));
    }

    private String compute(Map<String, Map<String, LinkNature>> graph) {
        Map<String, Set<LinkNature>> res =
                FixpointPropagationAlgorithm.computePathLabels(s -> graph.getOrDefault(s, Map.of()),
                        graph.keySet(), START, LinkNatureImpl.EMPTY, LinkNature::combine);
        Map<String, LinkNature> eventual = res.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                e -> e.getValue().stream().reduce(LinkNatureImpl.EMPTY, LinkNature::combine)));
        return eventual.entrySet().stream()
                .filter(e -> !START.equals(e.getKey()))
                .map(e -> e.getKey() + ": " + e.getValue()).sorted()
                .collect(Collectors.joining(", "));
    }
}
