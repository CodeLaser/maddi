package org.e2immu.analyzer.modification.prepwork.variable.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class Reads {

    public static final Reads NOT_YET_READ = new Reads(List.of());

    private final List<String> indices;

    public Reads(List<String> indices) {
        this.indices = indices;
    }

    public Reads(String readIndexOrNull) {
        this.indices = readIndexOrNull == null ? List.of() : List.of(readIndexOrNull);
    }

    public List<String> indicesBetween(String fromIncl, String toExcl) {
        int pos0 = Collections.binarySearch(indices, fromIncl);
        int p0 = pos0 >= 0 ? pos0 : -(pos0 + 1);
        int pos1 = Collections.binarySearch(indices, toExcl);
        int p1 = pos1 >= 0 ? pos1 : -(pos1 + 1);
        if (p0 >= p1) return List.of();
        return indices.subList(p0, p1);
    }

    public boolean between(String fromIncl, String toExcl) {
        if (fromIncl.compareTo(toExcl) >= 0) return false;

        int pos0 = Collections.binarySearch(indices, fromIncl);
        int p0 = pos0 >= 0 ? pos0 : -(pos0 + 1);
        if (p0 >= indices.size()) return false;
        String s0 = indices.get(p0);
        if (s0.compareTo(fromIncl) < 0) return false;

        return s0.compareTo(toExcl) < 0;
    }

    public List<String> indices() {
        return indices;
    }

    public boolean isEmpty() {
        return indices.isEmpty();
    }

    // we re-sort, because sometimes indices come in later (DoStatement, ForStatement condition)
    public Reads with(List<String> newIndices) {
        return new Reads(Stream.concat(indices.stream(), newIndices.stream()).distinct().sorted().toList());
    }

    @Override
    public String toString() {
        if (indices.isEmpty()) return "-";
        return String.join(", ", indices);
    }
}
