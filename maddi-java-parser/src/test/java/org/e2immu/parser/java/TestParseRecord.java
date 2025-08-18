package org.e2immu.parser.java;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseRecord extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            record C(String s, int i) {

              private record P() {}

              public record R(C... cs) {
                 public R {
                   assert cs[0] != null;
                   System.out.println(cs.length);
                 }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        assertTrue(typeInfo.typeNature().isRecord());

        MethodInfo cc = typeInfo.findConstructor(2);
        assertTrue(cc.isSyntheticConstructor());
        assertTrue(cc.isSynthetic());
        assertEquals(2, cc.methodBody().size());
        Statement s0 = cc.methodBody().statements().getFirst();
        assertEquals("this.s=s;", s0.toString());
        assertEquals("0", s0.source().index());

        MethodInfo accessor0 = typeInfo.findUniqueMethod("s", 0);
        Statement a0 = accessor0.methodBody().statements().getFirst();
        assertEquals("0", a0.source().index());

        TypeInfo p = typeInfo.findSubType("P");
        assertTrue(p.typeNature().isRecord());
        assertTrue(p.hasBeenInspected());
        assertTrue(p.fields().isEmpty());
        assertTrue(p.isPrivate());

        TypeInfo r = typeInfo.findSubType("R");
        assertTrue(r.typeNature().isRecord());
        assertEquals(1, r.fields().size());
        FieldInfo cs = r.getFieldByName("cs", true);
        assertEquals("a.b.C.R.cs", cs.fullyQualifiedName());
        assertEquals("Type a.b.C[]", cs.type().toString());

        MethodInfo ccR = r.findConstructor(1);
        assertTrue(ccR.methodType().isCompactConstructor());
        //2 visible, 1 synthetic assignment
        assertEquals(3, ccR.methodBody().size());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class Record_0 {
                interface OutputElement {
                }
                record QualifiedName(String name, QualifiedName.Required required) implements OutputElement {
                    public enum Required {
                        YES, // always write
                        NO_FIELD, // don't write unless a field-related option says so
                        NO_METHOD, // don't write unless a method-related option says so
                        NEVER // never write
                    }
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = parse(INPUT2);
        test23(typeInfo);
    }

    private static void test23(TypeInfo typeInfo) {
        TypeInfo qn = typeInfo.findSubType("QualifiedName");
        assertTrue(qn.typeNature().isRecord());
        TypeInfo req = qn.findSubType("Required");
        assertTrue(req.typeNature().isEnum());
        MethodInfo qnConstructor = qn.findConstructor(2);
        ParameterizedType paramType = qnConstructor.parameters().get(1).parameterizedType();
        assertSame(req, paramType.typeInfo());
    }

    // difference with INPUT2 is the absence of the qualifier QualifiedName.Required in the 2nd parameter/field declaration
    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            public class Record_1 {
                interface OutputElement {
                }
                record QualifiedName(String name, Required required) implements OutputElement {
                    public enum Required {
                        YES, // always write
                        NO_FIELD, // don't write unless a field-related option says so
                        NO_METHOD, // don't write unless a method-related option says so
                        NEVER // never write
                    }
                }

            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = parse(INPUT3);
        test23(typeInfo);
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            public record C(@SuppressWarnings("?") String name) {}
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = parse(INPUT4);
        assertEquals(1, typeInfo.fields().size());
        FieldInfo fieldInfo = typeInfo.getFieldByName("name", true);
        assertEquals(1, fieldInfo.annotations().size());
    }

}
