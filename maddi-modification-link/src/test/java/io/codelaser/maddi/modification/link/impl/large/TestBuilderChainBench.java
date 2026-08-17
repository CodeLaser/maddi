package org.e2immu.analyzer.modification.link.impl.large;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.localvar.SharedVariable;
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
 * defect.
 * <p>
 * ⚠ <b>THE "after" COLUMN IS AS-OF {@code 835b0ea56} (2026-07-27) AND IS NO LONGER WHAT THIS MACHINE MEASURES —
 * n=400 is now ~1500 ms, 1.3x that figure. IT IS NOT A REGRESSION, AND THE COUNTS PROVE IT.</b> Bisected over the
 * 328 commits to {@code e15b1267f} "reuse per-statement extractions outside the dirty web; scope-part vertex
 * index", confirmed by direct A/B (1219 ms at its parent, 1518 ms at it, non-overlapping over three runs each) —
 * an OPTIMISATION whose own message measures elasticsearch-server 17-20% faster and fernflower CPU −26%. It adds
 * caches keyed on group version, and its comment states the assumption: <i>"membership mutations are rare,
 * per-statement drains are not"</i>. This fixture is the adversarial case for exactly that assumption — ONE group
 * mutated at essentially every statement (the 403 rebuilds at n=400 below are one per mutation), so every such
 * cache is cleared before it can pay off while its maintenance is paid in full. A microbenchmark of the shape a
 * cache is invalidated by is not evidence against the cache.
 * <p>
 * ⭐ The reason this is stated as fact rather than suspicion: {@code computes} is <b>80 605 at n=400 at BOTH
 * ends of those 328 commits</b>, unchanged to the digit while the wall clock moved 1.3x. The counter asserted
 * below is blind to constant-factor drift by construction, which is precisely what makes it the right guard —
 * and what a millisecond threshold could never have told apart from a real regression.
 * <p>
 * ⛔⛔ <b>THIS ASSERTED WALL-CLOCK MILLISECONDS UNTIL 2026-08-15, AND THAT WAS WRONG IN BOTH DIRECTIONS.</b> It
 * failed in CI at 31.7x against a threshold of 30 with nothing regressed — at n=100 the work is ~100 ms, and the
 * denominator alone swings 53↔118 ms between runs on one machine, moving the ratio from 15x to 32x. That is the
 * flakiness. The worse half is what the control showed: with the memo bypassed — a genuine return to cubic — the
 * measured times were 135 ms → 3156 ms, a ratio of <b>23x, which PASSES a threshold of 30</b>. So the timing
 * guard reported failure on a healthy build and success on a broken one. ▶ <b>A THRESHOLD ON A NOISY RATIO IS NOT
 * A WEAK GUARD, IT IS AN UNRELATED ONE.</b>
 * <p>
 * ⭐ It now counts the CORE LOOP instead: misses on the {@code assignmentSources} memo, which is precisely the
 * quantity the fix removed. Measured on this machine, byte-identical across three forced re-runs while the times
 * moved by 15%:
 * <pre>
 *   n     computes   rebuilds        computes, memo BYPASSED (the control)
 *   25         355         28              5 588
 *   50       1 330         53             43 038
 *  100       5 155        103            338 563
 *  200      20 305        203          2 687 113
 *  400      80 605        403         21 414 213
 *   ratio 100→400:  15.64x  (quadratic)        63.25x  (cubic)
 * </pre>
 * The two regimes sit 4x apart in a deterministic integer, where the timings sat 23x vs 32x THE WRONG WAY ROUND.
 * The threshold is 25: ~60% headroom above the healthy 15.64, and 2.5x clear of the cubic 63.25.
 * <p>
 * ⚠ The timings are still printed, because the growth curve is the thing being reasoned about and a human reading
 * a regression wants them — but nothing asserts on them.
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

    /** One size: the memo misses that linking the method costs, plus the wall clock for the human reader. */
    private record Measurement(long computes, long rebuilds, long millis) {
    }

    private Measurement link(int n) {
        TypeInfo x = javaInspector.parse("a.b.X" + n, input(n));
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(x);
        SharedVariable.resetCoreLoopCounters();
        long t0 = System.nanoTime();
        new LinkComputerImpl(javaInspector).doPrimaryType(x);
        long millis = (System.nanoTime() - t0) / 1_000_000;
        return new Measurement(SharedVariable.assignmentSourceComputations(),
                SharedVariable.forwardAssignmentRebuilds(), millis);
    }

    @Test
    public void bench() {
        int[] sizes = {25, 50, 100, 200, 400};
        Measurement[] m = new Measurement[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            m[i] = link(sizes[i]);
            System.out.printf("PROBE n=%4d computes=%8d rebuilds=%6d  (%6d ms)%n",
                    sizes[i], m[i].computes(), m[i].rebuilds(), m[i].millis());
        }
        // n=100 -> n=400 is 4x the statements: quadratic is ~16x, cubic ~64x. Endpoints rather than a single
        // doubling, because 4x separates the two regimes twice as far as 2x does.
        int lo = sizes.length - 3, hi = sizes.length - 1;

        double computeRatio = m[hi].computes() / (double) Math.max(1, m[lo].computes());
        assertTrue(computeRatio < 25.0,
                "linking must not go cubic again as a straight-line method grows: assignmentSources was computed "
                + m[lo].computes() + " times at n=" + sizes[lo] + " and " + m[hi].computes() + " times at n="
                + sizes[hi] + " (" + computeRatio + "x for 4x the statements; ~16x is the quadratic curve this"
                + " guards, ~64x is the cubic one it used to have)");

        // The second half of the same fix, which the wall-clock assertion never covered: the forward adjacency
        // map is built once per group version, so its rebuild count must stay LINEAR. Measured 103 -> 403.
        double rebuildRatio = m[hi].rebuilds() / (double) Math.max(1, m[lo].rebuilds());
        assertTrue(rebuildRatio < 8.0,
                "the forward-assignment map must stay memoized per group version: " + m[lo].rebuilds()
                + " rebuilds at n=" + sizes[lo] + ", " + m[hi].rebuilds() + " at n=" + sizes[hi] + " ("
                + rebuildRatio + "x for 4x the statements, linear is ~4x)");
    }
}
