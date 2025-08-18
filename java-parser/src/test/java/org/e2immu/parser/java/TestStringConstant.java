package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.StringConstant;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.output.QualificationImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestStringConstant extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              String s = "[\\\\d]{5}\\\\.xml$";
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        FieldInfo s = typeInfo.getFieldByName("s", true);
        if (s.initializer() instanceof StringConstant sc) {
            assertEquals("\"[\\\\d]{5}\\\\.xml$\"", sc.print(QualificationImpl.SIMPLE_NAMES).toString());
        } else fail();
    }
}
