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
import org.e2immu.language.inspection.resource.SummaryImpl;
import org.e2immu.language.java.openjdk.InMemoryJavaFileObject;
import org.e2immu.language.java.openjdk.ScanCompilationUnits;
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.UNCHANGED;

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
            _ -> UNCHANGED, false, false);
    public static final ParseOptions DETAILED_SOURCES = new ParseOptionsBuilder().setDetailedSources(true).build();

    public static class ParseOptionsBuilder implements JavaInspector.ParseOptionsBuilder {
        private boolean failFast;
        private boolean detailedSources;
        private boolean parallel;
        private boolean lombok;
        private Invalidated invalidated;

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
            return new ParseOptions(failFast, detailedSources, invalidated, parallel, lombok);
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
        CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(inputConfiguration.javaBase());
        compiledTypesManager = ctm;
        runtime = new RuntimeWithCompiledTypesManager(ctm);
        javaBase().computePriorityDependencies();
        return List.of();
    }

    // main method, generally called with empty map; only tests use the map
    @Override
    public Summary parse(Map<String, String> sourcesByTestProtocolURIString, ParseOptions parseOptions) {
        Summary summary = new SummaryImpl(parseOptions.failFast());
        List<SourceSet> linearization = computeScanOrder(); // from input configuration
        for (SourceSet sourceSet : linearization) {
            try {
                singleSourceSet(summary, sourceSet, !parseOptions.failFast());
            } catch (IOException ioe) {
                if (parseOptions.failFast()) {
                    // add parse exception
                    break;
                }
            }
        }
        return summary;
    }

    private List<SourceSet> computeScanOrder() {
        return List.of(); // TODO
    }

    // single file
    @Override
    public List<TypeInfo> parse(String transformedString, CompilationUnit compilationUnit, ParseResult parseResult, ParseOptions parseOptions) {
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

    // TODO new convention for SourceSets of sources: uri is the location of the class directory!
    private void singleSourceSet(Summary summary, SourceSet sourceSet, boolean ignoreErrors) throws IOException {
        /*
        TODO:
            from the inputConfiguration, find out what the dependencies of this source set are
            translate that in a list of jars
         */
        List<File> jarsAndClassDirectories = new ArrayList<>();
        for (SourceSet dependency : sourceSet.dependencies()) {
            jarsAndClassDirectories.add(Path.of(dependency.uri()).toFile());
        }
        List<File> sources = new ArrayList<>();
        Map<String, String> sourcesByClassName = new HashMap<>();

        DiagnosticCollector<JavaFileObject> diagnostics = ignoreErrors ? null : new DiagnosticCollector<>();
        JavacTask javacTask = createTask(sourcesByClassName, sources, jarsAndClassDirectories, diagnostics);
        ScanCompilationUnits scanCompilationUnits = new ScanCompilationUnits(runtime, inputConfiguration,
                compiledTypesManager.javaBase(), javacTask, sourceSet, true, diagnostics);
        List<Info> scanned = scanCompilationUnits.scan();

        // copy from scanned into summary
        for (Info info : scanned) {
            if (info instanceof TypeInfo typeInfo) {
                summary.addType(typeInfo);
            } else if (info instanceof ModuleInfo moduleInfo) {
                summary.putSourceSetToModuleInfo(sourceSet, moduleInfo);
            }
        }
        // copy into CTM
        for (TypeInfo typeInfo : scanCompilationUnits.classSymbolScanner().typesLoaded()) {
            TypeInfo inMap = compiledTypesManager.get(typeInfo.fullyQualifiedName(), sourceSet);
            if (inMap == null) {
                SourceFile sourceFile = null; // FIXME where to find this? from URI?
                compiledTypesManager.addTypeInfo(sourceFile, typeInfo);
            } else if (!inMap.hasBeenInspected()) {
                scanCompilationUnits.classSymbolScanner().commitType(inMap);
                SourceFile sourceFile = null; // FIXME where to find this? from URI?
                compiledTypesManager.addTypeInfo(sourceFile, inMap);
            }
        }
    }

    private JavacTask createTask(Map<String, String> sourcesByClassName,
                                 List<File> sources,
                                 List<File> jarsAndClassDirectories,
                                 @Nullable DiagnosticCollector<JavaFileObject> diagnostics) throws IOException {
        try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(diagnostics, null, null)) {
            if (!jarsAndClassDirectories.isEmpty()) {
                fm.setLocation(StandardLocation.CLASS_PATH, jarsAndClassDirectories);
            }

            // Wrap each source string in an InMemoryJavaFileObject
            List<JavaFileObject> inMemory = sourcesByClassName.entrySet().stream()
                    .map(e -> new InMemoryJavaFileObject(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            Iterable<? extends JavaFileObject> compilationUnits
                    = fm.getJavaFileObjects(sources.toArray(new File[0]));
            Iterable<? extends JavaFileObject> allCompilationUnits
                    = Stream.concat(StreamSupport.stream(compilationUnits.spliterator(), false),
                            inMemory.stream())
                    .toList();

            return (JavacTask) javaCompiler.getTask(
                    null, fm, diagnostics,
                    List.of("-proc:none", "--enable-preview", "--release=25"),
                    null,
                    allCompilationUnits
            );
        }
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
