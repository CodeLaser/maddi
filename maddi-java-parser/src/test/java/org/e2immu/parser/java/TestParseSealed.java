package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseSealed extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package org.e2immu.analyser.resolver.testexample;

            public sealed class Sealed_0 permits Sealed_0.Sub1, Sealed_0.Sub2 {

                static final class Sub1 extends Sealed_0 {

                }

                static final class Sub2 extends Sealed_0 {

                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        assertTrue(typeInfo.typeNature().isClass());
        assertTrue(typeInfo.isSealed());
    }

}
