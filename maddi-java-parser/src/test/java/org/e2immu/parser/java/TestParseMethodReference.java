package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.MethodReference;
import org.e2immu.language.cst.api.expression.TypeExpression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseMethodReference extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class C {
                interface I {
                   String map(C c);
                }
                Function<C, String> mapper(I i) {
                    return i::map;
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        TypeInfo i = typeInfo.findSubType("I");
        assertSame(typeInfo, i.primaryType());
        MethodInfo map = i.findUniqueMethod("map", 1);
        assertSame(map, i.singleAbstractMethod());

        MethodInfo mapper = typeInfo.findUniqueMethod("mapper", 1);
        if (mapper.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof MethodReference mr) {
            assertEquals("i::map", mr.toString());
            if (mr.scope() instanceof VariableExpression ve) {
                assertEquals("i", ve.variable().simpleName());
            } else fail();
            assertSame(map, mr.methodInfo());
        } else fail();
    }
}
