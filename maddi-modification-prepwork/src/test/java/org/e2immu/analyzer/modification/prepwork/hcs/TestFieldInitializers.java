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

package org.e2immu.analyzer.modification.prepwork.hcs;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class TestFieldInitializers extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import org.junit.jupiter.api.Test;
            
            import java.util.HashMap;
            import java.util.Map;
            import java.util.function.BiFunction;
            
            import static org.junit.jupiter.api.Assertions.assertEquals;
            
            public class Map_Compute {
            
            
                private final BiFunction<String, String, String> concatWithKey = (k, v) -> k + v;
            
                public String nativeVersion(Map<String, String> map, String key, BiFunction<String, String, String> mapFunction) {
                    return map.compute(key, mapFunction);
                }
            
                public String JDK_version(Map<String, String> map, String key, BiFunction<String, String, String> mapFunction) {
                    String oldValue = map.get(key);
            
                    String newValue = mapFunction.apply(key, oldValue);
                    if (newValue == null) {
                        // delete mapping
                        if (oldValue != null || map.containsKey(key)) {
                            // something to remove
                            map.remove(key);
                            return null;
                        } else {
                            // nothing to do. Leave things as they were.
                            return null;
                        }
                    } else {
                        // add or replace old mapping
                        map.put(key, newValue);
                        return newValue;
                    }
                }
            
            
            
                public String alternativeWithPutIfAbsentAndReplace(Map<String, String> map, String key, BiFunction<String, String, String> mapFunction) {
                    if (map.containsKey(key)) {
                        String newValue = mapFunction.apply(key, map.get(key));
                        map.replace(key, newValue);
                        return newValue;
                    } else {
                        String newValue = mapFunction.apply(key, null);
                        map.putIfAbsent(key, newValue);
                        return newValue;
                    }
                }
            
                public String withComputeIf(Map<String, String> map, String key, BiFunction<String, String, String> mapFunction) {
                    map.computeIfPresent(key, (k, v) -> mapFunction.apply(k, v));
                    return map.computeIfAbsent(key, k -> mapFunction.apply(k, null));
                    /*Note that both `computeIfPresent()` and `computeIfAbsent()` return `null` if the key is not present
                     or if the key exists but the mapping function returns `null`. In this case, due to the chaining of
                     the methods, if the key is updated by `computeIfPresent()`, then `computeIfAbsent()` will not be
                     executed since the value is already present.*/
                }
            
            
            
                @Test
                public void testNativeVersion() {
                    Map<String, String> map = new HashMap<>();
                    map.put("key1", "Hello");
                    String result = nativeVersion(map, "key1", concatWithKey);
                    assertEquals("key1Hello", result);
                    assertEquals("key1Hello", map.get("key1"));
            
                    result = nativeVersion(map, "key2", concatWithKey);
                    assertEquals("key2null", result);
                    assertEquals("key2null", map.get("key2"));
                }
            
                @Test
                public void testAlternativeWithPutIfAbsentAndReplace() {
                    Map<String, String> map = new HashMap<>();
                    map.put("key1", "Hello");
                    String result = alternativeWithPutIfAbsentAndReplace(map, "key1", concatWithKey);
                    assertEquals("key1Hello", result);
                    assertEquals("key1Hello", map.get("key1"));
            
                    result = alternativeWithPutIfAbsentAndReplace(map, "key2", concatWithKey);
                    assertEquals("key2null", result);
                    assertEquals("key2null", map.get("key2"));
                }
            
                @Test
                public void testJDKversion() {
                    Map<String, String> map = new HashMap<>();
                    map.put("key1", "Hello");
                    String result = JDK_version(map, "key1", concatWithKey);
                    assertEquals("key1Hello", result);
                    assertEquals("key1Hello", map.get("key1"));
            
                    result = JDK_version(map, "key2", concatWithKey);
                    assertEquals("key2null", result);
                    assertEquals("key2null", map.get("key2"));
                }
            
                @Test
                public void testComputeIf() {
                    Map<String, String> map = new HashMap<>();
                    map.put("key1", "Hello");
                    String result = withComputeIf(map, "key1", concatWithKey);
                    assertEquals("key1Hello", result);
                    assertEquals("key1Hello", map.get("key1"));
            
                    result = withComputeIf(map, "key2", concatWithKey);
                    assertEquals("key2null", result);
                    assertEquals("key2null", map.get("key2"));
                }
            
            }""";

    @DisplayName("empty block causes issues")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        List<Info> analysisOrder = prepAnalyzer.doPrimaryType(B);
        FieldInfo concatWithKey = B.getFieldByName("concatWithKey", true);
        Lambda lambda = (Lambda) concatWithKey.initializer();
        assertEquals("Map_Compute.$2.apply(String,String)", lambda.methodInfo().fullyQualifiedName());
        VariableData vdConcat = concatWithKey.analysisOfInitializer().getOrNull(VariableDataImpl.VARIABLE_DATA,
                VariableDataImpl.class);
        assertNotNull(vdConcat);
        assertEquals("", vdConcat.knownVariableNamesToString());
        for (Info info : analysisOrder) {
            if (info instanceof MethodInfo methodInfo && !methodInfo.isConstructor()) {
                assertTrue(methodInfo.analysis().haveAnalyzedValueFor(HiddenContentSelector.HCS_METHOD),
                        "for " + methodInfo);
            }
        }
    }
}
