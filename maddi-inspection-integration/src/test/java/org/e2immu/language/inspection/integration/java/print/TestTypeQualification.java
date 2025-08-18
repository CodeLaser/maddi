package org.e2immu.language.inspection.integration.java.print;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.print.FormatterImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypeQualification extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.util.Date;
            class X {
                public java.sql.Date method(Date date) {
                   return new java.sql.Date(date.getTime());
                }
            }
            """;

    @Language("java")
    public static final String OUTPUT1 = """
            package a.b;
            import java.util.Date;
            class X {public java.sql.Date method(Date date) { return new java.sql.Date(date.getTime()); } }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        assertEquals(OUTPUT1, javaInspector.print2(X));
    }
}
