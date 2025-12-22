package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMap extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            import java.util.Set;
            public class X<K, V> {
                Map<K, V> map = new HashMap<>();
                public V getWithDefault(K key, V defaultValue) {
                    return map.getOrDefault(key, defaultValue);
                }
                public V getWithDefault2(K key, V defaultValue) {
                    return getWithDefault(key, defaultValue);
                }
                Set<K> keySet() { return map.keySet(); }
            }
            """;

    @DisplayName("Analyze 'get', map access, manually inserting links for Map.get(K)")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo getOrDefault = X.findUniqueMethod("getWithDefault", 2);
        MethodInfo getOrDefault2 = X.findUniqueMethod("getWithDefault2", 2);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodLinkedVariables mlv = getOrDefault.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(getOrDefault));
        assertEquals("getWithDefault==1:defaultValue,getWithDefault∈this.map.§kvs[-2].§v",
                mlv.ofReturnValue().toString());

        MethodLinkedVariables mlv2 =  getOrDefault2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(getOrDefault2));
        assertEquals("getWithDefault2==1:defaultValue,getWithDefault2∈this.map,getWithDefault2∈this.map.§kvs[-2].§v",
                mlv2.ofReturnValue().toString());
    }

}
