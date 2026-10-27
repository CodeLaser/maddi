/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.java.openjdk.type;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

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
        TypeInfo typeInfo = scan("a.b.X", INPUT1);

    }
}
