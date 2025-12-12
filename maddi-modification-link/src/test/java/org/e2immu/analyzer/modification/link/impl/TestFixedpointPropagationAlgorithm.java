package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFixedpointPropagationAlgorithm {

    public static final String START = "a";

    @Test
    public void test() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", LinkNature.IS_IDENTICAL_TO));
        assertEquals("b: ==", compute(graph));
    }

    @Test
    public void test2() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", LinkNature.IS_IDENTICAL_TO));
        graph.put("b", Map.of("c", LinkNature.CONTAINS));

        assertEquals("b: ==, c: >", compute(graph));
    }

    // a > b == c < d --> a ~ d
    @Test
    public void test3() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", LinkNature.CONTAINS));
        graph.put("b", Map.of("c", LinkNature.IS_IDENTICAL_TO));
        graph.put("c", Map.of("d", LinkNature.IS_ELEMENT_OF));
        assertEquals("b: >, c: >, d: ~", compute(graph));
    }

    // a < b == c > d --> no relation between a and d
    @Test
    public void test3b() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", LinkNature.IS_ELEMENT_OF));
        graph.put("b", Map.of("c", LinkNature.IS_IDENTICAL_TO));
        graph.put("c", Map.of("d", LinkNature.CONTAINS));
        assertEquals("b: <, c: <, d: X", compute(graph));
    }

    // ≥ is "hasField", ≤ is "is field of"
    // a ≥ b == c < d --> a ~ d
    @Test
    public void test4() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", LinkNature.HAS_FIELD));
        graph.put("b", Map.of("c", LinkNature.IS_IDENTICAL_TO));
        graph.put("c", Map.of("d", LinkNature.IS_ELEMENT_OF));
        assertEquals("b: ≥, c: ≥, d: ~", compute(graph));
    }

    // a ≥ b == c > d --> a > d
    @Test
    public void test4b() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", LinkNature.HAS_FIELD));
        graph.put("b", Map.of("c", LinkNature.IS_IDENTICAL_TO));
        graph.put("c", Map.of("d", LinkNature.CONTAINS));
        assertEquals("b: ≥, c: ≥, d: >", compute(graph));
    }

    // a ≥ b == c ~ d --> a ~ d
    @Test
    public void test4c() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", LinkNature.HAS_FIELD));
        graph.put("b", Map.of("c", LinkNature.IS_IDENTICAL_TO));
        graph.put("c", Map.of("d", LinkNature.IS_ELEMENT_OF));
        assertEquals("b: ≥, c: ≥, d: ~", compute(graph));
    }

    // a ≥ b == c : d --> a ~ d
    @Test
    public void test4d() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", LinkNature.HAS_FIELD));
        graph.put("b", Map.of("c", LinkNature.IS_IDENTICAL_TO));
        graph.put("c", Map.of("d", LinkNature.IS_FIELD_OF));
        assertEquals("b: ≥, c: ≥, d: ~", compute(graph));
    }

    // a ≤ b < c --> a < c
    @Test
    public void test5a() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", LinkNature.IS_FIELD_OF));
        graph.put("b", Map.of("c", LinkNature.IS_ELEMENT_OF));
        assertEquals("b: ≤, c: <", compute(graph));
    }

    // a ≤ b ~ c --> a ~ c
    @Test
    public void test5b() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", LinkNature.IS_FIELD_OF));
        graph.put("b", Map.of("c", LinkNature.INTERSECTION_NOT_EMPTY));
        assertEquals("b: ≤, c: ~", compute(graph));
    }

    // a ≤ b > c --> a X c
    @Test
    public void test5c() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", LinkNature.IS_FIELD_OF));
        graph.put("b", Map.of("c", LinkNature.CONTAINS));
        assertEquals("b: ≤, c: X", compute(graph));
    }

    // a ≤ b ≥ c --> a X c
    @Test
    public void test5d() {
        Map<String, Map<String, LinkNature>> graph = new HashMap<>();
        graph.put(START, Map.of("b", LinkNature.IS_FIELD_OF));
        graph.put("b", Map.of("c", LinkNature.HAS_FIELD));
        assertEquals("b: ≤, c: X", compute(graph));
    }

    private String compute(Map<String, Map<String, LinkNature>> graph) {
        Map<String, Set<LinkNature>> res =
                FixpointPropagationAlgorithm.computePathLabels(s -> graph.getOrDefault(s, Map.of()),
                        graph.keySet(), START, LinkNature.EMPTY, LinkNature::combine);
        Map<String, LinkNature> eventual = res.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                e -> e.getValue().stream().reduce(LinkNature.EMPTY, LinkNature::combine)));
        return eventual.entrySet().stream()
                .filter(e -> !START.equals(e.getKey()))
                .map(e -> e.getKey() + ": " + e.getValue()).sorted()
                .collect(Collectors.joining(", "));
    }
}
