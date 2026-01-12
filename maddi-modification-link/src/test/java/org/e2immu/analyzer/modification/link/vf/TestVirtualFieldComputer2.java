package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

}
