package org.e2immu.language.java.openjdk.print;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
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

    private static String commas(DetailedSources ds, Object key) {
        return ds.details(key).stream().map(Source::compact2).collect(Collectors.joining(", "));
    }

    @Language("java")
    public static final String INPUT_K = """
            package a.b;
            public class K implements java.io.Serializable, Cloneable, java.util.RandomAccess {
                java.util.Map<String, Integer> f;
                void m() throws java.io.IOException, RuntimeException {
                }
            }
            """;

    @Test
    public void testImplementsThrowsTypeArgumentCommas() {
        TypeInfo K = scan("a.b.K", INPUT_K);

        // class K implements ..., ..., ...  (commas 2-47, 2-58)
        assertEquals("2-47:2-47, 2-58:2-58", commas(K.source().detailedSources(), DetailedSources.IMPLEMENTS_COMMAS));

        // void m() throws ..., ...  (comma 4-40)
        MethodInfo m = K.findUniqueMethod("m", 0);
        assertEquals("4-40:4-40", commas(m.source().detailedSources(), DetailedSources.THROWS_COMMAS));

        // Map<String, Integer> f : the type-argument comma (3-25) sits on the field type's own source
        FieldInfo f = K.getFieldByName("f", true);
        Source typeSource = f.source().detailedSources().detail(f.type());
        assertEquals("3-25:3-25", commas(typeSource.detailedSources(), DetailedSources.TYPE_ARGUMENT_COMMAS));
    }

    @Language("java")
    public static final String INPUT_I = """
            package a.b;
            public interface I extends java.io.Serializable, Cloneable, java.util.RandomAccess {
            }
            """;

    @Test
    public void testExtendsCommas() {
        TypeInfo I = scan("a.b.I", INPUT_I);
        // interface I extends ..., ..., ...  (commas 2-48, 2-59)
        assertEquals("2-48:2-48, 2-59:2-59", commas(I.source().detailedSources(), DetailedSources.EXTENDS_COMMAS));
    }

    @Language("java")
    public static final String INPUT_J = """
            package a.b;
            public sealed interface J permits P1, P2 {
            }
            final class P1 implements J {
            }
            final class P2 implements J {
            }
            """;

    @Test
    public void testPermitsCommas() {
        // three top-level types in one unit, so look J up by FQN rather than relying on declaration order
        TypeInfo J = scan(false, "a.b.J", INPUT_J).get("a.b.J");
        // sealed interface J permits P1, P2  (comma 2-37)
        assertEquals("2-37:2-37", commas(J.source().detailedSources(), DetailedSources.PERMITS_COMMAS));
    }
}
