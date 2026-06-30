package org.e2immu.language.java.openjdk;

import com.sun.source.tree.CompilationUnitTree;
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
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
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

    public record Result(List<TypeInfo> primaryTypes, List<ModuleInfo> modules, List<TypeInfo> preloads) {
    }

    public ScanCompilationUnits(Runtime runtime,
                                InputConfiguration inputConfiguration,
                                JavacTask task,
                                SourceSet sourceSet,
                                InfoByFqn infoByFqn,
                                boolean detailedSources,
                                MaddiDiagnosticCollector diagnosticCollector,
                                List<String> packagesToPreload,
                                org.e2immu.language.inspection.api.resource.ParameterNameIndex parameterNameIndex) {
        this.runtime = runtime;
        this.diagnosticCollector = diagnosticCollector;
        this.task = task;
        this.sourceSet = sourceSet;
        this.detailedSources = detailedSources;
        this.packagesToPreload = packagesToPreload;

        trees = Trees.instance(task);
        sourcePositions = trees.getSourcePositions();
        types = Types.instance(((BasicJavacTask) task).getContext());
        Elements elements = task.getElements();
        computeMethodOverrides = new ComputeMethodOverrides(types, elements);
        flagHelper = new FlagHelper(runtime);
        classSymbolScanner = new ClassSymbolScanner(runtime, inputConfiguration, infoByFqn, sourceSet,
                flagHelper, types, elements, diagnosticCollector, parameterNameIndex);
        resolveJavaDoc = new ResolveJavaDoc(runtime, classSymbolScanner);
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
        if (detailedSources) {
            LOGGER.info("Collected {} class symbols for source set {}", topLevelClassSymbols.size(), sourceSet.name());
            int nThreads = java.lang.Runtime.getRuntime().availableProcessors();
            // Extract everything the parallel task 1 needs from javac (source content, is-module) on THIS (main)
            // thread, BEFORE launching the pipeline. javac's trees and file manager are not thread-safe, and task 1
            // runs concurrently with task 2's javac work (symbol resolution etc.) on the shared Context; touching
            // javac from the task-1 worker threads corrupts that state (e.g. JCCompilationUnit.starImportScope set
            // to null). The congocc-based SourceCodeScan itself is javac-free, so once fed plain content it is safe.
            Map<CompilationUnitTree, CharSequence> contentByUnit = new IdentityHashMap<>();
            for (CompilationUnitTree unit : units) {
                contentByUnit.put(unit, unit.getSourceFile().getCharContent(false));
            }
            try (ExecutorService task1Executor = Executors.newFixedThreadPool(nThreads);
                 ExecutorService task2Executor = Executors.newSingleThreadExecutor()) {

                CompletableFuture<Void> previousTask2 = CompletableFuture.completedFuture(null);
                AtomicInteger done = new AtomicInteger();

                for (CompilationUnitTree unit : units) {
                    boolean isModule = unit.getModule() != null;
                    CharSequence content = contentByUnit.get(unit);
                    // Task 1 fires immediately, in parallel; it only touches the (javac-free) congocc parser
                    CompletableFuture<SourceCodeScan.Result> task1 = CompletableFuture.supplyAsync(
                            () -> new SourceCodeScan(runtime).go(content, isModule), task1Executor);

                    // Task 2 waits for: (a) this item's task 1 (the scanResult), AND (b) previous item's task 2 (sequence!)
                    previousTask2 = previousTask2.thenCombineAsync(task1, (_, scanResult) -> {
                                singleCompilationUnit(unit, scanResult, primaryTypes, modules, topLevelClassSymbols);
                                return null; // Void
                            }, task2Executor)
                            .thenAccept(_ -> TIMED_LOGGER.info("Done {}", done.incrementAndGet()));
                }

                previousTask2.join(); // wait for everything to finish
                LOGGER.info("Start scanning javaDocs, committing {} primary types", primaryTypes.size());
            }
        } else {
            int done = 0;
            for (CompilationUnitTree unit : units) {
                singleCompilationUnit(unit, null, primaryTypes, modules, topLevelClassSymbols);
                TIMED_LOGGER.info("Done {}", ++done);
            }
        }
        for (TypeInfo primaryType : primaryTypes) {
            if (!primaryType.hasBeenInspected()) {
                scanJavaDocsAndCommit(primaryType);
            }
        }
        return new Result(List.copyOf(primaryTypes), List.copyOf(modules), List.copyOf(preloads));
    }


    private void singleCompilationUnit(CompilationUnitTree unit,
                                       SourceCodeScan.Result scanResult,
                                       List<TypeInfo> primaryTypes,
                                       List<ModuleInfo> modules,
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

        LineMap lineMap = unit.getLineMap();
        DocTrees docTrees = DocTrees.instance(task);
        ScanCompilationUnit scanCompilationUnit = new ScanCompilationUnit(runtime, classSymbolScanner,
                compilationUnitBuilder, unit, trees, sourcePositions, lineMap, task.getElements(), types,
                docTrees, scanResult, computeMethodOverrides, flagHelper, classSymbolScanner,
                topLevelClassSymbols);
        try {
            scanCompilationUnit.scan(unit, null);
        } catch (RuntimeException | AssertionError e) {
            LOGGER.error("Caught exception in source set {}", sourceSet.name());
            throw e;
        }
        List<TypeInfo> scanned = scanCompilationUnit.types();
        primaryTypes.addAll(scanned);
        modules.addAll(scanCompilationUnit.modules());
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
}
