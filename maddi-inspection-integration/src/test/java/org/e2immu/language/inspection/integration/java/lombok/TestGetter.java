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

public class TestGetter extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
            import lombok.Getter;
            import java.util.List;
            
            public class X {
            
                @Getter private List<String> list;
                @Getter static int i;
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1,
                new JavaInspectorImpl.ParseOptionsBuilder().setLombok(true).build());
        FieldInfo fieldInfo = typeInfo.getFieldByName("list", true);
        assertEquals("java.util.List", fieldInfo.type().typeInfo().fullyQualifiedName());
        {
            MethodInfo getList = typeInfo.findUniqueMethod("getList", 0);
            assertTrue(getList.isSynthetic());
            assertEquals("org.e2immu.test.X.getList()", getList.fullyQualifiedName());
            assertEquals("{return this.list;}", getList.methodBody().toString());
        }
        {
            MethodInfo i = typeInfo.findUniqueMethod("getI", 0);
            assertTrue(i.isSynthetic());
            assertTrue(i.isStatic());
            assertEquals("org.e2immu.test.X.getI()", i.fullyQualifiedName());
            assertEquals("{return X.i;}", i.methodBody().toString());
        }
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.test;
            
            import lombok.Getter;
            import java.util.List;
            
            @Getter
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
            MethodInfo getList = typeInfo.findUniqueMethod("getList", 0);
            assertTrue(getList.isSynthetic());
            assertEquals("org.e2immu.test.X.getList()", getList.fullyQualifiedName());
            assertEquals("{return this.list;}", getList.methodBody().toString());
        }
        assertThrows(NoSuchElementException.class, () -> typeInfo.findUniqueMethod("getI", 0));
    }
}
