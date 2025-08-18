package org.e2immu.parser.java;

import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.Summary;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParserError extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public static void method1(String[] args) {
                System.out.println(arguments.length);
              }
              public void method2(int i) {
                 // nothing wrong here
              }
            }
            """;

    @Test
    public void test() {
        assertThrows(Summary.FailFastException.class, () -> parse(INPUT));

        Context c = parseReturnContext(INPUT);
        Summary s = c.summary();
        assertTrue(s.haveErrors());
        assertEquals("""
                        Exception: org.e2immu.language.inspection.api.parser.Summary.ParseException
                        In: input
                        In: a.b.C.method1(String[])
                        Message: In: input
                        In: a.b.C.method1(String[])
                        Message: Unknown identifier 'arguments'\
                        """,
                s.parseExceptions().getFirst().getMessage());
    }
}
