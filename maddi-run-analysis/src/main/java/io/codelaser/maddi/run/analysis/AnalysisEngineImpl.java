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

package io.codelaser.maddi.run.analysis;

import io.codelaser.maddi.aapi.parser.AnalysisHints;
import io.codelaser.maddi.aapi.parser.AnalysisHintsCompiler;
import io.codelaser.maddi.aapi.parser.AnalysisHintsComposer;
import io.codelaser.maddi.analysis.api.AnalysisEngine;
import io.codelaser.maddi.analysis.api.AnalysisProblem;
import io.codelaser.maddi.analysis.api.HintsSpec;
import io.codelaser.maddi.analysis.api.ModificationOptions;
import io.codelaser.maddi.analysis.api.ModificationOutcome;
import io.codelaser.maddi.analysis.api.ModificationRequest;
import io.codelaser.maddi.analysis.api.PrepOutcome;
import io.codelaser.maddi.analysis.api.PrepRequest;
import io.codelaser.maddi.callgraph.ComputeCallGraph;
import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.cst.api.analysis.Message;
import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.integration.JavaInspectorFactory;
import io.codelaser.maddi.modification.analyzer.CheckpointWriter;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import io.codelaser.maddi.modification.analyzer.impl.EventualCluster;
import io.codelaser.maddi.modification.analyzer.impl.StaticSideEffectAnalyzerImpl;
import io.codelaser.maddi.modification.analyzer.shadow.ShadowModificationPass;
import io.codelaser.maddi.modification.common.AnalyzerException;
import io.codelaser.maddi.modification.common.defaults.ShallowMethodAnalyzer;
import io.codelaser.maddi.modification.link.io.LinkCodec;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.io.AnalysisFingerprint;
import io.codelaser.maddi.modification.prepwork.io.DecoratorImpl;
import io.codelaser.maddi.modification.prepwork.io.IncrementalState;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import io.codelaser.maddi.modification.prepwork.io.PrepWorkCodec;
import io.codelaser.maddi.modification.prepwork.io.WriteAnalysisResults;
import io.codelaser.maddi.cst.impl.analysis.ConsumptionEdgeRecorder;
import io.codelaser.maddi.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The implementation of {@link AnalysisEngine}, registered for {@link java.util.ServiceLoader} both in
 * {@code module-info.java} ({@code provides}) and in {@code META-INF/services} (class-path callers: the build
 * plugins' workers, the IDE daemon).
 * <p>
 * The bodies are the ones the run drivers held before split stage 3, moved rather than rewritten: the prep and
 * modification blocks of run-openjdk's {@code RunAnalyzer}, including its experimental environment gates, which
 * run only when {@link ModificationOptions#environmentGates()} asks for them.
 */
public class AnalysisEngineImpl implements AnalysisEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisEngineImpl.class);

    @Override
    public String name() {
        return "maddi-run-analysis";
    }

    // ---- prep

    @Override
    public PrepOutcome prep(PrepRequest request) {
        PrepAnalyzer prepAnalyzer = request.faultTolerant() == null
                ? new PrepAnalyzer(request.runtime())
                : new PrepAnalyzer(request.runtime(),
                new PrepAnalyzer.Options.Builder().setFaultTolerant(request.faultTolerant()).build());
        ComputeCallGraph ccg = prepAnalyzer.doPrimaryTypesReturnComputeCallGraph(request.primaryTypes(),
                request.moduleInfos(), request.externalsToAccept(), request.parallel());
        return new PrepOutcome(ccg, problems(prepAnalyzer.exceptions()));
    }

    private static List<AnalysisProblem> problems(List<AnalyzerException> exceptions) {
        return exceptions.stream()
                .map(ae -> new AnalysisProblem(ae.getInfo(), ae.getCause() == null ? ae : ae.getCause()))
                .toList();
    }

    @Override
    public Set<Property> bookkeepingProperties() {
        return Set.of(PrepAnalyzer.PREPPED);
    }

    // ---- modification

    @Override
    public ModificationOutcome modification(ModificationRequest request) throws IOException {
        // The two feature switches are process-wide statics of the analyzer; a request that names one sets it for
        // this run only and puts the previous value back, as the tests that toggled them directly always did.
        ModificationOptions o = request.options();
        boolean sseBefore = StaticSideEffectAnalyzerImpl.ENABLED;
        boolean clusterBefore = EventualCluster.ENABLED;
        if (o.staticSideEffects() != null) StaticSideEffectAnalyzerImpl.ENABLED = o.staticSideEffects();
        if (o.eventualCluster() != null) EventualCluster.ENABLED = o.eventualCluster();
        try {
            return modificationWithSwitchesSet(request);
        } finally {
            StaticSideEffectAnalyzerImpl.ENABLED = sseBefore;
            EventualCluster.ENABLED = clusterBefore;
        }
    }

    private ModificationOutcome modificationWithSwitchesSet(ModificationRequest request) throws IOException {
        ModificationOptions o = request.options();
        JavaInspector javaInspector = request.javaInspector();
        List<Info> order = request.order();

        IteratingAnalyzerImpl.ConfigurationBuilder builder = new IteratingAnalyzerImpl.ConfigurationBuilder();
        if (o.maxIterations() != null) builder.setMaxIterations(o.maxIterations());
        if (o.stopWhenCycleDetectedAndNoImprovements() != null) {
            builder.setStopWhenCycleDetectedAndNoImprovements(o.stopWhenCycleDetectedAndNoImprovements());
        }
        if (o.environmentGates()) {
            // SHADOWDIFF (phase-1 reachability diff, PLAN §13) needs LINKED_VARIABLES_ARGUMENTS,
            // which only trackObjectCreations produces; note track-on shifts some verdicts
            // (P2.1 measured: nil cost, 0.12% churn on fernflower)
            builder.setTrackObjectCreations(System.getenv("SHADOWDIFF") != null);
            // MODREACH (PLAN §14 P2.3a, presence-only house convention): post-convergence
            // reachability pass becomes the single writer of the three modification
            // properties; implies trackObjectCreations
            // UNGATED 2026-08-01 alongside EVENTUALCLUSTER: the eventual layer needs the honest,
            // post-cutover modification state (without it the abstract-union race returns);
            // MODREACH=0 is the opt-out
            builder.setModificationViaReachability(!"0".equals(System.getenv("MODREACH")));
        } else {
            if (o.trackObjectCreations() != null) builder.setTrackObjectCreations(o.trackObjectCreations());
            if (o.modificationViaReachability() != null) {
                builder.setModificationViaReachability(o.modificationViaReachability());
            }
        }
        if (o.faultTolerant() != null) builder.setFaultTolerant(o.faultTolerant());
        if (o.warnNearMisses() != null) builder.setWarnNearMisses(o.warnNearMisses());
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(javaInspector, builder.build());
        if (request.valueFeed() != null) {
            analyzer.setValueFeed(request.valueFeed());
        }

        String checkpointDir = o.environmentGates() ? System.getenv("CHECKPOINT") : null;
        boolean checkpoint = checkpointDir != null && !checkpointDir.isBlank();
        if (checkpoint) {
            // task #34: CHECKPOINT=<dir> writes pass-boundary deltas so a crashed multi-hour run can
            // resume; CHECKPOINT_RESTORE (presence, with CHECKPOINT set) preloads the directory first —
            // the verify-certify sweep of the resumed run is the soundness net. Value-carrying gates,
            // FPDUMP convention.
            if (System.getenv("CHECKPOINT_RESTORE") != null) {
                try {
                    int loaded = new LoadAnalysisResults(javaInspector.runtime(), javaInspector.mainSources())
                            .goDirTolerant(new LinkCodec(javaInspector).restoreCodec(), new File(checkpointDir));
                    LOGGER.info("CHECKPOINT_RESTORE: preloaded {} primary types from {}", loaded, checkpointDir);
                } catch (IOException | RuntimeException e) {
                    LOGGER.error("CHECKPOINT_RESTORE failed, continuing cold: {}", e.toString());
                }
            }
            var linkCodec = new LinkCodec(javaInspector);
            analyzer.setValueFeed(new CheckpointWriter(javaInspector.runtime(), linkCodec::codec,
                    new File(checkpointDir)));
            LOGGER.info("CHECKPOINT: writing pass-boundary deltas to {}", checkpointDir);
        }
        Set<Info> initialDirty = o.environmentGates() ? incrementalSeed(request, analyzer) : null;

        if (initialDirty != null) {
            // clear-before-recompute: a dirtied element's carried cross-type-derived values
            // must not block the fresh, possibly-lowering re-analysis
            Consumer<Info> clearHook = info -> {
                info.analysis().removeIf(AnalysisFingerprint.CROSS_TYPE_DERIVED_ONLY);
                if (info instanceof MethodInfo mi) {
                    mi.parameters().forEach(p -> p.analysis().removeIf(AnalysisFingerprint.CROSS_TYPE_DERIVED_ONLY));
                }
            };
            analyzer.analyze(order, request.callGraph(), initialDirty, clearHook);
        } else if (request.callGraph() != null) {
            analyzer.analyze(order, request.callGraph()); // graph enables worklist narrowing (default ON, NOWORKLIST=1 opts out)
        } else {
            analyzer.analyze(order);
        }

        // phase-1 shadow diff (PLAN §13): one-shot reachability over the converged artifacts,
        // no writes; names the frozen optimistic values the evidence contradicts (§9.4 cross-read)
        if (o.environmentGates() && System.getenv("SHADOWDIFF") != null) {
            try {
                var report = new ShadowModificationPass().go(order);
                LOGGER.info("SHADOWDIFF {}", report.summary());
                // cause chain appended: distinguishes direct refused-downgrades from the E2/E6
                // union-over-implementations conservatism (§7.2) when classifying
                report.divergences().stream()
                        .sorted(Comparator.comparing(Object::toString))
                        .forEach(d -> LOGGER.info("SHADOWDIFF DIV {} || {}", d, report.explain(d.info())));
                // reverse = the pass missed something frozen-modified: a shadow-pass gap, must be
                // triaged to zero before the pass can gate phase 2 (its own soundness contract)
                report.reverseDivergences().forEach(d -> LOGGER.info("SHADOWDIFF REV {}", d));
            } catch (RuntimeException | AssertionError | StackOverflowError e) {
                LOGGER.error("SHADOWDIFF failed: {}", e.toString());
            }
        }
        List<Message> messages = analyzer.messages();

        int fpSets = 0;
        if (o.storeFingerprints()) {
            // analysisFingerprint: store each source set's rollup for incremental early-cutoff (docs/design/analysis-rewiring.md)
            fpSets = AnalysisFingerprint.storePerSourceSet(javaInspector.runtime(), request.primaryTypes()).size();
            LOGGER.info("Stored analysis fingerprints for {} source set(s)", fpSets);
        }
        // task #35 phase C: a checkpointed run leaves per-type OUTPUT fingerprints + the
        // recorded consumption edges (CHECKPOINT arms the recorder) so the next run can seed
        // the early-cutoff worklist with changed types + their DIRECT consumers
        if (checkpoint) {
            try {
                IncrementalState.capture(javaInspector.runtime(), request.primaryTypes(),
                                ConsumptionEdgeRecorder.edgesSnapshot())
                        .save(new File(checkpointDir));
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Cannot save incremental state: {}", e.toString());
            }
        }
        return new ModificationOutcome(messages, fpSets);
    }

    /**
     * task #35 phase C/D: INCREMENTAL=<dir of a prior CHECKPOINT run> — restore that run's
     * values, detect changed primary types by SOURCE fingerprint, seed the early-cutoff
     * worklist with the changed types' elements, and union the persisted consumption edges
     * into the wake relation. Unchanged elements keep their carried (restored) values; the
     * run stops when the worklist is dry. Value-carrying gate, FPDUMP convention.
     *
     * @return the seed, or null for a cold run
     */
    private static Set<Info> incrementalSeed(ModificationRequest request, IteratingAnalyzer analyzer)
            throws IOException {
        String incrementalDir = System.getenv("INCREMENTAL");
        if (incrementalDir == null || incrementalDir.isBlank()) return null;
        JavaInspector javaInspector = request.javaInspector();
        try {
            var state = IncrementalState.load(new File(incrementalDir));
            if (state.sourceFingerprints().isEmpty()) {
                LOGGER.warn("INCREMENTAL: no usable state in {}; running cold", incrementalDir);
                return null;
            }
            int loaded = new LoadAnalysisResults(javaInspector.runtime(), javaInspector.mainSources())
                    .goDirTolerant(new LinkCodec(javaInspector).restoreCodec(), new File(incrementalDir));
            Set<String> changed = state.changedTypes(request.primaryTypes());
            Set<Info> initialDirty = new HashSet<>();
            int unrestored = 0;
            Map<String, TypeInfo> typesByFqn = new HashMap<>();
            for (var info : request.order()) {
                var pt = info.typeInfo() == null ? null : info.typeInfo().primaryType();
                if (pt == null) continue;
                typesByFqn.putIfAbsent(pt.fullyQualifiedName(), pt);
                if (changed.contains(pt.fullyQualifiedName())) {
                    initialDirty.add(info);
                } else if (info.analysis().isEmpty()) {
                    // the restore's decode tail: an element with NO carried values stays
                    // null (no verification pass in incremental mode). Re-analyzing them
                    // (INCREMENTAL_FILL, presence gate) floods the worklist far past the
                    // tail itself (measured: slower than a cold run on fernflower) — the
                    // real fix is restore coverage (the shared codec fix list). Default:
                    // fast resume, holes counted here and reported.
                    unrestored++;
                    if (System.getenv("INCREMENTAL_FILL") != null) initialDirty.add(info);
                }
            }
            Map<Info, Set<Info>> wake = new HashMap<>();
            state.consumers().forEach((consumedFqn, consumerFqns) -> {
                var consumedType = typesByFqn.get(consumedFqn);
                if (consumedType == null) return;
                Set<Info> consumers = new HashSet<>();
                for (String c : consumerFqns) {
                    var t = typesByFqn.get(c);
                    if (t != null) consumers.add(t);
                }
                if (!consumers.isEmpty()) wake.put(consumedType, consumers);
            });
            if (analyzer instanceof IteratingAnalyzerImpl iai) {
                iai.setExternalWakeEdges(wake);
            }
            LOGGER.info("INCREMENTAL: restored {} type files, {} changed primary type(s), "
                        + "{} dirty seed element(s) ({} unrestored), {} wake-edge sources",
                    loaded, changed.size(), initialDirty.size(), unrestored, wake.size());
            return initialDirty;
        } catch (RuntimeException e) {
            LOGGER.error("INCREMENTAL setup failed; running cold: {}", e.toString());
            return null;
        }
    }

    // ---- results IO

    @Override
    public ResultsLoader resultsLoader(Runtime runtime, SourceSet sourceSetOfRequest) {
        LoadAnalysisResults loader = new LoadAnalysisResults(runtime, sourceSetOfRequest);
        return new ResultsLoader() {
            private Codec prepWorkCodec;

            @Override
            public int load(List<String> directories) throws IOException {
                return loader.go(directories);
            }

            @Override
            public int loadContent(String content) {
                if (prepWorkCodec == null) prepWorkCodec = new PrepWorkCodec(runtime, sourceSetOfRequest).codec();
                return loader.go(prepWorkCodec, content);
            }
        };
    }

    @Override
    public void writeResults(Runtime runtime, String targetDirectory, Trie<TypeInfo> types) throws IOException {
        new WriteAnalysisResults(runtime).write(targetDirectory, types);
    }

    @Override
    public void writeResultsWithLinks(Runtime runtime, JavaInspector javaInspector, SourceSet sourceSetOfRequest,
                                      File targetDirectory, Trie<TypeInfo> types) throws IOException {
        new WriteAnalysisResults(runtime).write(targetDirectory, types,
                new LinkCodec(javaInspector, sourceSetOfRequest).codec());
    }

    // ---- analysis hints

    @Override
    public HintsCompiler hintsCompiler(JavaInspectorFactory javaInspectorFactory) {
        AnalysisHintsCompiler compiler = new AnalysisHintsCompiler(javaInspectorFactory);
        return spec -> compiler.go(toAnalysisHints(spec));
    }

    static AnalysisHints toAnalysisHints(HintsSpec spec) {
        return new AnalysisHints.Builder()
                .setLibraryName(spec.libraryName())
                .setHintsPath(spec.hintsPath())
                .setPackagePrefix(spec.packagePrefix())
                .setPreloadAnalysisResultsDirs(spec.preloadAnalysisResultsDirs())
                .setAnalysisResultsDir(spec.analysisResultsDir())
                .setUpdatedHintsPath(spec.updatedHintsPath())
                .build();
    }

    @Override
    public HintsComposer hintsComposer(JavaInspector javaInspector, Function<SourceSet, String> destinationPackage,
                                       Predicate<Info> accept) {
        AnalysisHintsComposer composer = new AnalysisHintsComposer(javaInspector, destinationPackage, accept);
        return new HintsComposer() {
            @Override
            public Collection<TypeInfo> compose(Collection<TypeInfo> primaryTypes) {
                return composer.compose(primaryTypes);
            }

            @Override
            public Map<Element, Element> translateFromDollarToReal() {
                return composer.translateFromDollarToReal();
            }

            @Override
            public void write(Collection<TypeInfo> apiTypes, File base, Qualification.Decorator decorator)
                    throws IOException {
                composer.write(apiTypes, base, decorator);
            }
        };
    }

    @Override
    public Qualification.Decorator decorator(Runtime runtime, SourceSet sourceSetOfRequest,
                                             Map<Element, Element> translationMap) {
        return translationMap == null ? new DecoratorImpl(runtime, sourceSetOfRequest)
                : new DecoratorImpl(runtime, sourceSetOfRequest, translationMap);
    }

    @Override
    public void applyShallowDefaults(Runtime runtime, Collection<MethodInfo> methods) {
        ShallowMethodAnalyzer shallow = new ShallowMethodAnalyzer(runtime, Element::annotations);
        methods.forEach(shallow::analyze);
    }
}
