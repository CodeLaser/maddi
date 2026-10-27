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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypeParameter2 extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a;
            import java.util.HashMap;import java.util.List;
            import java.util.Map;

            class X {
                static class MyMap<T> extends HashMap<Long, T> {
                }
                MyMap<String[]> stringArrayMap = new MyMap<>();
                String method(long l, int i) {
                    var a = stringArrayMap.get(l);
                    return a[i];
                }
                String method2(long l, int i) {
                    return stringArrayMap.get(l)[i];
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = scan("a.X", INPUT1);
        FieldInfo sam = X.getFieldByName("stringArrayMap", true);
        assertEquals("Type a.X.MyMap<String[]>", sam.type().toString());
        MethodInfo method = X.findUniqueMethod("method", 2);
        LocalVariableCreation lvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        assertEquals("Type String[]", lvc.localVariable().parameterizedType().toString());
    }


    @Language("java")
    public static final String INPUT2 = """
            package a;
            import java.util.List;
            import java.util.ArrayList;
            import java.util.Objects;

            class X {
                String getBusinessUnitReference(String s) {
                    return s+"ref";
                }
                void method() {
                    List<?> dealerships = new ArrayList<>();
                    String[] buExternalReferences = dealerships.stream()
                        .map(dealership -> getBusinessUnitReference(dealership.toString()))
                    \t.filter(Objects::nonNull).distinct().toArray(String[]::new);
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = scan("a.X", INPUT2);

    }
}
