package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.ArrayInitializer;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseAnonymousType extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class C {
              public void test(String s) {
                Function<Integer,String> f = new Function<>() {
                  @Override
                  public Integer apply(Integer i) {
                    return i+s;
                  }
                };
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("test", 1);
        if (methodInfo.methodBody().statements().get(0) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            assertEquals("a.b.C.$0", cc.anonymousClass().fullyQualifiedName());
            assertEquals("Type java.util.function.Function<Integer,String>", cc.parameterizedType().toString());
            assertEquals(cc.parameterizedType(), cc.anonymousClass().interfacesImplemented().get(0));
            MethodInfo apply = cc.anonymousClass().findUniqueMethod("apply", 1);
            if (apply.methodBody().statements().get(0) instanceof ReturnStatement rs) {
                assertEquals("return i+s;", rs.toString());
                assertEquals("0", rs.source().index());
            }
            assertSame(methodInfo, cc.anonymousClass().enclosingMethod());
        } else fail();
    }
}
