package org.e2immu.language.inspection.openjdk;

import com.sun.source.util.JavacTask;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.Info;
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
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.language.inspection.resource.SummaryImpl;
import org.e2immu.language.java.openjdk.InMemoryJavaFileObject;
import org.e2immu.language.java.openjdk.ScanCompilationUnits;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.ImmutableGraph;
import org.e2immu.util.internal.graph.op.Linearize;
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.UNCHANGED;

public class JavaInspectorImpl implements JavaInspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaInspectorImpl.class);
    private static final TimedLogger TIMED_LOGGER = new TimedLogger(LOGGER, 1000L);
    public static final SourceSet TEST_PROTOCOL_SOURCE_SET = new SourceSetImpl.Builder()
            .setName(TEST_PROTOCOL).setUri(URI.create("file:/")).build();

    private Runtime runtime;
    private Map<SourceFile, List<TypeInfo>> sourceFiles;
    private CompiledTypesManager compiledTypesManager;
    private InputConfiguration inputConfiguration; // kept for tests
    private final boolean computeFingerPrints;
    private final boolean allowCreationOfStubTypes;
    private final JavaCompiler javaCompiler;

    public JavaInspectorImpl() {
        this(false, false);
    }

    public JavaInspectorImpl(boolean computeFingerPrints, boolean allowCreationOfStubTypes) {
        this.computeFingerPrints = computeFingerPrints;
        this.allowCreationOfStubTypes = allowCreationOfStubTypes;
        javaCompiler = ToolProvider.getSystemJavaCompiler();
    }

    public static final String JAR_WITH_PATH = "jar-on-classpath";
    public static final String JAR_WITH_PATH_PREFIX = "jar-on-classpath:";
    public static final String E2IMMU_SUPPORT = JAR_WITH_PATH_PREFIX + "org/e2immu/annotation";

    public static final String TEST_PROTOCOL_PREFIX = TEST_PROTOCOL + ":";
    public static final ParseOptions FAIL_FAST = new ParseOptions(true, false,
            _ -> UNCHANGED, false, false, false);
    public static final ParseOptions DETAILED_SOURCES = new ParseOptionsBuilder().setDetailedSources(true).build();

    public static class ParseOptionsBuilder implements JavaInspector.ParseOptionsBuilder {
        private boolean failFast;
        private boolean detailedSources;
        private boolean parallel;
        private boolean lombok;
        private boolean ignoreModule;
        private Invalidated invalidated;

        public ParseOptionsBuilder setIgnoreModule(boolean ignoreModule) {
            this.ignoreModule = ignoreModule;
            return this;
        }

        @Override
        public ParseOptionsBuilder setInvalidated(Invalidated invalidated) {
            this.invalidated = invalidated;
            return this;
        }

        @Override
        public ParseOptionsBuilder setLombok(boolean lombok) {
            this.lombok = lombok;
            return this;
        }

        @Override
        public ParseOptionsBuilder setFailFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        @Override
        public ParseOptionsBuilder setDetailedSources(boolean detailedSources) {
            this.detailedSources = detailedSources;
            return this;
        }

        @Override
        public ParseOptionsBuilder setParallel(boolean parallel) {
            this.parallel = parallel;
            return this;
        }

        @Override
        public ParseOptions build() {
            return new ParseOptions(failFast, detailedSources, invalidated, parallel, lombok, ignoreModule);
        }
    }

    @Override
    public void invalidateAllSources() {
        sourceFiles.values().stream().flatMap(Collection::stream).forEach(ti ->
                compiledTypesManager.invalidate(ti));
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
    @Override
    public void preload(String thePackage) {

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

    // main method, generally called with empty map; only tests use the map
    @Override
    public Summary parse(Map<String, String> sourcesByFqn, ParseOptions parseOptions) {
        Summary summary = new SummaryImpl(parseOptions.failFast());
        List<SourceSet> linearization = computeScanOrder(); // from input configuration
        Map<String, Info> previouslyLoaded = new HashMap<>();
        for (SourceSet sourceSet : linearization) {
            try {
                singleSourceSet(summary, sourcesByFqn, previouslyLoaded, sourceSet, !parseOptions.failFast(),
                        parseOptions.ignoreModule());
            } catch (IOException ioe) {
                throw new UnsupportedOperationException("TODO error handling");
            }
        }
        return summary;
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
        return List.of();
    }

    // single file
    @Override
    public List<TypeInfo> parseReturnAll(String input, String inputName, String sourceSetName, ParseOptions parseOptions) {
        return List.of();
    }

    // single file
    @Override
    public Summary parse(URI typeInfo, SourceSet sourceSet, ParseOptions parseOptions) {
        Summary summary = new SummaryImpl(parseOptions.failFast());
        //
        return summary;
    }

    private void singleSourceSet(Summary summary,
                                 Map<String, String> sourcesByFqn,
                                 Map<String, Info> previouslyLoaded,
                                 SourceSet sourceSet,
                                 boolean ignoreErrors,
                                 boolean ignoreModule) throws IOException {

        DiagnosticCollector<JavaFileObject> diagnostics = ignoreErrors ? null : new DiagnosticCollector<>();
        JavacTask javacTask = createTask(sourceSet, ignoreModule, sourcesByFqn, diagnostics);
        ScanCompilationUnits scanCompilationUnits = new ScanCompilationUnits(runtime, inputConfiguration,
                javacTask, sourceSet, previouslyLoaded, true, diagnostics);
        List<Info> scanned = scanCompilationUnits.scan();

        // copy from scanned into summary
        for (Info info : scanned) {
            if (info instanceof TypeInfo typeInfo) {
                summary.addType(typeInfo);
                assert typeInfo.hasBeenInspected();
            } else if (info instanceof ModuleInfo moduleInfo) {
                summary.putSourceSetToModuleInfo(sourceSet, moduleInfo);
            }
        }
        // copy into CTM
        List<TypeInfo> loaded = List.copyOf(scanCompilationUnits.classSymbolScanner().typesLoaded());
        for (TypeInfo typeInfo : loaded) {
            if (!typeInfo.hasBeenInspected()) {
                scanCompilationUnits.classSymbolScanner().commitType(typeInfo);
                LOGGER.debug("Committed {}", typeInfo);
            }
            compiledTypesManager.addTypeInfo(null, typeInfo);
        }

        scanCompilationUnits.mergeIntoPreviouslyLoaded();
    }

    private JavacTask createTask(SourceSet sourceSet,
                                 boolean ignoreModule,
                                 Map<String, String> sourcesByFqn,
                                 @Nullable DiagnosticCollector<JavaFileObject> diagnostics) throws IOException {
        List<File> sources = new ArrayList<>();
        Map<String, String> sourcesByClassName;
        if (TEST_PROTOCOL.equals(sourceSet.name())) {
            sourcesByClassName = sourcesByFqn;
        } else {
            sourcesByClassName = Map.of();
            for (Path path : sourceSet.sourceDirectories()) {
                sources.add(path.toFile());
            }
        }

        try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> allCompilationUnits = computeCompilationUnits(ignoreModule, sources,
                    sourcesByClassName, fm);
            boolean hasModuleInfo = hasModuleInfo(allCompilationUnits);

            List<File> jarsAndClassDirectories = new ArrayList<>();
            List<File> moduleJars = new ArrayList<>();

            for (SourceSet classPathPart : inputConfiguration.classPathParts()) {
                // ignore jmod:, ignore jar-on-classpath: they are handled by the ClassSymbolScanner
                if (!classPathPart.name().startsWith(JAR_WITH_PATH_PREFIX) && !classPathPart.partOfJdk()) {
                    File file = Path.of(classPathPart.uri()).toFile();
                    if (ignoreModule || !classPathPart.isModule()) {
                        jarsAndClassDirectories.add(file);
                    } else {
                        moduleJars.add(file);
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
            if (!ignoreModule && hasModuleInfo && moduleJars.isEmpty()) {
                LOGGER.warn("The source set {} declares a module but no module path was provided.", sourceSet.name());
            }
            return (JavacTask) javaCompiler.getTask(
                    null, fm, diagnostics,
                    List.of("-proc:none", "--enable-preview", "--release=25"),
                    null,
                    allCompilationUnits
            );
        }
    }

    private static @NotNull Iterable<? extends JavaFileObject> computeCompilationUnits
            (boolean ignoreModule,
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
                .map(e -> new InMemoryJavaFileObject(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        Iterable<? extends JavaFileObject> compilationUnits = fm.getJavaFileObjects(allSources.toArray(new File[0]));
        Iterable<? extends JavaFileObject> allCompilationUnits
                = Stream.concat(StreamSupport.stream(compilationUnits.spliterator(), false),
                        inMemory.stream())
                .toList();
        return allCompilationUnits;
    }

    private boolean hasModuleInfo(Iterable<? extends JavaFileObject> allCompilationUnits) {
        for (JavaFileObject jfo : allCompilationUnits) {
            if (jfo.toUri().getPath().endsWith("module-info.java")) return true;
        }
        return false;
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
    public ReloadResult reloadSources(InputConfiguration inputConfiguration, Map<String, String> sourcesByTestProtocolURIString) throws IOException {
        return null;
    }
}
