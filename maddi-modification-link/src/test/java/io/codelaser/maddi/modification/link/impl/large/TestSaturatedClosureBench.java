package org.e2immu.analyzer.modification.link.impl.large;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Toolkit task #31: what the link engine's per-method work ceiling is actually spent on.
 * <p>
 * The ceiling ({@code IncrementalFixpointEngine}, 10M edge visits per method) degrades 110 Elasticsearch
 * methods. Instrumenting the corpus ({@code LINKWORK} lines) ruled out the two standing explanations:
 * <ul>
 *   <li>It is <b>not method length</b>. {@code ClusterState.copyAndUpdateMetadata} is one line and
 *       {@code ClusterStateUpdaters.setLocalNode} is four.</li>
 *   <li>There is <b>no cliff</b>. 549 methods spend over 100k, p90 is 4.5M, the largest survivor 9.81M —
 *       the tail runs continuously into the ceiling, so the line cuts a distribution rather than catching a
 *       distinct pathological class.</li>
 * </ul>
 * What the expensive methods share is a <b>saturated closure</b>: the median links <b>55% of all ordered
 * variable pairs</b> and 24 methods link over 95% — every variable to every other. Since propagation walks
 * the closure, saturation IS the cost: N linked variables mean ~N² facts and cubic propagation.
 * <p>
 * The three variants below are reductions of the smallest fully saturated ES method,
 * {@code TypeParsers.parseMeta} (38 variables, 1338 facts, 100% saturated), which is a plain validation
 * method — no builder, no loop worth the name. <b>Two of the three are negative results, and they are kept
 * because they were the expensive part of the answer:</b>
 * <ol>
 *   <li>{@code accumulator} — a fluent {@code field(name, value)} chain, the shape suggested by the
 *       {@code toXContent}/{@code writeTo}/{@code buildTable} names dominating the saturated list, and the
 *       one #22 hit from the other side. <b>Flat</b>: 80 values cost what 10 do.</li>
 *   <li>{@code messages} — n exception messages each concatenating a shared parameter with a distinct value,
 *       which is literally what parseMeta's bulk is. <b>Flat</b>, with {@code Object} operands or without.</li>
 *   <li>{@code containerViews} — one container decomposed into keySet / values / entrySet / stream, which is
 *       what parseMeta does with the map it validates. Every view legitimately links back to the container,
 *       so every view links to every other view: saturation that is correct rather than accidental.</li>
 * </ol>
 * PROBE lines carry the numbers. The assertions are loose regression guards, not complexity specifications.
 */
public class TestSaturatedClosureBench extends CommonTest {

    /** A fresh type name per size: an inspection can only be committed once per JavaInspector instance. */
    private long linkMillis(String fqn, String source) {
        TypeInfo t = javaInspector.parse(fqn, source);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(t);
        long t0 = System.nanoTime();
        new LinkComputerImpl(javaInspector).doPrimaryType(t);
        return (System.nanoTime() - t0) / 1_000_000;
    }

    /** Variant 1: a fluent accumulator fed n distinct values. */
    private static String accumulator(int n) {
        StringBuilder sb = new StringBuilder();
        sb.append("package a.b;\nimport java.util.*;\nclass Acc").append(n).append(" {\n");
        sb.append("    static class Builder {\n");
        sb.append("        private final Map<String, Object> map = new HashMap<>();\n");
        sb.append("        Builder field(String name, Object value) { map.put(name, value); return this; }\n");
        sb.append("        Map<String, Object> build() { return map; }\n");
        sb.append("    }\n");
        sb.append("    Map<String, Object> toXContent(Builder builder");
        for (int i = 0; i < n; i++) sb.append(", Object v").append(i);
        sb.append(") {\n        return builder");
        for (int i = 0; i < n; i++) sb.append("\n            .field(\"f").append(i).append("\", v").append(i).append(")");
        sb.append("\n            .build();\n    }\n}\n");
        return sb.toString();
    }

    /** Variant 2: n exception messages, each concatenating the SHARED parameter with a distinct value. */
    private static String messages(int n) {
        StringBuilder sb = new StringBuilder();
        sb.append("package a.b;\nclass Msg").append(n).append(" {\n");
        sb.append("    static class Failure extends RuntimeException {\n");
        sb.append("        Failure(String message) { super(message); }\n    }\n");
        sb.append("    void validate(String name");
        for (int i = 0; i < n; i++) sb.append(", Object v").append(i);
        sb.append(") {\n");
        for (int i = 0; i < n; i++) {
            sb.append("        if (v").append(i).append(" == null) {\n");
            sb.append("            throw new Failure(\"value ").append(i).append(" is null, got [\" + v")
                    .append(i).append(" + \"] for field [\" + name + \"]\");\n        }\n");
        }
        sb.append("    }\n}\n");
        return sb.toString();
    }

    /** Variant 3: ONE container decomposed into n views — parseMeta's actual shape. */
    private static String containerViews(int n) {
        StringBuilder sb = new StringBuilder();
        sb.append("package a.b;\nimport java.util.*;\nimport java.util.stream.*;\n");
        sb.append("class Views").append(n).append(" {\n");
        sb.append("    List<Object> validate(Map<String, Object> meta) {\n");
        sb.append("        List<Object> out = new ArrayList<>();\n");
        for (int i = 0; i < n; i++) {
            switch (i % 5) {
                case 0 -> sb.append("        Set<String> keys").append(i).append(" = meta.keySet();\n")
                        .append("        out.add(keys").append(i).append(");\n");
                case 1 -> sb.append("        Collection<Object> values").append(i).append(" = meta.values();\n")
                        .append("        out.add(values").append(i).append(");\n");
                case 2 -> sb.append("        Set<Map.Entry<String, Object>> entries").append(i)
                        .append(" = meta.entrySet();\n        out.add(entries").append(i).append(");\n");
                case 3 -> sb.append("        List<String> sorted").append(i)
                        .append(" = meta.keySet().stream().sorted().collect(Collectors.toList());\n")
                        .append("        out.add(sorted").append(i).append(");\n");
                default -> sb.append("        Map<String, Object> copy").append(i)
                        .append(" = new HashMap<>(meta);\n        out.add(copy").append(i).append(");\n");
            }
        }
        sb.append("        return out;\n    }\n}\n");
        return sb.toString();
    }

    @DisplayName("#31: which reduction of a saturated ES method actually costs")
    @Test
    public void bench() {
        int[] sizes = {5, 10, 20, 40};
        long[] views = new long[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            long acc = linkMillis("a.b.Acc" + sizes[i], accumulator(sizes[i]));
            long msg = linkMillis("a.b.Msg" + sizes[i], messages(sizes[i]));
            views[i] = linkMillis("a.b.Views" + sizes[i], containerViews(sizes[i]));
            System.out.printf("PROBE n=%3d  accumulator=%6d ms   messages=%6d ms   containerViews=%6d ms%n",
                    sizes[i], acc, msg, views[i]);
        }
        double ratio = views[sizes.length - 1] / (double) Math.max(1, views[0]);
        assertTrue(ratio < 400.0,
                "decomposing one container into views must not explode: " + sizes[0] + " views took "
                + views[0] + " ms, " + sizes[sizes.length - 1] + " took " + views[sizes.length - 1]
                + " ms (" + ratio + "x for 8x the views)");
    }
}
