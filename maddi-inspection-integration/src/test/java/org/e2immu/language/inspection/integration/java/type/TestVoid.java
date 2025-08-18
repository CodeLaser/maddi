package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVoid extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            
            class X {
                private static final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap<>();
	            static  {
                    primitiveWrapperMap.put(int.class, Integer.class);
                    primitiveWrapperMap.put(long.class, Long.class);
                    primitiveWrapperMap.put(boolean.class, Boolean.class);
                    primitiveWrapperMap.put(double.class, Double.class);
                    primitiveWrapperMap.put(float.class, Float.class);
                    primitiveWrapperMap.put(char.class, Character.class);
                    primitiveWrapperMap.put(byte.class, Byte.class);
                    primitiveWrapperMap.put(short.class, Short.class);
                    primitiveWrapperMap.put(void.class, Void.class);
	            }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);

    }
}
