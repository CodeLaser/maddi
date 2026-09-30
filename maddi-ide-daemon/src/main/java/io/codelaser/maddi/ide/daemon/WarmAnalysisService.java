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

package io.codelaser.maddi.ide.daemon;

import io.codelaser.maddi.modification.common.AnalyzerException;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.callgraph.ComputeAnalysisOrder;
import io.codelaser.maddi.callgraph.ComputeCallGraph;
import io.codelaser.maddi.cst.api.analysis.Message;
import io.codelaser.maddi.cst.api.element.ModuleInfo;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.DetectKotlinSources;
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;
import io.codelaser.maddi.kotlin.realm.K2Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Runs a whole-project maddi analysis per request, on a warm JVM. A <b>fresh</b> inspector is built each
 * request so analysis state is clean (the process, not the inspector, is what stays warm — no spawn / JIT
 * cost). Ports {@code RunAnalyzer.runAnalyzer()} (parse → prep → order → analyze), with two IDE-oriented
 * differences: {@code failFast=false} (partial projects still yield findings) and results collected into plain
 * JSON rather than written to disk.
 * <p>
 * Two parse paths share everything after the parse. A Java-only project takes the openjdk inspector, as it
 * always has. A project with Kotlin sources takes {@link MixedProjectInspector} — the path of
 * {@code RunMixedPrepAnalyzer} — when the Kotlin front end can be installed ({@link K2Realm}: the compiler's
 * jars are not on this JVM's classpath but found through {@code -Dmaddi.k2.home} / {@code -Dmaddi.k2.classpath});
 * otherwise it falls back to the Java half, and says so.
 */
public class WarmAnalysisService implements AnalyzeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WarmAnalysisService.class);

    private final InputConfigurationAssembler assembler = new InputConfigurationAssembler();

    /** What either parse path hands to the shared tail. */
    private record Parsed(JavaInspector inspector,
                          Runtime runtime,
                          SourceSet sourceSetOfRequest,
                          Collection<TypeInfo> primaryTypes,
                          Collection<ModuleInfo> moduleInfos,
                          Summary summary,
                          boolean partial,
                          int hints) {
    }

    @Override
    public DaemonProtocol.Result analyze(DaemonProtocol.AnalyzeProject request, StatusSink status) throws Exception {
        long start = System.currentTimeMillis();
        String requestId = request.requestId();
        boolean parallel = request.config().parallel();

        emit(status, requestId, "initialize", "building inspector", null, null);
        InputConfiguration inputConfiguration = assembler.build(request.config());
        List<String> initProblems = new ArrayList<>();

        DetectKotlinSources kotlinSources = DetectKotlinSources.in(inputConfiguration);
        String kotlinUnavailable = kotlinSources.found() ? installKotlinFrontEnd() : null;
        Parsed parsed;
        if (kotlinSources.found() && kotlinUnavailable == null) {
            parsed = parseMixed(inputConfiguration, requestId, status, initProblems);
        } else {
            if (kotlinSources.found()) {
                // ⚠ Not a refusal, as the CLI's is: an IDE asking about a mixed project is better served by the
                // Java half plus a visible problem than by nothing at all -- and initProblems is the channel that
                // already carries "your analysis is not what you think it is" to the editor.
                String problem = kotlinSources.fileCount() + " Kotlin source file(s) in "
                                 + kotlinSources.sourceSetNames() + " are NOT analyzed: " + kotlinUnavailable
                                 + " The findings below cover the Java sources alone.";
                LOGGER.error("{}", problem);
                initProblems.add(problem);
            }
            parsed = parseJava(inputConfiguration, parallel, requestId, status, initProblems);
        }
        Summary summary = parsed.summary();

        ResultCollector collector = new ResultCollector(parsed.runtime(), parsed.sourceSetOfRequest());

        emit(status, requestId, "prep", "call graph", null, parsed.primaryTypes().size());
        // Fault-tolerant like RunAnalyzer's: on a partial parse a type whose supertype did not resolve is far
        // likelier to trip prep, and one such type must not abort the run for everything else. The isolated
        // failures are reported through prepAnalyzer.exceptions() below.
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(parsed.runtime(),
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        ComputeCallGraph ccg = prepAnalyzer.doPrimaryTypesReturnComputeCallGraph(
                Set.copyOf(parsed.primaryTypes()),
                parsed.moduleInfos(),
                typeInfo -> false,
                parallel);

        emit(status, requestId, "order", "analysis order", null, parsed.primaryTypes().size());
        List<Info> order = new ComputeAnalysisOrder().go(ccg.graph(), parallel);

        emit(status, requestId, "analyze", "modification analysis", 0, order.size());
        IteratingAnalyzer.Configuration modConfig = new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(10)
                .setTrackObjectCreations(false)
                .setFaultTolerant(true) // isolate a crash on one element into a finding; don't abort the run
                // advisory "you are one member away from @Container/@Immutable/..." warnings; opt-in, as in
                // RunAnalyzer, because they are noisy on a codebase that has not been curated for them
                .setWarnNearMisses(request.config().warnNearMisses())
                .build();
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(parsed.inspector(), modConfig);
        // Stream what each pass established, so the IDE can annotate the file on screen long before the run
        // ends: the first pass decides most of the output, and the tail is long but decides little.
        StreamingValueFeed valueFeed = new StreamingValueFeed(status, requestId, collector);
        analyzer.setValueFeed(valueFeed);
        // analyze() is one long blocking step with no sub-progress; run it on a worker and heartbeat so the
        // client's socket read never times out on a large project. A throw propagates: DaemonMain turns it into
        // an error{}, the daemon survives.
        runWithHeartbeat(status, requestId, order.size(), () -> analyzer.analyze(order));
        List<Message> messages = analyzer.messages();

        emit(status, requestId, "collect", "collecting results", null, null);
        List<DaemonProtocol.Finding> findings = collector.collectFindings(messages, summary);
        List<DaemonProtocol.ElementAnnotation> elementAnnotations =
                collector.collectElementAnnotations(parsed.primaryTypes());

        long elapsed = System.currentTimeMillis() - start;
        // A partial parse can never be certified: the missing compilation units contribute no references, so a
        // surviving type's properties may be weaker than its code allows. Downgrade rather than let the client
        // read a fixpoint outcome as final. (parseErrorCount on the Result carries the detail; no protocol change.)
        String outcome = parsed.partial() ? DaemonProtocol.OUTCOME_UNKNOWN : valueFeed.outcome();
        List<AnalyzerException> prepExceptions = prepAnalyzer.exceptions();
        if (!prepExceptions.isEmpty()) {
            LOGGER.warn("prep isolated {} type(s)/method(s); they were skipped", prepExceptions.size());
        }
        LOGGER.info("analysis complete in {} ms ({}{}): {} findings, {} element annotations",
                elapsed, outcome, parsed.partial() ? ", PARTIAL parse" : "", findings.size(), elementAnnotations.size());
        int parseErrors = summary == null ? 0 : summary.parseExceptions().size();
        return new DaemonProtocol.Result(requestId, findings, elementAnnotations, initProblems,
                parseErrors, parsed.hints(), elapsed, outcome);
    }

    /** The Java-only parse: the openjdk inspector, hints loaded before the parse. */
    private static Parsed parseJava(InputConfiguration inputConfiguration, boolean parallel, String requestId,
                                    StatusSink status, List<String> initProblems) throws IOException {
        JavaInspector inspector = new JavaInspectorImpl(true, false); // openjdk inspector (run-openjdk style)
        inspector.initialize(inputConfiguration).stream().map(String::valueOf).forEach(initProblems::add);

        // Eagerly parse the JDK packages whose hints we load (after initialize, so the classpath is set), then
        // load the bundled JDK + library analysis hints so modification/immutability/independence of library
        // types is known, not guessed from shallow defaults. Order mirrors run-openjdk's RunAnalyzer.
        HintsLoader hintsLoader = new HintsLoader();
        hintsLoader.preload(inspector);
        inspector.onlyPreload(); // commit preloaded types so LoadAnalysisResults can resolve them (like CommonTest)

        SourceSet sourceSet = inspector.mainSources();
        if (sourceSet == null) {
            sourceSet = inputConfiguration.sourceSets().stream().findAny().orElse(null);
        }

        emit(status, requestId, "hints", "loading analysis hints", null, null);
        int hints = hintsLoader.loadHints(inspector.runtime(), sourceSet);
        LOGGER.info("preloaded {} primary types of analysis hints", hints);

        JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true)      // precise Source positions, required for inline hints
                .setFailFast(false)            // IDE: a project with in-progress errors still yields partial results
                .setParallel(parallel)
                .setLombok(inputConfiguration.containsLombok())
                .build();

        emit(status, requestId, "parse", "parsing sources", null, null);
        Summary summary = inspector.parse(parseOptions);

        // A parse error must not cost the whole project its analysis. This used to return findings-only on
        // haveErrors(), which meant ONE unresolvable module yielded zero annotations everywhere — measured on
        // Pulsar: 344 parse errors out of 98 source roots, and not one element annotation. That also
        // contradicted setFailFast(false) above, which is set precisely so a mid-edit tree still yields
        // something. So: analyse what did parse, and report the run as partial via parseErrorCount.
        boolean partialParse = summary.haveErrors();
        if (partialParse) {
            LOGGER.info("parse produced {} error(s); analysing the {} type(s) that did parse",
                    summary.parseExceptions().size(), summary.types().size());
        }
        ParseResult parseResult = partialParse ? summary.parseResultIgnoringErrors() : summary.parseResult();
        return new Parsed(inspector, inspector.runtime(), sourceSet, parseResult.primaryTypes(),
                parseResult.sourceSetToModuleInfoMap().values(), summary, partialParse, hints);
    }

    /**
     * The mixed Java+Kotlin parse, as {@code RunMixedPrepAnalyzer} does it: one shared core, each type in its
     * own source set, hints loaded AFTER the parse (only by then is the compiled-types manager populated).
     */
    private static Parsed parseMixed(InputConfiguration inputConfiguration, String requestId, StatusSink status,
                                     List<String> initProblems) {
        HintsLoader hintsLoader = new HintsLoader();
        JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true)      // precise Source positions, required for inline hints
                .setFailFast(false)            // IDE: a project with in-progress errors still yields partial results
                // ⚠ not parallel: the mixed driver interleaves javac and K2 on one thread
                .setLombok(inputConfiguration.containsLombok())
                .build();
        MixedProjectInspector.Settings settings =
                new MixedProjectInspector.Settings(parseOptions, true, hintsLoader::preload);

        emit(status, requestId, "parse", "parsing Java and Kotlin sources", null, null);
        MixedProjectInspector.Result result = new MixedProjectInspector(settings).parse(inputConfiguration);
        Runtime runtime = result.getRuntime();

        // A placeholder is EMPTY to every consumer downstream, so a hole in a Kotlin body reads as a body with
        // nothing to say. The editor must be told how many there are, and a run that has any is not certifiable.
        PlaceholderCensus census = PlaceholderCensus.of(result.getKotlinTypes());
        if (census.getTotal() > 0) {
            LOGGER.warn("{}", census.report(8));
            initProblems.add(census.report(8));
        }

        SourceSet sourceSetOfRequest = result.getKotlinBySourceSet().keySet().stream().findFirst()
                .orElseGet(() -> inputConfiguration.sourceSets().stream().findAny().orElse(null));
        emit(status, requestId, "hints", "loading analysis hints", null, null);
        int hints = hintsLoader.loadHints(runtime, sourceSetOfRequest);
        LOGGER.info("preloaded {} primary types of analysis hints", hints);

        Summary summary = result.getJavaSummary();
        boolean partialParse = summary != null && summary.haveErrors();
        if (partialParse) {
            LOGGER.info("Java parse produced {} error(s); analysing the type(s) that did parse",
                    summary.parseExceptions().size());
        }
        List<TypeInfo> primaryTypes = Stream.concat(result.getKotlinTypes().stream(), result.getJavaTypes().stream())
                .map(TypeInfo::primaryType)
                .distinct()
                .collect(Collectors.toList());
        LOGGER.info("mixed parse: {} Kotlin and {} Java type(s), {} primary", result.getKotlinTypes().size(),
                result.getJavaTypes().size(), primaryTypes.size());
        return new Parsed(result.getJavaInspector(), runtime, sourceSetOfRequest, primaryTypes, List.of(), summary,
                partialParse || census.getTotal() > 0, hints);
    }

    /**
     * Install the Kotlin front end in this JVM, once. @return null when it is (or already was) installed, else why
     * not — the reason reaches the editor as an init problem.
     */
    private static String installKotlinFrontEnd() {
        try {
            K2Realm.installIfAbsent();
            return null;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Kotlin front end not available: {}", e.toString());
            return "the Kotlin front end is not installed (" + e.getMessage() + ").";
        }
    }

    /**
     * Run the (blocking, possibly long) analysis on the CURRENT thread — maddi's runtime type-cache is not
     * thread-safe, so all pipeline work must stay on one thread. A separate heartbeat thread only sends status
     * frames (socket I/O, never touches maddi state) so the client's read doesn't time out on a large project.
     */
    private static void runWithHeartbeat(StatusSink status, String requestId, int total, Runnable analysis) {
        Thread heartbeat = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(2_000);
                    emit(status, requestId, "analyze", "modification analysis (running)", null, total);
                }
            } catch (InterruptedException ignored) {
                // stopped
            }
        }, "maddi-heartbeat");
        heartbeat.setDaemon(true);
        heartbeat.start();
        try {
            analysis.run();
        } finally {
            heartbeat.interrupt();
        }
    }

    private static void emit(StatusSink status, String requestId, String phase, String message,
                             Integer typesDone, Integer typesTotal) {
        if (status != null) {
            status.status(new DaemonProtocol.Status(requestId, phase, message, typesDone, typesTotal));
        }
    }
}
