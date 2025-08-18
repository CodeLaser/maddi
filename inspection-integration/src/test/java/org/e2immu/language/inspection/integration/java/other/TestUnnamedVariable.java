package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ForEachStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.api.parser.ErasedExpression;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestUnnamedVariable extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            
            import java.util.List;
            class X {
               void method(List<String> list) {
                   for(String _: list) {
                       System.out.println("?");
                   }
               }
            }
            """;

    @DisplayName("unnamed variable in for loop")
    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
        ForEachStatement fe = (ForEachStatement) methodInfo.methodBody().statements().getFirst();
        assertTrue(fe.initializer().localVariable().isUnnamed());
    }

}
