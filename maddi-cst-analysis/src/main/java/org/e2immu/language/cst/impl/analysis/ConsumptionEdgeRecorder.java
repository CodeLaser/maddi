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

package org.e2immu.language.cst.impl.analysis;

import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.ParameterInfo;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task #35 Phase A (measure, GO/NO-GO): records, per analyzed element, which OTHER elements'
 * analysis maps were touched during its analysis — an UPPER BOUND on true summary consumption
 * (every read AND write goes through {@code analysis()}; writes to other elements are rare).
 * The GO/NO-GO metric is consumer-closure sparsity: if even this upper bound's reverse closure is
 * a small fraction of the corpus, invalidation over consumption edges beats SCC-transitive
 * invalidation by construction.
 *
 * <p>PRESENCE-only env gates CONSEDGES or CHECKPOINT (house convention) — a checkpointed run
 * records so the NEXT run can wake direct consumers of changed elements (task #35 phase C; the
 * Phase A measurement proved recording inert via a 0-diff FPDUMP A/B). Near-zero overhead when
 * off (plain static boolean, effectively JIT-foldable; {@link #arm()} exists for in-JVM tests
 * that cannot set the environment). Hooked at InfoImpl/ParameterInfoImpl.analysis() and armed
 * around SingleIterationAnalyzerImpl.processElement. Parameters normalize to their method, so
 * edges live at analysis-order granularity (type/method/field).
 */
public class ConsumptionEdgeRecorder {
    public static boolean ENABLED = System.getenv("CONSEDGES") != null || System.getenv("CHECKPOINT") != null;

    /** for in-JVM tests; production arming is by environment at process start */
    public static void arm() {
        ENABLED = true;
    }

    private static final ThreadLocal<Info> CURRENT = new ThreadLocal<>();
    private static final ConcurrentHashMap<Info, Set<Info>> EDGES = new ConcurrentHashMap<>();

    private ConsumptionEdgeRecorder() {
    }

    public static void setCurrent(Info info) {
        if (ENABLED) CURRENT.set(normalize(info));
    }

    public static void clearCurrent() {
        if (ENABLED) CURRENT.remove();
    }

    public static void record(Info read) {
        if (!ENABLED) return;
        Info current = CURRENT.get();
        if (current == null) return;
        Info normalized = normalize(read);
        if (normalized != current) {
            EDGES.computeIfAbsent(current, _ -> ConcurrentHashMap.newKeySet()).add(normalized);
        }
    }

    private static Info normalize(Info info) {
        return info instanceof ParameterInfo pi ? pi.methodInfo() : info;
    }

    public static void reset() {
        EDGES.clear();
    }

    /** snapshot of the recorded edges (consumer -> consumed elements), for persistence (task #35 phase C) */
    public static Map<Info, Set<Info>> edgesSnapshot() {
        Map<Info, Set<Info>> copy = new HashMap<>();
        EDGES.forEach((k, v) -> copy.put(k, Set.copyOf(v)));
        return copy;
    }

    /** distribution report; sampled reverse-closure sizes are THE Phase A metric */
    public static String statistics() {
        int elements = EDGES.size();
        long edgeCount = EDGES.values().stream().mapToLong(Set::size).sum();
        List<Integer> outDegrees = EDGES.values().stream().map(Set::size).sorted().toList();

        // reverse adjacency for the consumer closure
        Map<Info, Set<Info>> reverse = new ConcurrentHashMap<>();
        EDGES.forEach((from, tos) -> tos.forEach(to ->
                reverse.computeIfAbsent(to, _ -> ConcurrentHashMap.newKeySet()).add(from)));
        List<Integer> inDegrees = reverse.values().stream().map(Set::size).sorted().toList();

        // deterministic sample: every k-th element by FQN; BFS the reverse (consumer) closure
        List<Info> sorted = EDGES.keySet().stream()
                .sorted(Comparator.comparing(Info::fullyQualifiedName)).toList();
        int sampleSize = Math.min(100, sorted.size());
        List<Integer> closures = new ArrayList<>(sampleSize);
        if (sampleSize > 0) {
            int step = Math.max(1, sorted.size() / sampleSize);
            for (int i = 0; i < sorted.size(); i += step) {
                Set<Info> seen = new HashSet<>();
                Deque<Info> stack = new ArrayDeque<>();
                stack.push(sorted.get(i));
                while (!stack.isEmpty()) {
                    Info v = stack.pop();
                    for (Info consumer : reverse.getOrDefault(v, Set.of())) {
                        if (seen.add(consumer)) stack.push(consumer);
                    }
                }
                closures.add(seen.size());
            }
            closures.sort(Comparator.naturalOrder());
        }
        return "Consumption edges: " + elements + " consuming elements, " + edgeCount + " edges"
               + "; out-degree median/p90/max " + pct(outDegrees, 50) + "/" + pct(outDegrees, 90)
               + "/" + (outDegrees.isEmpty() ? 0 : outDegrees.getLast())
               + "; in-degree median/p90/max " + pct(inDegrees, 50) + "/" + pct(inDegrees, 90)
               + "/" + (inDegrees.isEmpty() ? 0 : inDegrees.getLast())
               + "; sampled consumer-closure median/p90/max " + pct(closures, 50) + "/"
               + pct(closures, 90) + "/" + (closures.isEmpty() ? 0 : closures.getLast())
               + " (of " + elements + " elements, " + closures.size() + " samples)";
    }

    private static int pct(List<Integer> sorted, int percentile) {
        if (sorted.isEmpty()) return 0;
        return sorted.get(Math.min(sorted.size() - 1, sorted.size() * percentile / 100));
    }
}
