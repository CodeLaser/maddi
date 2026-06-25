package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

public class TestMap extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Collection;import java.util.Map;
            import java.util.HashMap;
            import java.util.Set;
            public class C<K, V> {
                Map<K, V> map = new HashMap<>();
            
                private V get(K k) {
                    return map.get(k);
                }
                private V put(K k, V v) {
                    return map.put(k, v);
                }
            
                public static <X, Y> Y staticGet(X x, C<X,Y> c) {
                    Y y = c.get(x);
                    return y;
                }
                public static <X, Y> Y staticPut(X x, Y y, C<X,Y> c) {
                    Y yy = c.put(x, y);
                    return yy;
                }
                public static <X, Y> Y staticPut2(C<X,Y> c, X x, Y y) {
                    Y yy = c.put(x, y);
                    return yy;
                }
            
                // these methods are here to give Map multiplicity 2 for the shallow computations
                public Collection<V> values() { return map.values(); }
                public Set<K> keySet() { return map.keySet(); }
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse("a.b.C", INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        TypeInfo map = javaInspector.compiledTypesManager().getOrLoad(Map.class);
        MethodInfo mapGet = map.findUniqueMethod("get", 1);
        ParameterInfo mapGet0 = mapGet.parameters().getFirst();
        Value.Independent mapGet0Independent = mapGet0.analysis().getOrNull(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.class);
        assertTrue(mapGet0Independent.isIndependent());

        {
            MethodInfo staticGet = C.findUniqueMethod("staticGet", 2);
            MethodLinkedVariables mlvSGet = staticGet.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(staticGet));
            VariableData vd0 = VariableDataImpl.of(staticGet.methodBody().statements().getFirst());
            VariableInfo y0 = vd0.variableInfo("y");
            Links tlvY0 = y0.linkedVariablesOrEmpty();
            // NOTE: kvs and not xys because it is a real field, not a virtual one
            assertEquals("y∈1:c.map.§kvs[-2]", tlvY0.toString());
            VariableInfo x0 = vd0.variableInfo(staticGet.parameters().getFirst());

            assertEquals("[-, -] --> staticGet∈1:c.map.§kvs[-2]", mlvSGet.toString());

            Links lX0 = x0.linkedVariablesOrEmpty();
            assertEquals("-", lX0.toString());
        }

        {
            MethodInfo staticPut = C.findUniqueMethod("staticPut", 3);
            MethodLinkedVariables mlvSPut = staticPut.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(staticPut));
            VariableData vd0 = VariableDataImpl.of(staticPut.methodBody().statements().getFirst());
            VariableInfo yy0 = vd0.variableInfo("yy");
            Links tlvY0 = yy0.linkedVariablesOrEmpty();
            assertEquals("yy∈2:c.map.§kvs[-2]", tlvY0.toString());
            ParameterInfo x = staticPut.parameters().getFirst();
            VariableInfo x0 = vd0.variableInfo(x);
            Links tlvX0 = x0.linkedVariablesOrEmpty();
            assertEquals("0:x∈2:c.map.§kvs[-1]", tlvX0.toString());
            assertEquals("Type param K[]", tlvX0.link(0).to().parameterizedType().toString());
            assertEquals("""
                    [0:x*∈2:c.map*.§kvs[-1], 1:y*∈2:c.map*.§kvs[-2], 2:c.map*.§kvs[-1]∋0:x*,2:c.map*.§kvs[-2]∋1:y*] --> \
                    staticPut∈2:c.map*.§kvs[-2]\
                    """, mlvSPut.toString());
        }

        {
            MethodInfo staticPut2 = C.findUniqueMethod("staticPut2", 3);
            MethodLinkedVariables tlvSPut = staticPut2.analysis().getOrCreate(METHOD_LINKS,
                    () -> tlc.doMethod(staticPut2));
            VariableData vd0 = VariableDataImpl.of(staticPut2.methodBody().statements().getFirst());
            VariableInfo yy0 = vd0.variableInfo("yy");
            Links tlvY0 = yy0.linkedVariablesOrEmpty();
            assertEquals("yy∈0:c.map.§kvs[-2]", tlvY0.toString());
            ParameterInfo x = staticPut2.parameters().get(1);
            VariableInfo x0 = vd0.variableInfo(x);
            Links tlvX0 = x0.linkedVariablesOrEmpty();
            assertEquals("1:x∈0:c.map.§kvs[-1]", tlvX0.toString());

            assertEquals("""
                    [0:c.map*.§kvs[-1]∋1:x*,0:c.map*.§kvs[-2]∋2:y*, 1:x*∈0:c.map*.§kvs[-1], 2:y*∈0:c.map*.§kvs[-2]] --> \
                    staticPut2∈0:c.map*.§kvs[-2]\
                    """, tlvSPut.toString());
        }
    }

    @Test
    public void testShallow1() {
        TypeInfo X = javaInspector.parse("a.b.C", INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, LinkComputer.Options.FORCE_SHALLOW);
        tlc.doPrimaryType(X);

        // NOTE: we need map to have multiplicity 2, so we've added the keyset and values methods
        MethodInfo get = X.findUniqueMethod("get", 1);
        MethodLinkedVariables tlvGet = get.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(get));
        assertEquals("[0:k∈this*.§kvs[-1]] --> get∈this*.§kvs[-2]", tlvGet.toString());

        MethodInfo put = X.findUniqueMethod("put", 2);
        MethodLinkedVariables tlvPut = put.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(put));
        assertEquals("[0:k∈this*.§kvs[-1], 1:v∈this*.§kvs[-2]] --> put∈this*.§kvs[-2]", tlvPut.toString());

        // NOTE: links between parameters need to be marked using the @Independent annotation
        // that's why there are no links from x into c

        MethodInfo staticGet = X.findUniqueMethod("staticGet", 2);
        MethodLinkedVariables tlvSGet = staticGet.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(staticGet));
        assertEquals("[-, -] --> staticGet∈1:c*.§xys[-2]", tlvSGet.toString());

        MethodInfo staticPut = X.findUniqueMethod("staticPut", 3);
        MethodLinkedVariables tlvSPut = staticPut.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(staticPut));
        assertEquals("[-, -, -] --> staticPut←1:y,staticPut∈2:c*.§xys[-2]", tlvSPut.toString());

        MethodInfo staticPut2 = X.findUniqueMethod("staticPut2", 3);
        MethodLinkedVariables tlvS2Put = staticPut2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(staticPut2));
        assertEquals("[-, -, -] --> staticPut2∈0:c*.§xys[-2],staticPut2←2:y", tlvS2Put.toString());
    }

    @Language("java")
    private static final String INPUT1B = """
            package a.b;
            import java.util.List;
            import java.util.Map;
            public class C<K, V> {
                Map<K, V> map;
            
                C(Map<K, V> map) { this.map = map; }
            
                public void init(List<K> keys, List<V> values) {
                    int i=0;
                    for(K key: keys) {
                        V value = values.get(i++);
                        map.put(key, value);
                    }
                }
            }
            """;

    @Test
    public void test1b() {
        TypeInfo C = javaInspector.parse("a.b.C", INPUT1B);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        MethodInfo init = C.findUniqueMethod("init", 2);
        ParameterInfo keys = init.parameters().getFirst();
        ParameterInfo values = init.parameters().getLast();

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodLinkedVariables mlv = init.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(init));

        VariableData vd1 = VariableDataImpl.of(init.methodBody().statements().getLast());
        VariableInfo keys1 = vd1.variableInfo(keys);
        assertEquals("0:keys.§ks∋key,0:keys.§ks~this.map.§kvs[-1]", keys1.linkedVariables().toString());

        VariableInfo values1 = vd1.variableInfo(values);
        assertEquals("1:values.§vs∋value,1:values.§vs~this.map.§kvs[-2]", values1.linkedVariables().toString());

        VariableInfo map1 = vd1.variableInfo("a.b.C.map");
        assertEquals("""
                this.map.§kvs[-1]∋key,\
                this.map.§kvs[-1]~0:keys.§ks,\
                this.map.§kvs[-2]∋value,\
                this.map.§kvs[-2]~1:values.§vs\
                """, map1.linkedVariables().toString());

        assertEquals("[0:keys.§ks~this.map*.§kvs[-1], 1:values.§vs~this.map*.§kvs[-2]] --> -",
                mlv.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Map;
            import java.util.HashMap;
            import java.util.Set;
            import java.util.stream.Collectors;
            public class C<K, V> {
                Map<K, V> map;
            
                C(Map<K, V> map) { this.map = map; }
            
                private C<V, K> reverse0() {
                    Map<V, K> map = new HashMap<>();
                    Set<Map.Entry<K,V>> entries = this.map.entrySet();
                    for(Map.Entry<K, V> entry: entries) {
                        map.put(entry.getValue(), entry.getKey());
                    }
                    return new C<>(map);
                }
            
                private C<V, K> reverse() {
                    Map<V, K> map = new HashMap<>();
                    for(Map.Entry<K, V> entry: this.map.entrySet()) {
                        map.put(entry.getValue(), entry.getKey());
                    }
                    return new C<>(map);
                }
            
                public static <Y, X> C<Y, X> staticReverse(C<X,Y> c) {
                     C<Y, X> r = c.reverse();
                    return r;
                }
            }
            """;

    @Test
    public void test2Reverse0() {
        TypeInfo C = javaInspector.parse("a.b.C", INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo reverse0 = C.findUniqueMethod("reverse0", 0);
        MethodLinkedVariables mlvReverse0 = reverse0.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(reverse0));

        // statement 1

        Statement s1 = reverse0.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo entries = vd1.variableInfo("entries");
        Links entriesLinks = entries.linkedVariablesOrEmpty();
        assertEquals("entries.§kvs⊆this.map.§kvs,entries.§m≡this.map.§m",
                entriesLinks.toString());

        VariableInfo thisMap1 = vd1.variableInfo("a.b.C.map");
        assertFalse(thisMap1.isModified());

        VariableInfo this1 = vd1.variableInfo("a.b.C.this");
        assertFalse(this1.isModified());

        // statement 2 EVAL

        Statement s2 = reverse0.methodBody().statements().get(2);
        VariableData vd2 = VariableDataImpl.of(s2);

        // map, entries, this.map, this, entry

        VariableInfo entries2E = vd2.variableInfo("entries", Stage.EVALUATION);
        assertEquals("""
                entries.§kvs∋entry,entries.§kvs⊆this.map.§kvs,entries.§m≡this.map.§m\
                """, entries2E.linkedVariables().toString());
        assertFalse(entries2E.isModified());

        VariableInfo thisMap2E = vd2.variableInfo("a.b.C.map", Stage.EVALUATION);
        assertEquals("""
                this.map.§kvs∋entry,this.map.§kvs⊇entries.§kvs,this.map.§m≡entries.§m\
                """, thisMap2E.linkedVariables().toString());
        assertFalse(thisMap2E.isModified());

        VariableInfo this2E = vd2.variableInfo("a.b.C.this", Stage.EVALUATION);
        assertTrue(this2E.linkedVariables().isEmpty());
        assertFalse(this2E.isModified());

        // statement 2.0.0

        Statement s200 = reverse0.methodBody().statements().get(2).block().statements().getFirst();
        VariableData vd200 = VariableDataImpl.of(s200);
        assertEquals("[map, entries, a.b.C.map, a.b.C.this, entry]", vd200.knownVariableNames().toString());

        VariableInfo map200 = vd200.variableInfo("map");
        assertEquals("""
                map.§vks[-1]∋entry.§kv.§v,map.§vks[-1]∩this.map.§kvs,map.§vks[-1]∩entries.§kvs,\
                map.§vks[-2]∋entry.§kv.§k,map.§vks[-2]∩this.map.§kvs,map.§vks[-2]∩entries.§kvs,\
                map.§vks∋entry,map.§vks~this.map.§kvs,map.§vks~entries.§kvs\
                """, map200.linkedVariables().toString());
        assertTrue(map200.isModified());

        VariableInfo entries200 = vd200.variableInfo("entries");
        assertEquals("""
                entries.§kvs∋entry,entries.§kvs~map.§vks,entries.§kvs≥entry.§kv.§k,entries.§kvs≥entry.§kv.§v,\
                entries.§kvs∩map.§vks[-1],entries.§kvs∩map.§vks[-2],entries.§m≡this.map.§m\
                """, entries200.linkedVariables().toString());
        assertFalse(entries200.isModified());

        VariableInfo thisMap200 = vd200.variableInfo("a.b.C.map");
        assertEquals("""
                this.map.§kvs∋entry,this.map.§kvs~map.§vks,this.map.§kvs∩map.§vks[-1],this.map.§kvs∩map.§vks[-2],\
                this.map.§m≡entries.§m\
                """, thisMap200.linkedVariables().toString());
        assertFalse(thisMap200.isModified());

        VariableInfo this200 = vd200.variableInfo("a.b.C.this");
        assertTrue(this200.linkedVariables().isEmpty());
        assertFalse(this200.isModified());

        VariableInfo entry200 = vd200.variableInfo("entry");
        assertEquals("""
                entry.§kv.§k∈map.§vks[-2],entry.§kv.§k≤this.map.§kvs,entry.§kv.§k≤entries.§kvs,\
                entry.§kv.§v∈map.§vks[-1],entry.§kv.§v≤this.map.§kvs,entry.§kv.§v≤entries.§kvs,\
                entry∈this.map.§kvs,entry∈map.§vks,entry∈entries.§kvs\
                """, entry200.linkedVariables().toString());
        assertFalse(entry200.isModified());


        // statement 2 merge

        assertEquals("[map, entries, a.b.C.map, a.b.C.this, entry]", vd2.knownVariableNames().toString());

        VariableInfo map2 = vd2.variableInfo("map");
        assertEquals("""
                map.§vks[-1]∋entry.§kv.§v,map.§vks[-1]∩this.map.§kvs,map.§vks[-1]∩entries.§kvs,\
                map.§vks[-2]∋entry.§kv.§k,map.§vks[-2]∩this.map.§kvs,map.§vks[-2]∩entries.§kvs,\
                map.§vks∋entry,map.§vks~this.map.§kvs,map.§vks~entries.§kvs\
                """, map2.linkedVariables().toString());
        assertTrue(map2.isModified());

        VariableInfo entries2 = vd2.variableInfo("entries");
        assertEquals("""
                entries.§kvs∋entry,entries.§kvs⊆this.map.§kvs,\
                entries.§m≡this.map.§m,\
                entries.§kvs~map.§vks,entries.§kvs≥entry.§kv.§k,entries.§kvs≥entry.§kv.§v,entries.§kvs∩map.§vks[-1],\
                entries.§kvs∩map.§vks[-2]\
                """, entries2.linkedVariables().toString());
        assertFalse(entries2.isModified());

        VariableInfo thisMap2 = vd2.variableInfo("a.b.C.map");
        assertEquals("""
                this.map.§kvs∋entry,this.map.§kvs⊇entries.§kvs,this.map.§m≡entries.§m,this.map.§kvs~map.§vks,\
                this.map.§kvs∩map.§vks[-1],this.map.§kvs∩map.§vks[-2]\
                """, thisMap2.linkedVariables().toString());

        VariableInfo this2 = vd2.variableInfo("a.b.C.this");
        assertNull(this2.linkedVariables());
        assertFalse(this2.isModified());

        VariableInfo entry2 = vd2.variableInfo("entry", Stage.MERGE);
        assertEquals("entry∈this.map.§kvs,entry∈entries.§kvs", entry2.linkedVariables().toString());
        assertFalse(entry2.isModified());

        assertFalse(thisMap2.isModified());

        // statement 3

        Statement s3 = reverse0.methodBody().statements().getLast();
        VariableData vd3 = VariableDataImpl.of(s3);
        assertEquals("""
                [map, entries, a.b.C.map, a.b.C.this, a.b.C.reverse0()]\
                """, vd3.knownVariableNames().toString());
        VariableInfo viMap = vd3.variableInfo("map");
        assertEquals("""
                map.§m≡reverse0.map.§m,\
                map.§vks[-1]→reverse0.map.§vks[-1],\
                map.§vks[-1]∩this.map.§kvs,\
                map.§vks[-1]∩reverse0.map.§vks[-2],\
                map.§vks[-1]∩entries.§kvs,\
                map.§vks[-2]→reverse0.map.§vks[-2],\
                map.§vks[-2]∩this.map.§kvs,\
                map.§vks[-2]∩reverse0.map.§vks[-1],\
                map.§vks[-2]∩entries.§kvs,\
                map.§vks~this.map.§kvs,\
                map.§vks~reverse0.map.§vks,\
                map.§vks~entries.§kvs,map→reverse0.map\
                """, viMap.linkedVariables().toString());

        VariableInfo viEntries = vd3.variableInfo("entries");
        assertEquals("""
                entries.§kvs~map.§vks,entries.§kvs∩map.§vks[-1],entries.§kvs∩map.§vks[-2],entries.§m≡this.map.§m\
                """, viEntries.linkedVariables().toString());

        VariableInfo viThisMap = vd3.variableInfo("a.b.C.map");
        assertEquals("""
                this.map.§kvs~map.§vks,this.map.§kvs∩map.§vks[-1],this.map.§kvs∩map.§vks[-2],this.map.§m≡entries.§m\
                """, viThisMap.linkedVariables().toString());

        // NOTE: map.§vks∋entry,map.§vks~this.map.§kvs,map.§vks~entries.§kvs has been generated by
        // Expand.completeSliceInformation().
        assertEquals("", mlvReverse0.sortedModifiedString());
        assertEquals("""
                [] --> reverse0.map.§vks[-1]∩this.map.§kvs,\
                reverse0.map.§vks[-2]∩this.map.§kvs,\
                reverse0.map.§vks~this.map.§kvs\
                """, mlvReverse0.toString());
        // reverse0.map.§vks~this.map.§kvs is there thanks to Expand.completeSliceInformation().
        // reverse0.map∩this.map.§kvs is lost due to new version of Util.isPartOf()
    }

    @Test
    public void test2Reverse() {
        TypeInfo C = javaInspector.parse("a.b.C", INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo reverse = C.findUniqueMethod("reverse", 0);
        MethodLinkedVariables mlvReverse = reverse.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(reverse));

        Statement s1 = reverse.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo thisMap1 = vd1.variableInfo("a.b.C.map");
        Links thisMap1Links = thisMap1.linkedVariablesOrEmpty();
        assertEquals("""
                this.map.§kvs∋entry,this.map.§kvs~map.§vks,this.map.§kvs≥entry.§kv.§k,this.map.§kvs≥entry.§kv.§v,this.map.§kvs∩map.§vks[-1],this.map.§kvs∩map.§vks[-2]\
                """, thisMap1Links.toString());

        // reverse.map∩this.map.§kvs is lost due to new version of Util.isPartOf()
        Statement s100 = reverse.methodBody().statements().get(1).block().statements().getFirst();
        VariableData vd100 = VariableDataImpl.of(s100);
        VariableInfo viEntry100 = vd100.variableInfo("entry");
        Links entry100Links = viEntry100.linkedVariablesOrEmpty();
        // note: the last entry is due to ExpandSlice.completeSliceInformation
        assertEquals("""
                entry.§kv.§k∈map.§vks[-2],\
                entry.§kv.§k≤this.map.§kvs,\
                entry.§kv.§v∈map.§vks[-1],\
                entry.§kv.§v≤this.map.§kvs,entry∈this.map.§kvs,\
                entry∈map.§vks\
                """, entry100Links.toString());

        Statement s2 = reverse.methodBody().statements().getLast();
        VariableData vd2 = VariableDataImpl.of(s2);
        assertFalse(vd2.isKnown("entry"));

        assertEquals("""
                [] --> reverse.map.§vks[-1]∩this.map.§kvs,\
                reverse.map.§vks[-2]∩this.map.§kvs,\
                reverse.map.§vks~this.map.§kvs\
                """, mlvReverse.toString());

        MethodInfo staticReverse = C.findUniqueMethod("staticReverse", 1);
        MethodLinkedVariables tlvSReverse = staticReverse.analysis().getOrCreate(METHOD_LINKS,
                () -> tlc.doMethod(staticReverse));

        VariableData vd0 = VariableDataImpl.of(staticReverse.methodBody().statements().getFirst());
        VariableInfo r0 = vd0.variableInfo("r");
        Links tlvR0 = r0.linkedVariablesOrEmpty();
        // Not as correct as could be
        assertEquals("""
                r.map.§vks[-1]∩0:c.map.§kvs,\
                r.map.§vks[-2]∩0:c.map.§kvs,\
                r.map.§vks~0:c.map.§kvs\
                """, tlvR0.toString());

        assertEquals("""
                [-] --> staticReverse.map.§vks[-1]∩0:c.map.§kvs,\
                staticReverse.map.§vks[-2]∩0:c.map.§kvs,\
                staticReverse.map.§vks~0:c.map.§kvs\
                """, tlvSReverse.toString());
    }


    @Test
    public void testShallow2() {
        TypeInfo X = javaInspector.parse("a.b.C", INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, LinkComputer.Options.FORCE_SHALLOW);

        // note: * (modification) due to shallow
        MethodInfo reverse = X.findUniqueMethod("reverse", 0);
        MethodLinkedVariables tlvReverse = reverse.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(reverse));
        assertEquals("[] --> reverse.§vk←this*.§kv,reverse.§m≡this*.§m", tlvReverse.toString());

        MethodInfo method = X.findUniqueMethod("staticReverse", 1);
        MethodLinkedVariables tlvSReverse = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("[-] --> staticReverse.§yx←0:c*.§xy", tlvSReverse.toString());
    }

}
