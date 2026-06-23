package org.e2immu.language.java.openjdk.print;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Language("java")
    public static final String INPUT_END_OF_PARAMETER_LIST = """
            package a.b;
            class X {
                int method(String s, int k) {
                    return k;
                }
                X(int a) {
                }
            }
            """;

    @Test
    public void testEndOfParameterList() {
        TypeInfo X = scan("a.b.X", INPUT_END_OF_PARAMETER_LIST);

        MethodInfo method = X.findUniqueMethod("method", 2);
        DetailedSources ds = method.source().detailedSources();
        assertEquals("3-31:3-31", ds.detail(DetailedSources.END_OF_PARAMETER_LIST).compact2());

        MethodInfo constructor = X.constructors().getFirst();
        DetailedSources dsc = constructor.source().detailedSources();
        assertEquals("6-12:6-12", dsc.detail(DetailedSources.END_OF_PARAMETER_LIST).compact2());
    }

    @Language("java")
    public static final String INPUT_COMMAS = """
            package a.b;
            class X<A, B, C> {
                int i, j, k;
                void m(int a, int b, int c) {
                }
            }
            """;

    @Test
    public void testPrecedingSucceedingComma() {
        TypeInfo X = scan("a.b.X", INPUT_COMMAS);

        // class type parameter B (middle of <A, B, C>)
        TypeParameter b = X.typeParameters().get(1);
        DetailedSources dsB = b.source().detailedSources();
        assertEquals("2-10:2-10", dsB.detail(DetailedSources.PRECEDING_COMMA).compact2());
        assertEquals("2-13:2-13", dsB.detail(DetailedSources.SUCCEEDING_COMMA).compact2());

        // field j (middle of i, j, k)
        FieldInfo j = X.getFieldByName("j", true);
        DetailedSources dsJ = j.source().detailedSources();
        assertEquals("3-10:3-10", dsJ.detail(DetailedSources.PRECEDING_COMMA).compact2());
        assertEquals("3-13:3-13", dsJ.detail(DetailedSources.SUCCEEDING_COMMA).compact2());

        // method parameter 'int b' (middle of int a, int b, int c)
        ParameterInfo pb = X.findUniqueMethod("m", 3).parameters().get(1);
        DetailedSources dsPb = pb.source().detailedSources();
        assertEquals("4-17:4-17", dsPb.detail(DetailedSources.PRECEDING_COMMA).compact2());
        assertEquals("4-24:4-24", dsPb.detail(DetailedSources.SUCCEEDING_COMMA).compact2());
    }

    @Language("java")
    public static final String INPUT_EQUALS = """
            package a.b;
            class X {
                int x = 5;
                int y;
            }
            """;

    @Test
    public void testSucceedingEquals() {
        TypeInfo X = scan("a.b.X", INPUT_EQUALS);

        // field with initialiser: '=' at 3-11
        FieldInfo x = X.getFieldByName("x", true);
        assertEquals("3-11:3-11", x.source().detailedSources().detail(DetailedSources.SUCCEEDING_EQUALS).compact2());

        // field without initialiser: no SUCCEEDING_EQUALS
        FieldInfo y = X.getFieldByName("y", true);
        assertNull(y.source().detailedSources().detail(DetailedSources.SUCCEEDING_EQUALS));
    }
}
