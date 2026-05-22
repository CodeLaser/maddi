package org.e2immu.language.java.openjdk;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.*;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.*;
import java.io.IOException;
import java.util.*;

public class ScanCompilationUnits {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanCompilationUnits.class);

    private final Runtime runtime;
    private final SourceCodeScan sourceCodeScan;
    private final DiagnosticCollector<JavaFileObject> diagnostics;
    private final JavacTask task;
    private final SourceSet sourceSet;
    private final Trees trees;
    private final SourcePositions sourcePositions;
    private final Types types;
    private final ComputeMethodOverrides computeMethodOverrides;
    private final FlagHelper flagHelper;
    private final ClassSymbolScanner classSymbolScanner;
    private final boolean detailedSources;

    public ScanCompilationUnits(Runtime runtime,
                                InputConfiguration inputConfiguration,
                                JavacTask task,
                                SourceSet sourceSet,
                                Map<String, Info> previouslyLoaded,
                                boolean detailedSources,
                                DiagnosticCollector<JavaFileObject> diagnostics) {
        this.runtime = runtime;
        this.diagnostics = diagnostics;
        this.task = task;
        this.sourceSet = sourceSet;
        this.detailedSources = detailedSources;

        sourceCodeScan = new SourceCodeScan(runtime);
        trees = Trees.instance(task);
        sourcePositions = trees.getSourcePositions();
        types = Types.instance(((BasicJavacTask) task).getContext());
        Elements elements = task.getElements();
        computeMethodOverrides = new ComputeMethodOverrides(types, elements);
        flagHelper = new FlagHelper(runtime);
        classSymbolScanner = new ClassSymbolScanner(runtime, inputConfiguration, previouslyLoaded, sourceSet,
                flagHelper, types, elements);
    }

    public List<Info> scan() throws IOException {
        Iterable<? extends CompilationUnitTree> units = task.parse();
        task.analyze();

        if (diagnostics != null) {
            boolean haveErrors = false;
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    LOGGER.info("Error found in {} at line {}, col {}: {}",
                            d.getSource(),
                            d.getLineNumber(), d.getColumnNumber(), d.getMessage(Locale.getDefault()));
                    haveErrors = true;
                }
            }
            if (haveErrors) throw new CompilationProblems();
        }

        // this is an edge case: ensure that all java.lang types are known for reference resolution
        // in the ScanJavaDoc scanner.
        indexJavaLangForJavaDocParsing((BasicJavacTask) task);

        List<Info> primaryTypesAndModules = new ArrayList<>();
        for (CompilationUnitTree unit : units) {
            LOGGER.info("Compilation unit {}", unit.getSourceFile().getName());

            boolean isModule = unit.getModule() != null;
            String packageName;
            if (isModule) {
                packageName = unit.getModule().getName().toString();
            } else if (unit.getPackage() != null) {
                packageName = unit.getPackageName().toString();
            } else {
                packageName = "";
            }
            CompilationUnit compilationUnit = runtime.newCompilationUnitBuilder()
                    .setPackageName(packageName)
                    .setSourceSet(sourceSet)
                    .build();

            SourceCodeScan.Result scanResult;
            if (detailedSources) {
                CharSequence content = unit.getSourceFile().getCharContent(false);
                scanResult = sourceCodeScan.go(content, isModule);
            } else {
                scanResult = SourceCodeScan.EMPTY_RESULT;
            }


            LineMap lineMap = unit.getLineMap();
            DocTrees docTrees = DocTrees.instance(task);

            ScanCompilationUnit scanCompilationUnit = new ScanCompilationUnit(runtime, classSymbolScanner,
                    compilationUnit, unit, trees, sourcePositions, lineMap, task.getElements(), types,
                    docTrees, scanResult, computeMethodOverrides, flagHelper, classSymbolScanner);
            scanCompilationUnit.scan(unit, null);
            primaryTypesAndModules.addAll(scanCompilationUnit.types());
        }
        return List.copyOf(primaryTypesAndModules);
    }

    public void mergeIntoPreviouslyLoaded() {
        classSymbolScanner.mergeIntoPreviouslyLoaded();
    }


    // for tests
    public ClassSymbolScanner classSymbolScanner() {
        return classSymbolScanner;
    }

    private void indexJavaLangForJavaDocParsing(BasicJavacTask task) throws IOException {
        JavaFileManager fm = task.getContext().get(JavaFileManager.class);
        JavaFileManager.Location javaBase = fm.getLocationForModule(StandardLocation.SYSTEM_MODULES,
                "java.base");

        Iterable<JavaFileObject> files = fm.list(javaBase, "java.lang", Set.of(JavaFileObject.Kind.CLASS),
                false); // non-recursive — just java.lang, not subpackages
        Elements elements = task.getElements();
        for (JavaFileObject file : files) {
            String binaryName = fm.inferBinaryName(javaBase, file);
            TypeElement te = elements.getTypeElement(binaryName);
            if (te instanceof Symbol.ClassSymbol cs) {
                try {
                    cs.complete();
                    if (cs.owner instanceof Symbol.PackageSymbol && null == classSymbolScanner.getType(binaryName)) {
                        classSymbolScanner.primaryType(cs);
                    } // else: not a primary type, or already known
                } catch (Symbol.CompletionFailure e) {
                    // ignore
                }
            }
        }
    }

    private void indexPackages(CompilationUnitTree compilationUnitTree) {
        PackageIndexScanner scanner = new PackageIndexScanner(null);
        scanner.scan(compilationUnitTree, null);
    }

    static class PackageIndexScanner extends TreePathScanner<Void, Void> {
        private final Map<String, Map<String, String>> packageIndex;
        private String currentPackage = "";

        PackageIndexScanner(Map<String, Map<String, String>> packageIndex) {
            this.packageIndex = packageIndex;
        }

        @Override
        public Void visitCompilationUnit(CompilationUnitTree node, Void p) {
            currentPackage = node.getPackageName() != null
                    ? node.getPackageName().toString() : "";
            return super.visitCompilationUnit(node, p);
        }

        @Override
        public Void visitClass(ClassTree node, Void p) {
            JCTree.JCClassDecl jcClass = (JCTree.JCClassDecl) node;
            Symbol.ClassSymbol cs = jcClass.sym;
            packageIndex
                    .computeIfAbsent(currentPackage, _ -> new HashMap<>())
                    .put(cs.getSimpleName().toString(),
                            cs.flatName().toString());
            return super.visitClass(node, p); // recurse for nested classes
        }
    }
}
