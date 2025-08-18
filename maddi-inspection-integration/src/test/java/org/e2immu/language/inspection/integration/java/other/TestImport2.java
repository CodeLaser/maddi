package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;


public class TestImport2 extends CommonTest2 {


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
    String CHILD = """
            package a.b;
            public class Child extends Parent {
                protected void gc(SubInterface si) {
                }
            }
            """;

    @Language("java")
    String GRANDCHILD = """
            package c.b;
            import a.b.Child;
            public class GrandChild extends Child {
                @Override
                protected void gc(SubInterface si) {
                    si.method();
                }
            }
            """;

    @Test
    public void testImport() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.Parent", PARENT, "a.b.Child", CHILD,
                "c.b.GrandChild", GRANDCHILD);
        ParseResult pr1 = init(sourcesByFqn);
        TypeInfo gc = pr1.findType("c.b.GrandChild");
    }

}
