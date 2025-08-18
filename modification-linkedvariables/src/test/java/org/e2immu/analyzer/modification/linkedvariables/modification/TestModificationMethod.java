package org.e2immu.analyzer.modification.linkedvariables.modification;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.EMPTY_PART_OF_CONSTRUCTION;
import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestModificationMethod extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.io.IOException;
            import java.io.InputStream;
            public class X {
                private int method(final int n) throws IOException {
                    int bsLiveShadow = this.bsLive;
                    int bsBuffShadow = this.bsBuff;
                    if (bsLiveShadow < n) {
                        final InputStream inShadow = this.in;
                        do {
                            int thech = inShadow.read();
                            if (thech < 0) {
                                throw new IOException("unexpected end of stream");
                            }
                            bsBuffShadow = (bsBuffShadow << 8) | thech;
                            bsLiveShadow += 8;
                        } while (bsLiveShadow < n);
                        this.bsBuff = bsBuffShadow;
                    }
                    this.bsLive = bsLiveShadow - n;
                    return (bsBuffShadow >> (bsLiveShadow - n)) & ((1 << n) - 1);
                }
            
                private int bsBuff;
                private int bsLive;
                private InputStream in;
            }
            """;

    @DisplayName("simple method modification")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        Value.SetOfInfo poc = X.analysis().getOrDefault(PART_OF_CONSTRUCTION, EMPTY_PART_OF_CONSTRUCTION);
        MethodInfo methodInfo = X.findUniqueMethod("method", 1);
        assertFalse(poc.infoSet().contains(methodInfo));
        assertTrue(methodInfo.isModifying());
    }
}

