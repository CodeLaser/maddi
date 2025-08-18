package org.e2immu.analyzer.modification.prepwork.hcs;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_PARAMETER;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVarargs extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.util.Collection;
            
            public class B {
            
                public static <T extends Collection<I>, I> T combine(T target, Collection<I>... collections) {
                    for (Collection<I> collection : collections) {
                        target.addAll(collection);
                    }
                    return target;
                }
            }
            """;

    @DisplayName("varargs")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.doPrimaryType(B);

        MethodInfo combine = B.findUniqueMethod("combine", 2);
        HiddenContentTypes combineHCT = combine.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals(" - 0=T, 1=I, 2=Collection", combineHCT.detailedSortedTypes());
        ParameterInfo pi0 = combine.parameters().get(0);
        assertEquals("0=*", pi0.analysis().getOrDefault(HCS_PARAMETER, HiddenContentSelector.NONE).detailed());
        ParameterInfo pi1 = combine.parameters().get(1);
        // the 0 is the index in the type parameters of Collection; 2=2 is for the array type
        assertEquals("1=0,2=2", pi1.analysis().getOrDefault(HCS_PARAMETER, HiddenContentSelector.NONE).detailed());
    }
}
