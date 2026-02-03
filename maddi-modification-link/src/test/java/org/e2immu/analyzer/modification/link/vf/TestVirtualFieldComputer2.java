package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVirtualFieldComputer2 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.util.Collection;
            
            public class B {
            
                public static <I> Collection<I> combine(Collection<I> target, Collection<I>[] collections) {
                    for (Collection<I> collection : collections) {
                        target.addAll(collection);
                    }
                    return target;
                }
            }
            """;

    @DisplayName("Collection<I>[]")
    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        MethodInfo method = C.findUniqueMethod("combine", 2);
        ParameterInfo collections = method.parameters().getLast();

        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFieldComputer.VfTm vfTm = vfc.compute(collections.parameterizedType(), true);
        assertEquals("""
                VfTm[virtualFields=§m - I[][] §iss, formalToConcrete=E=TP#0 in Collection [] --> I=TP#0 in B.combine [] dim 1
                T=TP#0 in Iterable [] --> I=TP#0 in B.combine [] dim 1]\
                """, vfTm.toString());

    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.HashSet;
            import java.util.Set;
            class B<C> {
                private final Set<C> set;
                B(Set<C> set) {
                    this.set = new HashSet<>(set);
                }
            }
            """;

    @DisplayName("new HashSet<>(set)")
    @Test
    public void test3() {
        TypeInfo B = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(B);
        MethodInfo method = B.findConstructor(1);
        Assignment assignment = (Assignment) method.methodBody().statements().getFirst().expression();
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("Type java.util.HashSet<C>", assignment.value().parameterizedType().toString());
        VirtualFieldComputer.VfTm vfTm = vfc.compute(assignment.value().parameterizedType(), true);
        assertEquals("""
                VfTm[virtualFields=§m - C[] §cs, formalToConcrete=E=TP#0 in AbstractCollection [] --> C=TP#0 in B []
                E=TP#0 in Collection [] --> C=TP#0 in B []
                E=TP#0 in HashSet [] --> C=TP#0 in B []
                E=TP#0 in Set [] --> C=TP#0 in B []
                T=TP#0 in Iterable [] --> C=TP#0 in B []]\
                """, vfTm.toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            class B {
                private final Map<Long, Integer[]> map;
                B() {
                    this.map = new HashMap<>();
                }
            }
            """;

    @DisplayName("new HashMap<Long, Integer[]>()")
    @Test
    public void test4() {
        TypeInfo B = javaInspector.parse(INPUT4);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(B);
        MethodInfo method = B.findConstructor(0);
        Assignment assignment = (Assignment) method.methodBody().statements().getFirst().expression();
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("Type java.util.HashMap<Long,Integer[]>", assignment.value().parameterizedType().toString());
        VirtualFieldComputer.VfTm vfTm = vfc.compute(assignment.value().parameterizedType(), true);
        assertEquals("""
                VfTm[virtualFields=§m - §$$S[] §$$ss, formalToConcrete=K=TP#0 in AbstractMap [] --> Long
                K=TP#0 in HashMap [] --> Long
                K=TP#0 in Map [] --> Long
                V=TP#1 in AbstractMap [] --> Integer[]
                V=TP#1 in HashMap [] --> Integer[]
                V=TP#1 in Map [] --> Integer[]]\
                """, vfTm.toString());

        VirtualFieldComputer.VfTm vfTm2 = vfc.compute(assignment.value().parameterizedType(), false);
        assertEquals("VfTm[virtualFields=§m - §$$S[] §$$ss, formalToConcrete=null]", vfTm2.toString());
    }

    @DisplayName("stack overflow when computing VF of File[]")
    @Test
    public void test5() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo path = javaInspector.compiledTypesManager().getOrLoad(Path.class);
        VirtualFieldComputer.VfTm vfTm = vfc.compute(path.asParameterizedType(), true);
        assertEquals("VfTm[virtualFields=/ - Path §$, formalToConcrete=null]", vfTm.toString());

        TypeInfo file = javaInspector.compiledTypesManager().getOrLoad(File.class);
        ParameterizedType fileArray = file.asParameterizedType().copyWithOneMoreArray();
        VirtualFieldComputer.VfTm vfTmFileArray = vfc.compute(fileArray, true);
        assertEquals("VfTm[virtualFields=§m - File[] §$s, formalToConcrete=]", vfTmFileArray.toString());
    }
}
