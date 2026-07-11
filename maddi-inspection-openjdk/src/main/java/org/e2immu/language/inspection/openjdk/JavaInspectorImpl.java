package org.e2immu.language.inspection.openjdk;

import com.sun.source.util.JavacTask;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.ImportComputer;
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
import org.e2immu.language.inspection.api.resource.ParameterNameIndex;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.resource.InfoByFqn;
import org.e2immu.language.inspection.resource.SummaryImpl;
import org.e2immu.language.java.openjdk.InMemoryJavaFileObject;
import org.e2immu.language.java.openjdk.MaddiDiagnosticCollector;
import org.e2immu.language.java.openjdk.ScanCompilationUnits;
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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

public class JavaInspectorImpl implements JavaInspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaInspectorImpl.class);
    private static final TimedLogger TIMED_LOGGER = new TimedLogger(LOGGER, 1000L);

    private Runtime runtime;
    private Map<SourceFile, List<TypeInfo>> sourceFiles;
    private CompiledTypesManager compiledTypesManager;
    private InputConfiguration inputConfiguration; // kept for tests
    private final boolean computeFingerPrints;
    private final boolean allowCreationOfStubTypes;
    private final JavaCompiler javaCompiler;
    private final InfoByFqn infoByFqn = new InfoByFqn();
    private final List<String> preload = new ArrayList<>();
    // the most recent scan's units, retained so its still-live javac task can resolve+load a compiled type by
    // FQN on demand (the CompiledTypesManager's lazy getOrLoad path). Single-threaded, like all javac use here.
    private ScanCompilationUnits lastScanUnits;
    private boolean parameterNames;
    private ParameterNameIndex parameterNameIndex; // lazily loaded when parameterNames is on

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
    public static final ParseOptions FAIL_FAST = new ParseOptions.Builder().setFailFast(true).build();
    public static final ParseOptions DETAILED_SOURCES = new ParseOptions.Builder().setDetailedSources(true).build();

    @Override
    public void invalidateAllSources() {
        infoByFqn.removeAllSources();
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
        CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(inputConfiguration.javaBase());
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
        for (SourceSet sourceSet : linearization) {
            try {
                singleSourceSet(summary, sourcesByFqn, infoByFqn, sourceSet, !parseOptions.failFast(),
                        parseOptions.ignoreModule(), parseOptions.parameterNames() || parameterNames);
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
    public Summary parseMultiSourceSet(Map<SourceSet, Map<String, String>> sourcesByFqnBySourceSet, ParseOptions parseOptions) {
        Summary summary = new SummaryImpl(parseOptions.failFast());
        List<SourceSet> linearization = computeScanOrder(); // from input configuration
        for (SourceSet sourceSet : linearization) {
            try {
                Map<String, String> sourcesByFqn = sourcesByFqnBySourceSet.get(sourceSet);
                singleSourceSet(summary, sourcesByFqn, infoByFqn, sourceSet, !parseOptions.failFast(),
                        parseOptions.ignoreModule(), parseOptions.parameterNames() || parameterNames);
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
                    parseOptions.parameterNames() || parameterNames);
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
                                 boolean parameterNames) throws IOException {
        MaddiDiagnosticCollector diagnostics = new MaddiDiagnosticCollector(ignoreErrors);
        JavacTask javacTask = createTask(sourceSet, ignoreModule, sourcesByFqn, diagnostics);
        if (javacTask == null) {
            LOGGER.warn("Have no sources in source set {}", sourceSet.name());
            return;
        }
        // when parameter names are requested, class-file methods get faithful formal parameter names from the
        // shipped index instead of javac's synthetic arg0, arg1, ...
        ParameterNameIndex pni = parameterNames ? parameterNameIndex() : null;
        ScanCompilationUnits scanCompilationUnits = new ScanCompilationUnits(runtime, inputConfiguration,
                javacTask, sourceSet, infoByFqn, true, diagnostics, preload, pni);
        ScanCompilationUnits.Result scanned = scanCompilationUnits.scan();
        this.lastScanUnits = scanCompilationUnits; // keep the live task for on-demand getOrLoad

        // copy from scanned into summary
        // register the source set so it appears in ParseResult.sourceSetsByName() (mirrors the congocc inspector)
        summary.ensureSourceSet(sourceSet);
        for (TypeInfo typeInfo : scanned.primaryTypes()) {
            summary.addType(typeInfo);
            assert typeInfo.hasBeenInspected();
        }
        if (!scanned.modules().isEmpty()) {
            summary.putSourceSetToModuleInfo(sourceSet, scanned.modules().getFirst());
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
            if (typeInfo.isPrimaryType() && !typeInfo.hasBeenInspected()) {
                scanCompilationUnits.classSymbolScanner().commitType(typeInfo);
            }
            compiledTypesManager.addTypeInfo(null, typeInfo);
        }
    }

    private JavacTask createTask(SourceSet sourceSet,
                                 boolean ignoreModule,
                                 Map<String, String> sourcesByFqn,
                                 MaddiDiagnosticCollector diagnostics) throws IOException {
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

        try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(diagnostics, null, null)) {
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
                    File file = Path.of(dependency.uri()).toFile();
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
            return (JavacTask) javaCompiler.getTask(
                    null, fm, diagnostics,
                    // -parameters makes javac's ClassReader keep formal parameter names read from the
                    // MethodParameters attribute (and the LocalVariableTable) of class files on the class/module
                    // path; without it Symbol.MethodSymbol.getParameters() yields synthetic arg0, arg1, ...
                    // -XDuseUnsharedTable=true: give each compilation its OWN javac name table instead of pulling
                    // from javac's process-wide SharedNameTable freelist. That freelist is shared static state
                    // across all JavacTask/Context instances in a JVM; under repeated parsing (e.g. hundreds of
                    // parseSingleFileInSourceSet calls) it intermittently corrupts and surfaces as
                    // "tree.starImportScope is null" during task.analyze(). maddi keys its CST by FQN strings, not
                    // javac Names, so not sharing names across compilations is safe here.
                    List.of("-proc:none", "--enable-preview", "--release=26", "-parameters",
                            "-XDuseUnsharedTable=true"),
                    null,
                    allCompilationUnits
            );
        }
    }

    private static @NotNull Iterable<? extends JavaFileObject> computeCompilationUnits
            (SourceSet sourceSet,
             boolean ignoreModule,
             List<File> sources,
             Map<String, String> sourcesByClassName, StandardJavaFileManager fm) throws IOException {
        List<File> allSources = new LinkedList<>();
        for (File sourceDir : sources) {
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
        Set<String> restrict = sourceSet.restrictToPackages();
        if (restrict == null || restrict.isEmpty()) return true;
        String fqn = inferFullyQualifiedName(sourceSet, jfo);
        if (fqn == null) {
            LOGGER.warn("Cannot infer package of {}; keeping it despite the package restriction", jfo.toUri());
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
    private static String inferFullyQualifiedName(SourceSet sourceSet, JavaFileObject jfo) {
        URI uri = jfo.toUri();
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
    public ReloadResult reloadSources(InputConfiguration inputConfiguration, Map<String, String> sourcesByTestProtocolURIString) {
        return null;
    }

    @Override
    public boolean isOpenJdk() {
        return true;
    }
}
