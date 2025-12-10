package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinksImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPrefix extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.stream.Stream;
            public class C<X, Y> {
            
                public Stream<Map.Entry<X, Y>> one(X x, Y y) {
                    Map.Entry<X, Y> entry = new AbstractMap.SimpleEntry<>(x, y);
                    Stream<Map.Entry<X,Y>> stream1 = Stream.of(entry);
                    return stream1;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        MethodInfo one = C.findUniqueMethod("one", 2);

        // test Stream.of()
        TypeInfo stream = javaInspector.compiledTypesManager().get(Stream.class);
        MethodInfo of1 = stream.methodStream()
                .filter(m -> "of".equals(m.name()) && 1 == m.parameters().size()
                             && m.parameters().getFirst().parameterizedType().arrays() == 0)
                .findFirst().orElseThrow();
        assertEquals("java.util.stream.Stream.of(T)", of1.fullyQualifiedName());
        MethodLinkedVariables tlvOf1 = of1.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> of.ts>0:t", tlvOf1.toString());

        // test SimpleEntry constructor
        TypeInfo simpleEntry = javaInspector.compiledTypesManager().get(AbstractMap.SimpleEntry.class);
        MethodInfo constructor1 = simpleEntry.findConstructor(2);
        assertEquals("java.util.AbstractMap.SimpleEntry.<init>(K,V)", constructor1.fullyQualifiedName());
        MethodLinkedVariables tlvConstructor1 = constructor1.analysis()
                .getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("key(0:*);value(1:*)", tlvConstructor1.toString());

        VariableData vd0 = VariableDataImpl.of(one.methodBody().statements().getFirst());
        VariableInfo viEntry = vd0.variableInfo("entry");
        Links tlvEntry = viEntry.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("x(0:*);y(1:*)", tlvEntry.toString());

        VariableData vd1 = VariableDataImpl.of(one.methodBody().statements().get(1));
        VariableInfo viStream1 = vd1.variableInfo("stream1");
        Links tlvStream1 = viStream1.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("entry(0:*);x([0]0:*);y([0]1:*)", tlvStream1.toString());

        MethodLinkedVariables tlvOne = one.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x([0]0:*);y([0]1:*)", tlvOne.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.stream.Stream;
            public class C<X, Y> {
            
                public Stream<Map.Entry<X, Y>> one(X x, Y y) {
                    return Stream.of(new AbstractMap.SimpleEntry<>(x, y));
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);

        MethodInfo one = C.findUniqueMethod("one", 2);
        MethodLinkedVariables tlvOne = one.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x([0]0:*);y([0]1:*)", tlvOne.toString());
    }


    @Language("java")
    private static final String INPUT3 = """
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

    @Test
    public void test3() {
        TypeInfo C = javaInspector.parse(INPUT3);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);

        MethodInfo oneStatic = C.findUniqueMethod("oneStatic", 2);

        MethodLinkedVariables tlv1Static = oneStatic.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x([0]0:*);y([1]0:*)", tlv1Static.toString());

        MethodInfo oneInstance = C.findUniqueMethod("oneInstance", 2);

        MethodLinkedVariables tlv1Instance = oneInstance.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("x([0]0:*);y([1]0:*)", tlv1Instance.toString());
    }
}
