package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseDetailedSources extends CommonTestParse {

    // be careful changing this string, many tests are dependent on exact positions
    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.Hashtable;
            class C {
              public static void main(String[] args) {
                var len = args.length;
                final int len2 = args[0].length();
              }
              private Hashtable<String, Integer> table;
              Hashtable<String, String> table2;
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT, true);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("main", 1);

        MethodModifier p = methodInfo.methodModifiers().stream()
                .filter(m -> m.equals(runtime.methodModifierPublic())).findFirst().orElseThrow();
        DetailedSources ds = methodInfo.source().detailedSources();
        assertEquals("4-3:4-8", ds.detail(p).compact2());
        MethodModifier s = methodInfo.methodModifiers().stream()
                .filter(m -> m.equals(runtime.methodModifierStatic())).findFirst().orElseThrow();
        assertEquals("4-10:4-15", ds.detail(s).compact2());
        assertEquals("4-17:4-20", ds.detail(methodInfo.returnType()).compact2());
        assertEquals("4-22:4-25", ds.detail(methodInfo.name()).compact2());

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertEquals("4-27:4-34", p0.source().detailedSources().detail(p0.parameterizedType()).compact2());
        assertEquals("4-36:4-39", p0.source().detailedSources().detail(p0.name()).compact2());

        LocalVariableCreation lvc0 = (LocalVariableCreation) methodInfo.methodBody().statements().getFirst();
        assertEquals("5-5:5-25", lvc0.source().compact2());
        LocalVariableCreation.Modifier v = lvc0.modifiers().stream().findFirst().orElseThrow();
        DetailedSources ds0 = lvc0.source().detailedSources();
        assertEquals("5-5:5-7", ds0.detail(v).compact2());
        assertEquals("5-9:5-11", ds0.detail(lvc0.localVariable()).compact2());

        LocalVariableCreation lvc1 = (LocalVariableCreation) methodInfo.methodBody().statements().get(1);
        assertEquals("6-5:6-37", lvc1.source().compact2());
        LocalVariableCreation.Modifier f = lvc1.modifiers().stream().findFirst().orElseThrow();
        DetailedSources ds1 = lvc1.source().detailedSources();
        assertEquals("6-5:6-9", ds1.detail(f).compact2());
        ParameterizedType pt1 = lvc1.localVariable().parameterizedType();
        assertEquals("6-11:6-13", ds1.detail(pt1).compact2());
        assertEquals("6-15:6-18", ds1.detail(lvc1.localVariable()).compact2());

        FieldInfo table = typeInfo.getFieldByName("table", true);
        FieldModifier fp = table.modifiers().stream().findFirst().orElseThrow();
        DetailedSources dst = table.source().detailedSources();
        assertEquals("8-3:8-9", dst.detail(fp).compact2());
        assertEquals("8-11:8-36", dst.detail(table.type()).compact2());
        assertEquals("8-11:8-19", dst.detail(table.type().typeInfo()).compact2());
        assertEquals("8-21:8-26", dst.detail(table.type().parameters().get(0)).compact2());
        assertEquals("8-29:8-35", dst.detail(table.type().parameters().get(1)).compact2());
        assertEquals("8-38:8-42", dst.detail(table.name()).compact2());

        FieldInfo table2 = typeInfo.getFieldByName("table2", true);
        DetailedSources dst2 = table2.source().detailedSources();
        assertEquals("9-3:9-27", dst2.detail(table2.type()).compact2());
        assertEquals("9-3:9-11", dst2.detail(table2.type().typeInfo()).compact2());
        ParameterizedType stringPt = table2.type().parameters().get(0);
        assertNotSame(runtime.stringParameterizedType(), stringPt);
        assertEquals(runtime.stringParameterizedType(), stringPt);
        assertEquals("9-13:9-18", dst2.detail(stringPt).compact2());
        assertEquals("9-21:9-26", dst2.detail(table2.type().parameters().get(1)).compact2());
        assertEquals("9-29:9-34", dst2.detail(table2.name()).compact2());
    }
}
