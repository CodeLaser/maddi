package org.e2immu.language.inspection.openjdk;

import com.sun.source.util.JavacTask;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter2.Formatter2Impl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.resource.MD5FingerPrint;
import org.e2immu.language.inspection.api.resource.ParameterNameIndex;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.resource.InfoByFqn;
import org.e2immu.language.inspection.resource.SummaryImpl;
import org.e2immu.language.java.openjdk.InMemoryJavaFileObject;
import org.e2immu.language.java.openjdk.MaddiDiagnosticCollector;
import org.e2immu.language.java.openjdk.ScanCompilationUnits;
import org.e2immu.language.java.openjdk.UnresolvedSymbolException;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.ImmutableGraph;
import org.e2immu.util.internal.graph.op.Linearize;
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.*;

public class JavaInspectorImpl implements JavaInspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaInspectorImpl.class);
    private static final TimedLogger TIMED_LOGGER = new TimedLogger(LOGGER, 1000L);

    private Runtime runtime;
    // which primary types came out of which source file, keyed by (uri, source set) -- SourceFile's equality ignores
    // path and fingerprint. Filled after every scan; this is the map reloadSources diffs the source tree against.
    private final Map<SourceFile, List<TypeInfo>> sourceFiles = new HashMap<>();
    private CompiledTypesManager compiledTypesManager;
    private InputConfiguration inputConfiguration; // kept for tests
    private org.e2immu.language.cst.api.info.InfoMapView lastRewireInfoMap; // the last re-parse's rewire, read-only
    private final boolean computeFingerPrints;
    private final boolean allowCreationOfStubTypes;
    private final JavaCompiler javaCompiler;
    private final InfoByFqn infoByFqn = new InfoByFqn();
    private final List<String> preload = new ArrayList<>();
    // the most recent scan's units, retained so its still-live javac task can resolve+load a compiled type by
    // FQN on demand (the CompiledTypesManager's lazy getOrLoad path). Single-threaded, like all javac use here.
    private ScanCompilationUnits lastScanUnits;
    // Each JavacTask's StandardJavaFileManager, kept OPEN for as long as its task can be driven (parse/analyze
    // after createTask returns, lazy getOrLoad long after). Closing it earlier is use-after-close: javac mostly
    // self-heals (closed containers are lazily re-created) but intermittently corrupts mid-read — the historical
    // low-count "tree.starImportScope is null" flakes that no concurrency fix could cure. Closed in
    // invalidateAllSources(), when every retained task is dropped.
    private final List<StandardJavaFileManager> openFileManagers = new ArrayList<>();
    private boolean parameterNames;
    private ParameterNameIndex parameterNameIndex; // lazily loaded when parameterNames is on
    private boolean jdkInternals; // "we're working with JDK internals": load jdk.internal.* types + open javac

    // the JDK modules for which a faithful parameter-name index is shipped in maddi-aapi-archive
    private static final List<String> PARAMETER_NAME_MODULES = List.of("java.base", "java.desktop", "java.net.http");
    private static final String PARAMETER_NAME_RESOURCE_PREFIX =
            "/org/e2immu/analyzer/aapi/archive/parameterNames/";

    public JavaInspectorImpl() {
        this(false, false);
    }

    public JavaInspectorImpl(boolean computeFingerPrints, boolean allowCreationOfStubTypes) {
        this.computeFingerPrints = computeFingerPrints;
        this.allowCreationOfStubTypes = allowCreationOfStubTypes;
        javaCompiler = ToolProvider.getSystemJavaCompiler();
    }

    public static final String JAR_WITH_PATH_PREFIX = "jar-on-classpath:";
    public static final String E2IMMU_SUPPORT = JAR_WITH_PATH_PREFIX + "org/e2immu/annotation";
    // how reloadSources' in-memory sources are keyed, as in the in-house inspector: "test-protocol:a.b.X"
    public static final String TEST_PROTOCOL_PREFIX = TEST_PROTOCOL + ":";
    public static final ParseOptions FAIL_FAST = new ParseOptions.Builder().setFailFast(true).build();
    public static final ParseOptions DETAILED_SOURCES = new ParseOptions.Builder().setDetailedSources(true).build();

    @Override
    public void invalidateAllSources() {
        infoByFqn.removeAllSources();
        // all retained javac tasks are now unreachable through this inspector; their file managers can close
        for (StandardJavaFileManager fm : openFileManagers) {
            try {
                fm.close();
            } catch (IOException e) {
                LOGGER.debug("Ignoring exception closing a javac file manager: {}", e.toString());
            }
        }
        openFileManagers.clear();
        lastScanUnits = null;
        // the lazy getOrLoad path can no longer serve compiled-type misses; tell the CTM to surface them
        // (log/throw) instead of silently returning null. Re-armed by the next scan (see singleSourceSet).
        if (compiledTypesManager instanceof CompiledTypesManagerImpl ctm) ctm.setLazyLoaderDisabled(true);
    }

    @Override
    public String print2(CompilationUnit compilationUnit, Qualification qualification, ImportComputer importComputer) {
        OutputBuilder ob = runtime.newCompilationUnitPrinter(compilationUnit, true)
                .print(importComputer, qualification);
        Formatter formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        return formatter.write(ob);
    }

    @Override
    public SourceSet javaBase() {
        return inputConfiguration.javaBase();
    }

    @Override
    public SourceSet mainSources() {
        return inputConfiguration.sourceSets().stream().filter(set -> !set.test()).findFirst().orElse(null);
    }

    @Override
    public ParseOptions failFast() {
        return FAIL_FAST;
    }

    @Override
    public void setParameterNames(boolean parameterNames) {
        this.parameterNames = parameterNames;
    }

    @Override
    public void setJdkInternals(boolean jdkInternals) {
        this.jdkInternals = jdkInternals;
    }

    // lazily load and merge the per-module .paramnames.gz indices shipped in maddi-aapi-archive
    private ParameterNameIndex parameterNameIndex() {
        if (parameterNameIndex == null) {
            ParameterNameIndex index = new ParameterNameIndex();
            for (String module : PARAMETER_NAME_MODULES) {
                String resource = PARAMETER_NAME_RESOURCE_PREFIX + module + ".paramnames.gz";
                try (InputStream in = JavaInspectorImpl.class.getResourceAsStream(resource)) {
                    if (in == null) {
                        LOGGER.warn("No parameter-name index resource {} (is maddi-aapi-archive on the classpath?)", resource);
                        continue;
                    }
                    try (Reader r = new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8)) {
                        index.putAll(ParameterNameIndex.read(r));
                    }
                } catch (IOException e) {
                    LOGGER.warn("Cannot read parameter-name index {}: {}", resource, e.toString());
                }
            }
            LOGGER.info("Loaded faithful parameter-name index: {} methods", index.size());
            parameterNameIndex = index;
        }
        return parameterNameIndex;
    }

    // do a preload, with a real recursive load as long as we stay in the package
    // NOTE: module::package java.base::java.util.concurrent
    @Override
    public void preload(String thePackage) {
        preload.add(thePackage);
    }

    /**
     * The shared type registry. Exposed so a mixed-language driver can inject it into the Kotlin front-end
     * (they register into and resolve from the same registry, keyed by (FQN, SourceSet)), letting each
     * front-end reuse the types the other built — see the mixed-language integration doc, Phases 3/4.
     */
    public InfoByFqn infoByFqn() {
        return infoByFqn;
    }

    // load ONE compiled type by FQN on demand, via the most recent scan's still-live javac task; null before
    // any scan has run, or when the type is not on the classpath. Injected as the CompiledTypesManager's
    // lazy-loader so its getOrLoad works for types no scan has touched yet (e.g. requested by the Kotlin front-end).
    private TypeInfo loadCompiledTypeOrNull(String fullyQualifiedName) {
        return lastScanUnits == null ? null : lastScanUnits.loadCompiledTypeOrNull(fullyQualifiedName);
    }

    @Override
    public List<InitializationProblem> initialize(InputConfiguration inputConfiguration) throws IOException {
        this.inputConfiguration = inputConfiguration;
        CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(inputConfiguration.javaBase(), infoByFqn);
        ctm.setLazyLoader(this::loadCompiledTypeOrNull); // on-demand bytecode load for getOrLoad misses
        compiledTypesManager = ctm;
        runtime = new RuntimeWithCompiledTypesManager(ctm);
        javaBase().computePriorityDependencies();
        return List.of();
    }

    @Override
    public void onlyPreload() {
        // a throwaway compilation unit whose sole purpose is to trigger the configured preloads. Its package is
        // kept consistent with (and unique to) its key, so the warmup type never collides with a type a test
        // later parses — in particular a default-package 'X' (the old "a.b.X" key with package-less content
        // registered a default-package X, which then clashed with such tests).
        parse(Map.of("e2immu.preload.WarmUp", "package e2immu.preload; public class WarmUp { }"),
                new JavaInspector.ParseOptions.Builder().build());
    }

    // main method, generally called with empty map; only tests use the map
    @Override
    public Summary parse(Map<String, String> sourcesByFqn, ParseOptions parseOptions) {
        Summary summary = new SummaryImpl(parseOptions.failFast());
        List<SourceSet> linearization = computeScanOrder(); // from input configuration
        if (linearization.isEmpty()) {
            LOGGER.warn("No source sets in the input configuration!");
            if (!sourcesByFqn.isEmpty()) {
                LOGGER.warn("Suggestion: add InputConfigurationImpl.TEST_PROTOCOL_SOURCE_SET");
            }
        }
        // Only go incremental when the caller actually asked. ParseOptions.invalidated() is never null -- the Builder
        // defaults it -- so the default is recognised by identity. It matters: onlyPreload() parses a warmup type,
        // which records its source set, and a subsequent full parse would then find it "known and unchanged" and
        // scan nothing at all.
        if (parseOptions.invalidated() == NOT_INVALIDATED) {
            for (SourceSet sourceSet : linearization) {
                scanSourceSet(summary, sourcesByFqn, sourceSet, parseOptions);
            }
        } else {
            reparse(summary, sourcesByFqn, parseOptions, linearization, parseOptions.invalidated());
        }
        return summary;
    }

    private void scanSourceSet(Summary summary,
                               Map<String, String> sourcesByFqn,
                               SourceSet sourceSet,
                               ParseOptions parseOptions) {
        try {
            singleSourceSet(summary, sourcesByFqn, infoByFqn, sourceSet, !parseOptions.failFast(),
                    parseOptions.ignoreModule(), parseOptions.parameterNames() || parameterNames,
                    parseOptions.syntheticListField(), parseOptions.lombok());
        } catch (IOException ioe) {
            // register the failure in the Summary (preserving the cause) instead of dropping it and aborting
            // with a cause-less UnsupportedOperationException; harmonizes with the in-house inspector
            LOGGER.error("Cannot set up/parse source set {}", sourceSet.name(), ioe);
            summary.addParseException(new Summary.ParseException(sourceSet.uri(), sourceSet.name(),
                    "Cannot set up/parse source set: " + ioe.getMessage(), ioe));
        }
    }

    /** What a re-parse does with one source set. The source set is the unit of work; see {@link #reparse}. */
    private enum SourceSetAction {
        RESCAN,  // javac + CST over the whole set: every type in it comes back as a new object
        REWIRE,  // not re-scanned; its types are copied so they point at the new objects they depend on
        KEEP     // untouched: the very same objects
    }

    /**
     * Re-parse against an {@link Invalidated}, rebuilding only what a change reaches. The CST is effectively
     * immutable, so a changed type cannot be patched in place: it, and everything downstream of it, must be rebuilt.
     * Everything upstream stays as it is.
     * <p>
     * <b>The source set is the unit of work</b>, because that is javac's unit: a source set holding any INVALID type
     * is re-scanned in full, so its <em>unchanged</em> types are rebuilt too — this is coarser than the in-house
     * inspector, which re-parses per source file. Source sets that only depend on a re-scanned one are not
     * re-scanned; their types are rewired, which copies them onto the new objects while keeping their compilation
     * units (hence their fingerprints). Untouched source sets are kept as they are.
     * <p>
     * The linearization guarantees that a source set is handled after everything it depends on, so by the time a set
     * is rewired, the objects it must point at already exist.
     */
    private void reparse(Summary summary,
                         Map<String, String> sourcesByFqn,
                         ParseOptions parseOptions,
                         List<SourceSet> linearization,
                         Invalidated invalidated) {
        this.lastRewireInfoMap = null; // reset: a parse with no rewiring exposes no map
        // snapshot: a RESCAN re-records sourceFiles for its own set as it goes
        Map<SourceSet, List<TypeInfo>> typesBySourceSet = typesBySourceSet();
        Set<TypeInfo> toRewire = new LinkedHashSet<>();
        Set<SourceSet> rescanned = new LinkedHashSet<>();

        for (SourceSet sourceSet : linearization) {
            List<TypeInfo> types = typesBySourceSet.getOrDefault(sourceSet, List.of());
            SourceSetAction action = actionFor(types, invalidated);
            LOGGER.info("Re-parse: source set {} -> {} ({} primary type(s))", sourceSet.name(), action, types.size());
            switch (action) {
                case RESCAN -> {
                    types.forEach(compiledTypesManager::invalidate);
                    scanSourceSet(summary, sourcesByFqn, sourceSet, parseOptions);
                    rescanned.add(sourceSet);
                }
                case REWIRE -> {
                    summary.ensureSourceSet(sourceSet); // not scanned, but it is part of the result
                    toRewire.addAll(types);
                }
                case KEEP -> {
                    summary.ensureSourceSet(sourceSet);
                    types.forEach(summary::addType);
                }
            }
        }
        if (toRewire.isEmpty()) return;

        // the types the re-scan just produced. Without them the rewired copies would keep pointing at the objects
        // they replaced -- the very thing REWIRE exists to prevent (see InfoMap).
        Map<SourceSet, List<TypeInfo>> afterRescan = typesBySourceSet();
        Set<TypeInfo> rebuilt = rescanned.stream()
                .flatMap(sourceSet -> afterRescan.getOrDefault(sourceSet, List.of()).stream())
                .collect(Collectors.toUnmodifiableSet());
        InfoMap infoMap = runtime.newInfoMap(toRewire, rebuilt);
        Set<TypeInfo> rewired = infoMap.rewireAll();
        this.lastRewireInfoMap = infoMap; // expose the completed map (read-only view) for an outside-reload carry

        // every type it built, not just the primary ones: subtypes, and the anonymous/local/lambda types phase 3
        // rewires on demand. Registering only the primary types leaves the rest answering with stale objects.
        infoMap.rewiredTypes().forEach(compiledTypesManager::setRewiredType);
        rewired.forEach(summary::addType);
        // sourceFiles must hold the live objects: the next reloadSources reads their compilation unit's fingerprint
        sourceFiles.replaceAll((_, types) -> types.stream()
                .map(ti -> toRewire.contains(ti) ? infoMap.typeInfo(ti) : ti).toList());
        LOGGER.info("Re-parse: rewired {} primary type(s)", rewired.size());
    }

    /**
     * A source set holding a type that changed (or vanished) must be re-scanned: javac cannot rebuild one file of it
     * in isolation. Otherwise, if anything in it must be rewired, the whole set is rewired; if not, it is kept.
     * A source set we have no types for has never been parsed (or is new), so it is scanned.
     */
    private SourceSetAction actionFor(List<TypeInfo> types, Invalidated invalidated) {
        if (types.isEmpty()) return SourceSetAction.RESCAN;
        boolean rewire = false;
        for (TypeInfo typeInfo : types) {
            InvalidationState state = invalidated.apply(typeInfo);
            if (state == INVALID || state == REMOVED) return SourceSetAction.RESCAN;
            if (state == REWIRE) rewire = true;
        }
        return rewire ? SourceSetAction.REWIRE : SourceSetAction.KEEP;
    }

    /** The primary types we last parsed, per source set; from {@link #sourceFiles}. */
    private Map<SourceSet, List<TypeInfo>> typesBySourceSet() {
        Map<SourceSet, List<TypeInfo>> map = new LinkedHashMap<>();
        sourceFiles.forEach((sourceFile, types) ->
                map.computeIfAbsent(sourceFile.sourceSet(), _ -> new ArrayList<>()).addAll(types));
        return map;
    }

    @Override
    public Summary parseMultiSourceSet(Map<SourceSet, Map<String, String>> sourcesByFqnBySourceSet, ParseOptions parseOptions) {
        Summary summary = new SummaryImpl(parseOptions.failFast());
        List<SourceSet> linearization = computeScanOrder(); // from input configuration
        for (SourceSet sourceSet : linearization) {
            try {
                Map<String, String> sourcesByFqn = sourcesByFqnBySourceSet.get(sourceSet);
                singleSourceSet(summary, sourcesByFqn, infoByFqn, sourceSet, !parseOptions.failFast(),
                        parseOptions.ignoreModule(), parseOptions.parameterNames() || parameterNames,
                        parseOptions.syntheticListField(), parseOptions.lombok());
            } catch (IOException ioe) {
                // register the failure in the Summary (preserving the cause) instead of dropping it and aborting
                // with a cause-less UnsupportedOperationException; harmonizes with the in-house inspector
                LOGGER.error("Cannot set up/parse source set {}", sourceSet.name(), ioe);
                summary.addParseException(new Summary.ParseException(sourceSet.uri(), sourceSet.name(),
                        "Cannot set up/parse source set: " + ioe.getMessage(), ioe));
            }
        }
        return summary;
    }

    @Override
    public TypeInfo parse(String input) {
        throw new UnsupportedOperationException("Add fqn!");
    }

    @Override
    public TypeInfo parse(String fqn, String input) {
        return parse(Map.of(fqn, input), failFast()).parseResult().firstType();
    }

    @Override
    public TypeInfo parse(String fqn, String input, ParseOptions parseOptions) {
        return parse(Map.of(fqn, input), parseOptions).parseResult().firstType();
    }

    private List<SourceSet> computeScanOrder() {
        G.Builder<SourceSet> builder = new ImmutableGraph.Builder<>(Long::sum);
        for (SourceSet set : inputConfiguration.sourceSets()) {
            builder.add(set, set.dependencies().stream().filter(d -> !d.externalLibrary()).toList());
        }
        Linearize.Result<SourceSet> lin = Linearize.linearize(builder.build());
        if (!lin.remainingCycles().isEmpty()) {
            throw new UnsupportedOperationException("Cycles in the source set graph");
        }
        return lin.asList(Comparator.comparing(SourceSet::name));
    }

    // single file
    @Override
    public List<TypeInfo> parse(String transformedString,
                                CompilationUnit compilationUnit,
                                ParseResult parseResult,
                                ParseOptions parseOptions) {
        throw new UnsupportedOperationException();
    }

    // single file
    @Override
    public List<TypeInfo> parseReturnAll(String input, String inputName, String sourceSetName, ParseOptions parseOptions) {
        throw new UnsupportedOperationException();
    }

    // single file
    @Override
    public Summary parseSingleFileInSourceSet(URI javaUri, SourceSet sourceSet, ParseOptions parseOptions) {
        try {
            Path javaFile = Path.of(javaUri);
            String name = javaFile.getFileName().toString();
            String className = name.substring(0, name.length() - 5);
            String input = Files.readString(javaFile);
            Summary summary = new SummaryImpl(parseOptions.failFast());
            singleSourceSet(summary, Map.of(className, input), infoByFqn, sourceSet,
                    !parseOptions.failFast(), parseOptions.ignoreModule(),
                    parseOptions.parameterNames() || parameterNames, parseOptions.syntheticListField(),
                    parseOptions.lombok());
            return summary;
        } catch (IOException e) {
            LOGGER.error("Caught exception", e);
            return null;
        }
    }

    private void singleSourceSet(Summary summary,
                                 Map<String, String> sourcesByFqn,
                                 InfoByFqn infoByFqn,
                                 SourceSet sourceSet,
                                 boolean ignoreErrors,
                                 boolean ignoreModule,
                                 boolean parameterNames,
                                 boolean syntheticListField,
                                 boolean lombok) throws IOException {
        MaddiDiagnosticCollector diagnostics = new MaddiDiagnosticCollector(ignoreErrors);
        JavacTask javacTask = createTask(sourceSet, ignoreModule, sourcesByFqn, diagnostics, lombok);
        if (javacTask == null) {
            LOGGER.warn("Have no sources in source set {}", sourceSet.name());
            return;
        }
        // when parameter names are requested, class-file methods get faithful formal parameter names from the
        // shipped index instead of javac's synthetic arg0, arg1, ...
        ParameterNameIndex pni = parameterNames ? parameterNameIndex() : null;
        ScanCompilationUnits scanCompilationUnits = new ScanCompilationUnits(runtime, inputConfiguration,
                javacTask, sourceSet, infoByFqn, true, diagnostics, preload, pni, jdkInternals,
                computeFingerPrints, syntheticListField);
        ScanCompilationUnits.Result scanned;
        try {
            scanned = scanCompilationUnits.scan();
        } catch (RuntimeException re) {
            if (!lombok || !lombokFailure(re)) throw re;
            // The Lombok processor itself crashed inside javac -- typically a corpus pins a lombok version too
            // old for the embedded compiler (langchain4j's 1.18.30 reflects on TypeTag.UNKNOWN, gone in recent
            // JDKs). Degrade to the pre-processor behavior: parse without Lombok; its generated members are then
            // partially re-synthesized by the in-house support, as before the real-processor integration.
            LOGGER.warn("Lombok processor failed for source set {}; retrying without Lombok. Cause: {}",
                    sourceSet.name(), String.valueOf(re.getCause()));
            diagnostics = new MaddiDiagnosticCollector(ignoreErrors);
            javacTask = createTask(sourceSet, ignoreModule, sourcesByFqn, diagnostics, false);
            scanCompilationUnits = new ScanCompilationUnits(runtime, inputConfiguration, javacTask, sourceSet,
                    infoByFqn, true, diagnostics, preload, pni, jdkInternals, computeFingerPrints,
                    syntheticListField);
            scanned = scanCompilationUnits.scan();
        }
        this.lastScanUnits = scanCompilationUnits; // keep the live task for on-demand getOrLoad
        // a live task can serve getOrLoad misses again: undo any earlier drop-time disable (see invalidateAllSources)
        if (compiledTypesManager instanceof CompiledTypesManagerImpl ctm) ctm.setLazyLoaderDisabled(false);

        // copy from scanned into summary
        // register the source set so it appears in ParseResult.sourceSetsByName() (mirrors the congocc inspector)
        summary.ensureSourceSet(sourceSet);
        for (TypeInfo typeInfo : scanned.primaryTypes()) {
            summary.addType(typeInfo);
            assert typeInfo.hasBeenInspected();
        }
        recordSourceFiles(sourceSet, scanned.primaryTypes());
        if (!scanned.modules().isEmpty()) {
            summary.putSourceSetToModuleInfo(sourceSet, scanned.modules().getFirst());
        }
        // Surface compilation units that ScanCompilationUnits had to drop (accumulate mode): an unresolved symbol
        // on the partial classpath is a *warning* (the run proceeds and preps over what parsed); anything else is a
        // genuine *error* (non-zero exit) — but either way we no longer abort the whole run on the first bad file.
        for (ScanCompilationUnits.CompilationUnitFailure f : scanned.failures()) {
            if (f.tolerable()) {
                summary.addParseWarning(new Summary.ParseException(f.uri(), "compilation unit",
                        f.detail(), f.cause(), Message.Severity.WARN));
            } else {
                summary.addParseException(new Summary.ParseException(f.uri(), "compilation unit",
                        f.detail(), f.cause()));
            }
        }
        // Surface javac ERROR diagnostics as Summary *warnings* (not fatal errors): maddi runs javac on a
        // deliberately partial classpath, so unresolved references ("package x.y does not exist", "cannot find
        // symbol") are expected noise, not failures. Previously these were only logged (at INFO) in
        // ScanCompilationUnits and lost to the caller; now they reach the user via printSummaries() without
        // failing the run (genuine syntax errors still fail: the body parser throws, caught upstream).
        for (MaddiDiagnosticCollector.MaddiDiagnostic d : diagnostics.diagnostics()) {
            if (d.diagnosticKind() == MaddiDiagnosticCollector.DiagnosticKind.ERROR) {
                URI uri = d.path() == null ? sourceSet.uri() : new File(d.path()).toURI();
                summary.addParseWarning(new Summary.ParseException(uri,
                        "line " + d.line() + ", col " + d.col(), d.msg(), null, Message.Severity.WARN));
            }
        }

        // copy into CTM
        List<TypeInfo> loaded = Stream.concat(Stream.concat(scanned.primaryTypes().stream(),
                        scanCompilationUnits.classSymbolScanner().typesLoaded().stream()),
                scanned.preloads().stream()).toList();
        LOGGER.info("Committing types of source set {}, {} loaded", sourceSet.name(), loaded.size());
        for (TypeInfo typeInfo : loaded) {
            // TODO completing is a choice, and may be an unnecessary and expensive operation.
            //  offer this choice to the user
            try {
                if (typeInfo.isPrimaryType() && !typeInfo.hasBeenInspected()) {
                    scanCompilationUnits.classSymbolScanner().commitType(typeInfo);
                }
                compiledTypesManager.addTypeInfo(null, typeInfo);
            } catch (RuntimeException | AssertionError e) {
                // committing a type whose references were dropped by fault isolation can fail. fail-fast: rethrow;
                // accumulate: skip the type and record it, so the run still completes over what did commit.
                if (!ignoreErrors) throw e;
                URI uri;
                try {
                    uri = typeInfo.compilationUnit().uri();
                } catch (RuntimeException ignore) {
                    uri = null;
                }
                boolean tolerable = hasCause(e, UnresolvedSymbolException.class);
                String detail = "commit: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                if (tolerable) {
                    summary.addParseWarning(new Summary.ParseException(uri, typeInfo.fullyQualifiedName(),
                            detail, e, Message.Severity.WARN));
                } else {
                    summary.addParseException(new Summary.ParseException(uri, typeInfo.fullyQualifiedName(),
                            detail, e));
                }
            }
        }
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (type.isInstance(c)) return true;
        }
        return false;
    }

    /**
     * Record which primary types a scan produced per source file. {@code reloadSources} needs this: it re-lists the
     * source tree and, for each file it already knows, compares the fingerprint of the types held here against a
     * freshly computed one.
     * <p>
     * Several top-level types can share one compilation unit (see {@code TestCompilationUnitIdentity}), hence the
     * grouping by URI. A re-parse overwrites the entry: {@link SourceFile} hashes on (uri, source set) only, so the
     * key stays stable even though the fingerprint changed.
     */
    private void recordSourceFiles(SourceSet sourceSet, List<TypeInfo> primaryTypes) {
        Map<URI, List<TypeInfo>> byUri = new LinkedHashMap<>();
        for (TypeInfo typeInfo : primaryTypes) {
            byUri.computeIfAbsent(typeInfo.compilationUnit().uri(), _ -> new ArrayList<>()).add(typeInfo);
        }
        byUri.forEach((uri, types) -> {
            TypeInfo first = types.getFirst();
            SourceFile sourceFile = new SourceFile(pathOf(first), uri, sourceSet,
                    first.compilationUnit().fingerPrintOrNull());
            sourceFiles.put(sourceFile, List.copyOf(types));
        });
    }

    /**
     * A source file's path, as {@link SourceFile} wants it: package directories plus file name, never absolute (it
     * asserts that for .java). Derived from the primary type's FQN rather than from the URI, so that in-memory
     * (mem:) and file: sources look the same. Purely descriptive — SourceFile's equality ignores it. When a file
     * holds several top-level types, the first one names it, which need not be the public one; nothing reads it back.
     */
    private static String pathOf(TypeInfo primaryType) {
        return primaryType.fullyQualifiedName().replace('.', '/') + ".java";
    }

    // "we're working with JDK internals": open javac up to the JDK's non-exported packages, replacing the old
    // per-project env var. --release compiles against ct.sym (which OMITS jdk.internal/sun), so we drop it and
    // compile against the running system modules (-XDignore.symbol.file); --add-export every non-exported package
    // of the declared JDK modules to the unnamed module (the runner compiles with ignoreModule); and --limit-modules
    // to the declared ones so a JDK module's own sources (e.g. java.net.http) do not clash with the system module of
    // the same name ("package exists in another module").
    private static List<String> jdkInternalsJavacOptions(SourceSet sourceSet) {
        List<String> options = new ArrayList<>();
        options.add("-XDignore.symbol.file=true");
        List<String> jdkModules = sourceSet.dependencies().stream()
                .filter(SourceSet::partOfJdk).map(SourceSet::name).distinct().sorted().toList();
        if (!jdkModules.isEmpty()) {
            options.add("--limit-modules");
            options.add(String.join(",", jdkModules));
        }
        ModuleFinder systemModules = ModuleFinder.ofSystem();
        for (String modName : jdkModules) {
            systemModules.find(modName).ifPresent(ref -> {
                ModuleDescriptor d = ref.descriptor();
                Set<String> exported = d.exports().stream().filter(e -> !e.isQualified())
                        .map(ModuleDescriptor.Exports::source).collect(Collectors.toSet());
                new TreeSet<>(d.packages()).forEach(pkg -> {
                    if (!exported.contains(pkg)) {
                        options.add("--add-exports");
                        options.add(modName + "/" + pkg + "=ALL-UNNAMED");
                    }
                });
            });
        }
        return options;
    }

    // true when 'jre' is the JDK this analyzer is itself running on. Then --system would merely reload the running
    // platform via the full jimage (which, unlike --release/ct.sym, exposes jdk.internal.* the preload trips on),
    // so the caller uses --release instead.
    private static boolean isRunningJdk(Path jre) {
        Path running = Path.of(System.getProperty("java.home"));
        try {
            return jre.toRealPath().equals(running.toRealPath());
        } catch (IOException e) {
            return jre.toAbsolutePath().normalize().equals(running.toAbsolutePath().normalize());
        }
    }

    private JavacTask createTask(SourceSet sourceSet,
                                 boolean ignoreModule,
                                 Map<String, String> sourcesByFqn,
                                 MaddiDiagnosticCollector diagnostics,
                                 boolean lombok) throws IOException {
        List<File> sources = new ArrayList<>();
        Map<String, String> sourcesByClassName;
        // use in-memory sources when they are supplied (parse(Map,...) and parseSingleFileInSourceSet(...));
        // otherwise read the source set's directories from disk. Previously this was gated on the TEST_PROTOCOL
        // source-set name, which discarded the in-memory content supplied by parseSingleFileInSourceSet callers
        // that use their own source-set name (e.g. TestCloneBenchMethodHistogram).
        if (!sourcesByFqn.isEmpty()) {
            sourcesByClassName = sourcesByFqn;
        } else {
            sourcesByClassName = Map.of();
            // resolve a source set's (possibly relative) directories against the configured working directory, so
            // the analyzer does not depend on the process's current directory (e.g. when run from a Gradle worker)
            Path workingDirectory = inputConfiguration == null ? null : inputConfiguration.workingDirectory();
            for (Path path : sourceSet.sourceDirectories()) {
                Path resolved = workingDirectory == null || path.isAbsolute() ? path : workingDirectory.resolve(path);
                sources.add(resolved.toFile());
            }
        }

        // NOT try-with-resources: the returned JavacTask holds this manager and is driven long after this
        // method returns (parse/analyze in scan(), lazy getOrLoad far later). See openFileManagers.
        StandardJavaFileManager fm = javaCompiler.getStandardFileManager(diagnostics, null, null);
        openFileManagers.add(fm);
        {
            Iterable<? extends JavaFileObject> allCompilationUnits = computeCompilationUnits(sourceSet, ignoreModule,
                    sources, sourcesByClassName, fm);
            boolean hasModuleInfo = false;
            boolean haveSources = false;
            for (JavaFileObject jfo : allCompilationUnits) {
                if (jfo.toUri().getPath().endsWith("module-info.java")) hasModuleInfo = true;
                haveSources = true;
            }
            if (!haveSources) return null;

            List<File> jarsAndClassDirectories = new ArrayList<>();
            List<File> moduleJars = new ArrayList<>();

            for (SourceSet classPathPart : sourceSet.dependencies()) {
                // ignore jmod:, ignore jar-on-classpath: they are handled by the ClassSymbolScanner
                if (classPathPart.externalLibrary()
                    && !classPathPart.name().startsWith(JAR_WITH_PATH_PREFIX) && !classPathPart.partOfJdk()) {
                    try {
                        File file = Path.of(classPathPart.uri()).toFile();
                        if (ignoreModule || !classPathPart.isModule()) {
                            jarsAndClassDirectories.add(file);
                        } else {
                            moduleJars.add(file);
                        }
                    } catch (IllegalArgumentException iae) {
                        throw new IOException("Cannot parse classpath part " + classPathPart);
                    }
                }
            }
            for (SourceSet dependency : sourceSet.dependencies()) {
                if (!dependency.externalLibrary()) {
                    // A source-set dependency (e.g. test -> main): its types are parsed from source in this same
                    // run, so they are already in the CompiledTypesManager. Only add it as a compiled classpath
                    // entry when its URI is a real (hierarchical file) path -- a relative/opaque URI such as
                    // file:src/main/java (as the mvn plugin emits) is not a compiled output and Path.of would throw.
                    URI uri = dependency.uri();
                    if (uri.isOpaque() || !"file".equals(uri.getScheme())) continue;
                    File file = Path.of(uri).toFile();
                    if (ignoreModule || !dependency.isModule()) {
                        jarsAndClassDirectories.add(file);
                    } else {
                        moduleJars.add(file);
                    }
                }
            }
            if (!jarsAndClassDirectories.isEmpty()) {
                fm.setLocation(StandardLocation.CLASS_PATH, jarsAndClassDirectories);
            }
            if (!moduleJars.isEmpty()) {
                fm.setLocation(StandardLocation.MODULE_PATH, moduleJars);
            }
            // When the compilation is restricted to a subset of packages (see accept()), only the accepted
            // files are passed as compilation units and scanned into the CST. Put the source roots on the
            // source path so javac can still resolve references into the excluded source packages by
            // parsing them on demand. (No effect for the in-memory test protocol, which has no source dirs.)
            boolean restricting = sourceSet.restrictToPackages() != null && !sourceSet.restrictToPackages().isEmpty();
            if (restricting && !sources.isEmpty()) {
                fm.setLocation(StandardLocation.SOURCE_PATH, sources);
            }
            if (!ignoreModule && hasModuleInfo && moduleJars.isEmpty()) {
                LOGGER.warn("The source set {} declares a module but no module path was provided.", sourceSet.name());
            }
            // -parameters makes javac's ClassReader keep formal parameter names read from the MethodParameters
            // attribute (and the LocalVariableTable) of class files on the class/module path; without it
            // Symbol.MethodSymbol.getParameters() yields synthetic arg0, arg1, ...
            // -XDuseUnsharedTable=true: give each compilation its OWN javac name table instead of pulling from
            // javac's process-wide SharedNameTable freelist. That freelist is shared static state across all
            // JavacTask/Context instances in a JVM; under repeated parsing (e.g. hundreds of
            // parseSingleFileInSourceSet calls) it intermittently corrupts and surfaces as
            // "tree.starImportScope is null" during task.analyze(). maddi keys its CST by FQN strings, not javac
            // Names, so not sharing names across compilations is safe here.
            List<String> options = new ArrayList<>(List.of("-parameters", "-XDuseUnsharedTable=true"));
            // The lombok flag is configuration-global (InputConfiguration.containsLombok()), but the processor can
            // only run for a source set that actually has the lombok jar among its own dependencies: javac discovers
            // -processor classes on this task's class path, and requesting a processor that is not there is a hard
            // error that aborts ENTER (seen on timefold-solver, where lombok sits in a single module's test deps).
            boolean lombokOnClassPath = lombok && sourceSet.dependencies().stream()
                    .anyMatch(d -> d.externalLibrary() && d.name().startsWith("lombok-"));
            if (lombokOnClassPath) {
                // Run the real Lombok annotation processor inside javac: it mutates the AST (generating getters,
                // setters, constructors, @Builder, loggers, ...) and the scanner then reads those members into the
                // CST like hand-written code -- full fidelity, unlike the in-house parser's partial re-synthesis.
                options.add("-processor");
                options.add("lombok.launch.AnnotationProcessorHider$AnnotationProcessor");
            } else {
                // No Lombok on the classpath: disable all annotation processing (faster, avoids surprises).
                options.add("-proc:none");
            }
            // Platform (java.*) types come from the JDK running the analyzer by default: --release is derived from
            // the runtime feature version (Runtime.version().feature()), so a new JDK (27, ...) needs no code change
            // here, and --enable-preview stays valid (it requires --release to equal the running version). An
            // alternative JRE (InputConfiguration.alternativeJREDirectory / --jre) that is a DIFFERENT JDK is loaded
            // with --system, so types removed in a newer JDK (e.g. java.applet.Applet, gone in JDK 26) stay
            // resolvable. But when the alternative JRE IS the JDK we run on -- the IDE daemon runs on its configured
            // sdkHome and passes it as the alternative JRE -- --system is redundant AND harmful: it serves the full
            // runtime image, which (unlike --release/ct.sym) surfaces jdk.internal.* types the JDK preload cannot
            // handle ("Type nature of jdk.internal.vm.ThreadContainer has not been set"). So fall back to --release
            // when the alternative JRE resolves to the running JDK.
            Path altJre = inputConfiguration == null ? null : inputConfiguration.alternativeJREDirectory();
            if (altJre != null && isRunningJdk(altJre)) altJre = null;
            if (jdkInternals) {
                if (altJre != null) {
                    LOGGER.warn("Ignoring alternative JRE {} while compiling {} against JDK internals: internals are" +
                                " opened on the running JDK.", altJre, sourceSet.name());
                }
                options.addAll(jdkInternalsJavacOptions(sourceSet));
            } else if (altJre != null) {
                options.add("--system");
                options.add(altJre.toString());
            } else {
                options.add("--enable-preview");
                // java.lang.Runtime: the maddi CST 'Runtime' is imported in this file and would shadow it
                options.add("--release=" + java.lang.Runtime.version().feature());
            }
            return (JavacTask) javaCompiler.getTask(null, fm, diagnostics, options, null, allCompilationUnits);
        }
    }

    // does the cause chain point into Lombok's own code? (processor init/handler crash, not a source problem)
    private static boolean lombokFailure(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            for (StackTraceElement ste : c.getStackTrace()) {
                if (ste.getClassName().startsWith("lombok.")) return true;
            }
        }
        return false;
    }

    private static @NotNull Iterable<? extends JavaFileObject> computeCompilationUnits
            (SourceSet sourceSet,
             boolean ignoreModule,
             List<File> sources,
             Map<String, String> sourcesByClassName, StandardJavaFileManager fm) throws IOException {
        List<File> allSources = new LinkedList<>();
        for (File sourceDir : sources) {
            if (!Files.isDirectory(sourceDir.toPath())) {
                // a configured source root that doesn't exist on disk (e.g. a build tool emits a default
                // test-source dir that the project never created); treat as empty rather than aborting the scan
                LOGGER.warn("Skipping source directory {}: does not exist", sourceDir);
                continue;
            }
            try (Stream<Path> walk = Files.walk(sourceDir.toPath())) {
                walk.filter(p -> p.toString().endsWith(".java"))
                        .sorted()
                        .map(Path::toFile)
                        .filter(f -> !ignoreModule || !"module-info.java".equals(f.getName()))
                        .forEach(allSources::add);
            }
        }
        // Wrap each source string in an InMemoryJavaFileObject
        List<JavaFileObject> inMemory = sourcesByClassName.entrySet().stream()
                .map(e -> new InMemoryJavaFileObject(sourceSet.name(), e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        Iterable<? extends JavaFileObject> compilationUnits = fm.getJavaFileObjects(allSources.toArray(new File[0]));
        return Stream.concat(StreamSupport.stream(compilationUnits.spliterator(), false),
                        inMemory.stream())
                .filter(jfo -> accept(sourceSet, jfo))
                .toList();
    }

    /*
    Decide, before javac parses anything, whether a source file should be part of the compilation, given
    this source set's package restriction (SourceSet.restrictToPackages()). Delegates the package-matching
    semantics to the single source of truth, SourceSet.acceptSource(packageName, typeName).

    Note: code is pretty slow but not expected to be used in large set-ups.
     */
    private static boolean accept(SourceSet sourceSet, JavaFileObject jfo) {
        return accept(sourceSet, jfo.toUri());
    }

    private static boolean accept(SourceSet sourceSet, URI uri) {
        Set<String> restrict = sourceSet.restrictToPackages();
        if (restrict == null || restrict.isEmpty()) return true;
        String fqn = inferFullyQualifiedName(sourceSet, uri);
        if (fqn == null) {
            LOGGER.warn("Cannot infer package of {}; keeping it despite the package restriction", uri);
            return true;
        }
        int lastDot = fqn.lastIndexOf('.');
        String packageName = lastDot < 0 ? "" : fqn.substring(0, lastDot);
        String typeName = lastDot < 0 ? fqn : fqn.substring(lastDot + 1);
        return sourceSet.acceptSource(packageName, typeName);
    }

    /*
    Infer the primary type's fully qualified name from a source file object, before it is parsed.
    In-memory sources (test protocol) encode it in their URI as mem:///<sourceSet>/<a/b/C>.java; file
    sources encode it as the file path below one of the source directories. Returns null when it cannot
    be determined.
     */
    private static String inferFullyQualifiedName(SourceSet sourceSet, URI uri) {
        if ("mem".equals(uri.getScheme())) {
            String path = uri.getPath(); // /<sourceSet>/a/b/C.java
            String prefix = "/" + sourceSet.name() + "/";
            if (path == null || !path.startsWith(prefix) || !path.endsWith(".java")) return null;
            return path.substring(prefix.length(), path.length() - ".java".length()).replace('/', '.');
        }
        if (!"file".equals(uri.getScheme())) return null;
        Path file = Path.of(uri).toAbsolutePath().normalize();
        if (!file.getFileName().toString().endsWith(".java")) return null;
        for (Path dir : sourceSet.sourceDirectories()) {
            Path abs = dir.toAbsolutePath().normalize();
            if (file.startsWith(abs)) {
                Path rel = abs.relativize(file);
                StringBuilder fqn = new StringBuilder();
                for (int i = 0; i < rel.getNameCount(); i++) {
                    String segment = rel.getName(i).toString();
                    if (i == rel.getNameCount() - 1) {
                        segment = segment.substring(0, segment.length() - ".java".length());
                    }
                    if (!fqn.isEmpty()) fqn.append('.');
                    fqn.append(segment);
                }
                return fqn.toString();
            }
        }
        return null;
    }

    @Override
    public Runtime runtime() {
        return runtime;
    }

    @Override
    public CompiledTypesManager compiledTypesManager() {
        return compiledTypesManager;
    }

    @Override
    public Set<SourceFile> sourceFiles() {
        return sourceFiles.keySet();
    }

    @Override
    public org.e2immu.language.cst.api.info.InfoMapView lastRewireInfoMap() {
        return lastRewireInfoMap;
    }

    /*
    Strategy (the same as the in-house inspector's): re-list the source tree and compare each file's fingerprint
    against the one held by the types we built from it last time.
    - new files: add to sourceFiles with no types; nothing to report, the code compiles.
    - removed files: drop from sourceFiles; nothing to report either.
    - changed files: report their types, so the caller's Invalidated can return INVALID for them and compute the
      dependents that need rewiring (see RunRewireTests).
    Nothing is invalidated or re-parsed here: this only answers "what changed?".
     */
    @Override
    public ReloadResult reloadSources(InputConfiguration inputConfiguration,
                                      Map<String, String> sourcesByTestProtocolURIString) throws IOException {
        if (!computeFingerPrints) {
            throw new UnsupportedOperationException("The reloadSources method requires fingerprints to be computed");
        }
        List<InitializationProblem> problems = new ArrayList<>();
        Set<TypeInfo> changed = new HashSet<>();
        Set<SourceFile> removed = new HashSet<>(this.sourceFiles.keySet());
        int newSourceFiles = 0;
        int changedSourceFiles = 0;

        List<SourceFile> current = listSourceFiles(inputConfiguration, sourcesByTestProtocolURIString, problems);
        for (SourceFile sourceFile : current) {
            List<TypeInfo> types = this.sourceFiles.get(sourceFile);
            if (types == null) {
                this.sourceFiles.put(sourceFile, List.of()); // NEW
                ++newSourceFiles;
                continue;
            }
            removed.remove(sourceFile);
            if (types.isEmpty()) continue; // known, but nothing was parsed from it
            FingerPrint currentFingerPrint = types.getFirst().compilationUnit().fingerPrintOrNull();
            String sourceCode = loadSource(sourceFile, sourcesByTestProtocolURIString, problems);
            FingerPrint newFingerPrint = sourceCode == null
                    ? MD5FingerPrint.NO_FINGERPRINT : MD5FingerPrint.compute(sourceCode);
            // a missing 'current' fingerprint means the file was parsed without them; treat as changed rather than
            // silently keeping a type we cannot vouch for
            if (currentFingerPrint == null || !currentFingerPrint.equals(newFingerPrint)) {
                changed.addAll(types); // CHANGE
                ++changedSourceFiles;
            } // else: UNCHANGED
        }
        this.sourceFiles.keySet().removeAll(removed);
        LOGGER.info("Reloaded sources: {} source file(s) removed, {} new, {} of {} remaining changed",
                removed.size(), newSourceFiles, changedSourceFiles, current.size());
        return new ReloadResult(List.copyOf(problems), Set.copyOf(changed));
    }

    /**
     * The source files as they are on disk (or in memory) right now, independent of what was parsed before.
     * <p>
     * Mirrors {@code createTask}'s either/or: when in-memory sources are supplied they are the whole source tree and
     * no directory is walked; otherwise every source set's directories are walked, resolved against the working
     * directory. Both apply the source set's package restriction, exactly as {@code accept} does for the compilation
     * units themselves, so a file javac never sees cannot show up as new here.
     */
    private List<SourceFile> listSourceFiles(InputConfiguration inputConfiguration,
                                             Map<String, String> sourcesByTestProtocolURIString,
                                             List<InitializationProblem> problems) {
        List<SourceFile> result = new ArrayList<>();
        for (SourceSet sourceSet : inputConfiguration.sourceSets()) {
            if (!sourcesByTestProtocolURIString.isEmpty()) {
                for (String key : sourcesByTestProtocolURIString.keySet()) {
                    String fqn = key.startsWith(TEST_PROTOCOL_PREFIX) ? key.substring(TEST_PROTOCOL_PREFIX.length())
                            : key;
                    URI uri = inMemoryUri(sourceSet, fqn);
                    if (accept(sourceSet, uri)) {
                        result.add(new SourceFile(fqn.replace('.', '/') + ".java", uri, sourceSet, null));
                    }
                }
            } else {
                for (Path dir : sourceSet.sourceDirectories()) {
                    Path resolved = inputConfiguration.workingDirectory() == null || dir.isAbsolute()
                            ? dir : inputConfiguration.workingDirectory().resolve(dir);
                    if (!Files.isDirectory(resolved)) continue;
                    try (Stream<Path> walk = Files.walk(resolved)) {
                        walk.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> {
                            URI uri = p.toUri();
                            if (accept(sourceSet, uri)) {
                                String fqn = inferFullyQualifiedName(sourceSet, uri);
                                String path = fqn == null ? p.getFileName().toString()
                                        : fqn.replace('.', '/') + ".java";
                                result.add(new SourceFile(path, uri, sourceSet, null));
                            }
                        });
                    } catch (IOException ioe) {
                        LOGGER.error("Cannot walk source directory {}", resolved, ioe);
                        problems.add(new InitializationProblem("Cannot walk source directory " + resolved, ioe));
                    }
                }
            }
        }
        return result;
    }

    /** The URI {@link InMemoryJavaFileObject} gives an in-memory source, so the keys match what a scan recorded. */
    private static URI inMemoryUri(SourceSet sourceSet, String fqn) {
        return URI.create("mem:///" + sourceSet.name() + "/" + fqn.replace('.', '/') + ".java");
    }

    /** The current text of a source file: from the supplied map for in-memory sources, from disk otherwise. */
    private String loadSource(SourceFile sourceFile,
                              Map<String, String> sourcesByTestProtocolURIString,
                              List<InitializationProblem> problems) {
        URI uri = sourceFile.uri();
        if ("mem".equals(uri.getScheme())) {
            String fqn = inferFullyQualifiedName(sourceFile.sourceSet(), uri);
            return fqn == null ? null : sourcesByTestProtocolURIString.get(TEST_PROTOCOL_PREFIX + fqn);
        }
        try {
            return Files.readString(Path.of(uri), sourceFile.sourceSet().sourceEncoding());
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Cannot read source file {}", uri, e);
            problems.add(new InitializationProblem("Cannot read source file " + uri, e));
            return null;
        }
    }

    @Override
    public boolean isOpenJdk() {
        return true;
    }
}
