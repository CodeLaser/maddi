package org.e2immu.analyzer.modification.analyzer.shadow;

import ch.qos.logback.classic.Level;
import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.analyzer.impl.ModAnalyzerForTesting;
import org.e2immu.analyzer.modification.analyzer.impl.SingleIterationAnalyzerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults.ANALYZED_RESULTS;
import static org.e2immu.language.inspection.resource.SourceSetImpl.testProtocolSourceSet;
import org.junit.jupiter.api.Tag;

/**
 * PLAN-modification-reachability phase 1: the shadow diff over the clone-bench corpus (testarchive,
 * 'analyzed' branch) — real-world snippets, analyzed per primary type exactly like TestCloneBench,
 * but with trackObjectCreations (the shadow's E1 needs LINKED_VARIABLES_ARGUMENTS) and without
 * writing analyzed sources. Aggregates divergences (frozen optimistic TRUE, shadow says modified)
 * for classification, and reverse divergences (frozen modified, shadow unreached), which indicate
 * shadow-pass gaps.
 */
@Tag("slow")
public class TestShadowCloneBench extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestShadowCloneBench.class);
    private final JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder().build();

    public TestShadowCloneBench() {
        super("jmod:java.desktop",
                "jmod:java.compiler",
                "jmod:java.datatransfer",
                "jmod:java.sql",
                "jmod:java.logging",
                "jmod:java.instrument",
                "jmod:java.rmi",
                "jmod:java.management");
    }

    private static final String[] DIRS = {"bubblesort_for_withunit", "collections_layered",
            "dowhile_pure_compiles", "dowhile_pure_selected_withunit",
            "foreach_pure_compiles", "foreach_selection1_withunit",
            "fors_pure_compiles", "fors_pure_selected_withunit",
            "switch_fors_compiles", "switch_fors_selected_withunit",
            "switch_pure_compiles", "switch_pure_selected_withunit",
            "try_pure_compiles", "try_wr_compiles",
            "while_pure_compiles", "while_pure_selected_withunit"
    };

    @Override
    @BeforeEach
    public void beforeEach() throws IOException {
        List<SourceSet> dirSets = new ArrayList<>();
        for (String dir : DIRS) {
            Path srcDir = Path.of("../../testarchive/" + dir + "/src/main/java");
            dirSets.add(new SourceSetImpl.Builder()
                    .setName(dir)
                    .setSourceDirectories(List.of(srcDir))
                    .setUri(srcDir.toUri())
                    .build());
        }
        List<String> jdkModules = Arrays.stream(jmods)
                .map(s -> s.startsWith("jmod:") ? s.substring("jmod:".length()) : s).toList();
        javaInspector = org.e2immu.analyzer.modification.common.CommonTest.javaInspectorWithExtras(
                dirSets.getFirst(), dirSets.subList(1, dirSets.size()), jdkModules);
        dirSets.forEach(SourceSet::computePriorityDependencies);
        runtime = javaInspector.runtime();
        javaInspector.setParameterNames(true);
        javaInspector.onlyPreload();
        new LoadAnalysisResults(javaInspector.runtime(), testProtocolSourceSet()).go(ANALYZED_RESULTS);
        prepAnalyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer = new SingleIterationAnalyzerImpl(javaInspector, new IteratingAnalyzerImpl.ConfigurationBuilder().build());
    }

    private record TypeResult(TypeInfo typeInfo, ShadowModificationPass.Report report,
                              List<String> divergences, List<String> reverseExplained) {
    }

    @Test
    public void test() throws Exception {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("graph-algorithm")).setLevel(Level.WARN);

        Summary summary = javaInspector.parse(parseOptions);
        List<TypeInfo> types = summary.types().stream()
                .filter(TypeInfo::isPrimaryType)
                .filter(t -> {
                    URI uri = t.compilationUnit().uri();
                    return uri != null && uri.getPath() != null
                           && uri.getPath().endsWith(".java") && !uri.getPath().endsWith("_t.java");
                })
                .toList();
        LOGGER.warn("Parsed {} types to analyze", types.size());

        int parallelism = Integer.getInteger("clonebench.parallelism",
                Math.max(1, Math.min(java.lang.Runtime.getRuntime().availableProcessors(), 4)));
        AtomicInteger counter = new AtomicInteger();
        ConcurrentLinkedQueue<TypeInfo> queue = new ConcurrentLinkedQueue<>(types);
        ConcurrentLinkedQueue<TypeResult> results = new ConcurrentLinkedQueue<>();

        List<Callable<Void>> workers = new ArrayList<>();
        for (int w = 0; w < parallelism; w++) {
            workers.add(() -> {
                PrepAnalyzer prep = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
                ModAnalyzerForTesting an = new SingleIterationAnalyzerImpl(javaInspector,
                        new IteratingAnalyzerImpl.ConfigurationBuilder().setTrackObjectCreations(true).build());
                TypeInfo typeInfo;
                while ((typeInfo = queue.poll()) != null) {
                    int count = counter.incrementAndGet();
                    LOGGER.info("Analyzing #{}, {}", count, typeInfo);
                    List<Info> analysisOrder = prep.doPrimaryType(typeInfo);
                    an.go(analysisOrder);
                    ShadowModificationPass.Report report = new ShadowModificationPass().go(analysisOrder);
                    if (!report.divergences().isEmpty() || !report.reverseDivergences().isEmpty()) {
                        List<String> divs = report.divergences().stream()
                                .map(d -> d + " [" + (report.cause().containsKey(d.info()) ? "propagated" : "seed")
                                     + "] || " + report.explain(d.info())).toList();
                        List<String> revs = report.reverseDivergences().stream()
                                .map(ShadowModificationPass.Divergence::toString).toList();
                        results.add(new TypeResult(typeInfo, report, divs, revs));
                    }
                    accumulate(report);
                }
                return null;
            });
        }
        List<Future<Void>> futures;
        try (ExecutorService exec = Executors.newFixedThreadPool(parallelism)) {
            futures = exec.invokeAll(workers);
        }
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof AssertionError ae) throw ae;
                if (cause instanceof Exception ex) throw ex;
                throw new RuntimeException(cause);
            }
        }

        System.out.println("SHADOWBENCH types=" + counter.get()
                           + " methods=" + totalMethods + " seeds=" + totalSeeds + " edges=" + totalEdges
                           + " missingArgLinks=" + totalMissingArgLinks
                           + " unprojectedReceivers=" + totalUnprojectedReceivers);
        List<TypeResult> sorted = results.stream()
                .sorted(Comparator.comparing(tr -> tr.typeInfo().fullyQualifiedName())).toList();
        int totalDiv = 0, totalRev = 0;
        Map<String, Integer> byProperty = new TreeMap<>();
        Map<String, Integer> byClass = new TreeMap<>();
        for (TypeResult tr : sorted) {
            totalDiv += tr.divergences().size();
            totalRev += tr.reverseExplained().size();
            tr.report().divergences().forEach(d -> {
                byProperty.merge(d.property(), 1, Integer::sum);
                byClass.merge(tr.report().cause().containsKey(d.info()) ? "propagated" : "seed", 1, Integer::sum);
            });
            String src = tr.typeInfo().compilationUnit().uri().getPath();
            String shortSrc = src.substring(src.indexOf("testarchive"));
            tr.divergences().forEach(d -> System.out.println("SHADOWBENCH DIV [" + shortSrc + "] " + d));
            tr.reverseExplained().forEach(d -> System.out.println("SHADOWBENCH REV [" + shortSrc + "] " + d));
        }
        System.out.println("SHADOWBENCH totals: " + totalDiv + " divergences " + byProperty + " " + byClass
                           + ", " + totalRev + " reverse, in " + sorted.size() + " of " + counter.get() + " types");

        // the phase-1 baseline (2026-07-19, engine at kotlin fba60b23): every divergence is a
        // frozen optimistic TRUE the reachability evidence contradicts — directly in the frozen
        // method's own converged summary ("seed": the refused-downgrade class the STRICTCERT
        // counter measures), or via multi-hop propagation (the deep-capture-chain class). A change
        // in these numbers means the engine or the pass moved: re-baseline and reclassify, don't
        // just bump. Re-baselined 2026-07-19 from {1,6,272}/{71,208} when the pass gained the
        // statement-level field-modification seed channel (mirror of FieldAnalyzerImpl's
        // UNMODIFIED_VARIABLE read, closing the 8 fernflower reverse divergences): +2 fields
        // (TestData.expected/.other modified via a local in an anonymous execute()) +2 downstream
        // constructor parameters, all classified seed = refused downgrades.
        // Re-baselined AGAIN 2026-07-19 (precision fixes, TestElementFlowWidening diagnosis): E3
        // now uses the engine's own nature filter (relevantLinkForModification — content-tier
        // element flow is not modification transfer) and the closure no longer propagates THROUGH
        // immutable-typed nodes. The seed class was untouched (212 = genuine refused downgrades,
        // each in the method's own summary); the propagated class collapsed 71 -> 12 (59 were
        // shadow artifacts). Fernflower: 971 -> 452 divergences, 0 reverse throughout.
        org.junit.jupiter.api.Assertions.assertEquals(0, totalRev,
                "reverse divergences are shadow-pass bugs (incomplete seeds or edges)");
        org.junit.jupiter.api.Assertions.assertEquals(
                Map.of("nonModifyingMethod", 1, "unmodifiedField", 8, "unmodifiedParameter", 215),
                byProperty);
        org.junit.jupiter.api.Assertions.assertEquals(Map.of("propagated", 12, "seed", 212), byClass);
    }

    private volatile int totalMethods, totalSeeds, totalEdges, totalMissingArgLinks, totalUnprojectedReceivers;

    private synchronized void accumulate(ShadowModificationPass.Report r) {
        totalMethods += r.methods();
        totalSeeds += r.seeds();
        totalEdges += r.edgeCount();
        totalMissingArgLinks += r.callSitesWithoutArgumentLinks();
        totalUnprojectedReceivers += r.unprojectedReceivers();
    }
}
