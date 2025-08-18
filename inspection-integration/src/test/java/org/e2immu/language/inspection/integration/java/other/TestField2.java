package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestField2 extends CommonTest2 {


    @Language("java")
    String PARENT = """
            package a.b;
            public class Parent {
                public static final String FIELD = "abc";
            }
            """;

    @Language("java")
    String CHILD = """
            package a.b;
            public class Child extends Parent {
                private class Create {
                    String newString(String k) {
                        return k + FIELD;
                    }
                }
            }
            """;

    @Test
    public void test() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.Parent", PARENT, "a.b.Child", CHILD);
        ParseResult pr1 = init(sourcesByFqn);
        TypeInfo child = pr1.findType("a.b.Child");
        TypeInfo create = child.findSubType("Create");
        MethodInfo newString = create.findUniqueMethod("newString", 1);
        BinaryOperator bo = (BinaryOperator) newString.methodBody().lastStatement().expression();
        if (bo.rhs() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
            assertEquals("a.b.Parent.FIELD", fr.fullyQualifiedName());
        } else fail();
    }


    @Language("java")
    String PARENT1 = """
            package a.b;
            public class Parent {
                public static class Sub {
                    
                }
            }
            """;

    @Language("java")
    String CHILD1 = """
            package a.b;
            public class Child {
                private class Create extends Parent {
                    Sub sub = new Sub();
                }
            }
            """;

    @Test
    public void test1() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.Parent", PARENT1, "a.b.Child", CHILD1);
        ParseResult pr1 = init(sourcesByFqn);
        TypeInfo child = pr1.findType("a.b.Child");
        TypeInfo create = child.findSubType("Create");
        FieldInfo sub = create.getFieldByName("sub", true);
        assertEquals("new Sub()", sub.initializer().toString());
    }


    @Language("java")
    String PARENT2 = """
            package a.b;
            public class Parent {
                protected int v;
            }
            """;

    @Language("java")
    String CHILD2 = """
            package a.b;
            public class Child extends Parent {
               //nothing
            }
            """;

    @Language("java")
    String USE2 = """
            package a.c;
            import a.b.Child;
            public class Use {
                void method() {
                    Child child = new Child();
                    child.v = 3;
                }
            }
            """;

    @Test
    public void test2() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.Parent", PARENT2, "a.b.Child", CHILD2,
                "a.c.Use", USE2);
        ParseResult pr1 = init(sourcesByFqn);
        TypeInfo use = pr1.findType("a.c.Use");
        MethodInfo method = use.findUniqueMethod("method", 0);

    }



    @Language("java")
    String PARENT3 = """
            package a.b;
            public class Parent {
                public static final String FIELD = "abc";
                private static final String ROUNDABOUT = "x" + Child.FIELD;
            }
            """;

    @Language("java")
    String CHILD3 = """
            package a.b;
            public class Child extends Parent {
               // no code needed here
            }
            """;

    @Test
    public void test3() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.Parent", PARENT3, "a.b.Child", CHILD3);
        ParseResult pr1 = init(sourcesByFqn);
        TypeInfo parent = pr1.findType("a.b.Parent");
        FieldInfo r = parent.getFieldByName("ROUNDABOUT", true);
        assertEquals("\"x\"+Parent.FIELD", r.initializer().toString());
    }

}
