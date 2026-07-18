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

package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.analyzer.modification.common.util.TolerantWrite;
import org.e2immu.analyzer.modification.analyzer.CycleBreakingStrategy;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.SingleIterationAnalyzer;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class IteratingAnalyzerImpl extends CommonAnalyzerImpl implements IteratingAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(IteratingAnalyzerImpl.class);

    private final JavaInspector javaInspector;
    private SingleIterationAnalyzer lastRun;
    private final List<Message> guardMessages = new ArrayList<>();

    private org.e2immu.analyzer.modification.analyzer.AnalysisValueFeed valueFeed;

    @Override
    public void setValueFeed(org.e2immu.analyzer.modification.analyzer.AnalysisValueFeed feed) {
        this.valueFeed = feed;
    }

    // feed exceptions must never disturb the analysis
    private void feed(java.util.function.Consumer<org.e2immu.analyzer.modification.analyzer.AnalysisValueFeed> action) {
        if (valueFeed != null) {
            try {
                action.accept(valueFeed);
            } catch (RuntimeException re) {
                LOGGER.warn("AnalysisValueFeed threw; ignoring", re);
            }
        }
    }

    public IteratingAnalyzerImpl(JavaInspector javaInspector, Configuration configuration) {
        super(configuration, null, null);
        this.javaInspector = javaInspector;
    }

    public record ConfigurationImpl(int maxIterations,
                                    boolean stopWhenCycleDetectedAndNoImprovements,
                                    CycleBreakingStrategy cycleBreakingStrategy,
                                    boolean trackObjectCreations,
                                    boolean guardContracts,
                                    boolean faultTolerant,
                                    boolean warnNearMisses,
                                    NearMissPolicy nearMissPolicy) implements Configuration {
    }

    public static class ConfigurationBuilder {
        private int maxIterations = 1;
        private boolean stopWhenCycleDetectedAndNoImprovements;
        private boolean trackObjectCreations;
        private boolean guardContracts = true;
        private boolean faultTolerant;
        private boolean warnNearMisses;
        private NearMissPolicy nearMissPolicy = NearMissPolicy.STRICT;
        private CycleBreakingStrategy cycleBreakingStrategy = CycleBreakingStrategy.NONE;

        public ConfigurationBuilder setCycleBreakingStrategy(CycleBreakingStrategy cycleBreakingStrategy) {
            this.cycleBreakingStrategy = cycleBreakingStrategy;
            return this;
        }

        public ConfigurationBuilder setStopWhenCycleDetectedAndNoImprovements(boolean stopWhenCycleDetectedAndNoImprovements) {
            this.stopWhenCycleDetectedAndNoImprovements = stopWhenCycleDetectedAndNoImprovements;
            return this;
        }

        public ConfigurationBuilder setMaxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public ConfigurationBuilder setTrackObjectCreations(boolean trackObjectCreations) {
            this.trackObjectCreations = trackObjectCreations;
            return this;
        }

        public ConfigurationBuilder setGuardContracts(boolean guardContracts) {
            this.guardContracts = guardContracts;
            return this;
        }

        public ConfigurationBuilder setFaultTolerant(boolean faultTolerant) {
            this.faultTolerant = faultTolerant;
            return this;
        }

        public ConfigurationBuilder setWarnNearMisses(boolean warnNearMisses) {
            this.warnNearMisses = warnNearMisses;
            return this;
        }

        public ConfigurationBuilder setNearMissPolicy(NearMissPolicy nearMissPolicy) {
            this.nearMissPolicy = nearMissPolicy;
            return this;
        }

        public Configuration build() {
            return new ConfigurationImpl(maxIterations, stopWhenCycleDetectedAndNoImprovements, cycleBreakingStrategy,
                    trackObjectCreations, guardContracts, faultTolerant, warnNearMisses, nearMissPolicy);
        }
    }

    /**
     * A/B equivalence signal for worklist/convergence experiments: the distribution of the key verdicts over
     * all analysis-order elements. Two runs with identical fingerprints (very likely) computed identical verdicts.
     */
    private static void logVerdictFingerprint(List<Info> analysisOrder) {
        java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
        for (Info info : analysisOrder) {
            String key;
            if (info instanceof org.e2immu.language.cst.api.info.MethodInfo) {
                var v = info.analysis().getOrNull(org.e2immu.language.cst.impl.analysis.PropertyImpl.NON_MODIFYING_METHOD,
                        org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.class);
                key = "method.nonModifying=" + (v == null ? "?" : v);
            } else if (info instanceof org.e2immu.language.cst.api.info.FieldInfo) {
                var v = info.analysis().getOrNull(org.e2immu.language.cst.impl.analysis.PropertyImpl.UNMODIFIED_FIELD,
                        org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.class);
                key = "field.unmodified=" + (v == null ? "?" : v);
            } else if (info instanceof org.e2immu.language.cst.api.info.TypeInfo) {
                var v = info.analysis().getOrNull(org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE,
                        org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.class);
                key = "type.immutable=" + (v == null ? "?" : v);
            } else {
                key = "other";
            }
            counts.merge(key, 1, Integer::sum);
        }
        LOGGER.info("Verdict fingerprint: {}", counts);
        if (System.getenv("VL2OTIER") != null) {
            LOGGER.info(org.e2immu.analyzer.modification.link.impl.LinkComputerImpl.vl2oTierStats());
        }
        String dump = System.getenv("FPDUMP");
        if (dump != null) {
            try (java.io.PrintWriter pw = new java.io.PrintWriter(dump)) {
                for (Info info : analysisOrder) {
                    String v;
                    if (info instanceof org.e2immu.language.cst.api.info.MethodInfo) {
                        var b = info.analysis().getOrNull(org.e2immu.language.cst.impl.analysis.PropertyImpl.NON_MODIFYING_METHOD,
                                org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.class);
                        v = "method nonModifying=" + b;
                    } else if (info instanceof org.e2immu.language.cst.api.info.FieldInfo) {
                        var b = info.analysis().getOrNull(org.e2immu.language.cst.impl.analysis.PropertyImpl.UNMODIFIED_FIELD,
                                org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.class);
                        v = "field unmodified=" + b;
                    } else if (info instanceof org.e2immu.language.cst.api.info.TypeInfo) {
                        var b = info.analysis().getOrNull(org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE,
                                org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.class);
                        v = "type immutable=" + b;
                    } else continue;
                    pw.println(v + " " + info.fullyQualifiedName());
                }
            } catch (java.io.IOException e) {
                LOGGER.error("FPDUMP failed", e);
            }
        }
    }

    @Override
    public List<Message> messages() {
        return Stream.concat(lastRun == null ? Stream.of() : lastRun.messages().stream(),
                guardMessages.stream()).toList();
    }

    @Override
    public void analyze(List<Info> analysisOrder) {
        analyze(analysisOrder, null);
    }

    // Worklist narrowing: iterations 2+ only re-analyze changed elements + their dependents (reverse
    // dependency edges); requires the dependency graph. ON by default since the corpus proof (2026-07-17):
    // certified fixpoints on timefold + langchain4j, verdict dumps identical to full re-analysis.
    // Opt out with NOWORKLIST=1 (e.g. for baseline A/B runs).
    private static final boolean WORKLIST = System.getenv("NOWORKLIST") == null;

    @Override
    public void analyze(List<Info> analysisOrder, org.e2immu.util.internal.graph.G<Info> dependencyGraph) {
        analyze(analysisOrder, dependencyGraph, null, null);
    }

    @Override
    public void analyze(List<Info> analysisOrder, org.e2immu.util.internal.graph.G<Info> dependencyGraph,
                        java.util.Set<Info> initialDirty) {
        analyze(analysisOrder, dependencyGraph, initialDirty, null);
    }

    @Override
    public void analyze(List<Info> analysisOrder, org.e2immu.util.internal.graph.G<Info> dependencyGraph,
                        java.util.Set<Info> initialDirty, java.util.function.Consumer<Info> beforeFirstRecompute) {
        // incremental (early-cutoff) mode: seed the worklist with initialDirty and stop the moment it runs dry,
        // WITHOUT the full verification / cycle-breaking passes — those re-touch the untouched (carried) elements
        // and would defeat the skip. See analysis-rewiring.md and IteratingAnalyzer#analyze(List,G,Set).
        boolean incremental = initialDirty != null;
        // clear-before-recompute: elements already analysed (never to be re-cleared). Seeded with the seed itself,
        // which is fresh (INVALID) source, never carried — so the hook fires only for elements the worklist later
        // pulls in, i.e. carried types entering the dirty frontier, exactly once each, before their first analysis.
        java.util.Set<Info> everAnalyzed = incremental ? new java.util.HashSet<>(initialDirty) : java.util.Set.of();
        int iterations = 0;
        SingleIterationAnalyzer singleIterationAnalyzer = new SingleIterationAnalyzerImpl(javaInspector, configuration);
        this.lastRun = singleIterationAnalyzer;
        boolean cycleBreakingActive = false;
        int previousPropertiesChanged = Integer.MAX_VALUE;
        // reverse adjacency: dependersOf(Y) = { X | X depends on Y } — the elements to re-analyze when Y changes
        java.util.Map<Info, java.util.Set<Info>> dependersOf;
        if (WORKLIST && dependencyGraph != null) {
            // SYMMETRIC closure: the analyzers' true information flow runs along graph edges in BOTH
            // directions — the AbstractMethodAnalyzer derives an abstract method's verdicts from its
            // IMPLEMENTATIONS (graph edge impl -> abstract), the FieldAnalyzer reads its REFERRING methods
            // (graph edge reader -> field). One-directional reverse adjacency froze 88 abstract-method and
            // 23 field verdicts at conservative values (fpdump diffs, runs 16-18). Symmetric is a correct
            // superset wherever the graph has at least one edge per real dependency; tighten later if the
            // dirty-set growth ever matters (late-iteration sets are tiny).
            dependersOf = new java.util.HashMap<>();
            dependencyGraph.edgeStream().forEach(e -> {
                dependersOf.computeIfAbsent(e.to().t(), _ -> new java.util.HashSet<>()).add(e.from().t());
                dependersOf.computeIfAbsent(e.from().t(), _ -> new java.util.HashSet<>()).add(e.to().t());
            });
            // overrides may span source sets beyond the graph's vertex subset: keep the explicit relation
            for (Info info : analysisOrder) {
                if (info instanceof org.e2immu.language.cst.api.info.MethodInfo mi) {
                    for (org.e2immu.language.cst.api.info.MethodInfo overridden : mi.overrides()) {
                        dependersOf.computeIfAbsent(mi, _ -> new java.util.HashSet<>()).add(overridden);
                        dependersOf.computeIfAbsent(overridden, _ -> new java.util.HashSet<>()).add(mi);
                    }
                }
            }
            LOGGER.info("Worklist narrowing ACTIVE; symmetric reverse adjacency for {} elements", dependersOf.size());
        } else {
            dependersOf = null;
        }
        java.util.Set<Info> dirty = initialDirty; // null = analyze everything; a seed = incremental early-cutoff
        java.util.Set<Info> orderSet = java.util.Set.copyOf(analysisOrder);
        boolean verifying = false; // worklist ran dry -> one full pass certifies (0 changes = true fixpoint)
        // strata-parallel first iteration (PARALLEL=n): dependency waves from the same call graph
        java.util.List<java.util.List<java.util.List<Info>>> firstIterationWaves;
        if (!incremental && SingleIterationAnalyzerImpl.PARALLEL_THREADS > 1 && dependencyGraph != null
            && analysisOrder.size() >= SingleIterationAnalyzerImpl.MIN_ELEMENTS_FOR_PARALLEL) {
            firstIterationWaves = org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder
                    .waves(dependencyGraph);
            LOGGER.info("Computed {} first-iteration waves", firstIterationWaves.size());
        } else {
            firstIterationWaves = null;
        }
        while (true) {
            ++iterations;
            LOGGER.info("{}, cycle breaking active? {}", highlight("Start iteration " + iterations),
                    cycleBreakingActive);
            TolerantWrite.resetChangeCounts();
            List<Info> subset;
            if (dirty == null) {
                subset = analysisOrder;
            } else {
                java.util.Set<Info> d = dirty;
                subset = analysisOrder.stream().filter(d::contains).toList();
                LOGGER.info("Worklist: {} of {} elements dirty", subset.size(), analysisOrder.size());
            }
            if (incremental && beforeFirstRecompute != null) {
                // clear-before-recompute: newly-dirtied elements (never analysed before in this run) may be carried
                // types whose stale cross-type-derived values must be cleared before the fresh, possibly-lowering
                // re-analysis. Fire the hook once per element, before it enters go().
                for (Info info : subset) {
                    if (everAnalyzed.add(info)) beforeFirstRecompute.accept(info);
                }
            }
            Instant start = Instant.now();
            singleIterationAnalyzer.go(subset, cycleBreakingActive, iterations == 1,
                    iterations == 1 ? firstIterationWaves : null);
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            LOGGER.info("Duration of single iteration: {} min {} sec {} ms", duration.toMinutesPart(),
                    duration.toSecondsPart(), duration.toMillisPart());
            int propertiesChanged = singleIterationAnalyzer.propertiesChanged();
            {
                boolean fullPass = dirty == null;
                List<Info> analyzed = subset;
                int it = iterations;
                feed(f -> f.passCompleted(it, fullPass, analyzed));
            }
            // convergence diagnosis: which properties are still moving? (value-changing writes per property)
            String topChanges = TolerantWrite.changeCounts().entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(10)
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + ", " + b).orElse("-");
            LOGGER.info("Iteration {} property changes (top 10): {}", iterations, topChanges);
            // under an active worklist, a zero-change SUBSET iteration is not a fixpoint certificate — only a
            // zero-change FULL pass is; route through the verification branch below instead of stopping here
            boolean done = propertiesChanged == 0 && (dependersOf == null || verifying);
            // certification blind spot (PLAN-modification-reachability §9): refused downgrades are invisible
            // to the change count, so "no changes" can certify prematurely-frozen optimistic values. Surface
            // them; STRICTCERT=1 additionally refuses certification (the run then ends at maxIterations,
            // honestly uncertified).
            java.util.Map<String, Long> refused = TolerantWrite.refusedDowngrades();
            if (done && !refused.isEmpty()) {
                LOGGER.error("Certification reached with {} refused downgrade attempt(s) — frozen optimistic "
                             + "values survive the fixpoint (see PLAN-modification-reachability): {}",
                        refused.values().stream().mapToLong(Long::longValue).sum(), refused);
                if (System.getenv("STRICTCERT") != null) {
                    LOGGER.error("STRICTCERT=1: refusing certification");
                    done = false;
                }
            }
            // certification point. If types are still immutability-UNDECIDED, they are waiting on information
            // that will never arrive (external supers without values, call cycles): activate cycle breaking
            // for the remaining rounds and run one more full pass, so the fixpoint we certify includes the
            // broken-cycle conclusions. Opt-out NOCYCLEBREAKING=1.
            if (done && !cycleBreakingActive && !incremental && System.getenv("NOCYCLEBREAKING") == null) {
                long undecided = analysisOrder.stream()
                        .filter(i -> i instanceof org.e2immu.language.cst.api.info.TypeInfo t
                                     && t.analysis().getOrNull(
                                org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE,
                                org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.class) == null)
                        .count();
                if (undecided > 0) {
                    LOGGER.info("Certification point, but {} types immutability-undecided: activating cycle breaking",
                            undecided);
                    cycleBreakingActive = true;
                    {
                        int it = iterations;
                        feed(f -> f.phase(org.e2immu.analyzer.modification.analyzer.AnalysisValueFeed.Phase
                                .CYCLE_BREAKING_ACTIVATED, it));
                    }
                    dirty = null; // one more FULL pass, now with cycle breaking
                    if (dependersOf != null) verifying = true;
                    previousPropertiesChanged = propertiesChanged;
                    continue;
                }
            }
            // plateau: the change count no longer meaningfully decreases (an oscillation floor); further full
            // re-analyses only pay for the same flips again. Opt-in via stopWhenCycleDetectedAndNoImprovements.
            // Irrelevant under an active worklist: the verify-certify loop reaches a machine-checked fixpoint
            // (and a subset iteration's change count is not comparable to the previous iteration's anyway).
            boolean plateau = dependersOf == null
                              && configuration.stopWhenCycleDetectedAndNoImprovements()
                              && System.getenv("NOPLATEAU") == null
                              && iterations >= 3 && propertiesChanged >= 0.95 * previousPropertiesChanged;
            if (iterations == configuration.maxIterations() || done || plateau) {
                LOGGER.info("Stop iterating after {} iterations, done? {}{}", iterations, done,
                        plateau ? " (plateau: " + propertiesChanged + " vs " + previousPropertiesChanged + ")" : "");
                {
                    var terminal = done
                            ? org.e2immu.analyzer.modification.analyzer.AnalysisValueFeed.Phase.TERMINAL_CERTIFIED
                            : plateau
                            ? org.e2immu.analyzer.modification.analyzer.AnalysisValueFeed.Phase.TERMINAL_PLATEAU
                            : org.e2immu.analyzer.modification.analyzer.AnalysisValueFeed.Phase.TERMINAL_MAX_ITERATIONS;
                    int it = iterations;
                    feed(f -> f.phase(terminal, it));
                }
                logVerdictFingerprint(analysisOrder);
                if (configuration.guardContracts() || configuration.warnNearMisses()) {
                    // values are final now: verify user-written contracts, and/or warn on near-misses
                    new GuardAnalyzerImpl(javaInspector.runtime(), configuration, guardMessages).go(analysisOrder);
                }
                return;
            }
            previousPropertiesChanged = propertiesChanged;
            if (dependersOf != null) {
                // union in the summary-consumption edges the link computer discovered: a consumer re-runs
                // when a summary it ACTUALLY READ changes. Catches value-mediated flows (functional-
                // interface application) with no syntactic call-graph edge — the measured source of the
                // verification passes' residue (timefold: 142 modified-set changes per full pass).
                for (java.util.Map.Entry<org.e2immu.language.cst.api.info.MethodInfo,
                        java.util.Set<org.e2immu.language.cst.api.info.MethodInfo>> e
                        : singleIterationAnalyzer.consumedSummaries().entrySet()) {
                    Info consumer = mapToOrderElement(e.getKey(), orderSet);
                    if (consumer == null) continue;
                    for (org.e2immu.language.cst.api.info.MethodInfo consumed : e.getValue()) {
                        dependersOf.computeIfAbsent(consumed, _ -> new java.util.HashSet<>()).add(consumer);
                    }
                }
                // v2: only SUMMARY changes propagate — dependents cannot observe element-internal changes.
                // The changed element itself IS re-run: a self-recursive method's summary feeds its own next
                // analysis, and the call graph does not guarantee a self-edge (v2's first cut excluded self and
                // froze recursive methods at their early conservative verdicts).
                java.util.Set<Info> changed = singleIterationAnalyzer.summaryChangedInfos();
                java.util.Set<Info> next = new java.util.HashSet<>();
                for (Info c : changed) {
                    // an anonymous-class/lambda method is a graph vertex but NOT an analysis-order element:
                    // it only recomputes when its ENCLOSING method re-runs. Unmapped, such dirty elements
                    // were silently dropped by the subset filter — THE residue the verification passes kept
                    // finding (timefold: ~160 $N.test(...) methods rewriting their summaries per full pass).
                    Info mc = mapToOrderElement(c, orderSet);
                    if (mc != null) next.add(mc);
                    java.util.Set<Info> deps = dependersOf.get(c); // adjacency stays keyed by the raw vertex
                    if (deps != null) {
                        for (Info d : deps) {
                            Info md = mapToOrderElement(d, orderSet);
                            if (md != null) next.add(md);
                        }
                    }
                }
                dirty = next;
                if (verifying) {
                    if (propertiesChanged == 0) {
                        // certified: a FULL iteration changed nothing — this is the true fixpoint, regardless
                        // of any dependency-edge kinds the worklist's reverse adjacency might miss
                        LOGGER.info("Stop iterating after {} iterations: worklist dry + full verification pass clean",
                                iterations);
                        logVerdictFingerprint(analysisOrder);
                        if (configuration.guardContracts() || configuration.warnNearMisses()) {
                            new GuardAnalyzerImpl(javaInspector.runtime(), configuration, guardMessages).go(analysisOrder);
                        }
                        return;
                    }
                    // the full pass found changes the worklist missed: resume narrowing from them
                    LOGGER.info("Verification pass found {} changes; resuming worklist", propertiesChanged);
                    // residue diagnosis: WHO changed on a settled state? (adjacency-gap root-causing)
                    java.util.Set<Info> residue = singleIterationAnalyzer.summaryChangedInfos();
                    LOGGER.info("Verification-pass residue ({} elements): {}", residue.size(), residue.stream()
                            .map(Info::fullyQualifiedName).sorted().limit(12)
                            .reduce((a, b) -> a + ", " + b).orElse("-"));
                    verifying = false;
                } else if (dirty.isEmpty() || propertiesChanged == 0) {
                    if (incremental) {
                        // incremental early-cutoff: the seeded worklist is dry. STOP here, trusting the elements it
                        // never reached to keep their carried values — a full verification pass would re-analyze
                        // them (and hit the carried-value monotonic guards), defeating the skip. Correctness rests
                        // on the carried elements being fingerprint-stable, established before this call.
                        LOGGER.info("Incremental worklist dry after {} iterations; stopping (carried elements spared)",
                                iterations);
                        logVerdictFingerprint(analysisOrder);
                        if (configuration.guardContracts() || configuration.warnNearMisses()) {
                            new GuardAnalyzerImpl(javaInspector.runtime(), configuration, guardMessages).go(analysisOrder);
                        }
                        return;
                    }
                    // worklist dry (or a zero-change subset round): do NOT stop yet — run one full pass to
                    // certify (or catch missed dependencies)
                    LOGGER.info("Worklist empty/quiet after {} iterations; running a full verification pass", iterations);
                    dirty = null;
                    verifying = true;
                }
            }
            LOGGER.info("Run again, properties changed {}", propertiesChanged);
            // TODO any strategy, e.g. after 3 iterations, activate cycle breaking
        }
    }

    /**
     * Map an element to its analysis-order element: an anonymous-class or lambda method (and its members)
     * is not itself an order element — walk up through the enclosing methods until one is.
     */
    private static Info mapToOrderElement(Info info, java.util.Set<Info> orderSet) {
        Info c = info;
        int guard = 0;
        while (c != null && !orderSet.contains(c) && guard++ < 10) {
            org.e2immu.language.cst.api.info.TypeInfo t =
                    c instanceof org.e2immu.language.cst.api.info.TypeInfo ti ? ti : c.typeInfo();
            c = t == null ? null : t.enclosingMethod();
        }
        return c != null && orderSet.contains(c) ? c : null;
    }
}