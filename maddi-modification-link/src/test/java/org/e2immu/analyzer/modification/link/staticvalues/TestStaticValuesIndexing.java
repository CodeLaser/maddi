package org.e2immu.analyzer.modification.link.staticvalues;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStaticValuesIndexing extends CommonTest {

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class X<Y> {
                Y[] ys;
                public void setYs(int i, Y y) {
                    ys[i]=y;
                }
                public Y getYs(int i) {
                    return ys[i];
                }
                Y method() {
                    Y y = getYs(0);
                    setYs(1, y);
                    return y;
                }
            }
            """;

    @DisplayName("indexing in array")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo setYs = X.findUniqueMethod("setYs", 2);
        MethodLinkedVariables mlvSetYs = setYs.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(setYs));
        assertEquals("[-, 1:y≡this.ys[0:i],1:y∈this.ys] --> -", mlvSetYs.toString());

        MethodInfo getYs = X.findUniqueMethod("getYs", 1);
        MethodLinkedVariables mlvGetYs = getYs.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(getYs));
        assertEquals("[-] --> getYs≡this.ys[0:i],getYs∈this.ys", mlvGetYs.toString());

        MethodInfo method = X.findUniqueMethod("method", 0);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viY = vd0.variableInfo("y");
        assertEquals("y≡this.ys[$__l1],y∈this.ys", viY.linkedVariables().toString());

        VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
        VariableInfo viY1 = vd1.variableInfo("y");
        assertEquals("y≡this.ys[$__l1],y≡this.ys[$__l3],y∈this.ys", viY1.linkedVariables().toString());
        assertEquals("[] --> method∈this.ys", mlv.toString());
    }
}
