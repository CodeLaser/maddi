package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestShallowPrefix extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.stream.Collectors;
            import java.util.stream.Stream;
            public class C<X, Y> {
            
                public Stream<Map.Entry<X, Y>> oneInstance(X x, Y y) {
                    return Stream.of(new AbstractMap.SimpleEntry<>(x, y));
                }
            
                public static <X, Y> Stream<Map.Entry<X, Y>> oneStatic(X x, Y y) {
                    return Stream.of(new AbstractMap.SimpleEntry<>(x, y));
                }
            }
            """;

    // see also TestPrefix,1 and 2
    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, true, true);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        MethodInfo oneStatic = C.findUniqueMethod("oneStatic", 2);
        VirtualFields vfOneStatic = vfc.computeAllowTypeParameterArray(oneStatic.returnType(), false).virtualFields();
        assertEquals("$m - XY[] xys", vfOneStatic.toString());

        MethodInfo oneInstance = C.findUniqueMethod("oneInstance", 2);
        VirtualFields vfOneInstance = vfc.computeAllowTypeParameterArray(oneStatic.returnType(), false).virtualFields();
        assertEquals("$m - XY[] xys", vfOneInstance.toString());

        MethodLinkedVariables tlv1Static = oneStatic.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(oneStatic));
        assertEquals("[-, -] --> oneStatic.kvs[-1]>0:x,oneStatic.kvs[-1]>1:y", tlv1Static.toString());

        MethodLinkedVariables tlv1Instance = oneInstance.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(oneInstance));
        assertEquals("""
                return oneInstance([0]0:0,[0]1:1)[\
                #0:return oneInstance(*:0), \
                #1:return oneInstance(*:1)]\
                """, tlv1Instance.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.stream.Stream;
            public class C<X, Y> {
            
                public Map.Entry<Stream<X>, Stream<Y>> oneInstance(X x, Y y) {
                    return new AbstractMap.SimpleEntry<>(Stream.of(x), Stream.of(y));
                }
            
                public static <X, Y> Map.Entry<Stream<X>, Stream<Y>> oneStatic(X x, Y y) {
                    return new AbstractMap.SimpleEntry<>(Stream.of(x), Stream.of(y));
                }
            }
            """;

    // see also TestPrefix,3
    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, true, true);
        tlc.doPrimaryType(C);

        MethodInfo oneStatic = C.findUniqueMethod("oneStatic", 2);

        MethodLinkedVariables tlv1Static = oneStatic.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x([0]0:*);y([1]0:*)", tlv1Static.toString());

        MethodInfo oneInstance = C.findUniqueMethod("oneInstance", 2);
        MethodLinkedVariables tlv1Instance = oneInstance.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("""
                return oneInstance([0]0:0,[1]0:1)[\
                #0:return oneInstance(*:0), \
                #1:return oneInstance(*:1)]\
                """, tlv1Instance.toString());
    }

}
