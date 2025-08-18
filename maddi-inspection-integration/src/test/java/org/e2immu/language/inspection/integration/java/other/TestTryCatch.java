package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.SingleLineComment;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestTryCatch extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.io.IOException;
            import java.io.Writer;
            class X {
                public void method(String in, Writer writer) {
                    try { //
                        writer.append("input: ").append(in);
                    } catch (IOException | RuntimeException e) {
                        System.out.println("Caught io or runtime exception!");
                        throw e;
                    } finally {
                        System.out.println("this was method1");
                    }
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 2);
        TryStatement ts = (TryStatement) methodInfo.methodBody().statements().get(0);
        Statement s0 = ts.block().statements().get(0);
        assertEquals(1, s0.comments().size());
        Comment c = s0.comments().get(0);
        if(c instanceof SingleLineComment slc) {
            assertEquals("//\n", slc.print(javaInspector.runtime().qualificationQualifyFromPrimaryType()).toString());
        } else fail();
        TryStatement.CatchClause cc = ts.catchClauses().get(0);
        assertEquals("e", cc.catchVariable().simpleName());
        assertEquals("Type Exception", cc.catchVariable().parameterizedType().toString());
    }
}
