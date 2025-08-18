package org.e2immu.language.inspection.integration.java.stub;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestStub2 extends CommonTest {

    public TestStub2() {
        super(true);
    }

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
            public class X {
                private Y y;
                public X(Y y) {
                    this.y = y;
                }
            }
            """;


    @Disabled("at the moment, stubs from the type context are disabled")
    @Test
    public void test() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1, JavaInspectorImpl.FAIL_FAST);
        FieldInfo y = typeInfo.getFieldByName("y", true);
        TypeInfo Y = y.type().bestTypeInfo();
        assertEquals("Y", Y.fullyQualifiedName());
        assertTrue(Y.typeNature().isStub());
        MethodInfo constructor = typeInfo.findConstructor(1);
        ParameterInfo constructor0 = constructor.parameters().getFirst();
        assertEquals(Y, constructor0.parameterizedType().typeInfo());
        assertSame(Y, constructor0.parameterizedType().typeInfo());
    }
}
