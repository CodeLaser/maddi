package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
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
        VirtualFields vfOneStatic = vfc.compute(oneStatic.returnType(), false).virtualFields();
        assertEquals("§m - XY[] §xys", vfOneStatic.toString());

        MethodInfo oneInstance = C.findUniqueMethod("oneInstance", 2);
        VirtualFields vfOneInstance = vfc.compute(oneStatic.returnType(), false).virtualFields();
        assertEquals("§m - XY[] §xys", vfOneInstance.toString());

        MethodLinkedVariables tlv1Static = oneStatic.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(oneStatic));
        assertEquals("[-, -] --> oneStatic.§xys[-1]∋0:x,oneStatic.§xys[-2]∋1:y", tlv1Static.toString());

        VirtualFields vfThis = vfc.compute(C);
        // see TestVirtualFieldComputer,10
        assertEquals("§m - XY §xy", vfThis.toString());

        MethodLinkedVariables tlv1Instance = oneInstance.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(oneInstance));
        assertEquals("""
                [0:x→this.§xy.§x, 1:y→this.§xy.§y] --> oneInstance.§xys∋this.§xy,oneInstance.§m≡this.§m\
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

    // see also TestPrefix,3, which analyses the statements
    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, true, true);

        MethodInfo oneInstance = C.findUniqueMethod("oneInstance", 2);
        MethodInfo oneStatic = C.findUniqueMethod("oneStatic", 2);

        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("§m - XSYS §xsys",
                vfc.compute(oneStatic.returnType(), false).virtualFields().toString());
        assertEquals("§m - XSYS §xsys",
                vfc.compute(oneInstance.returnType(), false).virtualFields().toString());

        MethodLinkedVariables mlv1Static = oneStatic.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(oneStatic));
        assertEquals("[-, -] --> oneStatic.§xsys.§xs∋0:x,oneStatic.§xsys.§ys∋1:y", mlv1Static.toString());

        VirtualFields vfThis = vfc.compute(C);
        // see TestVirtualFieldComputer,10
        assertEquals("§m - XY §xy", vfThis.toString());

        MethodLinkedVariables tlv1Instance = oneInstance.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(oneInstance));
        assertEquals("""
                [0:x→this.§xy.§x, 1:y→this.§xy.§y] --> \
                oneInstance.§xsys.§xs∋this.§xy.§x,oneInstance.§xsys.§ys∋this.§xy.§y,oneInstance.§m≡this.§m\
                """, tlv1Instance.toString());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.Optional;
            public class C<X, Y> {
            
                public Optional<Map.Entry<X, Y>> oneInstance(X x, Y y) {
                    return Optional.of(new AbstractMap.SimpleEntry<>(x, y));
                }
            
                public static <X, Y> Optional<Map.Entry<X, Y>> oneStatic(X x, Y y) {
                    return Optional.of(new AbstractMap.SimpleEntry<>(x, y));
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo C = javaInspector.parse(INPUT3);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, true, true);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        MethodInfo oneStatic = C.findUniqueMethod("oneStatic", 2);
        VirtualFields vfOneStatic = vfc.compute(oneStatic.returnType(), false).virtualFields();
        assertEquals("§m - XY §xy", vfOneStatic.toString());

        MethodInfo oneInstance = C.findUniqueMethod("oneInstance", 2);
        VirtualFields vfOneInstance = vfc.compute(oneStatic.returnType(), false).virtualFields();
        assertEquals("§m - XY §xy", vfOneInstance.toString());

        MethodLinkedVariables tlv1Instance = oneInstance.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(oneInstance));
        assertEquals("""
                [0:x→this.§xy.§x, 1:y→this.§xy.§y] --> oneInstance.§xy←this.§xy,oneInstance.§m≡this.§m\
                """, tlv1Instance.toString());

        MethodLinkedVariables tlv1Static = oneStatic.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(oneStatic));
        assertEquals("[-, -] --> oneStatic.§xy.§x←0:x,oneStatic.§xy.§y←1:y", tlv1Static.toString());
    }


}
