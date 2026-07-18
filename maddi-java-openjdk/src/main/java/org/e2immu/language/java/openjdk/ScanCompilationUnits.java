package org.e2immu.language.java.openjdk;

import com.sun.source.tree.CompilationUnitTree;

import java.net.URI;
import com.sun.source.tree.LineMap;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.resource.MD5FingerPrint;
import org.e2immu.language.inspection.resource.InfoByFqn;
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ScanCompilationUnits {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanCompilationUnits.class);
    private static final TimedLogger TIMED_LOGGER = new TimedLogger(LOGGER, 1000);

    private final Runtime runtime;
    private final MaddiDiagnosticCollector diagnosticCollector;
    private final JavacTask task;
    private final SourceSet sourceSet;
    private final Trees trees;
    private final SourcePositions sourcePositions;
    private final Types types;
    private final ComputeMethodOverrides computeMethodOverrides;
    private final FlagHelper flagHelper;
    private final ClassSymbolScanner classSymbolScanner;
    private final boolean detailedSources;
    private final ResolveJavaDoc resolveJavaDoc;
    private final List<String> packagesToPreload;
    private final boolean computeFingerPrints;

    public record Result(List<TypeInfo> primaryTypes, List<ModuleInfo> modules, List<TypeInfo> preloads,
                         List<CompilationUnitFailure> failures) {
    }

    /**
     * A single compilation unit that could not be scanned. When errors are ignored (accumulate mode), such a unit
     * is dropped and recorded here rather than aborting the whole source set — so a run over a partial classpath
     * reports <em>all</em> problem files and preps over the rest. {@code tolerable} is true when the cause is an
     * {@link UnresolvedSymbolException} (an expected partial-classpath miss → a warning); otherwise it is a genuine
     * failure → an error. The caller surfaces these into the {@code Summary}.
     */
    public record CompilationUnitFailure(URI uri, String detail, boolean tolerable, Throwable cause) {
    }

    public ScanCompilationUnits(Runtime runtime,
                                InputConfiguration inputConfiguration,
                                JavacTask task,
                                SourceSet sourceSet,
                                InfoByFqn infoByFqn,
                                boolean detailedSources,
                                MaddiDiagnosticCollector diagnosticCollector,
                                List<String> packagesToPreload,
                                org.e2immu.language.inspection.api.resource.ParameterNameIndex parameterNameIndex,
                                boolean jdkInternals,
                                boolean computeFingerPrints,
                                boolean syntheticListField) {
        this.runtime = runtime;
        this.diagnosticCollector = diagnosticCollector;
        this.task = task;
        this.sourceSet = sourceSet;
        this.detailedSources = detailedSources;
        this.packagesToPreload = packagesToPreload;
        this.computeFingerPrints = computeFingerPrints;

        trees = Trees.instance(task);
        sourcePositions = trees.getSourcePositions();
        types = Types.instance(((BasicJavacTask) task).getContext());
        assert nameTableIsNotShared(task) : "-XDuseUnsharedTable=true passed but NOT honored: javac is using "
                + "the process-wide SharedNameTable freelist (the 'tree.starImportScope is null' family)";
        Elements elements = task.getElements();
        computeMethodOverrides = new ComputeMethodOverrides(types, elements);
        flagHelper = new FlagHelper(runtime);
        classSymbolScanner = new ClassSymbolScanner(runtime, inputConfiguration, infoByFqn, sourceSet,
                flagHelper, types, elements, diagnosticCollector, parameterNameIndex, jdkInternals, syntheticListField);
        resolveJavaDoc = new ResolveJavaDoc(runtime, classSymbolScanner);
    }

    // -XDuseUnsharedTable=true must be HONORED, not merely passed (task #40 lead): with the shared table,
    // name bytes come from a process-wide freelist and repeated parsing intermittently corrupts. Assert-only
    // diagnostic; tolerant of future javac internals changes.
    private static boolean nameTableIsNotShared(JavacTask task) {
        try {
            com.sun.tools.javac.util.Names names =
                    com.sun.tools.javac.util.Names.instance(((BasicJavacTask) task).getContext());
            return !"SharedNameTable".equals(names.table.getClass().getSimpleName());
        } catch (RuntimeException e) {
            return true;
        }
    }

    public Result scan() throws IOException {
        Iterable<? extends CompilationUnitTree> units = task.parse();
        task.analyze();

        for (MaddiDiagnosticCollector.MaddiDiagnostic md : diagnosticCollector.diagnostics()) {
            if (md.diagnosticKind() == MaddiDiagnosticCollector.DiagnosticKind.ERROR) {
                LOGGER.info("Error found in {} at line {}, col {}: {}", md.path(), md.line(), md.col(), md.msg());
            }
        }
        if (diagnosticCollector.isHalt()) throw new CompilationProblems();

        List<TypeInfo> primaryTypes = new ArrayList<>();
        List<ModuleInfo> modules = new ArrayList<>();
        // compilation units dropped by fault isolation (accumulate mode), and their recorded failures
        List<CompilationUnitFailure> failures = new ArrayList<>();

        // only index in the first pass; in the second pass, all predefined objects will be present
        List<TypeInfo> preloads;
        if (!runtime.objectTypeInfo().hasBeenInspected()) {
            preloads = new LinkedList<>(indexJavaLangForJavaDocParsing());
            for (String modulePackage : packagesToPreload) {
                int sep = modulePackage.indexOf("::");
                if (sep < 0) {
                    preloads.addAll(preloadClassPath(modulePackage));
                } else {
                    String module = modulePackage.substring(0, sep);
                    String packageName = modulePackage.substring(sep + 2);
                    preloads.addAll(preloadJdk(module, packageName));
                }
            }
        } else {
            preloads = List.of();
        }
        IdentityHashMap<Symbol.ClassSymbol, Boolean> topLevelClassSymbols
                = StreamSupport.stream(units.spliterator(), false)
                .flatMap(unit -> unit.getTypeDecls().stream()
                        .filter(td -> td instanceof JCTree.JCClassDecl)
                        .map(td -> ((JCTree.JCClassDecl) td).sym))
                .collect(Collectors.toMap(cs -> cs, _ -> true, (_, _) -> {
                            throw new RuntimeException();
                        },
                        IdentityHashMap::new));
        classSymbolScanner.setTopLevelClassSymbolsOfSources(topLevelClassSymbols);

        // Task 1 (detailed sources only): compute each unit's congocc scan result. It is javac-free, hence safe to
        // run in parallel; we barrier on all results before touching javac again. Source content is extracted on
        // THIS (main) thread first -- javac's file manager is not thread-safe.
        Map<CompilationUnitTree, SourceCodeScan.Result> scanResults = new IdentityHashMap<>();
        if (detailedSources) {
            LOGGER.info("Collected {} class symbols for source set {}", topLevelClassSymbols.size(), sourceSet.name());
            Map<CompilationUnitTree, CharSequence> contentByUnit = new IdentityHashMap<>();
            for (CompilationUnitTree unit : units) {
                contentByUnit.put(unit, unit.getSourceFile().getCharContent(false));
            }
            int nThreads = java.lang.Runtime.getRuntime().availableProcessors();
            try (ExecutorService task1Executor = Executors.newFixedThreadPool(nThreads)) {
                Map<CompilationUnitTree, Future<SourceCodeScan.Result>> futures = new IdentityHashMap<>();
                for (CompilationUnitTree unit : units) {
                    boolean isModule = unit.getModule() != null;
                    CharSequence content = contentByUnit.get(unit);
                    futures.put(unit, task1Executor.submit(() -> new SourceCodeScan(runtime).go(content, isModule)));
                }
                for (Map.Entry<CompilationUnitTree, Future<SourceCodeScan.Result>> e : futures.entrySet()) {
                    try {
                        scanResults.put(e.getKey(), e.getValue().get());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    } catch (ExecutionException ee) {
                        Throwable cause = ee.getCause() == null ? ee : ee.getCause();
                        // the congocc pre-scan failed for this unit (e.g. a grammar gap on a construct javac
                        // accepts, such as 'super(new X(){...})'). It is auxiliary -- it only supplies source
                        // positions for detailed sources. fail-fast: rethrow. accumulate: keep the unit with no
                        // scan result (all scanResult reads are null-guarded, so detailed sources simply degrade
                        // for this one file) rather than dropping a file javac parsed successfully.
                        if (!diagnosticCollector.ignoreErrors()) {
                            throw cause instanceof RuntimeException re ? re : new RuntimeException(cause);
                        }
                        CompilationUnitTree failedUnit = e.getKey();
                        LOGGER.warn("Detailed-source pre-scan failed for {}; continuing without detailed sources: {}",
                                failedUnit.getSourceFile() == null ? "?" : failedUnit.getSourceFile().toUri(),
                                cause.toString());
                    }
                }
            }
        }

        // Phase 1: build every unit's CompilationUnit and register it with the class-symbol scanner, BEFORE any
        // body is scanned. This is the fix for the intermittent null-source CompilationUnit: a cross-file reference
        // (e.g. a.A naming b.B before b.B's own source is scanned) now lazily loads b.B onto its real source CU,
        // instead of the class-symbol scanner minting a source-less twin that the later source scan would reuse.
        List<ScanCompilationUnit> scanners = new ArrayList<>();
        List<CompilationUnitTree> unitList = new ArrayList<>();
        for (CompilationUnitTree unit : units) {
            try {
                ScanCompilationUnit scu = createScanner(unit, scanResults.get(unit), topLevelClassSymbols);
                scu.buildCompilationUnit(unit);
                classSymbolScanner.registerSourceCompilationUnit(scu.currentCompilationUnit());
                scanners.add(scu);
                unitList.add(unit);
            } catch (RuntimeException | AssertionError e) {
                // fail-fast: preserve the historical abort. accumulate: drop this unit and keep going, so one
                // unresolved reference (partial classpath) no longer kills the whole source set.
                if (!diagnosticCollector.ignoreErrors()) {
                    LOGGER.error("Caught exception (compilation-unit build) in source set {}", sourceSet.name());
                    throw e;
                }
                recordFailure(failures, unit, e);
            }
        }

        // Phase 2: scan the bodies. Serial -- it touches shared javac state (symbol resolution, trees).
        int done = 0;
        for (int i = 0; i < scanners.size(); i++) {
            ScanCompilationUnit scu = scanners.get(i);
            try {
                scu.scanBodies(unitList.get(i));
                primaryTypes.addAll(scu.types());
                modules.addAll(scu.modules());
            } catch (RuntimeException | AssertionError e) {
                // fail-fast: preserve the historical abort. accumulate: drop this unit's (partial) types and keep
                // going, so the run reports every problem file instead of dying on the first.
                if (!diagnosticCollector.ignoreErrors()) {
                    LOGGER.error("Caught exception in source set {}", sourceSet.name());
                    throw e;
                }
                recordFailure(failures, unitList.get(i), e);
            }
            TIMED_LOGGER.info("Done {}", ++done);
        }
        LOGGER.info("Start scanning javaDocs, committing {} primary types", primaryTypes.size());

        Iterator<TypeInfo> ptIt = primaryTypes.iterator();
        while (ptIt.hasNext()) {
            TypeInfo primaryType = ptIt.next();
            if (!primaryType.hasBeenInspected()) {
                try {
                    scanJavaDocsAndCommit(primaryType);
                } catch (RuntimeException | AssertionError e) {
                    // a type we could not commit (e.g. a malformed supertype set after dropped units). fail-fast:
                    // rethrow; accumulate: drop the type and keep going, so it is not returned to the analysis.
                    if (!diagnosticCollector.ignoreErrors()) throw e;
                    URI uri;
                    try {
                        uri = primaryType.compilationUnit().uri();
                    } catch (RuntimeException ignore) {
                        uri = null;
                    }
                    addFailure(failures, uri, e);
                    ptIt.remove();
                }
            }
        }
        return new Result(List.copyOf(primaryTypes), List.copyOf(modules), List.copyOf(preloads),
                List.copyOf(failures));
    }

    /**
     * The MD5 of a unit's source text, so that a later run can tell whether the file changed (see
     * {@code JavaInspector.reloadSources}). Computed over exactly what javac read, which is what the in-house
     * inspector fingerprints too, so the two agree on a given file.
     * <p>
     * Called from the serial phase-1 loop only: javac's file manager is not thread-safe.
     * {@link MD5FingerPrint#NO_FINGERPRINT} when the content cannot be read — reloadSources then treats the file as
     * changed, which is the safe direction (re-parse rather than silently keep a stale type).
     */
    private FingerPrint fingerPrintOf(CompilationUnitTree unit) {
        try {
            CharSequence content = unit.getSourceFile().getCharContent(false);
            return content == null ? MD5FingerPrint.NO_FINGERPRINT : MD5FingerPrint.compute(content.toString());
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot compute fingerprint of {}: {}", unit.getSourceFile().toUri(), e.toString());
            return MD5FingerPrint.NO_FINGERPRINT;
        }
    }

    /** Record a dropped compilation unit; an {@link UnresolvedSymbolException} cause is tolerable (→ warning). */
    private static void recordFailure(List<CompilationUnitFailure> failures, CompilationUnitTree unit, Throwable e) {
        addFailure(failures, unit.getSourceFile() == null ? null : unit.getSourceFile().toUri(), e);
    }

    private static void addFailure(List<CompilationUnitFailure> failures, URI uri, Throwable e) {
        boolean tolerable = hasCause(e, UnresolvedSymbolException.class);
        String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        LOGGER.warn("Dropping compilation unit {} ({}): {}", uri, tolerable ? "unresolved symbol" : "error", detail);
        failures.add(new CompilationUnitFailure(uri, detail, tolerable, e));
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (type.isInstance(c)) return true;
        }
        return false;
    }

    // create the scanner for one unit (builder + construction). The constructor registers it as the current source
    // provider; it does not scan anything itself -- callers drive registerPrimaries() then scanBodies().
    private ScanCompilationUnit createScanner(CompilationUnitTree unit,
                                              SourceCodeScan.Result scanResult,
                                              IdentityHashMap<Symbol.ClassSymbol, Boolean> topLevelClassSymbols) {
        boolean isModule = unit.getModule() != null;
        String packageName;
        if (isModule) {
            packageName = unit.getModule().getName().toString();
        } else if (unit.getPackage() != null) {
            packageName = unit.getPackageName().toString();
        } else {
            packageName = "";
        }
        CompilationUnit.Builder compilationUnitBuilder = runtime.newCompilationUnitBuilder()
                .setPackageName(packageName)
                .setURI(unit.getSourceFile().toUri())
                .setSourceSet(sourceSet);
        if (computeFingerPrints) {
            compilationUnitBuilder.setFingerPrint(fingerPrintOf(unit));
        }

        LineMap lineMap = unit.getLineMap();
        DocTrees docTrees = DocTrees.instance(task);
        return new ScanCompilationUnit(runtime, classSymbolScanner,
                compilationUnitBuilder, unit, trees, sourcePositions, lineMap, task.getElements(), types,
                docTrees, scanResult, computeMethodOverrides, flagHelper, classSymbolScanner,
                topLevelClassSymbols);
    }

    private void scanJavaDocsAndCommit(TypeInfo typeInfo) {
        for (TypeInfo sub : typeInfo.subTypes()) {
            scanJavaDocsAndCommit(sub);
            // TODO javadoc inside lambdas/anonymous types? we should be able to do that efficiently
        }
        if (typeInfo.javaDoc() != null) {
            typeInfo.builder().setJavaDoc(resolveJavaDoc.resolve(typeInfo, null, typeInfo.javaDoc()));
        }
        typeInfo.builder().commit();
        for (MethodInfo methodInfo : typeInfo.constructorsAndMethods()) {
            if (methodInfo.javaDoc() != null) {
                methodInfo.builder().setJavaDoc(resolveJavaDoc.resolve(typeInfo, methodInfo, methodInfo.javaDoc()));
            }
            if (!methodInfo.hasBeenInspected()) {
                methodInfo.builder().commit();
            } // possible: sythetics
        }
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            if (!fieldInfo.hasBeenInspected()) {
                if (fieldInfo.initializer() == null) {
                    fieldInfo.builder().setInitializer(runtime.newEmptyExpression());
                }
                assert fieldInfo.access() != null : "Null access for " + fieldInfo;
                fieldInfo.builder().commit();
            }
        }
    }

    // for tests
    public ClassSymbolScanner classSymbolScanner() {
        return classSymbolScanner;
    }

    private List<TypeInfo> indexJavaLangForJavaDocParsing() throws IOException {
        JavaFileManager fm = ((BasicJavacTask) task).getContext().get(JavaFileManager.class);
        JavaFileManager.Location javaBase = fm.getLocationForModule(StandardLocation.SYSTEM_MODULES,
                "java.base");

        Iterable<JavaFileObject> files = fm.list(javaBase, "java.lang", Set.of(JavaFileObject.Kind.CLASS),
                false); // non-recursive — just java.lang, not subpackages
        Elements elements = task.getElements();
        Symbol.ClassSymbol objectCs = null;
        List<TypeInfo> list = new LinkedList<>(runtime.predefinedObjects());
        for (JavaFileObject file : files) {
            String binaryName = fm.inferBinaryName(javaBase, file);
            TypeElement te = elements.getTypeElement(binaryName);
            if (te instanceof Symbol.ClassSymbol cs) {
                try {
                    cs.complete();
                    if (cs.owner instanceof Symbol.PackageSymbol) {
                        TypeInfo inMap = classSymbolScanner.getType(binaryName);
                        if (inMap == null) {
                            TypeInfo ti = classSymbolScanner.lazilyLoadPrimaryTypeFromClassFile(cs);
                            list.add(ti);
                        } else {
                            list.add(inMap);
                        }
                    } // else: not a primary type, or already known
                    if ("java.lang.Object".equals(binaryName)) {
                        objectCs = cs;
                    }
                } catch (Symbol.CompletionFailure e) {
                    // ignore
                }
            }
        }
        assert objectCs != null;
        classSymbolScanner.loadType(objectCs, runtime.objectTypeInfo(), ClassSymbolScanner.LoadMode.LOAD_MEMBERS);
        assert runtime.objectTypeInfo().hasBeenInspected();
        LOGGER.info("Preloaded {}, loaded {}", javaBase, list.size());
        return list;
    }

    // preload a package from a named module on the system module path (the JDK)
    private List<TypeInfo> preloadJdk(String module, String packageName) throws IOException {
        JavaFileManager fm = ((BasicJavacTask) task).getContext().get(JavaFileManager.class);
        JavaFileManager.Location locationForModule =
                Objects.requireNonNull(fm.getLocationForModule(StandardLocation.SYSTEM_MODULES, module),
                        "Cannot find location for module '" + module + "', package '" + packageName + "'");
        return preload(locationForModule, packageName);
    }

    // preload a package from the classpath (including JARs); classpath types live in the unnamed module
    private List<TypeInfo> preloadClassPath(String packageName) throws IOException {
        return preload(StandardLocation.CLASS_PATH, packageName);
    }

    private List<TypeInfo> preload(JavaFileManager.Location location, String packageNameIn) throws IOException {
        boolean recurse = packageNameIn.endsWith(".");
        String packageName = recurse ? packageNameIn.substring(0, packageNameIn.length() - 1) : packageNameIn;
        JavaFileManager fm = ((BasicJavacTask) task).getContext().get(JavaFileManager.class);
        Iterable<JavaFileObject> files = fm.list(location, packageName, Set.of(JavaFileObject.Kind.CLASS), recurse);
        Elements elements = task.getElements();
        List<TypeInfo> list = new LinkedList<>();
        for (JavaFileObject file : files) {
            String binaryName = fm.inferBinaryName(location, file);
            if (binaryName == null) continue; // e.g. module-info.class, or a name that cannot be inferred
            TypeElement te = elements.getTypeElement(binaryName);
            if (te instanceof Symbol.ClassSymbol cs) {
                try {
                    cs.complete();
                    if (cs.owner instanceof Symbol.PackageSymbol) {
                        TypeInfo pt = classSymbolScanner.getType(binaryName);
                        if (pt == null) {
                            pt = classSymbolScanner.lazilyLoadPrimaryTypeFromClassFile(cs);
                            classSymbolScanner.loadType(cs, pt, ClassSymbolScanner.LoadMode.LOAD_MEMBERS);
                        } else {
                            classSymbolScanner.loadType(cs, pt, ClassSymbolScanner.LoadMode.COMPLETE);
                        }
                        if (!pt.hasBeenInspected()) {
                            pt.builder().commit();
                        }
                        list.add(pt);
                    }
                } catch (Symbol.CompletionFailure e) {
                    // ignore
                }
            }
        }
        LOGGER.info("Preloaded {}::{}, recurse? {}, loaded {}", location, packageName, recurse, list.size());
        return list;
    }

    /**
     * Load ONE compiled type by its fully-qualified (canonical) name on demand, reusing this scan's javac task
     * (whose {@code Elements} resolves+completes classpath symbols lazily, exactly as {@link #preload} does per
     * type). Backs the CompiledTypesManager's lazy {@code getOrLoad}, so a type first referenced by another
     * front-end (e.g. Kotlin) resolves to the same bytecode-authoritative TypeInfo. Returns null when the type's
     * module is not among the configured class-path parts, when it is not a primary (package-owned) type, or when
     * it fails to complete. Single-threaded, like all javac use here.
     */
    public TypeInfo loadCompiledTypeOrNull(String fullyQualifiedName) {
        try {
            TypeElement te = task.getElements().getTypeElement(fullyQualifiedName);
            if (!(te instanceof Symbol.ClassSymbol cs)) return null;
            cs.complete();
            /*
            javac resolves against the whole platform, so without this every JDK type would load, whatever the
            configured class path -- and callers that use a null to mean "this module is absent" would never see one.
            LoadAnalysisResults is one: it skips the analysis hints of a type that does not resolve, which is what
            keeps swing/awt/http hints out of a run that never asked for java.desktop.
            The check is before the nested-type branch on purpose: findModule walks up to the primary type, so a
            nested type's module is its top-level type's, and answering here saves the recursion.
             */
            if (!classSymbolScanner.moduleOnClassPath(cs)) return null;
            if (!(cs.owner instanceof Symbol.PackageSymbol)) {
                // a nested type (e.g. java.util.Map.Entry, io.codelaser...Try.TryData): it is loaded as part of its
                // enclosing type, not on its own. Load the top-level enclosing type -- which registers all nested
                // types -- then return the requested one by its (dotted) fully-qualified name.
                Symbol.ClassSymbol top = cs;
                while (top.owner instanceof Symbol.ClassSymbol enclosing) top = enclosing;
                if (!(top.owner instanceof Symbol.PackageSymbol)) return null; // local/anonymous: not addressable
                loadCompiledTypeOrNull(top.getQualifiedName().toString());
                return classSymbolScanner.getType(fullyQualifiedName);
            }
            TypeInfo pt = classSymbolScanner.getType(fullyQualifiedName);
            if (pt == null) {
                pt = classSymbolScanner.lazilyLoadPrimaryTypeFromClassFile(cs);
                classSymbolScanner.loadType(cs, pt, ClassSymbolScanner.LoadMode.LOAD_MEMBERS);
            } else {
                classSymbolScanner.loadType(cs, pt, ClassSymbolScanner.LoadMode.COMPLETE);
            }
            if (!pt.hasBeenInspected()) pt.builder().commit();
            return pt;
        } catch (Symbol.CompletionFailure e) {
            return null;
        }
    }
}
