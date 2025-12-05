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
