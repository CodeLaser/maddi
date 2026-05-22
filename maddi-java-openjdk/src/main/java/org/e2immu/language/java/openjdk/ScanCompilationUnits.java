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
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
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
    private final ResolveJavaDoc resolveJavaDoc;

    public record Result(List<TypeInfo> primaryTypes, List<ModuleInfo> modules) {
    }

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
        resolveJavaDoc = new ResolveJavaDoc(classSymbolScanner);
    }

    public Result scan() throws IOException {
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

        List<TypeInfo> primaryTypes = new ArrayList<>();
        List<ModuleInfo> modules = new ArrayList<>();

        indexJavaLangForJavaDocParsing();

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
            primaryTypes.addAll(scanCompilationUnit.types());
            modules.addAll(scanCompilationUnit.modules());
        }
        for (TypeInfo primaryType : primaryTypes) {
            scanJavaDocsAndCommit(primaryType);
        }
        return new Result(List.copyOf(primaryTypes), List.copyOf(modules));
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
                fieldInfo.builder().commit();
            }
        }
    }

    public void mergeIntoPreviouslyLoaded() {
        classSymbolScanner.mergeIntoPreviouslyLoaded();
    }

    // for tests
    public ClassSymbolScanner classSymbolScanner() {
        return classSymbolScanner;
    }

    private void indexJavaLangForJavaDocParsing() throws IOException {
        JavaFileManager fm = ((BasicJavacTask)task).getContext().get(JavaFileManager.class);
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
}
