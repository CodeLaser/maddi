package org.e2immu.language.inspection.integration.java.lombok;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestData extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
            import lombok.Data;
            
            @Data
            public class X {
                private final String s;
                private final int t;
                private char c;
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1,
                new JavaInspectorImpl.ParseOptionsBuilder().setLombok(true).build());
        MethodInfo rac = typeInfo.findConstructor(2);
        assertTrue(rac.isSynthetic());
        typeInfo.findUniqueMethod("getC", 0);
        typeInfo.findUniqueMethod("setC", 1);
        typeInfo.findUniqueMethod("getS", 0);
        typeInfo.findUniqueMethod("setS", 1);
        typeInfo.findUniqueMethod("getT", 0);
        typeInfo.findUniqueMethod("setT", 1);
    }

}
