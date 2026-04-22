package org.e2immu.language.inspection.integration.java.print;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
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
        TypeInfo X = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = X.findUniqueMethod("method", 0);
        LocalVariableCreation lvc = (LocalVariableCreation) methodInfo.methodBody().statements().getFirst();
        DetailedSources ds = lvc.source().detailedSources();
        TypeInfo entry = lvc.localVariable().parameterizedType().parameters().getFirst().typeInfo();
        assertEquals("java.util.Map.Entry", entry.fullyQualifiedName());
        assertEquals("java.util.Map", ds.qualifier(entry).fullyQualifiedName());

        ImportComputer importComputer = javaInspector.importComputer(6, X.compilationUnit().sourceSet());
        Qualification qualification = javaInspector.runtime().qualificationExistingSources();
        ImportComputer.Result r = importComputer.go(X.compilationUnit(), qualification);
        // should not contain java.util.Map.Entry!!
        assertEquals("java.util.ArrayList, java.util.List, java.util.Map",
                r.imports().stream().map(ImportComputer.ImportDetails::importString)
                        .collect(Collectors.joining(", ")));
    }
}
