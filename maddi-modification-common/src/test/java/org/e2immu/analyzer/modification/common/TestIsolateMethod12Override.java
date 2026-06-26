package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// @Override on the isolated method: it must override something or the frame does not compile, so the frame is
// given an abstract supertype that declares the method
public class TestIsolateMethod12Override extends CommonIsolateMethodTest {

    @Language("java")
    public static final String O1 = """
            package a.b;
            interface I { boolean validate(String s); }
            public class X implements I {
                @Override
                public boolean validate(String s) {
                    return s != null;
                }
            }
            """;

    @DisplayName("@Override method gets an abstract supertype declaring it")
    @Test
    public void o1() {
        TypeInfo x = parse("a.b.X", O1);
        String m = """
                @Override
                public boolean validate(String s) {
                    return s != null;
                }""";
        String out = isolate(x, "validate", 1, m);
        @Language("java")
        String expected = """
                abstract class X_validate_super { abstract boolean validate(String s);
                    } public class X_validate extends X_validate_super {
                    @Override
                public boolean validate(String s) {
                    return s != null;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_validate", out));
    }
}
