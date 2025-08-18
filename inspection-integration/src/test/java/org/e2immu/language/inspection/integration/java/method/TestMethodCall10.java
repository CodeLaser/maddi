package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

public class TestMethodCall10 extends CommonTest2 {

    @Language("java")
    private static final String ABX = """
            package a.b;
            import static a.c.Y.when;
            class X {
                interface Mocked {
                    void deleteObject(String key, String id);
                }
                static <T> T any() { return null; }
                interface Stubber {
                    <T> T when(T t);
                }
                static Stubber doNothing() { return null; }
            
                void method(Mocked mocked){
                    doNothing().when(mocked).deleteObject(any(), any());
                }
            }
            """;

    @Language("java")
    private static final String ACY = """
            package a.c;
            public class Y {
                public static <T> String when(T t) { return "" + t; }
            }
            """;

    @DisplayName("instance and static method, in instance setup")
    @Test
    public void test1() throws IOException {
        init(Map.of("a.b.X", ABX, "a.c.Y", ACY));
    }
}
