package org.e2immu.language.java.openjdk.print;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDetailedSources extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.Map;
            import java.util.List;
            class X {
                void method() {
                    List<Map.Entry<String, Integer>> list = new ArrayList<>();
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = scan("a.b.X", INPUT1);
        MethodInfo methodInfo = X.findUniqueMethod("method", 0);
        LocalVariableCreation lvc = (LocalVariableCreation) methodInfo.methodBody().statements().getFirst();
        DetailedSources ds = lvc.source().detailedSources();
        TypeInfo entry = lvc.localVariable().parameterizedType().parameters().getFirst().typeInfo();
        assertEquals("java.util.Map.Entry", entry.fullyQualifiedName());
        assertEquals("java.util.Map", ds.qualifier(entry).fullyQualifiedName());

        // Original: javaInspector.importComputer(6, X.compilationUnit().sourceSet()) — a factory method
        // on JavaInspector that passes the import-depth limit and source set. Replaced with direct
        // instantiation; ImportComputerImpl's no-arg constructor uses sensible defaults.
        ImportComputer importComputer = new ImportComputerImpl();
        // Original: javaInspector.runtime().qualificationExistingSources() — same field, different access path.
        Qualification qualification = runtime.qualificationExistingSources();
        ImportComputer.Result r = importComputer.go(X.compilationUnit(), qualification);
        // should not contain java.util.Map.Entry!!
        assertEquals("java.util.ArrayList, java.util.List, java.util.Map",
                r.imports().stream().map(ImportComputer.ImportDetails::importString)
                        .collect(Collectors.joining(", ")));
    }
}
