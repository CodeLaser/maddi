package org.e2immu.language.inspection.integration.java.lombok;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class TestSetter extends CommonTest {

    public TestSetter() {
        super(false);
    }

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
            import lombok.Setter;
            import java.util.List;
            
            public class X {
            
                @Setter private List<String> list;
                @Setter static int i;
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1,
                new JavaInspectorImpl.ParseOptionsBuilder().setLombok(true).build());
        FieldInfo fieldInfo = typeInfo.getFieldByName("list", true);
        assertEquals("java.util.List", fieldInfo.type().typeInfo().fullyQualifiedName());
        {
            MethodInfo m = typeInfo.findUniqueMethod("setList", 1);
            assertTrue(m.isSynthetic());
            assertFalse(m.isStatic());
            assertEquals("org.e2immu.test.X.setList(java.util.List<String>)", m.fullyQualifiedName());
            assertEquals("{this.list=list;}", m.methodBody().toString());
        }
        {
            MethodInfo m = typeInfo.findUniqueMethod("setI", 1);
            assertTrue(m.isSynthetic());
            assertTrue(m.isStatic());
            assertEquals("org.e2immu.test.X.setI(int)", m.fullyQualifiedName());
            assertEquals("{X.i=i;}", m.methodBody().toString());
        }

    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.test;
            
            import lombok.Setter;
            import java.util.List;
            
            @Setter
            public class X {
            
                private List<String> list;
                static int i;
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2,
                new JavaInspectorImpl.ParseOptionsBuilder().setLombok(true).build());
        FieldInfo fieldInfo = typeInfo.getFieldByName("list", true);
        assertEquals("java.util.List", fieldInfo.type().typeInfo().fullyQualifiedName());
        {
            MethodInfo m = typeInfo.findUniqueMethod("setList", 1);
            assertTrue(m.isSynthetic());
            assertFalse(m.isStatic());
            assertEquals("org.e2immu.test.X.setList(java.util.List<String>)", m.fullyQualifiedName());
            assertEquals("{this.list=list;}", m.methodBody().toString());
        }
        assertThrows(NoSuchElementException.class, () -> typeInfo.findUniqueMethod("setI", 1));
    }
}
