package org.e2immu.language.inspection.openjdk;

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
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public JavaInspectorImpl() {
        this(false, false);
    }

    public JavaInspectorImpl(boolean computeFingerPrints, boolean allowCreationOfStubTypes) {
        this.computeFingerPrints = computeFingerPrints;
        this.allowCreationOfStubTypes = allowCreationOfStubTypes;
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
    public List<TypeInfo> parse(String transformedString, CompilationUnit compilationUnit, ParseResult parseResult, ParseOptions parseOptions) {
        return List.of();
    }

    @Override
    public List<InitializationProblem> initialize(InputConfiguration inputConfiguration) throws IOException {
        compiledTypesManager = new TypeData(null); // FIXME
        return List.of();
    }

    @Override
    public void preload(String thePackage) {

    }

    @Override
    public Summary parse(Map<String, String> sourcesByTestProtocolURIString, ParseOptions parseOptions) {
        return null;
    }

    @Override
    public ParseOptions failFast() {
        return FAIL_FAST;
    }

    @Override
    public List<TypeInfo> parseReturnAll(String input, String inputName, String sourceSetName, ParseOptions parseOptions) {
        return List.of();
    }

    @Override
    public Summary parse(URI typeInfo, SourceSet sourceSet, ParseOptions parseOptions) {
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
    public ReloadResult reloadSources(InputConfiguration inputConfiguration, Map<String, String> sourcesByTestProtocolURIString) throws IOException {
        return null;
    }
}
