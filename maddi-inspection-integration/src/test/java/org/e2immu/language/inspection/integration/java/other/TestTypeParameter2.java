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

package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
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
        TypeInfo X = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
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
                    	.filter(Objects::nonNull).distinct().toArray(String[]::new);
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2, JavaInspectorImpl.DETAILED_SOURCES);

    }
}