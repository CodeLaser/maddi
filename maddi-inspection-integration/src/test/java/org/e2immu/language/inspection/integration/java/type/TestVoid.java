/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
