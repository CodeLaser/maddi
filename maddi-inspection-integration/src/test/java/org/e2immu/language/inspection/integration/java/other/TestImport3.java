package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestImport3 extends CommonTest2 {


    @Language("java")
    String PARENT = """
            package a.b;
            public class Parent {
                public interface SubInterface {
                    void method();
                }
            }
            """;

    @Language("java")
    String SIBLING = """
            package a.b;
            public class Sibling {
                static class Child extends Parent {
                    SubInterface si;
                }
            }
            """;

    @Test
    public void testImport() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.Parent", PARENT, "a.b.Sibling", SIBLING);
        ParseResult pr = init(sourcesByFqn);
        TypeInfo sibling = pr.findType("a.b.Sibling");
        TypeInfo child = sibling.findSubType("Child");
        FieldInfo si = child.getFieldByName("si", true);
        assertEquals("Type a.b.Parent.SubInterface", si.type().toString());
    }


    @Language("java")
    String PARENT2 = """
            package a.b;
            public class Parent {
                public static String[] STRINGS = { "a", "b", "c" };
            }
            """;
    @Language("java")
    String CHILD2 = """
            package a.b;
            public class Child extends Parent {
            }
            """;

    @Language("java")
    String USE2 = """
            package c;
            import static a.b.Child.STRINGS;
            public class Use {
                void method() {
                    for(String s: STRINGS) {
                        System.out.println(s);
                    }
                }
            }
            """;

    @Test
    public void testImport2() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.Parent", PARENT2, "a.b.Child", CHILD2, "c.Use", USE2);
        ParseResult pr = init(sourcesByFqn);

    }

}
