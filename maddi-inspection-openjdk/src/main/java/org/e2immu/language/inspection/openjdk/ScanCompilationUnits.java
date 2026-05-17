package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Types;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScanCompilationUnits {
    private static final Logger LOGGER = LoggerFactory.getLogger(org.e2immu.parser.java.ScanCompilationUnit.class);

    private final Runtime runtime;
    private final SourceCodeScan sourceCodeScan;
    private final DiagnosticCollector<JavaFileObject> diagnostics;
    private final JavacTask task;
    private final SourceSet sourceSet;
    private final JavaInspector.ParseOptions parseOptions;
    private final Trees trees;
    private final SourcePositions sourcePositions;
    private final Types types;
    private final ComputeMethodOverrides computeMethodOverrides;
    private final FlagHelper flagHelper;
    private final ClassSymbolScanner classSymbolScanner;

    public ScanCompilationUnits(Runtime runtime,
                                SourceSet javaBase,
                                JavacTask task,
                                SourceSet sourceSet,
                                JavaInspector.ParseOptions parseOptions,
                                DiagnosticCollector<JavaFileObject> diagnostics) {
        this.runtime = runtime;
        sourceCodeScan = new SourceCodeScan(runtime);
        this.diagnostics = diagnostics;
        this.task = task;
        this.sourceSet = sourceSet;
        this.parseOptions = parseOptions;

        trees = Trees.instance(task);
        sourcePositions = trees.getSourcePositions();
        types = Types.instance(((BasicJavacTask) task).getContext());
        Elements elements = task.getElements();
        computeMethodOverrides = new ComputeMethodOverrides(types, elements);
        flagHelper = new FlagHelper(runtime);
        classSymbolScanner = new ClassSymbolScanner(runtime, sourceSet, flagHelper, types, elements, javaBase);
    }

    public List<Info> scan() throws IOException {
        Iterable<? extends CompilationUnitTree> units = task.parse();
        task.analyze();

        if (diagnostics != null) {
            boolean haveErrors = false;
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    LOGGER.info("Error found: {} at line {}, col {}", d.getMessage(Locale.getDefault()),
                            d.getLineNumber(), d.getColumnNumber());
                    haveErrors = true;
                }
            }
            if (haveErrors) throw new CompilationProblems();
        }

        List<Info> primaryTypesAndModules = new ArrayList<>();
        for (CompilationUnitTree unit : units) {
            System.out.println("=== " + unit.getSourceFile().getName() + " ===\n");

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
            if (parseOptions.detailedSources()) {
                CharSequence content = unit.getSourceFile().getCharContent(false);
                scanResult = sourceCodeScan.go(content, isModule);
            } else {
                scanResult = SourceCodeScan.EMPTY_RESULT;
            }


            LineMap lineMap = unit.getLineMap();
            ScanCompilationUnit scanCompilationUnit = new ScanCompilationUnit(runtime, classSymbolScanner,
                    compilationUnit, unit, trees, sourcePositions, lineMap, task.getElements(), types, scanResult,
                    computeMethodOverrides, flagHelper, classSymbolScanner);
            scanCompilationUnit.scan(unit, null);
            primaryTypesAndModules.addAll(scanCompilationUnit.types());
        }
        return List.copyOf(primaryTypesAndModules);
    }

    // for tests

    public ClassSymbolScanner classSymbolScanner() {
        return classSymbolScanner;
    }
}
