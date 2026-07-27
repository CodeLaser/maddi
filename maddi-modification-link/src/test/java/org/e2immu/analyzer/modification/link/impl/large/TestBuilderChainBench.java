package org.e2immu.analyzer.modification.link.impl.large;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shape that makes the linker unusable on Elasticsearch (toolkit task #22): ONE long straight-line method
 * that calls the same local object over and over. {@code RestIndicesAction} has two of them —
 * {@code getTableWithHeader} and {@code buildTable}, 294 {@code table.addCell(...)} calls between them — and
 * running MODIFICATION_LINK over the ES parse died there: ten minutes in, the per-statement time still climbing
 * (3.0 s → 7.8 s inside the one method), JVM killed at 12 G.
 * <p>
 * Nothing about that shape is deep or generic; it is the flattest code in the codebase. So this measures the
 * GROWTH CURVE rather than a single number: the same method emitted at several lengths.
 * <p>
 * The cause was {@code SharedVariables.assignmentSources}, called from a triple-nested loop in
 * {@code FollowGraph.followGraph} — per group sibling, per rep expansion, per graph vertex — and that whole walk
 * runs once per method call in a statement. Each call rebuilt a forward adjacency map over the group's
 * assignments and re-walked it, and every {@code addCell} adds an assignment to the same group. Both halves are
 * now memoized on {@link org.e2immu.analyzer.modification.link.impl.localvar.SharedVariable}, where every
 * mutation of the group passes:
 * <pre>
 *   n     before      after
 *   25      91 ms      88 ms
 *   50     133 ms      48 ms
 *  100     737 ms      72 ms
 *  200   10004 ms     230 ms      (43x)
 *  400   (~4 min)    1130 ms
 * </pre>
 * That takes the curve from cubic to quadratic. It is deliberately NOT asserted as linear: what remains is
 * {@code followGraph} walking the graph's variables once per statement, which is a design property rather than a
 * defect. The assertion compares the ENDPOINTS (4x the statements) because a single doubling is too noisy: a
 * cubic linker needs ~64x there and a quadratic one ~16x, so the threshold separates them with room to spare.
 */
public class TestBuilderChainBench extends CommonTest {

    /**
     * The RestIndicesAction shape, reduced: a builder-ish object, mutated n times in one method. A fresh type
     * name per size — an inspection can only be committed once per JavaInspector instance.
     */
    private static String input(int n) {
        StringBuilder sb = new StringBuilder();
        sb.append("package a.b;\n");
        sb.append("import java.util.*;\n");
        sb.append("class X").append(n).append(" {\n");
        sb.append("    static class Table {\n");
        sb.append("        private final List<String> cells = new ArrayList<>();\n");
        sb.append("        Table addCell(String header, String attributes) {\n");
        sb.append("            cells.add(header + attributes);\n");
        sb.append("            return this;\n");
        sb.append("        }\n");
        sb.append("        List<String> cells() { return cells; }\n");
        sb.append("    }\n");
        sb.append("    Table build() {\n");
        sb.append("        Table table = new Table();\n");
        for (int i = 0; i < n; i++) {
            sb.append("        table.addCell(\"h").append(i).append("\", \"alias:a").append(i)
                    .append(";desc:column ").append(i).append("\");\n");
        }
        sb.append("        return table;\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private long linkMillis(int n) {
        TypeInfo x = javaInspector.parse("a.b.X" + n, input(n));
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(x);
        long t0 = System.nanoTime();
        new LinkComputerImpl(javaInspector).doPrimaryType(x);
        return (System.nanoTime() - t0) / 1_000_000;
    }

    @Test
    public void bench() {
        int[] sizes = {25, 50, 100, 200, 400};
        long[] times = new long[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            times[i] = linkMillis(sizes[i]);
            String ratio = i == 0 ? "-"
                    : String.format("%.2fx for 2x statements", times[i] / (double) Math.max(1, times[i - 1]));
            System.out.printf("PROBE n=%4d link=%6d ms   %s%n", sizes[i], times[i], ratio);
        }
        // n=100 -> n=400: 4x the statements. Measured 15.7x after the fix; a return to the cubic behaviour puts
        // this near 64x, so 30 separates them without being noise-sensitive.
        int lo = sizes.length - 3, hi = sizes.length - 1;
        double ratio = times[hi] / (double) Math.max(1, times[lo]);
        assertTrue(ratio < 30.0,
                "linking must not go cubic again as a straight-line method grows: n=" + sizes[lo] + " took "
                + times[lo] + " ms, n=" + sizes[hi] + " took " + times[hi] + " ms (" + ratio
                + "x for 4x the statements)");
    }
}
