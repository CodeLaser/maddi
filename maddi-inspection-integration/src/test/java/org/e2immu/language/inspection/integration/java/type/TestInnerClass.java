package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestInnerClass extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            class X {
                String x;
                class Y {
                    void method() {
                        System.out.println(x);
                    }
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
    }

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            class X {
                String[] x;
                class Y {
                    void method() {
                        System.out.println(x != null && x.length>0 ? x[0]: "?");
                    }
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
    }


    @Language("java")
    public static final String INPUT3 = """
            package a.b;
            class X {
                interface Element {
                    interface Builder<B extends Builder<?>> {
                    }
                }
                interface Statement extends Element {
                }
                interface TryStatement extends Statement {
                    interface CatchClause extends Element {
                        interface Builder extends Element.Builder<Builder> {
                            Builder setBlock(String x);
                        }
                    }
                    interface Builder extends Statement.Builder<Builder> {
                        Builder setBlock(String y);
                    }
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        TypeInfo element = X.findSubType("Element");
        TypeInfo elementBuilder = element.findSubType("Builder");
        TypeInfo tryStatement = X.findSubType("TryStatement");
        TypeInfo tryStatementBuilder = tryStatement.findSubType("Builder");
        MethodInfo tryStatementBuilderSetBlock = tryStatementBuilder.findUniqueMethod("setBlock", 1);
        assertEquals("a.b.X.TryStatement.Builder",
                tryStatementBuilderSetBlock.returnType().fullyQualifiedName());
        TypeInfo catchClause = tryStatement.findSubType("CatchClause");
        TypeInfo ccBuilder = catchClause.findSubType("Builder");
        MethodInfo ccBuilderSetBlock = ccBuilder.findUniqueMethod("setBlock", 1);
        assertEquals("a.b.X.TryStatement.CatchClause.Builder", ccBuilderSetBlock.returnType().fullyQualifiedName());
    }

    @Language("java")
    public static final String INPUT4 = """
            package a;
            import static a.A.B.*;
            public class A {
                public enum B {
                    CONSTANT
                }
                public void m() {
                    System.out.println(CONSTANT);
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
    }

}
