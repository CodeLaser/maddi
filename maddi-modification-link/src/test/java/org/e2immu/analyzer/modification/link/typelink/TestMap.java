package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
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
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMap extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Map;
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
                public Set<V> values() { return map.values(); }
                public Set<K> keySet() { return map.keySet(); }
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);
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
            Links tlvY0 = y0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            // NOTE: kvs and not xys because it is a real field, not a virtual one
            assertEquals("y<1:c.map.§kvs[-2].§v", tlvY0.toString());
            VariableInfo x0 = vd0.variableInfo(staticGet.parameters().getFirst());

            assertEquals("[-, -] --> staticGet<1:c.map.§kvs[-2].§v", mlvSGet.toString());

            Links lX0 = x0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("-", lX0.toString());
        }

        {
            MethodInfo staticPut = C.findUniqueMethod("staticPut", 3);
            MethodLinkedVariables mlvSPut = staticPut.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(staticPut));
            VariableData vd0 = VariableDataImpl.of(staticPut.methodBody().statements().getFirst());
            VariableInfo yy0 = vd0.variableInfo("yy");
            Links tlvY0 = yy0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("yy<2:c.map.§kvs[-2].§v", tlvY0.toString());
            ParameterInfo x = staticPut.parameters().getFirst();
            VariableInfo x0 = vd0.variableInfo(x);
            Links tlvX0 = x0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("0:x<2:c.map.§kvs[-1].§k", tlvX0.toString());

            assertEquals("""
                    [0:x<2:c.map.§kvs[-1].§k, 1:y<2:c.map.§kvs[-2].§v, 2:c.map.§kvs[-1].§k>0:x,2:c.map.§kvs[-2].§v>1:y]\
                     --> staticPut<2:c.map.§kvs[-2].§v\
                    """, mlvSPut.toString());
        }

        {
            MethodInfo staticPut2 = C.findUniqueMethod("staticPut2", 3);
            MethodLinkedVariables tlvSPut = staticPut2.analysis().getOrCreate(METHOD_LINKS,
                    () -> tlc.doMethod(staticPut2));
            VariableData vd0 = VariableDataImpl.of(staticPut2.methodBody().statements().getFirst());
            VariableInfo yy0 = vd0.variableInfo("yy");
            Links tlvY0 = yy0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("yy<0:c.map.kvs[-2].v", tlvY0.toString());
            ParameterInfo x = staticPut2.parameters().get(1);
            VariableInfo x0 = vd0.variableInfo(x);
            Links tlvX0 = x0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("1:x<0:c.map.kvs[-1].k", tlvX0.toString());

            assertEquals("""
                    [0:c.map.kvs[-1].k>1:x,0:c.map.kvs[-2].v>2:y, 1:x<0:c.map.kvs[-1].k, 2:y<0:c.map.kvs[-2].v]\
                     --> staticPut2<0:c.map.kvs[-2].v\
                    """, tlvSPut.toString());
        }
    }


    @Test
    public void testShallow1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, true, true);
        tlc.doPrimaryType(X);

        // NOTE: we need map to have multiplicity 2, so we've added the keyset and values methods
        MethodInfo get = X.findUniqueMethod("get", 1);
        MethodLinkedVariables tlvGet = get.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(get));
        assertEquals("[0:k<this.§kvs[-1].§k] --> get<this.§kvs[-2].§v", tlvGet.toString());

        MethodInfo put = X.findUniqueMethod("put", 2);
        MethodLinkedVariables tlvPut = put.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(put));
        assertEquals("[0:k<this.§kvs[-1].§k, 1:v<this.§kvs[-2].§v] --> put<this.§kvs[-2].§v", tlvPut.toString());

        // NOTE: links between parameters need to be marked using the @Independent annotation
        // that's why there are no links from x into c

        MethodInfo staticGet = X.findUniqueMethod("staticGet", 2);
        MethodLinkedVariables tlvSGet = staticGet.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(staticGet));
        assertEquals("[-, -] --> staticGet<1:c.§xys[-2].§y", tlvSGet.toString());

        MethodInfo staticPut = X.findUniqueMethod("staticPut", 3);
        MethodLinkedVariables tlvSPut = staticPut.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(staticPut));
        assertEquals("[-, -, -] --> staticPut<2:c.§xys[-2].§y,staticPut==1:y", tlvSPut.toString());

        MethodInfo staticPut2 = X.findUniqueMethod("staticPut2", 3);
        MethodLinkedVariables tlvS2Put = staticPut2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(staticPut2));
        assertEquals("[-, -, -] --> staticPut2<0:c.§xys[-2].§y,staticPut2==2:y", tlvS2Put.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Map;
            import java.util.HashMap;
            import java.util.Set;import java.util.stream.Collectors;
            
            public class C<K, V> {
                Map<K, V> map;
            
                C(Map<K, V> map) { this.map = map; }
            
                private C<V, K> reverse0() {
                    Map<V, K> map = new HashMap<>();
                    Set<Map.Entry<K,V>> entries = this.map.entrySet();
                    for(Map.Entry<K, V> entry: entries) {
                        map.put(entry.getValue(), entry.getKey());
                    }
                    return map;
                }
            
                private C<V, K> reverse() {
                    Map<V, K> map = new HashMap<>();
                    for(Map.Entry<K, V> entry: this.map.entrySet()) {
                        map.put(entry.getValue(), entry.getKey());
                    }
                    return map;
                }
            
                public static <Y, X> C<Y, X> staticReverse(C<X,Y> c) {
                     C<Y, X> r = c.reverse();
                    return r;
                }
            }
            """;

    @Test
    public void test2a() {
        TypeInfo C = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo reverse = C.findUniqueMethod("reverse0", 0);
        MethodLinkedVariables mlvReverse0 = reverse.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(reverse));

        Statement s1 = reverse.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo entries = vd1.variableInfo("entries");
        Links entriesLinks = entries.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("entries.$m==this.map.$m,entries.kvs~this.map.kvs,entries~this.map",
                entriesLinks.toString());
        assertEquals("entries, java.util.Set.$m#entries, java.util.Set.kvs#entries",
                entriesLinks.linkSet().stream().map(l -> l.from().fullyQualifiedName()).sorted()
                        .collect(Collectors.joining(", ")));

        Statement s2 = reverse.methodBody().statements().get(2);
        VariableData vd2 = VariableDataImpl.of(s2);
        VariableInfo viEntry2 = vd2.variableInfo("entry");
        Links entry2Links = viEntry2.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("entry<entries.kvs,entry<this.map.kvs", entry2Links.toString());

        // map.put(entry.getValue(), entry.getKey());
        Statement s200 = reverse.methodBody().statements().get(2).block().statements().getFirst();
        VariableData vd200 = VariableDataImpl.of(s200);
        VariableInfo viEntry200 = vd200.variableInfo("entry");
        Links entry200Links = viEntry200.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                entry.kv.k<entries.kvs,\
                entry.kv.k<map.vks[-2].k,\
                entry.kv.k<this.map.kvs,\
                entry.kv.v<entries.kvs,\
                entry.kv.v<map.vks[-1].v,\
                entry.kv.v<this.map.kvs,\
                entry<entries,\
                entry<this.map\
                """, entry200Links.toString());


        // IMPORTANT reverse0.vks[-1].v~this.map.kvs[-2].v would be correct; however,
        // because "IS_FIELD_OF" followed by "IS_ELEMENT_OF" == "IS_ELEMENT_OF", we lose information
        assertEquals("""
                [] --> reverse0.vks[-1].v~this.map.kvs,reverse0.vks[-2].k~this.map.kvs\
                """, mlvReverse0.toString());
    }

    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo reverse = C.findUniqueMethod("reverse", 0);
        MethodLinkedVariables mlvReverse = reverse.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(reverse));

        Statement s1 = reverse.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo thisMap1 = vd1.variableInfo("a.b.C.map");
        Links thisMap1Links = thisMap1.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                this.map.§kvs>entry.§kv.§k,this.map.§kvs>entry.§kv.§v,this.map.§kvs~map.§vks[-1].§v,\
                this.map.§kvs~map.§vks[-2].§k,this.map>entry\
                """, thisMap1Links.toString());
        // the problem here at the moment is that in the graph

        //  assertEquals("", mlvReverse.toString());

        Statement s100 = reverse.methodBody().statements().get(1).block().statements().getFirst();
        VariableData vd100 = VariableDataImpl.of(s100);
        VariableInfo viEntry100 = vd100.variableInfo("entry");
        Links entry100Links = viEntry100.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                entry.§kv.§k<map.§vks[-2].§k,entry.§kv.§k<this.map.§kvs,\
                entry.§kv.§v<map.§vks[-1].§v,entry.§kv.§v<this.map.§kvs,entry<this.map\
                """, entry100Links.toString());

        MethodInfo staticReverse = C.findUniqueMethod("staticReverse", 1);
        MethodLinkedVariables tlvSReverse = staticReverse.analysis().getOrCreate(METHOD_LINKS,
                () -> tlc.doMethod(staticReverse));

        VariableData vd0 = VariableDataImpl.of(staticReverse.methodBody().statements().getFirst());
        VariableInfo r0 = vd0.variableInfo("r");
        Links tlvR0 = r0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        // Not as correct as could be
        assertEquals("r.§vks[-1].§v~0:c.map.§kvs,r.§vks[-2].§k~0:c.map.§kvs", tlvR0.toString());

        assertEquals("""
                [-] --> staticReverse.§vks[-1].§v~0:c.map.§kvs,staticReverse.§vks[-2].§k~0:c.map.§kvs\
                """, tlvSReverse.toString());
    }


    @Test
    public void testShallow2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, true);

        MethodInfo reverse = X.findUniqueMethod("reverse", 0);
        MethodLinkedVariables tlvReverse = reverse.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(reverse));
        assertEquals("[] --> reverse.§m==this.§m,reverse.§vk==this.§kv", tlvReverse.toString());

        MethodInfo method = X.findUniqueMethod("staticReverse", 1);
        MethodLinkedVariables tlvSReverse = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("[-] --> staticReverse.§yx==0:c.§xy", tlvSReverse.toString());
    }

}
