package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.InstanceOf;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestParseInstanceOfPattern extends CommonTestParse {
    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class C {
              interface I {
                 String map(C c);
              }
              String method(Object o, C c) {
                if(o instanceof I i) {
                    return i.map(c);
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        if (method.methodBody().statements().get(0) instanceof IfElseStatement ifElse) {
            if (ifElse.expression() instanceof InstanceOf io) {
                assertEquals("i", io.patternVariable().localVariable().simpleName());
            } else fail();
        } else fail();
    }
}
