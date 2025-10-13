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
