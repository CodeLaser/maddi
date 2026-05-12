package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScanCompilationUnits {
    private final Runtime runtime;
    private final SourceCodeScan sourceCodeScan;

    public ScanCompilationUnits(Runtime runtime) {
        this.runtime = runtime;
        sourceCodeScan = new SourceCodeScan(runtime);
    }

    public List<TypeInfo> scan(JavacTask task, SourceSet sourceSet) throws IOException {
        Trees trees = Trees.instance(task);

        Iterable<? extends CompilationUnitTree> units = task.parse();
        task.analyze();

        List<TypeInfo> types = new ArrayList<>();
        for (CompilationUnitTree unit : units) {
            System.out.println("=== " + unit.getSourceFile().getName() + " ===\n");

            CompilationUnit compilationUnit = runtime.newCompilationUnitBuilder()
                    .setPackageName(unit.getPackageName().toString())
                    .setSourceSet(sourceSet)
                    .build();
            CharSequence content = unit.getSourceFile().getCharContent(false);
            SourceCodeScan.Result scanResult = sourceCodeScan.go(content);

            SourcePositions sourcePositions = trees.getSourcePositions();
            LineMap lineMap = unit.getLineMap();

            AnalysisScanner analysisScanner = new AnalysisScanner(runtime, compilationUnit, unit, trees,
                    sourcePositions, lineMap, task.getElements(), scanResult);
            analysisScanner.scan(unit, null);
            types.addAll(analysisScanner.types());
        }
        return List.copyOf(types);
    }
}
