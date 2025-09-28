package org.e2immu.language.inspection.integration;

import org.e2immu.bytecode.java.asm.ByteCodeInspectorImpl;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter2.Formatter2Impl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.Resolver;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.*;
import org.e2immu.language.inspection.impl.parser.ContextImpl;
import org.e2immu.language.inspection.impl.parser.ResolverImpl;
import org.e2immu.language.inspection.impl.parser.SummaryImpl;
import org.e2immu.language.inspection.impl.parser.TypeContextImpl;
import org.e2immu.language.inspection.resource.CompiledTypesManagerImpl;
import org.e2immu.language.inspection.resource.ResourcesImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.parser.java.*;
import org.e2immu.support.Either;
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.parsers.java.JavaParser;
import org.parsers.java.Node;
import org.parsers.java.ParseException;
import org.parsers.java.ast.ModularCompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.*;

/*
from input configuration
to classpath + sourceTypeMap/Trie

then, do a 1st round of parsing the source types -> map fqn->type/subType + list compilation unit->parsed object

finally, do the actual parsing for all primary types
 */
public class JavaInspectorImpl implements JavaInspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaInspectorImpl.class);
    private static final TimedLogger TIMED_LOGGER = new TimedLogger(LOGGER, 1000L);

    private Runtime runtime;
    private Map<SourceFile, List<TypeInfo>> sourceFiles;
    private CompiledTypesManager compiledTypesManager;
    private final boolean computeFingerPrints;
    private final boolean allowCreationOfStubTypes;

    public JavaInspectorImpl() {
        this(false, false);
    }

    public JavaInspectorImpl(boolean computeFingerPrints, boolean allowCreationOfStubTypes) {
        this.computeFingerPrints = computeFingerPrints;
        this.allowCreationOfStubTypes = allowCreationOfStubTypes;
    }

    /**
     * Use of this prefix in parts of the input classpath allows for adding jars
     * on the current classpath containing the path following the prefix.
     * <p>
     * For example, adding
     * <p>
     * Adds the content of
     * <p>
     * jar:file:/Users/bnaudts/.gradle/caches/modules-2/files-2.1/com.google.guava/guava/28.1-jre/b0e91dcb6a44ffb6221b5027e12a5cb34b841145/guava-28.1-jre.jar!/
     */
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
    public List<InitializationProblem> initialize(InputConfiguration inputConfiguration) throws IOException {
        List<InitializationProblem> initializationProblems = new LinkedList<>();
        try {

            Resources classPath = assembleClassPath(inputConfiguration.workingDirectory(),
                    inputConfiguration.classPathParts(), inputConfiguration.alternativeJREDirectory(),
                    initializationProblems);
            // we have a unified type map: both sources and compiled types are in the same map
            CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(classPath);
            runtime = new RuntimeWithCompiledTypesManager(ctm);
            ByteCodeInspector byteCodeInspector = new ByteCodeInspectorImpl(runtime, ctm, computeFingerPrints,
                    allowCreationOfStubTypes);
            ctm.setByteCodeInspector(byteCodeInspector);
            this.compiledTypesManager = ctm;

            for (String packageName : new String[]{"java.lang", "java.util.function"}) {
                preload(packageName);
            }

            Resources sourcePath = assembleSourcePath(inputConfiguration.workingDirectory(),
                    inputConfiguration.sourceSets(), initializationProblems);
            ctm.addToTrie(sourcePath, false);
            List<SourceFile> sourceFiles = computeSourceURIs(sourcePath);
            this.sourceFiles = new HashMap<>();
            sourceFiles.forEach(sf -> this.sourceFiles.put(sf, List.of()));
        } catch (URISyntaxException e) {
            LOGGER.error("Caught URISyntaxException, transforming into IOException", e);
            throw new IOException(e);
        }
        return List.copyOf(initializationProblems);
    }

    /*
    Strategy:
    Load all sourceFiles from the source path.
    On the basis of the source fingerprints, eep track of
    - types that are new. Action: add to sourceFiles; no need to report, since the code compiles.
    - types that have been removed. Action: remove from sourceFiles; no need to report, since the code compiles.
    - types that have been changed. Report this change, so that the Invalidated parse option can return
        INVALID for this type, and that the dependents can be computed so that they are rewired.

    The code that computes the Rewired types will have to make use of a type dependency graph.
     */
    public ReloadResult reloadSources(InputConfiguration inputConfiguration,
                                      Map<String, String> sourcesByTestProtocolURIString) throws IOException {
        if (!computeFingerPrints) {
            throw new UnsupportedOperationException("The reloadSources method requires fingerprints to be computed");
        }
        List<InitializationProblem> initializationProblems = new LinkedList<>();
        Set<TypeInfo> changed = new HashSet<>();
        try {
            Resources sourcePath = assembleSourcePath(inputConfiguration.workingDirectory(),
                    inputConfiguration.sourceSets(), initializationProblems);
            Set<SourceFile> removed = new HashSet<>(this.sourceFiles.keySet());
            List<SourceFile> sourceFiles = computeSourceURIs(sourcePath);
            AtomicInteger sourceFilesChanged = new AtomicInteger();
            AtomicInteger newSourceFiles = new AtomicInteger();
            sourceFiles.forEach(sf -> {
                List<TypeInfo> current = this.sourceFiles.get(sf);
                if (current != null) {
                    removed.remove(sf);
                    if (!current.isEmpty()) {
                        TypeInfo typeInfo = current.getFirst();
                        FingerPrint currentFingerprint = typeInfo.compilationUnit().fingerPrintOrNull();
                        String sourceCode = loadSource(sf, sourcesByTestProtocolURIString,
                                sf.sourceSet().sourceEncoding(),
                                e -> initializationProblems.add(new InitializationProblem("parsing", e)));
                        FingerPrint newFingerprint = sourceCode == null ? MD5FingerPrint.NO_FINGERPRINT : MD5FingerPrint.compute(sourceCode);
                        assert currentFingerprint != null && currentFingerprint != MD5FingerPrint.NO_FINGERPRINT;
                        if (!currentFingerprint.equals(newFingerprint)) {
                            // CHANGE
                            this.sourceFiles.put(sf, List.of());
                            changed.addAll(current);
                            sourceFilesChanged.incrementAndGet();
                        } // else: UNCHANGED
                    }
                } else {
                    // NEW
                    this.sourceFiles.put(sf, List.of());
                    newSourceFiles.incrementAndGet();
                }
            });
            // those that remain in "removed" are not present anymore, they should go.
            LOGGER.info("Reloaded sources: {} source file(s) removed, {} new, {} of {} remaining changed",
                    removed.size(), newSourceFiles.get(), sourceFilesChanged.get(), sourceFiles.size());
            this.sourceFiles.keySet().removeAll(removed);
        } catch (URISyntaxException e) {
            LOGGER.error("Caught URISyntaxException, transforming into IOException", e);
            throw new IOException(e);
        }
        return new ReloadResult(List.copyOf(initializationProblems), Set.copyOf(changed));
    }

    private List<SourceFile> computeSourceURIs(Resources sourcePath) {
        List<SourceFile> sourceFiles = new LinkedList<>();
        AtomicInteger ignored = new AtomicInteger();
        Map<SourceSet, Integer> perSourceSet = new HashMap<>();
        sourcePath.visit(new String[0], (parts, list) -> {
            if (parts.length >= 1 && !list.isEmpty()) {
                int n = parts.length - 1;
                String name = parts[n];
                if (name.endsWith(".java")) {
                    String typeName = name.substring(0, name.length() - 5);
                    String packageName = Arrays.stream(parts).limit(n).collect(Collectors.joining("."));
                    for (SourceFile sourceFile : list) {
                        if (sourceFile.sourceSet().acceptSource(packageName, typeName)) {
                            sourceFiles.add(sourceFile);
                            parts[n] = typeName;
                            perSourceSet.merge(sourceFile.sourceSet(), 1, Integer::sum);
                        } else {
                            ignored.incrementAndGet();
                        }
                    }
                }
            }
        });
        LOGGER.info("Found {} .java files in source path, skipped {}; per source set: {}", sourceFiles.size(),
                ignored, perSourceSet);
        return List.copyOf(sourceFiles);
    }

    /**
     * IMPORTANT: this method assumes that the jmod 'java.base.jmod' is on the class path
     * if not, the method will have little effect and no classes beyond the ones from
     * <code>initializeClassPath</code> will be present
     */
    @Override
    public void preload(String thePackage) {
        compiledTypesManager.preload(thePackage);
    }

    private Resources assembleClassPath(Path workingDirectory,
                                        List<SourceSet> sourceSets,
                                        Path alternativeJREDirectory,
                                        List<InitializationProblem> initializationProblems) throws IOException, URISyntaxException {
        Resources resources = new ResourcesImpl(workingDirectory);
        for (SourceSet sourceSet : sourceSets) {
            String scheme = sourceSet.uri().getScheme();
            String path = sourceSet.uri().getSchemeSpecificPart();
            handleSourceSet(workingDirectory, alternativeJREDirectory, "Class path", initializationProblems,
                    sourceSet, path, scheme, resources);
        }
        return resources;
    }

    private Resources assembleSourcePath(Path workingDirectory,
                                         List<SourceSet> sourceSets,
                                         List<InitializationProblem> initializationProblems) throws IOException, URISyntaxException {
        Resources resources = new ResourcesImpl(workingDirectory);
        for (SourceSet sourceSet : sourceSets) {
            if (sourceSet.sourceDirectories().isEmpty()) {
                String scheme = sourceSet.uri().getScheme();
                String path = sourceSet.uri().getSchemeSpecificPart();
                handleSourceSet(workingDirectory, null, "Source path", initializationProblems,
                        sourceSet, path, scheme, resources);
            } else {
                for (Path sourceDir : sourceSet.sourceDirectories()) {
                    handleSourceSet(workingDirectory, null, "Source path", initializationProblems,
                            sourceSet, sourceDir.toString(), "file", resources);
                }
            }
        }
        return resources;
    }

    private void handleSourceSet(Path workingDirectory, Path alternativeJREDirectory,
                                 String msg, List<InitializationProblem> initializationProblems,
                                 SourceSet sourceSet, String path, String scheme, Resources resources) throws IOException, URISyntaxException {
        Throwable throwable = null;
        assert path != null && !path.isBlank();
        switch (scheme) {
            case JAR_WITH_PATH -> {
                URL jarUrl = resources.findJarInClassPath(path);
                if (jarUrl == null) {
                    String msgString = msg + " part '" + sourceSet.uri() + "': jar not found";
                    LOGGER.warn(msgString);
                    initializationProblems.add(new InitializationProblem(msgString, null));
                } else {
                    addJar(resources, path, jarUrl, sourceSet);
                }
            }
            case "jmod" -> {
                try {
                    URL url = ResourcesImpl.constructJModURL(path, alternativeJREDirectory);
                    FingerPrint fingerPrint = makeFingerPrint(url);
                    sourceSet.setFingerPrint(fingerPrint);
                    int entries = resources.addJmod(new SourceFile(path, url.toURI(), sourceSet, null));
                    LOGGER.debug("Added {} entries for jmod {}", entries, path);
                } catch (IOException e) {
                    throwable = e;
                }
            }
            case TEST_PROTOCOL -> resources.addTestProtocol(new SourceFile(path, sourceSet.uri(), sourceSet,
                    null));
            case "file" -> {
                File file = toAbsoluteFile(workingDirectory, path);
                if (path.endsWith(".jar")) {
                    try {
                        // "jar:file:build/libs/equivalent.jar!/"
                        URL jarUrl = URI.create("jar:file:" + file.getPath() + "!/").toURL();
                        addJar(resources, path, jarUrl, sourceSet);
                    } catch (IOException e) {
                        throwable = e;
                    }
                } else {
                    if (file.isDirectory()) {
                        LOGGER.info("Adding {} to {}", file.getAbsolutePath(), msg);
                        resources.addDirectoryFromFileSystem(file, sourceSet);
                    } else {
                        String msgString = msg + " part '" + path + "' is not a .jar file, and not a directory: ignored";
                        LOGGER.warn(msgString);
                        initializationProblems.add(new InitializationProblem(msgString, null));
                    }
                }
            }
            case null, default -> throw new UnsupportedOperationException("Unknown URI scheme " + scheme);
        }
        if (throwable != null) {
            String msgString = msg + " part '" + path + "' ignored: " + throwable.getMessage();
            LOGGER.warn(msgString);
            initializationProblems.add(new InitializationProblem(msgString, throwable));
        }
    }

    private File toAbsoluteFile(Path workingDirectory, String path) {
        File file = new File(path);
        if (file.isAbsolute()) return file;
        return new File(workingDirectory.toFile(), path);
    }

    private void addJar(Resources resources, String part, URL jarUrl, SourceSet sourceSet) throws IOException, URISyntaxException {
        FingerPrint fingerPrint = makeFingerPrint(jarUrl);
        sourceSet.setFingerPrint(fingerPrint);
        int entries = resources.addJar(new SourceFile(part, jarUrl.toURI(), sourceSet, null));
        LOGGER.debug("Added {} entries for jar {}", entries, part);
    }

    private static final Pattern JAR_URL_PATTERN = Pattern.compile("jar:(file:.+)!/");

    private FingerPrint makeFingerPrint(URL jarUrl) throws URISyntaxException, IOException {
        if (computeFingerPrints) {
            try {
                Matcher m = JAR_URL_PATTERN.matcher(jarUrl.toString());
                if (m.matches()) {
                    Path path = Path.of(new URI(m.group(1)));
                    byte[] bytes = Files.readAllBytes(path);
                    return MD5FingerPrint.compute(bytes);
                } else {
                    throw new UnsupportedOperationException("? " + jarUrl);
                }
            } catch (RuntimeException re) {
                LOGGER.error("Caught exception trying to compute fingerprint of {}", jarUrl);
                throw re;
            }
        }
        return MD5FingerPrint.NO_FINGERPRINT;
    }

    // used for testing
    @Override
    public TypeInfo parse(String input, ParseOptions parseOptions) {
        return parseReturnAll(input, parseOptions).getFirst();
    }

    @Override
    public TypeInfo parse(String input) {
        return parseReturnAll(input, FAIL_FAST).getFirst();
    }

    @Override
    public List<TypeInfo> parseReturnAll(String input, ParseOptions parseOptions) {
        Summary failFastSummary = new SummaryImpl(true);
        try {
            URI uri = new URI("input");
            SourceSet dummy = new SourceSetImpl("test", List.of(), URI.create("file:doesNotExist"),
                    StandardCharsets.UTF_8, false, false, false, false,
                    false, Set.of(), Set.of());
            SourceFile sourceFile = new SourceFile("input", uri, dummy, MD5FingerPrint.compute(input));
            return internalParseSingleInput(failFastSummary, sourceFile, () -> {
                JavaParser parser = new JavaParser(input);
                parser.setParserTolerant(false);
                return parser;
            }, parseOptions);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // not the primary endpoint!
    private List<TypeInfo> internalParseSingleInput(Summary summary,
                                                    SourceFile sourceFile,
                                                    Supplier<JavaParser> parser,
                                                    ParseOptions parseOptions) {
        Resolver resolver = new ResolverImpl(runtime.computeMethodOverrides(), new ParseHelperImpl(runtime),
                parseOptions.parallel());
        TypeContextImpl typeContext = new TypeContextImpl(runtime, compiledTypesManager, false);
        //TODO  allowCreationOfStubTypes); code in TypeContextImpl needs improving
        Context rootContext = ContextImpl.create(runtime, compiledTypesManager, summary, resolver, typeContext,
                parseOptions.detailedSources(), parseOptions.lombok());
        ScanCompilationUnit scanCompilationUnit = new ScanCompilationUnit(summary, runtime);

        ScanCompilationUnit.ScanResult sr = scanCompilationUnit.scan(sourceFile.uri(), sourceFile.sourceSet(),
                sourceFile.fingerPrint(), parser.get().CompilationUnit(),
                parseOptions.detailedSources());
        sr.sourceTypes().forEach((_, typeInfo) -> compiledTypesManager.addTypeInfo(sourceFile, typeInfo));
        CompilationUnit cu = sr.compilationUnit();

        ParseCompilationUnit parseCompilationUnit = new ParseCompilationUnit(rootContext);
        List<Either<TypeInfo, ParseTypeDeclaration.DelayedParsingInformation>> types
                = parseCompilationUnit.parse(cu, parser.get().CompilationUnit());
        computeSingleAbstractMethods(sr.sourceTypes().values(), parseOptions.parallel());
        rootContext.resolver().resolve(true);
        return types.stream().map(Either::getLeft).toList();
    }

    @Override
    public Summary parse(URI uri, SourceSet sourceSet, ParseOptions parseOptions) {
        Summary summary = new SummaryImpl(true); // once stable, change to false
        try (InputStreamReader isr = makeInputStreamReader(uri, StandardCharsets.UTF_8); StringWriter sw = new StringWriter()) {
            isr.transferTo(sw);
            String sourceCode = sw.toString();
            SourceFile sourceFile = new SourceFile(uri.toString(), uri, sourceSet, MD5FingerPrint.compute(sourceCode));
            internalParseSingleInput(summary, sourceFile, () -> {
                JavaParser parser = new JavaParser(sourceCode);
                parser.setParserTolerant(false);
                return parser;
            }, parseOptions);
        } catch (IOException io) {
            LOGGER.error("Caught IO exception", io);

            summary.addParseException(new Summary.ParseException(uri, uri, io.getMessage(), io));
        }
        return summary;
    }

    private record SourceFileCompilationUnit(SourceFile sourceFile,
                                             org.parsers.java.ast.CompilationUnit cu,
                                             CompilationUnit parsedCu) {
    }

    private InputStreamReader makeInputStreamReader(URI uri, Charset charset) throws IOException {
        return new InputStreamReader(uri.toURL().openStream(), charset);
    }

    @Override
    public Summary parse(ParseOptions parseOptions) {
        return parse(Map.of(), parseOptions);
    }

    @Override
    public Summary parse(Map<String, String> sourcesByTestProtocolURIString, ParseOptions parseOptions) {
        Summary summary = new SummaryImpl(parseOptions.failFast()); // once stable, change to false
        Resolver resolver = new ResolverImpl(runtime.computeMethodOverrides(), new ParseHelperImpl(runtime),
                parseOptions.parallel());

        TypeContextImpl typeContext = new TypeContextImpl(runtime, compiledTypesManager, false);
        // TODO allowCreationOfStubTypes); would be better, but code in type context is not ready
        Context rootContext = ContextImpl.create(runtime, compiledTypesManager, summary, resolver, typeContext,
                parseOptions.detailedSources(), parseOptions.lombok());

        // PHASE 1: scanning all the types, call CongoCC parser

        Invalidated invalidated = parseOptions.invalidated();

        Map<SourceFile, String> sourceFilesToParse = new ConcurrentHashMap<>();
        Map<TypeInfo, Integer> typesToRewire = new ConcurrentHashMap<>();
        AtomicInteger count = new AtomicInteger();
        Stream<Map.Entry<SourceFile, List<TypeInfo>>> stream1 = this.sourceFiles.entrySet().stream();
        Stream<Map.Entry<SourceFile, List<TypeInfo>>> parallelStream1 = parseOptions.parallel()
                ? stream1.parallel()
                : stream1.sorted(Comparator.comparing(e -> e.getKey().uri()));
        parallelStream1.forEach(e -> {
            SourceFile sf = e.getKey();
            List<TypeInfo> typeInfos = e.getValue();
            summary.ensureSourceSet(sf.sourceSet());
            // TODO if there are multiple primary types here, and only one is invalid, we must make sure that
            //   all the descendants of the other must be rewired. This is an edge case.
            if (typeInfos.isEmpty() || typeInfos.stream().anyMatch(ti -> invalidated.apply(ti) == INVALID)) {
                typeInfos.forEach(compiledTypesManager::invalidate);
                //noinspection ALL
                String sourceCode = loadSource(sf, sourcesByTestProtocolURIString,
                        Objects.requireNonNullElse(sf.sourceSet().sourceEncoding(), StandardCharsets.UTF_8),
                        ioe -> new Summary.ParseException(sf.uri(), sf.uri(), ioe.getMessage(), ioe));
                if (sourceCode != null) {
                    FingerPrint fingerPrint = MD5FingerPrint.compute(sourceCode);
                    sourceFilesToParse.put(sf.withFingerprint(fingerPrint), sourceCode);
                }
            } else {
                for (TypeInfo ti : typeInfos) {
                    InvalidationState state = invalidated.apply(ti);
                    if (state == UNCHANGED) {
                        summary.addType(ti);
                    } else if (state == REWIRE) {
                        typesToRewire.merge(ti, 1, Integer::sum);
                    }
                }
            }
            count.incrementAndGet();
            TIMED_LOGGER.info("Phase 1: Loading sources/rewiring types, done {}", count);
        });

        count.set(0);
        Stream<Map.Entry<SourceFile, String>> stream2 = sourceFilesToParse.entrySet().stream();
        Stream<Map.Entry<SourceFile, String>> parallelStream2 = parseOptions.parallel()
                ? stream2.parallel()
                : stream2.sorted(Comparator.comparing(e -> e.getKey().uri()));
        List<SourceFileCompilationUnit> list = parallelStream2.map(entry -> {
            SourceFile sourceFile = entry.getKey();
            count.incrementAndGet();
            TIMED_LOGGER.info("Phase 2: Scanning compilation units, done {}", count);
            try {
                if (sourceFile.uri().toString().endsWith("module-info.java")) {
                    ModuleInfo moduleInfo = parseModuleInfo(entry.getValue(), sourceFile, rootContext);
                    if (moduleInfo == null) {
                        summary.addParseException(new Summary.ParseException(sourceFile.uri(), sourceFile.uri(),
                                "Expect ModularCompilationUnit", null));
                    } else {
                        summary.putSourceSetToModuleInfo(sourceFile.sourceSet(), moduleInfo);
                    }
                    return null;
                } else {
                    return parseSourceString(sourceFile, sourceFile.sourceSet(),
                            entry.getValue(), summary, parseOptions.detailedSources());
                }
            } catch (Exception parseException) {
                LOGGER.error("Caught parse exception in {}", sourceFile.uri());
                summary.addParseException(new Summary.ParseException(sourceFile.uri(), sourceFile.uri(), parseException.getMessage(),
                        parseException));
                return null;
            }
        }).filter(Objects::nonNull).toList();

        // PHASE 3: actual parsing of types, methods, fields
        count.set(0);
        InfoMap infoMap = invalidated == null ? null : runtime.newInfoMap(typesToRewire.keySet());
        Stream<SourceFileCompilationUnit> stream3;
        if (parseOptions.parallel()) {
            stream3 = list.parallelStream();
        } else {
            stream3 = list.stream(); // already sorted earlier
        }
        ParseCompilationUnit parseCompilationUnit = new ParseCompilationUnit(rootContext);
        List<DelayedCU> delayed = stream3.map(sfCu -> {
            try {
                LOGGER.debug("Parsing {}", sfCu.sourceFile().uri());
                List<Either<TypeInfo, ParseTypeDeclaration.DelayedParsingInformation>> types
                        = parseCompilationUnit.parse(sfCu.parsedCu, sfCu.cu);
                DelayedCU delayedCU = null;
                for (Either<TypeInfo, ParseTypeDeclaration.DelayedParsingInformation> either : types) {
                    if (either.isLeft()) {
                        TypeInfo ti = either.getLeft();
                        summary.addType(ti);
                    } else {
                        if (delayedCU == null) delayedCU = new DelayedCU(sfCu, new LinkedList<>());
                        delayedCU.delayed.add(either.getRight());
                    }
                }
                count.incrementAndGet();
                TIMED_LOGGER.info("Phase 3: parsing type/method/field declarations, done {}", count);
                if (delayedCU == null) {
                    sourceFilesPut(sfCu.sourceFile, types.stream().map(Either::getLeft).toList());
                    return null;
                }
                return delayedCU;
            } catch (ParseException parseException) {
                summary.addParseException(new Summary.ParseException(sfCu.sourceFile.uri(), sfCu.sourceFile.uri(),
                        parseException.getMessage(),
                        parseException));
                return null;
            }
        }).filter(Objects::nonNull).toList();

        AtomicInteger iteration = new AtomicInteger();
        while (true) {
            count.set(0);
            iteration.incrementAndGet();
            if (iteration.get() > 1000) throw new UnsupportedOperationException("Emergency brake");
            Stream<DelayedCU> stream = parseOptions.parallel() ? delayed.parallelStream() : delayed.stream();
            int todo = delayed.size();
            List<DelayedCU> newDelayed = stream.map(delayedCU -> {
                TIMED_LOGGER.info("Phase 3b, iteration {}: parsing delayed declarations, done {} of {}", iteration, count, todo);
                DelayedCU newDelayedCU = null;
                List<TypeInfo> successful = new ArrayList<>();
                for (ParseTypeDeclaration.DelayedParsingInformation d : delayedCU.delayed) {
                    Either<TypeInfo, ParseTypeDeclaration.DelayedParsingInformation> again = parseCompilationUnit.parseDelayedType(d);
                    if (again.isRight()) {
                        if (newDelayedCU == null) newDelayedCU = new DelayedCU(delayedCU.sfCu, new ArrayList<>());
                        newDelayedCU.delayed.add(again.getRight());
                    } else {
                        TypeInfo ti = again.getLeft();
                        summary.addType(ti);
                        successful.add(ti);
                    }
                }
                count.incrementAndGet();
                if (newDelayedCU == null) {
                    sourceFilesPut(delayedCU.sfCu.sourceFile, List.copyOf(successful));
                    return null;
                } else {
                    return newDelayedCU;
                }
            }).filter(Objects::nonNull).toList();
            if (newDelayed.isEmpty()) break;
            delayed = newDelayed;
        }

        computeSingleAbstractMethods(summary.types(), parseOptions.parallel());

        resolveModuleInfo(summary);

        if (infoMap != null) {
            Set<TypeInfo> rewired = infoMap.rewireAll();
           // FIXME rewiring, implement!! rewired.forEach(compiledTypesManager::set);
            rewired.forEach(summary::addType);
        }

        // PHASE 3: resolving: content of methods, field initializers

        rootContext.resolver().resolve(true);
        return summary;
    }

    private void computeSingleAbstractMethods(Collection<TypeInfo> types, boolean parallel) {
        AtomicInteger count = new AtomicInteger();
        ComputeMethodOverrides cmo = runtime.computeMethodOverrides();
        int todo3c = types.size();
        Stream<TypeInfo> typeInfoStream = parallel ? types.parallelStream() : types.stream();
        typeInfoStream.flatMap(TypeInfo::recursiveSubTypeStream).forEach(ti -> {
            if (!ti.hasBeenInspected()) {
                MethodInfo sam = cmo.computeFunctionalInterface(ti);
                ti.builder().setSingleAbstractMethod(sam);
            } // else: package-info
            TIMED_LOGGER.info("Phase 3c, setting single abstract method, done {} of {}", count, todo3c);
            count.incrementAndGet();
        });
    }

    private final Object sourceFilesLock = new Object();

    private void sourceFilesPut(SourceFile sourceFile, List<TypeInfo> list) {
        synchronized (sourceFilesLock) {
            sourceFiles.put(sourceFile, list);
        }
    }

    private record DelayedCU(SourceFileCompilationUnit sfCu,
                             List<ParseTypeDeclaration.DelayedParsingInformation> delayed) {
    }

    private void resolveModuleInfo(Summary summary) {
        for (Map.Entry<SourceSet, ModuleInfo> entry : summary.sourceSetToModuleInfoMap().entrySet()) {
            SourceSet sourceSet = entry.getKey();
            ModuleInfo moduleInfo = entry.getValue();
            for (ModuleInfo.Uses uses : moduleInfo.uses()) {
                TypeInfo resolved = compiledTypesManager.get(uses.api(), sourceSet);
                if (resolved != null) uses.setApiResolved(resolved);
            }
            for (ModuleInfo.Provides provides : moduleInfo.provides()) {
                TypeInfo r0 = compiledTypesManager.get(provides.api(), sourceSet);
                if (r0 != null) provides.setApiResolved(r0);
                TypeInfo r1 = compiledTypesManager.get(provides.implementation(), sourceSet);
                if (r1 != null) provides.setImplementationResolved(r1);
            }
        }
    }

    // public for testing, not in API
    public ModuleInfo parseModuleInfo(String javaSource, SourceFile sourceFile, Context rootContext) {
        JavaParser parser = new JavaParser(javaSource);
        parser.setParserTolerant(false);
        parser.ModularCompilationUnit();
        Node root = parser.rootNode();
        if (root instanceof ModularCompilationUnit mcu) {
            CompilationUnit compilationUnit = runtime.newCompilationUnitBuilder()
                    .setURI(sourceFile.uri())
                    .setSourceSet(sourceFile.sourceSet()).build();
            return new ParseModuleInfo(runtime, null).parse(mcu, compilationUnit, rootContext);
        }
        return null;
    }

    private String loadSource(SourceFile sourceFile,
                              Map<String, String> sourcesByTestProtocolURIString,
                              Charset sourceEncoding,
                              Consumer<IOException> summary) {
        String uriString = sourceFile.uri().toString();
        if (uriString.startsWith(TEST_PROTOCOL_PREFIX)) {
            return sourcesByTestProtocolURIString.get(uriString);
        }
        try (InputStreamReader isr = makeInputStreamReader(sourceFile.uri(), sourceEncoding); StringWriter sw = new StringWriter()) {
            isr.transferTo(sw);
            return sw.toString();
        } catch (IOException io) {
            LOGGER.error("Caught IO exception", io);
            summary.accept(io);
            return null;
        }
    }

    private SourceFileCompilationUnit parseSourceString(SourceFile sourceFile, SourceSet sourceSet, String sourceCode,
                                                        Summary summary, boolean addDetailedSources) {
        assert sourceCode != null;
        JavaParser parser = new JavaParser(sourceCode);
        parser.setParserTolerant(false);

        ScanCompilationUnit scanCompilationUnit = new ScanCompilationUnit(summary, runtime);
        org.parsers.java.ast.CompilationUnit cu = parser.CompilationUnit();
        LOGGER.debug("Scanning {}", sourceFile.uri());
        FingerPrint fingerPrint = sourceFile.fingerPrint() == null
                ? MD5FingerPrint.compute(sourceCode) : sourceFile.fingerPrint();
        ScanCompilationUnit.ScanResult sr = scanCompilationUnit.scan(sourceFile.uri(), sourceSet, fingerPrint, cu,
                addDetailedSources);
        sr.sourceTypes().values().forEach(ti -> compiledTypesManager.addTypeInfo(sourceFile, ti));
        CompilationUnit compilationUnit = sr.compilationUnit();
        return new SourceFileCompilationUnit(sourceFile, cu, compilationUnit);
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
    public ImportComputer importComputer(int minStar, SourceSet sourceSetOfRequest) {
        return runtime.newImportComputer(minStar, packageName ->
                compiledTypesManager.primaryTypesInPackageEnsureLoaded(packageName, sourceSetOfRequest));
    }

    @Override
    public String print2(TypeInfo typeInfo) {
        return print2(typeInfo, null, importComputer(4, typeInfo.compilationUnit().sourceSet()));
    }

    @Override
    public String print2(TypeInfo typeInfo, Qualification.Decorator decorator, ImportComputer importComputer) {
        OutputBuilder ob = runtime.newTypePrinter(typeInfo, true).print(importComputer,
                runtime.qualificationQualifyFromPrimaryType(decorator), true);
        Formatter formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        return formatter.write(ob);
    }
}

