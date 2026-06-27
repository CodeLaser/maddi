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
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

    // do a preload, with a real recursive load as long as we stay in the package
    // NOTE: module::package java.base::java.util.concurrent
    @Override
    public void preload(String thePackage) {
        preload.add(thePackage);
    }

    @Override
    public List<InitializationProblem> initialize(InputConfiguration inputConfiguration) throws IOException {
        this.inputConfiguration = inputConfiguration;
        CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(inputConfiguration.javaBase());
        compiledTypesManager = ctm;
        runtime = new RuntimeWithCompiledTypesManager(ctm);
        javaBase().computePriorityDependencies();
        return List.of();
    }

    @Override
    public void onlyPreload() {
        parse(Map.of("a.b.X", "class X { } "), new JavaInspector.ParseOptions.Builder().build());
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
                        parseOptions.ignoreModule());
            } catch (IOException ioe) {
                LOGGER.error("Caught exception", ioe);
                throw new UnsupportedOperationException("TODO error handling");
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
                        parseOptions.ignoreModule());
            } catch (IOException ioe) {
                LOGGER.error("Caught exception", ioe);
                throw new UnsupportedOperationException("TODO error handling");
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
                    !parseOptions.failFast(), parseOptions.ignoreModule());
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
                                 boolean ignoreModule) throws IOException {
        MaddiDiagnosticCollector diagnostics = new MaddiDiagnosticCollector(ignoreErrors);
        JavacTask javacTask = createTask(sourceSet, ignoreModule, sourcesByFqn, diagnostics);
        if (javacTask == null) {
            LOGGER.warn("Have no sources in source set {}", sourceSet.name());
            return;
        }
        // TODO when parseOptions.parameterNames() is set, load the ParameterNameIndex (the .paramnames.gz
        //  resources) and pass it here instead of null, so class-file methods get faithful parameter names
        ScanCompilationUnits scanCompilationUnits = new ScanCompilationUnits(runtime, inputConfiguration,
                javacTask, sourceSet, infoByFqn, true, diagnostics, preload, null);
        ScanCompilationUnits.Result scanned = scanCompilationUnits.scan();

        // copy from scanned into summary
        for (TypeInfo typeInfo : scanned.primaryTypes()) {
            summary.addType(typeInfo);
            assert typeInfo.hasBeenInspected();
        }
        if (!scanned.modules().isEmpty()) {
            summary.putSourceSetToModuleInfo(sourceSet, scanned.modules().getFirst());
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
        if (sourceSet.name().startsWith(TEST_PROTOCOL)) {
            sourcesByClassName = sourcesByFqn;
        } else {
            sourcesByClassName = Map.of();
            for (Path path : sourceSet.sourceDirectories()) {
                sources.add(path.toFile());
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
                    List.of("-proc:none", "--enable-preview", "--release=26", "-parameters"),
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
