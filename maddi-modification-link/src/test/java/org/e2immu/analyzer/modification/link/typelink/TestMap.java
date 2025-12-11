package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinksImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class TestMap extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Map;
            import java.util.HashMap;
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
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);

        MethodInfo get = C.findUniqueMethod("get", 1);
        ReturnVariable rvGet = new ReturnVariableImpl(get);
        ParameterizedType cPt = C.asParameterizedType();
      //  get.analysis().set(METHOD_LINKS, new MethodLinkedVariablesImpl(
      //          new LinksImpl(rvGet, new TypeLinkImpl(INDEX_ALL(get.returnType()), ONE(cPt))),
      //FIXME          null));

        MethodInfo put = C.findUniqueMethod("put", 2);
        ReturnVariable rvPut = new ReturnVariableImpl(put);
        ParameterInfo p0 = put.parameters().getFirst();
        ParameterInfo p1 = put.parameters().get(1);
       // put.analysis().set(METHOD_LINKS, new MethodLinkedVariablesImpl(
       //         new LinksImpl(rvPut, new TypeLinkImpl(INDEX_ALL(put.returnType()), ONE(cPt))),
       //         List.of(new LinksImpl(rvPut, new TypeLinkImpl(INDEX_ALL(p0.parameterizedType()), ZERO(cPt))),
       //FIXME                 new LinksImpl(rvPut, new TypeLinkImpl(INDEX_ALL(p1.parameterizedType()), ONE(cPt))))));

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);

        MethodInfo staticGet = C.findUniqueMethod("staticGet", 2);
        {
            VariableData vd0 = VariableDataImpl.of(staticGet.methodBody().statements().getFirst());
            VariableInfo y0 = vd0.variableInfo("y");
            Links tlvY0 = y0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("c(*:1)", tlvY0.toString());
            assertEquals("c(*[Type param Y]:1[Type a.b.C<X,Y>])", tlvY0.toString());

            MethodLinkedVariables tlvSGet = staticGet.analysis().getOrNull(METHOD_LINKS,
                    MethodLinkedVariablesImpl.class);
            assertEquals("c(*:1)", tlvSGet.toString());
        }

        MethodInfo staticPut = C.findUniqueMethod("staticPut", 3);
        {
            VariableData vd0 = VariableDataImpl.of(staticPut.methodBody().statements().getFirst());
            VariableInfo yy0 = vd0.variableInfo("yy");
            Links tlvY0 = yy0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("c(*:1);v(*:*);y(*:*)", tlvY0.toString());
            ParameterInfo x = staticPut.parameters().getFirst();
            VariableInfo x0 = vd0.variableInfo(x);
            Links tlvX0 = x0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("c(*:0);k(*:*)", tlvX0.toString());

            MethodLinkedVariables tlvSPut = staticPut.analysis().getOrNull(METHOD_LINKS,
                    MethodLinkedVariablesImpl.class);
            assertEquals("c(*:1);v(*:*);y(*:*)[#0:c(*:0);k(*:*), #1:c(*:1);v(*:*), #2:x(*:0);y(*:1)]",
                    tlvSPut.toString());
        }

        MethodInfo staticPut2 = C.findUniqueMethod("staticPut2", 3);
        {
            VariableData vd0 = VariableDataImpl.of(staticPut2.methodBody().statements().getFirst());
            VariableInfo yy0 = vd0.variableInfo("yy");
            Links tlvY0 = yy0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("c(*:1);v(*:*);y(*:*)", tlvY0.toString());
            ParameterInfo x = staticPut2.parameters().get(1);
            VariableInfo x0 = vd0.variableInfo(x);
            Links tlvX0 = x0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("c(*:0);k(*:*)", tlvX0.toString());

            MethodLinkedVariables tlvSPut = staticPut2.analysis().getOrNull(METHOD_LINKS,
                    MethodLinkedVariablesImpl.class);
            assertEquals("c(*:1);v(*:*);y(*:*)[#0:x(*:0);y(*:1), #1:c(*:0);k(*:*), #2:c(*:1);v(*:*)]",
                    tlvSPut.toString());
        }
    }


    @Test
    public void testShallow1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, true);
        tlc.doPrimaryType(X);

        MethodInfo get = X.findUniqueMethod("get", 1);
        MethodLinkedVariables tlvGet = get.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("return get(*:1)[#0:return get(*:0)]", tlvGet.toString());
        assertEquals("""
                return get(*[Type param V]:1[Type a.b.C<K,V>])[#0:return get(*[Type param K]:0[Type a.b.C<K,V>])]\
                """, tlvGet.toString());

        MethodInfo put = X.findUniqueMethod("put", 2);
        MethodLinkedVariables tlvPut = put.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("return put(*:1)[#0:return put(*:0), #1:return put(*:1)]", tlvPut.toString());


        MethodInfo staticGet = X.findUniqueMethod("staticGet", 2);
        MethodLinkedVariables tlvSGet = staticGet.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("c(*:1)[#0:c(*:0)]", tlvSGet.toString());

        MethodInfo staticPut = X.findUniqueMethod("staticPut", 3);
        MethodLinkedVariables tlvSPut = staticPut.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("c(*:1);y(*:*)[#0:c(*:0), #1:c(*:1)]", tlvSPut.toString());

        MethodInfo staticPut2 = X.findUniqueMethod("staticPut2", 3);
        MethodLinkedVariables tlvS2Put = staticPut2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("c(*:1);y(*:*)[#0:x(0:*);y(1:*)]", tlvS2Put.toString());
        assertEquals("""
                c(*[Type param Y]:1[Type a.b.C<X,Y>]);\
                y(*[Type param Y]:*[Type param Y])\
                [#0:x(0[Type a.b.C<X,Y>]:*[Type param X]);y(1[Type a.b.C<X,Y>]:*[Type param Y])]\
                """, tlvS2Put.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Map;
            import java.util.HashMap;
            import java.util.stream.Collectors;
            
            public class C<K, V> {
                Map<K, V> map;
            
                C(Map<K, V> map) { this.map = map; }
            
                private C<V, K> reverse() {
                    return null;
                }
            
                public static <Y, X> C<Y, X> staticReverse(C<X,Y> c) {
                     C<Y, X> r = c.reverse();
                    return r;
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);

        MethodInfo reverse = C.findUniqueMethod("reverse", 0);
        ReturnVariable rvGet = new ReturnVariableImpl(reverse);
        ParameterizedType cPt = C.asParameterizedType();
        //FIXME reverse.analysis().set(METHOD_LINKS, new MethodLinkedVariablesImpl(new LinksImpl(rvGet,
         //       new TypeLinkImpl(Map.of(ZERO(cPt), ONE(cPt), ONE(cPt), ZERO(cPt)))), null));

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);

        MethodInfo staticReverse = C.findUniqueMethod("staticReverse", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);

        VariableData vd0 = VariableDataImpl.of(staticReverse.methodBody().statements().getFirst());
        VariableInfo r0 = vd0.variableInfo("r");
        Links tlvR0 = r0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("c(0:1,1:0)", tlvR0.toString());
        assertEquals("c(0[Type a.b.C<X,Y>]:1[Type a.b.C<X,Y>],1[Type a.b.C<X,Y>]:0[Type a.b.C<X,Y>])",
                tlvR0.toString());

        MethodLinkedVariables tlvSReverse = staticReverse.analysis()
                .getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("c(0:1,1:0)", tlvSReverse.toString());
        assertEquals("c(0[Type a.b.C<X,Y>]:1[Type a.b.C<X,Y>],1[Type a.b.C<X,Y>]:0[Type a.b.C<X,Y>])",
                tlvSReverse.toString());
    }


    @Test
    public void testShallow2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, true);
        tlc.doPrimaryType(X);

        MethodInfo reverse = X.findUniqueMethod("reverse", 0);
        MethodLinkedVariables tlvReverse = reverse.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("return reverse(0:1,1:0)", tlvReverse.toString());

        MethodInfo method = X.findUniqueMethod("staticReverse", 1);
        MethodLinkedVariables tlvSReverse = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("c(0:1,1:0)", tlvSReverse.toString());
        assertEquals("c(0[Type a.b.C<Y,X>]:1[Type a.b.C<X,Y>],1[Type a.b.C<Y,X>]:0[Type a.b.C<X,Y>])",
                tlvSReverse.toString());
    }

}
