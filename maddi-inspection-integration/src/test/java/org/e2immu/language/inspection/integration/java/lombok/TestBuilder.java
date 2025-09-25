package org.e2immu.language.inspection.integration.java.lombok;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBuilder extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
            import lombok.Data;
            import lombok.Builder;
            
            @Data
            @Builder
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
        TypeInfo builder = typeInfo.findSubType("Builder");
        assertTrue(builder.access().isPublic());
        assertTrue(builder.typeNature().isClass());
        assertTrue(builder.isStatic());

        // fields
        assertEquals(3, builder.fields().size());
        assertTrue(builder.getFieldByName("s", true).access().isPrivate());

        // setters
        MethodInfo setS = builder.findUniqueMethod("setS", 1);
        assertEquals("org.e2immu.test.X.Builder.setS(String)", setS.fullyQualifiedName());
        assertEquals("{this.s=s;return this;}", setS.methodBody().toString());

        // build method
        MethodInfo build = builder.findUniqueMethod("build", 0);
        assertEquals(typeInfo.asParameterizedType(), build.returnType());
    }

}
