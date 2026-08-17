package io.codelaser.maddi.inspection.openjdk;

import com.sun.source.util.JavacTask;
import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.element.FingerPrint;
import io.codelaser.maddi.cst.api.element.ModuleInfo;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.impl.parser.ContextImpl;
import io.codelaser.maddi.inspection.impl.parser.ResolverImpl;
import io.codelaser.maddi.inspection.impl.parser.TypeContextImpl;
import io.codelaser.maddi.parser.java.ParseHelperImpl;
import io.codelaser.maddi.parser.java.ParseModuleInfo;
import org.parsers.java.JavaParser;
import org.parsers.java.Node;
import org.parsers.java.ast.ModularCompilationUnit;
import io.codelaser.maddi.cst.api.info.ImportComputer;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Formatter;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.cst.api.analysis.Message;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.api.resource.MD5FingerPrint;
import io.codelaser.maddi.inspection.api.resource.ParameterNameIndex;
import io.codelaser.maddi.inspection.api.resource.SourceFile;
import io.codelaser.maddi.inspection.resource.InfoByFqn;
import io.codelaser.maddi.inspection.resource.ResolveModuleDirectives;
import io.codelaser.maddi.inspection.resource.SummaryImpl;
import io.codelaser.maddi.java.openjdk.ClassSymbolScanner;
import io.codelaser.maddi.java.openjdk.InMemoryJavaFileObject;
import io.codelaser.maddi.java.openjdk.MaddiDiagnosticCollector;
import io.codelaser.maddi.java.openjdk.ScanCompilationUnits;
import io.codelaser.maddi.java.openjdk.UnresolvedSymbolException;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.ImmutableGraph;
import io.codelaser.maddi.graph.op.Linearize;
import io.codelaser.maddi.graph.util.TimedLogger;
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

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.InvalidationState.*;

public class JavaInspectorImpl implements JavaInspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaInspectorImpl.class);
    private static final TimedLogger TIMED_LOGGER = new TimedLogger(LOGGER, 1000L);

    private Runtime runtime;
    // which primary types came out of which source file, keyed by (uri, source set) -- SourceFile's equality ignores
    // path and fingerprint. Filled after every scan; this is the map reloadSources diffs the source tree against.
    private final Map<SourceFile, List<TypeInfo>> sourceFiles = new HashMap<>();
    private CompiledTypesManager compiledTypesManager;
    private InputConfiguration inputConfiguration; // kept for tests
    private io.codelaser.maddi.cst.api.info.InfoMapView lastRewireInfoMap; // the last re-parse's rewire, read-only
    private final boolean computeFingerPrints;
    private final boolean allowCreationOfStubTypes;
    private final JavaCompiler javaCompiler;
    private final InfoByFqn infoByFqn = new InfoByFqn();
    private final List<String> preload = new ArrayList<>();
    // the most recent scan's units, retained so its still-live javac task can resolve+load a compiled type by
    // FQN on demand (the CompiledTypesManager's lazy getOrLoad path). Single-threaded, like all javac use here.
    private ScanCompilationUnits lastScanUnits;
    // ... unless generation destroyed that task: JavacTask.generate() tears the compiler context down, so the
    // retained scan can no longer answer getElements(). Then compiled-type loading moves to loaderUnits below.
    private boolean lastScanUnitsGenerated;
    // A javac task with NO compilation units, built only to resolve+complete class-path symbols: the replacement
    // for a scan task that generation destroyed. It is never parsed, analysed or generated -- a zero-unit task
    // answers getElements().getTypeElement(fqn) fine, but parse()/analyze() on one fails with "no source files".
    // Created lazily on the first load that needs it (serialized by CompiledTypesManagerImpl.getOrLoad's monitor).
    private ScanCompilationUnits loaderUnits;
    // what loaderUnits must be rebuilt from, captured when the scan whose task we destroyed still knew its flags
    private LoaderSpec loaderSpec;
    // Every source set this inspector has scanned, and what a source-free loader task on it must be built from.
    // Recorded for ALL of them, not just the last: a compiled type has to be resolved against the class path of the
    // source set that ASKED for it, and only the requesting set's own task carries that class path.
    private final Map<SourceSet, LoaderSpec> loaderSpecBySourceSet = new LinkedHashMap<>();
    // per-source-set loader tasks, built on demand from loaderSpecBySourceSet and cached for the run. A source-free
    // task holds a file manager and a class path, no AST, so this stays cheap next to a retained scan.
    private final Map<SourceSet, ScanCompilationUnits> loaderUnitsBySourceSet = new LinkedHashMap<>();
    // Off: a request whose own source set cannot resolve the type falls back to the historical behaviour (the last
    // scan's task, then the single replacement), so nothing that resolves today stops resolving. On: the requesting
    // source set's class path is the only answer, and a type outside it is a miss. Turning this on is the follow-up
    // audit -- ~8 production call sites pass javaInspector.mainSources(), which is an arbitrary pick, not the set
    // that is actually asking.
    private boolean strictSourceSetLoading;
    // census for the strict-mode audit; see recordFallBack. Concurrent: getOrLoad runs on parallel analyzer threads.
    private final Set<String> fallBackResolutions = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * How to stand up a {@link #loaderUnits} equivalent to the scan task that generation destroyed: the same source
     * set (hence the same class path) and the same flags, so a type loaded through the replacement is built exactly
     * as the scan would have built it.
     */
    private record LoaderSpec(SourceSet sourceSet, boolean ignoreModule, boolean parameterNames,
                              boolean syntheticListField) {
    }
    // Each JavacTask's StandardJavaFileManager, kept OPEN for as long as its task can be driven (parse/analyze
    // after createTask returns, lazy getOrLoad long after). Closing it earlier is use-after-close: javac mostly
    // self-heals (closed containers are lazily re-created) but intermittently corrupts mid-read — the historical
    // low-count "tree.starImportScope is null" flakes that no concurrency fix could cure. Closed in
    // invalidateAllSources(), when every retained task is dropped.
    private final List<StandardJavaFileManager> openFileManagers = new ArrayList<>();
    private boolean parameterNames;
    private ParameterNameIndex parameterNameIndex; // lazily loaded when parameterNames is on
    private boolean jdkInternals; // "we're working with JDK internals": load jdk.internal.* types + open javac
    // where WE compile the source sets to, so that a dependent source set resolves against the code this inspector
    // actually read instead of against whatever the build last left behind. null = off; see
    // JavaInspector.setGeneratedClassesDirectory.
    private Path generatedClassesDirectory;
    // source set NAME -> the directory we generated its class files into, for the sets where generation produced
    // something. Keyed by name because SourceSet equality is by name and the objects are rebuilt across re-parses.
    // Survives a re-parse on purpose: a set that is not re-scanned keeps the class files its unchanged sources
    // compiled to, and the linearization guarantees a re-scanned set regenerates before any dependent re-scans.
    private final Map<String, Path> generatedClassOutput = new HashMap<>();

    // the JDK modules for which a faithful parameter-name index is shipped in maddi-aapi-archive
    private static final List<String> PARAMETER_NAME_MODULES = List.of("java.base", "java.desktop", "java.net.http");
    private static final String PARAMETER_NAME_RESOURCE_PREFIX =
            "/io/codelaser/maddi/aapi/archive/parameterNames/";

    public JavaInspectorImpl() {
        this(false, false);
    }

    public JavaInspectorImpl(boolean computeFingerPrints, boolean allowCreationOfStubTypes) {
        this.computeFingerPrints = computeFingerPrints;
        this.allowCreationOfStubTypes = allowCreationOfStubTypes;
        javaCompiler = ToolProvider.getSystemJavaCompiler();
    }

    /** @see InputConfiguration#JAR_ON_CLASSPATH_PREFIX — kept as the name this front end has always used. */
    public static final String JAR_WITH_PATH_PREFIX = InputConfiguration.JAR_ON_CLASSPATH_PREFIX;
    public static final String E2IMMU_SUPPORT = JAR_WITH_PATH_PREFIX + "io/codelaser/maddi/annotation";
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
        lastScanUnitsGenerated = false;
        loaderUnits = null; // its file manager was in openFileManagers and has just been closed
        loaderSpec = null;
        loaderUnitsBySourceSet.clear(); // same: every one of those file managers has just been closed
        loaderSpecBySourceSet.clear();
        // the lazy getOrLoad path can no longer serve compiled-type misses; tell the CTM to surface them
        // (log/throw) instead of silently returning null. Re-armed by the next scan (see singleSourceSet).
        if (compiledTypesManager instanceof CompiledTypesManagerImpl ctm) ctm.setLazyLoaderDisabled(true);
    }

    @Override
    public String print2(CompilationUnit compilationUnit, Qualification qualification, ImportComputer importComputer,
                         FormattingOptions formattingOptions) {
        OutputBuilder ob = runtime.newCompilationUnitPrinter(compilationUnit, true)
                .print(importComputer, qualification);
        FormattingOptions options = formattingOptions == null
                ? new FormattingOptionsImpl.Builder().build() : formattingOptions;
        Formatter formatter = new Formatter2Impl(runtime, options);
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

    @Override
    public void setGeneratedClassesDirectory(Path directory) {
        this.generatedClassesDirectory = directory;
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

    /**
     * Load ONE compiled type by FQN on demand, via a live javac task; null before any scan has run, or when the type
     * is not on the class path. Injected as the CompiledTypesManager's lazy loader, so its {@code getOrLoad} works
     * for types no scan has touched yet (e.g. requested by the Kotlin front-end).
     * <p>
     * ⛔ <b>A class path belongs to a source set.</b> {@code sourceSetOfRequest} is the set that asked, and its own
     * task is the only one that resolves names the way that set would. Ask it first. Historically this method took
     * only the FQN and always used {@link #unitsForCompiledTypeLoading()} — whichever set was scanned LAST — so in a
     * multi-source-set configuration the answer depended on scan order: a class-path preload of a package, followed
     * by the scan of a corpus source set without that jar, made every nested type of the preloaded package
     * unresolvable (jfocus "Cannot find …Loop.LoopData", fixed on the preload side in maddi-java-openjdk).
     * <p>
     * The fall-back to the historical path is deliberate and keeps this change additive: a caller that passes a
     * source set which cannot see the type (today, anything passing {@code mainSources()}) still gets the answer it
     * got before. {@link #setStrictSourceSetLoading} removes the fall-back; see the field's comment.
     */
    private TypeInfo loadCompiledTypeOrNull(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        ScanCompilationUnits ownUnits = unitsForSourceSet(sourceSetOfRequest);
        if (ownUnits != null) {
            TypeInfo typeInfo = ownUnits.loadCompiledTypeOrNull(fullyQualifiedName);
            if (typeInfo != null) return typeInfo;
        }
        if (strictSourceSetLoading && ownUnits != null) return null;
        ScanCompilationUnits units = unitsForCompiledTypeLoading();
        if (units == null || units == ownUnits) return null;
        TypeInfo viaFallBack = units.loadCompiledTypeOrNull(fullyQualifiedName);
        if (viaFallBack != null && ownUnits != null) recordFallBack(fullyQualifiedName, sourceSetOfRequest, units);
        return viaFallBack;
    }

    /**
     * The census behind the strict-mode audit: a type the requesting source set could NOT resolve, which the
     * fall-back found on another set's class path. Each one is a call site passing a source set that is not the one
     * really asking (in practice {@code mainSources()}), and would become a miss under
     * {@link #setStrictSourceSetLoading}. Logged once per FQN — the point is the distinct set, not the volume.
     */
    private void recordFallBack(String fullyQualifiedName, SourceSet sourceSetOfRequest, ScanCompilationUnits via) {
        if (fallBackResolutions.add(fullyQualifiedName)) {
            LOGGER.warn("SOURCE-SET FALL-BACK: {} is not on {}'s class path; resolved via {}. The caller passed a"
                        + " source set that is not the one asking; strict mode would make this a miss.",
                    fullyQualifiedName, sourceSetOfRequest.name(), via.sourceSet().name());
        }
    }

    /** Distinct FQNs that only the fall-back could resolve; empty means strict mode would cost this run nothing. */
    public Set<String> fallBackResolutions() {
        return Set.copyOf(fallBackResolutions);
    }

    /**
     * When on, a compiled type is resolved against the requesting source set's class path and nothing else — the
     * fall-back in {@link #loadCompiledTypeOrNull} is skipped whenever that set has a task of its own. Off by
     * default: several callers pass a source set that is not really theirs, and would lose types they resolve today.
     */
    public void setStrictSourceSetLoading(boolean strictSourceSetLoading) {
        this.strictSourceSetLoading = strictSourceSetLoading;
    }

    /**
     * A loader task on the source set that is asking, or null when it never was scanned (so we have no class path
     * for it) or when a task cannot be built. The last scan's own task is reused when it is that set's and still
     * intact, so the common single-source-set case builds nothing extra.
     * <p>
     * Called under {@code CompiledTypesManagerImpl.getOrLoad}'s monitor, like {@link #unitsForCompiledTypeLoading}.
     */
    private ScanCompilationUnits unitsForSourceSet(SourceSet sourceSetOfRequest) {
        if (sourceSetOfRequest == null) return null;
        if (lastScanUnits != null && !lastScanUnitsGenerated
            && sourceSetOfRequest.equals(lastScanUnits.sourceSet())) {
            return lastScanUnits;
        }
        ScanCompilationUnits cached = loaderUnitsBySourceSet.get(sourceSetOfRequest);
        if (cached != null) return cached;
        LoaderSpec spec = loaderSpecBySourceSet.get(sourceSetOfRequest);
        if (spec == null) return null; // never scanned: we do not know its class path
        ScanCompilationUnits units = createLoaderUnits(spec);
        if (units != null) loaderUnitsBySourceSet.put(sourceSetOfRequest, units);
        return units;
    }

    /**
     * The javac task that may still be asked to resolve a compiled type: the most recent scan's while it is intact,
     * otherwise a source-free replacement built on demand. Source-set agnostic — the historical behaviour, kept as
     * the fall-back of {@link #loadCompiledTypeOrNull}.
     * <p>
     * Only generation makes the difference. Without it the retained scan task lives until
     * {@link #invalidateAllSources()} and this is exactly the historical path. With it, that task has been torn down
     * by {@code generate()}, and reusing it throws {@code IllegalStateException} from {@code getElements()} — which
     * is precisely the risk {@code docs/partial-reparse-rewire.md} §7.1 flagged, and what a caller experienced as
     * on-demand library loading breaking the moment generation was switched on.
     * <p>
     * Called under {@code CompiledTypesManagerImpl.getOrLoad}'s monitor, which is what makes the lazy build safe
     * from the parallel analyzer threads that drive it.
     */
    private ScanCompilationUnits unitsForCompiledTypeLoading() {
        if (lastScanUnits != null && !lastScanUnitsGenerated) return lastScanUnits;
        if (loaderUnits != null) return loaderUnits;
        if (loaderSpec == null) return null; // nothing was ever scanned, or everything was invalidated
        loaderUnits = createLoaderUnits(loaderSpec);
        return loaderUnits;
    }

    /**
     * A {@link ScanCompilationUnits} over a task with no compilation units, for compiled-type loading only. Returns
     * {@code null} when it cannot be built, which leaves {@code getOrLoad} answering misses as it does for any type
     * that is not on the class path.
     */
    private ScanCompilationUnits createLoaderUnits(LoaderSpec spec) {
        try {
            // errors are not expected (nothing is compiled) and must not reach the caller's Summary either
            MaddiDiagnosticCollector diagnostics = new MaddiDiagnosticCollector(true);
            JavacTask task = createTask(spec.sourceSet(), spec.ignoreModule(), Map.of(), diagnostics, false,
                    null, true);
            if (task == null) return null;
            ParameterNameIndex pni = spec.parameterNames() || parameterNames ? parameterNameIndex() : null;
            LOGGER.info("Built a source-free javac task for compiled-type loading, on source set {}",
                    spec.sourceSet().name());
            return new ScanCompilationUnits(runtime, inputConfiguration, task, spec.sourceSet(), infoByFqn, true,
                    diagnostics, preload, pni, jdkInternals, computeFingerPrints, spec.syntheticListField());
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot build a compiled-type loader task on source set {}: {}", spec.sourceSet().name(),
                    e.toString());
            return null;
        }
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
        // ⛔⛔ #201: this inspector never resolved a module directive, so apiResolved() was null for EVERY
        // `uses`/`provides` and ComputeCallGraph lost every module→service edge in silence. It runs here, once,
        // after all source sets are scanned: a descriptor may name a type that lives in another source set.
        ResolveModuleDirectives.go(summary, compiledTypesManager);
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
        // Build and expose the map whenever a set was RESCANNED, even with nothing to REWIRE (a single source set,
        // no cross-set dependents). The map seeds each rebuilt (rescanned) type under itself, and Info equality is
        // fqn + source-set, so a caller can resolve an OLD (pre-reparse) rescanned type to its new object by fqn --
        // the basis of the same-source-set analysis carry (docs/analysis-rewiring). Only a no-op reparse (nothing
        // invalidated, nothing rewired) exposes no map.
        if (toRewire.isEmpty() && rescanned.isEmpty()) return;

        // the types the re-scan just produced. Without them the rewired copies would keep pointing at the objects
        // they replaced -- the very thing REWIRE exists to prevent (see InfoMap).
        Map<SourceSet, List<TypeInfo>> afterRescan = typesBySourceSet();
        Set<TypeInfo> rebuilt = rescanned.stream()
                .flatMap(sourceSet -> afterRescan.getOrDefault(sourceSet, List.of()).stream())
                .collect(Collectors.toUnmodifiableSet());
        // newInfoMap seeds the rebuilt types (see InfoMapImpl); rewireAll is a no-op when toRewire is empty, so the
        // seeded rescanned mappings are all this map carries in the rescan-only case.
        InfoMap infoMap = runtime.newInfoMap(toRewire, rebuilt);
        Set<TypeInfo> rewired = infoMap.rewireAll();
        // Handed out only as InfoMapView (read-only lookup facet): the reload consumer can resolve old->new but not
        // put/rewireAll. The map is complete once built (rebuilt seeded in the ctor; rewireAll fills the rewire
        // submaps and is not called again), and it is replaced wholesale by the next reparse -- so its lookups are
        // pure and stable for its lifetime, mapping onto the live CST objects the carry writes analysis onto.
        this.lastRewireInfoMap = infoMap;

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
        ResolveModuleDirectives.go(summary, compiledTypesManager);   // #201, as in parse()
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

    /**
     * The order in which the source sets are scanned. Edges come from {@link SourceSet#dependencies()}, but only
     * the non-external ones — and a build tool that hands us a multi-module project typically expresses a sibling
     * module as its <em>artifact</em> ({@code timefold-solver-core-999-SNAPSHOT.jar}, an external library part),
     * not as the sibling's source set. Such a graph has NO edges at all, and the whole order is then decided by
     * the tie-breaker.
     * <p>
     * ⛔ Which is why the tie-breaker is the input configuration's own order and not the name. Scanning a source
     * set BEFORE one it depends on is not an error, but it is expensive and lossy: the dependency's types are
     * materialized from its class files, and when its sources are scanned later, {@code InfoByFqn} keeps both
     * ("Create multi") — the analysis then reads a mixture of the two. It is also where the parse breaks. On
     * timefold (2026-08-11) a new module {@code constraint-streams} sorted alphabetically before the {@code core}
     * it depends on, and two of core's compilation units were dropped, both at a nested type whose members the
     * class-file pass had already created: {@code AssertionError: Duplicating FieldInfo …SupplyWithDemandCount
     * .supply} and an {@code UnsupportedOperationException} out of {@code ParameterInfoImpl.builder()}. The three
     * earlier modules peeled off the same corpus ({@code util}, {@code search}, {@code neighborhood}) had all
     * sorted after {@code core} — the order was right by accident, and the accident ran out.
     * <p>
     * A build tool lists its modules in dependency order (Maven's reactor is topologically sorted), so the
     * declared order is exactly the information the artifact-shaped dependencies threw away. It is no less
     * deterministic than sorting by name.
     */
    private List<SourceSet> computeScanOrder() {
        return computeScanOrder(inputConfiguration.sourceSets());
    }

    // package-private, static and taking its input: the order is decided by the source-set list alone, and a test
    // should be able to state one and read the order back without a corpus on disk.
    static List<SourceSet> computeScanOrder(List<SourceSet> sourceSets) {
        G.Builder<SourceSet> builder = new ImmutableGraph.Builder<>(Long::sum);
        Map<SourceSet, Integer> declarationOrder = new HashMap<>();
        int index = 0;
        for (SourceSet set : sourceSets) {
            builder.add(set, set.dependencies().stream().filter(d -> !d.externalLibrary()).toList());
            declarationOrder.put(set, index++);
        }
        Linearize.Result<SourceSet> lin = Linearize.linearize(builder.build());
        if (!lin.remainingCycles().isEmpty()) {
            throw new UnsupportedOperationException("Cycles in the source set graph");
        }
        // a dependency that is not itself a source set can turn up as a node; sort those last, by name
        return lin.asList(Comparator.<SourceSet, Integer>comparing(s ->
                        declarationOrder.getOrDefault(s, Integer.MAX_VALUE))
                .thenComparing(SourceSet::name));
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
        // must precede createTask: it is what the task's CLASS_OUTPUT is pointed at. null = we generate nothing for
        // this scan, and then we never set CLASS_OUTPUT and never call generate(), so javac cannot write class files
        // next to the sources it is reading.
        Path classOutput = sourcesByFqn.isEmpty() ? prepareGeneratedClassOutput(sourceSet) : null;
        MaddiDiagnosticCollector diagnostics = new MaddiDiagnosticCollector(ignoreErrors);
        JavacTask javacTask = createTask(sourceSet, ignoreModule, sourcesByFqn, diagnostics, lombok, classOutput,
                false);
        if (javacTask == null) {
            LOGGER.warn("Have no sources in source set {}", sourceSet.name());
            return;
        }
        // what javac is about to resolve this source set's dependencies against, checked against what we parsed
        validateClassOutput(summary, sourceSet);
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
            javacTask = createTask(sourceSet, ignoreModule, sourcesByFqn, diagnostics, false, classOutput, false);
            scanCompilationUnits = new ScanCompilationUnits(runtime, inputConfiguration, javacTask, sourceSet,
                    infoByFqn, true, diagnostics, preload, pni, jdkInternals, computeFingerPrints,
                    syntheticListField);
            scanned = scanCompilationUnits.scan();
        }
        this.lastScanUnits = scanCompilationUnits; // keep the live task for on-demand getOrLoad
        this.lastScanUnitsGenerated = false;       // intact until this source set is generated, at the very end
        // ...and remember how to rebuild a loader on THIS set once the scan has moved on to the next one: a request
        // carrying this source set must be answered against this class path, whatever is scanned after it
        loaderSpecBySourceSet.put(sourceSet, new LoaderSpec(sourceSet, ignoreModule, parameterNames,
                syntheticListField));
        loaderUnitsBySourceSet.remove(sourceSet); // a fresh scan supersedes any replacement built earlier
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
        } else {
            // javac compiled with ignoreModule (everything in the unnamed module), so module-info.java was filtered
            // out before compilation (see computeCompilationUnits) and javac produced no ModuleInfo. Refactorings
            // (module-info export reconciliation) still need the module descriptor, so parse it directly with the
            // home-made parser -- a purely syntactic parse of module-info.java, no javac, no module-path compilation.
            ModuleInfo moduleInfo = parseModuleInfoDescriptor(summary, sourceSet, sourcesByFqn);
            if (moduleInfo != null) {
                summary.putSourceSetToModuleInfo(sourceSet, moduleInfo);
            }
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
            } catch (RuntimeException | AssertionError | StackOverflowError e) {
                // committing a type whose references were dropped by fault isolation can fail. fail-fast: rethrow;
                // accumulate: skip the type and record it, so the run still completes over what did commit.
                if (!ignoreErrors) throw e;
                URI uri;
                try {
                    uri = typeInfo.compilationUnit().uri();
                } catch (RuntimeException ignore) {
                    uri = null;
                }
                // Same authority as the scan-time drop in ScanCompilationUnits: the two used to test this
                // independently and could drift. A CompletionFailure here is a class file that a CLASSPATH type
                // refers to and that is absent — see UnresolvedSymbolException#isTolerable for why that is
                // routine rather than fatal, and what it cost on trino when it was not.
                boolean tolerable = UnresolvedSymbolException.isTolerable(e);
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

        // Last, once nothing reads javac's trees for this source set any more: generation desugars them (see
        // ScanCompilationUnits.generateClassFiles). Its output is what this set's dependents will resolve against.
        if (classOutput != null) {
            // generate() tears this task's compiler context down, so the retained scan can no longer serve
            // compiled-type loads. Record what a source-free replacement must be built from, and mark the scan
            // spent BEFORE generating -- a generate() that fails halfway leaves the task just as unusable.
            lastScanUnitsGenerated = true;
            loaderUnits = null; // the previous replacement, if any, is superseded by this source set's
            loaderSpec = new LoaderSpec(sourceSet, ignoreModule, parameterNames, syntheticListField);
            if (scanCompilationUnits.generateClassFiles() > 0) {
                generatedClassOutput.put(sourceSet.name(), classOutput);
            } else {
                // nothing came out (the set does not compile, or javac aborted): leave the dependents on the
                // build's class output, which is no worse than before and may well be complete
                generatedClassOutput.remove(sourceSet.name());
                LOGGER.warn("No class files generated for source set {}; its dependents fall back to the build's"
                            + " class output", sourceSet.name());
            }
        }
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
    // the same name ("package exists in another module") -- but only when the SOURCE SET declares them, see below.
    private static List<String> jdkInternalsJavacOptions(SourceSet sourceSet, InputConfiguration inputConfiguration) {
        List<String> options = new ArrayList<>();
        options.add("-XDignore.symbol.file=true");
        // ⛔ --limit-modules ONLY when the SOURCE SET declares the modules itself. On the class-path fallback the
        // list is what the configuration happens to name, which is not the same claim: it is a class path, not a
        // statement about the module graph, and it is routinely incomplete. Limiting to it REMOVES modules that
        // resolved a moment ago.
        //
        // Measured on guava, same configuration, --jdk-internals throughout: the 20 jmods its class path declares
        // are all java.*, with no jdk.unsupported. Adding --limit-modules from that list fixed MacHashFunctionTest
        // (sun.security.jca) and simultaneously broke THREE main-source files that had been fine —
        // LittleEndianByteArray, UnsignedBytes, AbstractFutureState, all on "Type Unsafe not found", because
        // sun.misc.Unsafe lives in jdk.unsupported. Net 4 dropped compilation units -> 6.
        //
        // --add-exports is additive and safe to emit from either source: it opens packages of modules that are
        // present, and names no module it does not also export. Only the limiting is a claim about completeness.
        //
        // ⚠ THIS IS A NARROWING, NOT A CURE, and the remaining half is measured. A source set's dependencies are
        // a real statement about its module graph, but they are incomplete in exactly the same way: NO generator
        // emits jdk.unsupported, and none of the corpus configurations declares it. Counted 2026-08-16 over the
        // configurations in test-oss: camel, jenkins and langchain4j declare 21 JDK modules per source set and
        // still take the branch below, so any sun.misc.Unsafe in those corpora fails just as guava's three files
        // did. guava is merely out of the line of fire because its regenerated configuration takes the fallback.
        // The durable fix is for the generators to declare the jdk.* modules the sources actually use; that needs
        // regenerated configurations, which is why it is not done here.
        List<String> declaredBySourceSet = jdkModulesOf(sourceSet.dependencies());
        List<String> jdkModules = jdkModulesFor(sourceSet, inputConfiguration);
        if (!declaredBySourceSet.isEmpty()) {
            options.add("--limit-modules");
            options.add(String.join(",", declaredBySourceSet));
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

    /**
     * The JDK modules to open up, preferring the source set's own declared dependencies and falling back to the
     * ones the CONFIGURATION declares as class path parts.
     * <p>
     * ⛔ <b>WITHOUT THE FALLBACK, {@code --jdk-internals} IS A SILENT NO-OP FOR MOST CONFIGURATIONS.</b> The
     * {@code --add-exports} loop below is the only thing that opens a non-exported package, and it iterates this
     * list — so an empty list means the flag drops {@code --release}/{@code --system} and then adds nothing. The
     * failure is not silent to the user, but it is deeply misleading: {@code package sun.security.jca does not
     * exist} (ct.sym filtering) becomes {@code package sun.security.jca is not visible}, which reads like a
     * deliberate refusal rather than an option that was never emitted.
     * <p>
     * Both spellings are legitimate and both occur in the corpus catalogue. The plugin routes wire every source
     * set to the jmods ({@code ComputeDependencies}: "every external library is dependent on all the jmods"), so
     * {@code dependencies()} carries them; {@code CompileListToInputConfiguration} (the compile-log route) adds
     * the same jmods through {@code addClassPathParts} only, so it does not. Measured over the 14 generated
     * configurations in the test-oss catalogue: <b>10 of them have 20-21 {@code partOfJdk} class path parts and
     * ZERO source sets referencing one</b> — timefold-solver (0 of 65), pulsar (0 of 90), elasticsearch (0 of 27),
     * detekt, fernflower, guava (0 of 6), the three elasticsearch single-set configs; while jenkins, activemq,
     * camel and langchain4j do carry them. ⚠ It does NOT split cleanly by route, so do not reach for "the
     * compile-log corpora" as the rule: what decides it is how the configuration was authored, which is exactly
     * why the fix belongs here rather than in one generator.
     * <p>
     * ⚠ A CONFIGURATION CAN CHANGE SIDES, so treat the lists above as an illustration and not as a register.
     * guava was on the carrying side when this comment was first written and moved to the other on 2026-08-15,
     * when {@code config:guava} switched from the single-module plugin route to the compile-log route to pick up
     * guava-tests (b1a95656) — 47 minutes before the measurement was recorded, on the machine that recorded it.
     * Which side a corpus sits on is a property of the last generator that ran, not of the corpus.
     * <p>
     * ⚠ A configuration with no {@code partOfJdk} parts at all (coil, in that same catalogue) is untouched by
     * this: there is nothing to open, and the fallback returns empty just as the primary does.
     */
    private static List<String> jdkModulesOf(Collection<? extends SourceSet> sourceSets) {
        return sourceSets.stream().filter(SourceSet::partOfJdk).map(SourceSet::name).distinct().sorted().toList();
    }

    private static List<String> jdkModulesFor(SourceSet sourceSet, InputConfiguration inputConfiguration) {
        List<String> fromDependencies = jdkModulesOf(sourceSet.dependencies());
        if (!fromDependencies.isEmpty() || inputConfiguration == null) return fromDependencies;
        List<String> fromClassPath = jdkModulesOf(inputConfiguration.classPathParts());
        if (!fromClassPath.isEmpty()) {
            LOGGER.info("Source set {} declares no JDK module dependency; opening the {} JDK module(s) the"
                        + " configuration declares on the class path instead.", sourceSet.name(),
                    fromClassPath.size());
        }
        return fromClassPath;
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
                                 boolean lombok,
                                 Path classOutput,
                                 boolean loaderOnly) throws IOException {
        List<File> sources = new ArrayList<>();
        Map<String, String> sourcesByClassName;
        // use in-memory sources when they are supplied (parse(Map,...) and parseSingleFileInSourceSet(...));
        // otherwise read the source set's directories from disk. Previously this was gated on the TEST_PROTOCOL
        // source-set name, which discarded the in-memory content supplied by parseSingleFileInSourceSet callers
        // that use their own source-set name (e.g. TestCloneBenchMethodHistogram).
        if (loaderOnly) {
            // a loader task compiles nothing: no source directory is walked (that would re-read the whole tree
            // for a task that only ever resolves class-path symbols) and no compilation unit is handed over
            sourcesByClassName = Map.of();
        } else if (!sourcesByFqn.isEmpty()) {
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
            Iterable<? extends JavaFileObject> allCompilationUnits = loaderOnly ? List.of()
                    : computeCompilationUnits(sourceSet, ignoreModule, sources, sourcesByClassName, fm);
            boolean hasModuleInfo = false;
            boolean haveSources = false;
            for (JavaFileObject jfo : allCompilationUnits) {
                if (jfo.toUri().getPath().endsWith("module-info.java")) hasModuleInfo = true;
                haveSources = true;
            }
            // "no sources" is the normal state of a loader task, and the whole point of it
            if (!haveSources && !loaderOnly) return null;

            List<File> jarsAndClassDirectories = new ArrayList<>();
            List<File> moduleJars = new ArrayList<>();

            for (SourceSet classPathPart : sourceSet.dependencies()) {
                // ignore jmod:, ignore jar-on-classpath: they are handled by the ClassSymbolScanner
                if (classPathPart.externalLibrary()
                    && InputConfiguration.jarOnClasspathSelector(classPathPart) == null
                    && !classPathPart.partOfJdk()) {
                    try {
                        File file = Path.of(classPathPart.uri()).toFile();
                        // Route to the module path only when THIS source set is itself a module (has a module-info):
                        // a non-modular source set compiles into the unnamed module, which reads its dependencies from
                        // the classpath (a modular jar on the classpath is read as a plain jar). Putting a module on a
                        // non-modular consumer's module path leaves it "not in the module graph" -> not visible.
                        if (ignoreModule || !hasModuleInfo || !classPathPart.isModule()) {
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
                    // run and are already in the CompiledTypesManager -- but javac knows nothing of the CST and
                    // resolves every reference into them from CLASS FILES, hence this entry. Which files those are
                    // is classOutputOf's decision; null means we have none to offer (validateClassOutput reports it).
                    File file = classOutputOf(dependency);
                    if (file == null) continue;
                    // Same as above: only a modular consumer resolves a source-set dependency via the module path.
                    if (ignoreModule || !hasModuleInfo || !dependency.isModule()) {
                        jarsAndClassDirectories.add(file);
                    } else {
                        moduleJars.add(file);
                    }
                }
            }
            setCompileClassPath(fm, jarsAndClassDirectories, sourceSet);
            if (!moduleJars.isEmpty()) {
                fm.setLocation(StandardLocation.MODULE_PATH, moduleJars);
            }
            // Only set when we are going to call generate() (see singleSourceSet). Left unset, javac's default
            // class output is the directory of the source file it compiles -- so an unconditional generate() would
            // scatter .class files through the user's source tree.
            if (classOutput != null) {
                fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classOutput.toFile()));
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
            boolean lombokOnClassPath = lombok && !loaderOnly && sourceSet.dependencies().stream()
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
                options.addAll(jdkInternalsJavacOptions(sourceSet, inputConfiguration));
            } else if (altJre != null) {
                options.add("--system");
                options.add(altJre.toString());
            } else {
                // ⛔⛔ THE CORPUS'S RELEASE, NOT OURS, WHEN THE CORPUS SAID ONE. javac --release N is "compile
                // against N's API", and the running JDK is not the corpus's platform: an API removed after N is
                // simply absent, so the parse reports 'cannot find symbol' against source whose own build is
                // green. Measured on pulsar 5.0.0-M1 (all 105 invocations --release 17, maddi on JDK 26):
                // Thread.suspend()/resume() are gone in 26, three copies of bookkeeper's ZooKeeperUtil call
                // them, javac stopped attributing and the units behind them were dropped.
                // ⚠ --enable-preview is only legal for the release we RUN on; a corpus release is by definition
                // an older one, and javac refuses the combination.
                // java.lang.Runtime: the maddi CST 'Runtime' is imported in this file and would shadow it
                // ⭐ THE SET'S OWN RELEASE FIRST, the configuration's second. The global field states one answer
                // for the whole configuration and therefore ABSTAINS on a reactor that states several
                // (CompileListToInputConfiguration#setSourceRelease: max hides an API removed after it from the
                // module that still uses it, min invents errors in the module that does not). Per set the
                // question has an answer -- this set's own -- and this loop already runs one javac task per
                // source set, so nothing but the value had to change. OpenSearch states three releases.
                int running = java.lang.Runtime.version().feature();
                int perSet = sourceSet.sourceRelease();
                int global = inputConfiguration == null ? 0 : inputConfiguration.sourceRelease();
                int configured = perSet > 0 ? perSet : global;
                if (configured > 0 && configured != running) {
                    options.add("--release=" + configured);
                } else {
                    options.add("--enable-preview");
                    options.add("--release=" + running);
                }
            }
            // ⚠ AFTER the release branch and outside it, because it applies to all three: --add-modules is
            // orthogonal to how the platform is selected. One OpenSearch set compiles with
            // `--add-modules jdk.incubator.vector` and the other 46 must not -- without it the two units using
            // the vector API do not resolve and are dropped; with it granted to every set, the parse would
            // accept source those builds would reject.
            if (!sourceSet.addModules().isEmpty()) {
                options.add("--add-modules=" + String.join(",", sourceSet.addModules()));
            }
            return (JavacTask) javaCompiler.getTask(null, fm, diagnostics, options, null, allCompilationUnits);
        }
    }

    // Set javac's compile class path for this source set. With no file dependencies we leave javac's default
    // (process) class path untouched -- that is what carries jar-on-classpath libraries. As soon as we override it
    // with file dependencies, that default is gone, so the jar-on-classpath libraries are added back explicitly.
    private void setCompileClassPath(StandardJavaFileManager fm, List<File> fileDependencies, SourceSet sourceSet)
            throws IOException {
        if (fileDependencies.isEmpty()) return;
        List<File> classPath = new ArrayList<>(fileDependencies);
        classPath.addAll(resolveJarOnClassPathDependencies(sourceSet));
        fm.setLocation(StandardLocation.CLASS_PATH, classPath);
    }

    /**
     * The class files javac must resolve a source-set dependency against: the ones we generated for that set when
     * generation is on and produced output, otherwise the build's class output as configured on the source set.
     * <p>
     * The generated directory <em>replaces</em> the build's rather than shadowing it. Mixing the two is the worst of
     * both: a type would come from our (current) output while its sibling, dropped from ours because it no longer
     * compiles or no longer exists, would still be found in the build's — the stale-class-file failure this feature
     * exists to remove, now harder to see. When we generated nothing at all for a set, we fall back wholesale.
     * <p>
     * {@code null} when there is nothing usable: a relative or opaque URI such as {@code file:src/main/java} is not
     * something {@code Path.of} accepts, let alone a class-path entry. Silent here on purpose —
     * {@link #validateClassOutput} is what reports it, and only when we know the set holds types that will now fail
     * to resolve.
     */
    private File classOutputOf(SourceSet dependency) {
        Path generated = generatedClassOutput.get(dependency.name());
        if (generated != null) return generated.toFile();
        URI uri = dependency.uri();
        if (uri == null || uri.isOpaque() || !"file".equals(uri.getScheme())) return null;
        return Path.of(uri).toFile();
    }

    // how many type names a class-output warning names before it stops listing them
    private static final int REPORTED_EXAMPLES = 5;
    // a class file counted as stale must be older than its source by more than this; file-system timestamp
    // granularity (and build tools that copy rather than compile) make an exact comparison too eager
    private static final long STALE_GRACE_MILLIS = 1000L;

    /**
     * Check what javac is about to resolve this source set's dependencies against, and warn when it cannot be right.
     * <p>
     * The question this answers is narrow and factual: <em>we parsed N types from source set D; can javac resolve
     * all N while it compiles S?</em> It can when it finds either a class file or (its class path doubling as its
     * source path) a source file for the type in the single entry we give it. If not, references from S will not
     * resolve, the compilation units holding them are dropped (as tolerable warnings, see
     * {@code ScanCompilationUnits.CompilationUnitFailure}), and the analysis quietly covers less than it appears to.
     * That silence is the actual problem; a warning naming the source set, the directory and the first few types
     * turns it into something a user can act on.
     * <p>
     * Only dependencies we have parsed types for, from files on disk, are checked. A set we have not parsed makes no
     * claim we could verify, and an in-memory (test-protocol) set has no build output to be wrong about — checking
     * either would produce noise on every test parse.
     */
    private void validateClassOutput(Summary summary, SourceSet sourceSet) {
        Map<String, SourceSet> dependencies = sourceSet.dependencies().stream()
                .filter(d -> !d.externalLibrary())
                .collect(Collectors.toMap(SourceSet::name, d -> d, (a, _) -> a, LinkedHashMap::new));
        if (dependencies.isEmpty()) return;
        Map<String, List<TypeInfo>> parsedFromDisk = new LinkedHashMap<>();
        Map<TypeInfo, Long> sourceModified = new IdentityHashMap<>();
        sourceFiles.forEach((sourceFile, types) -> {
            URI uri = sourceFile.uri();
            if (types.isEmpty() || uri == null || uri.isOpaque() || !"file".equals(uri.getScheme())) return;
            SourceSet of = sourceFile.sourceSet();
            if (of == null || !dependencies.containsKey(of.name())) return;
            parsedFromDisk.computeIfAbsent(of.name(), _ -> new ArrayList<>()).addAll(types);
            long modified = Path.of(uri).toFile().lastModified();
            types.forEach(typeInfo -> sourceModified.put(typeInfo, modified));
        });
        parsedFromDisk.forEach((name, parsed) ->
                validateOneDependency(summary, sourceSet, dependencies.get(name), parsed, sourceModified));
    }

    private void validateOneDependency(Summary summary,
                                       SourceSet sourceSet,
                                       SourceSet dependency,
                                       List<TypeInfo> parsed,
                                       Map<TypeInfo, Long> sourceModified) {
        File dir = classOutputOf(dependency);
        if (dir == null) {
            reportClassOutputProblem(summary, sourceSet, dependency, dependency.uri(),
                    parsed.size() + " type(s) were parsed from it, but its URI (" + dependency.uri()
                    + ") is not a usable class-path entry, so javac has no class files for any of them");
            return;
        }
        if (!dir.isDirectory()) {
            reportClassOutputProblem(summary, sourceSet, dependency, dir.toURI(),
                    parsed.size() + " type(s) were parsed from it, but its class output " + dir
                    + " does not exist");
            return;
        }
        List<String> missing = new ArrayList<>();
        List<String> stale = new ArrayList<>();
        for (TypeInfo typeInfo : parsed) {
            // a primary type is top-level, so its class file sits at the package path under the output directory
            String path = typeInfo.fullyQualifiedName().replace('.', '/');
            File classFile = new File(dir, path + ".class");
            long lastModified = classFile.lastModified(); // 0 when it is not there
            if (lastModified == 0L) {
                // javac's class path doubles as its source path: a .java file found there is compiled implicitly,
                // so the type resolves after all. That is not a corner case — it is how both build plugins'
                // source sets work today, their uri() being the first SOURCE directory rather than a class output.
                // Miss this and every plugin user gets a warning about a set-up that is in fact fine.
                if (!new File(dir, path + ".java").isFile()) missing.add(typeInfo.fullyQualifiedName());
            } else {
                Long source = sourceModified.get(typeInfo);
                if (source != null && source > 0L && lastModified + STALE_GRACE_MILLIS < source) {
                    stale.add(typeInfo.fullyQualifiedName());
                }
            }
        }
        if (missing.isEmpty() && stale.isEmpty()) return;
        StringBuilder problem = new StringBuilder("of the ").append(parsed.size())
                .append(" type(s) parsed from it, ");
        if (!missing.isEmpty()) problem.append(missing.size()).append(" have neither a class file nor a source file");
        if (!missing.isEmpty() && !stale.isEmpty()) problem.append(" and ");
        if (!stale.isEmpty()) problem.append(stale.size()).append(" have a class file older than their source");
        problem.append(" in ").append(dir).append(" (e.g. ")
                .append(Stream.concat(missing.stream(), stale.stream()).limit(REPORTED_EXAMPLES)
                        .collect(Collectors.joining(", ")))
                .append(")");
        reportClassOutputProblem(summary, sourceSet, dependency, dir.toURI(), problem.toString());
    }

    private void reportClassOutputProblem(Summary summary, SourceSet sourceSet, SourceSet dependency, URI uri,
                                          String problem) {
        String detail = "Source set '" + sourceSet.name() + "' resolves its references into '" + dependency.name()
                        + "' through class files, and " + problem
                        + ". Those references will not resolve, and the compilation units holding them are dropped."
                        + " Rebuild '" + dependency.name() + "' before analysing, or have maddi compile it itself"
                        + " (JavaInspector.setGeneratedClassesDirectory).";
        LOGGER.warn(detail);
        summary.addParseWarning(new Summary.ParseException(uri, dependency.name(), detail, null,
                Message.Severity.WARN));
    }

    /**
     * The directory this source set is to be compiled into, emptied so that a type that was renamed or deleted
     * cannot linger in it, or {@code null} when generation is off (or the directory cannot be prepared, in which
     * case we simply carry on without it — this is a convenience, never a reason to fail a parse).
     * <p>
     * Callers reach this only for a scan that read the source set <em>from disk</em>, i.e. one whose in-memory
     * source map is empty. That is the rule that keeps the wipe honest: a scan driven by in-memory sources is
     * either the whole of a test-protocol set (which has no build output and no dependents that could want class
     * files) or, worse for us, a single file of a disk-backed set — {@code parseSingleFileInSourceSet}, and the
     * warm-up type {@code onlyPreload} parses. Generating for one of those would empty the set's directory and
     * refill it with a fraction of the set.
     */
    private Path prepareGeneratedClassOutput(SourceSet sourceSet) {
        if (generatedClassesDirectory == null || sourceSet.externalLibrary()) return null;
        // a relative directory is resolved against the configured working directory, as source directories are,
        // so the run does not depend on the process's current directory (e.g. under a Gradle worker)
        Path workingDirectory = inputConfiguration == null ? null : inputConfiguration.workingDirectory();
        Path root = workingDirectory == null || generatedClassesDirectory.isAbsolute()
                ? generatedClassesDirectory : workingDirectory.resolve(generatedClassesDirectory);
        Path dir = root.resolve(directoryNameOf(sourceSet));
        try {
            deleteRecursively(dir);
            Files.createDirectories(dir);
            return dir;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot prepare the generated-classes directory {} for source set {}: {}; its dependents"
                        + " fall back to the build's class output", dir, sourceSet.name(), e.toString());
            generatedClassOutput.remove(sourceSet.name());
            return null;
        }
    }

    /**
     * A source set's directory below the configured generated-classes directory. Source-set names carry characters
     * a path cannot ({@code :a:util/main}), so they are sanitised — and two different names can sanitise to the same
     * string, hence the suffix. {@code String.hashCode} is specified, so it is stable across JVMs and runs, which
     * matters: the directory must be found again on the next parse, not merely be unique within one.
     */
    private static String directoryNameOf(SourceSet sourceSet) {
        String name = sourceSet.name();
        String sanitized = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (sanitized.length() > 100) sanitized = sanitized.substring(0, 100);
        return sanitized + "-" + Integer.toHexString(name.hashCode());
    }

    /** Empty out (and remove) one source set's generated-classes directory; a no-op when it is not there. */
    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    // jar-on-classpath dependencies resolved to their real jar files (see ClassSymbolScanner#jarOnClassPathFile)
    private List<File> resolveJarOnClassPathDependencies(SourceSet sourceSet) {
        List<File> jars = new ArrayList<>();
        for (SourceSet dependency : sourceSet.dependencies()) {
            String selector = InputConfiguration.jarOnClasspathSelector(dependency);
            if (dependency.partOfJdk() || selector == null) continue;
            File jar = ClassSymbolScanner.jarOnClassPathFile(selector);
            if (jar != null) jars.add(jar);
        }
        return jars;
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

    /**
     * Parse a {@code module-info.java} into a {@link ModuleInfo} descriptor with the home-made (congocc) parser,
     * for the case where javac ran with {@code ignoreModule} and therefore produced no module. This is a purely
     * syntactic parse -- it never puts javac into module mode, so it cannot destabilise the unnamed-module
     * compilation of the rest of the source set. Returns {@code null} when the source set ships no
     * {@code module-info.java} (the common case), so it is a cheap no-op for non-modular source sets. In-memory
     * sources (the test protocol) may supply the descriptor under the {@code "module-info"} key.
     */
    private ModuleInfo parseModuleInfoDescriptor(Summary summary, SourceSet sourceSet,
                                                 Map<String, String> sourcesByFqn) {
        String source;
        URI uri;
        String inMemory = sourcesByFqn.get("module-info");
        if (inMemory != null) {
            source = inMemory;
            uri = URI.create("mem:" + sourceSet.name() + "/module-info.java");
        } else {
            Path workingDirectory = inputConfiguration == null ? null : inputConfiguration.workingDirectory();
            Path found = null;
            for (Path dir : sourceSet.sourceDirectories()) {
                Path resolved = workingDirectory == null || dir.isAbsolute() ? dir : workingDirectory.resolve(dir);
                Path candidate = resolved.resolve("module-info.java");
                if (Files.isRegularFile(candidate)) {
                    found = candidate;
                    break;
                }
            }
            if (found == null) return null;
            uri = found.toUri();
            try {
                source = Files.readString(found);
            } catch (IOException e) {
                LOGGER.warn("Could not read module descriptor {}: {}", found, e.getMessage());
                return null;
            }
        }
        return parseModuleInfoSource(summary, sourceSet, source, uri);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Its own {@link Summary}: the descriptor of a project this run never analysed is not part of this run's parse
     * result, and a warning about it does not belong in the summary the caller prints.
     */
    @Override
    public ModuleInfo parseModuleInfo(Path moduleInfoFile) {
        Path path = moduleInfoFile.isAbsolute() || inputConfiguration == null ? moduleInfoFile
                : inputConfiguration.workingDirectory().resolve(moduleInfoFile);
        String source;
        try {
            source = Files.readString(path);
        } catch (IOException e) {
            LOGGER.warn("Could not read module descriptor {}: {}", path, e.getMessage());
            return null;
        }
        return parseModuleInfoSource(new SummaryImpl(false), null, source, path.toUri());
    }

    private ModuleInfo parseModuleInfoSource(Summary summary, SourceSet sourceSet, String source, URI uri) {
        // A malformed or unexpected module-info must not sink the whole parse: degrade to "no descriptor" (the
        // pre-fix behaviour) so at worst the module-info export reconciliation is skipped, never a crash.
        try {
            JavaParser parser = new JavaParser(source);
            parser.setParserTolerant(false);
            parser.ModularCompilationUnit();
            Node root = parser.rootNode();
            if (!(root instanceof ModularCompilationUnit mcu)) return null;
            CompilationUnit.Builder compilationUnitBuilder = runtime.newCompilationUnitBuilder()
                    .setURI(uri).setSourceSet(sourceSet);
            var resolver = new ResolverImpl(runtime.computeMethodOverrides(), new ParseHelperImpl(runtime), false);
            var typeContext = new TypeContextImpl(runtime, compiledTypesManager, false);
            var context = ContextImpl.create(runtime, compiledTypesManager, summary, resolver, typeContext, true, false);
            return new ParseModuleInfo(runtime, null).parse(mcu, compilationUnitBuilder, context);
        } catch (RuntimeException re) {
            LOGGER.warn("Could not parse module descriptor {}: {}", uri, re.toString());
            return null;
        }
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
    public io.codelaser.maddi.cst.api.info.InfoMapView lastRewireInfoMap() {
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
