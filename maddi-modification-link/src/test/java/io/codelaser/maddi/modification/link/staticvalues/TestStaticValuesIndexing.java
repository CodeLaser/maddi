package io.codelaser.maddi.modification.link.staticvalues;

import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.link.impl.LinkComputerImpl;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import io.codelaser.maddi.modification.prepwork.variable.VariableData;
import io.codelaser.maddi.modification.prepwork.variable.VariableInfo;
import io.codelaser.maddi.modification.prepwork.variable.impl.VariableDataImpl;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStaticValuesIndexing extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class X {
                int method(int[] a) {
                    int j=3;
                    return a[j];
                }
            }
            """;

    @DisplayName("indexing: expand")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        Statement s0 = method.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(s0);

        VariableInfo vi0J = vd0.variableInfo("j");
        assertEquals("j←$_ce0", vi0J.linkedVariables().toString());
        assertEquals("[-] --> method∈0:a,method←0:a[3]", mlv.toString());
    }

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
        TypeInfo X = javaInspector.parse("a.b.X", INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo setYs = X.findUniqueMethod("setYs", 2);
        MethodLinkedVariables mlvSetYs = setYs.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(setYs));
        assertEquals("[-, 1:y∈this.ys*,1:y→this.ys*[0:i]] --> -", mlvSetYs.toString());

        MethodInfo getYs = X.findUniqueMethod("getYs", 1);
        MethodLinkedVariables mlvGetYs = getYs.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(getYs));
        assertEquals("[-] --> getYs∈this.ys,getYs←this.ys[0:i]", mlvGetYs.toString());

        MethodInfo method = X.findUniqueMethod("method", 0);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viY = vd0.variableInfo("y");
        assertEquals("y∈this.ys,y←this.ys[0]", viY.linkedVariables().toString());

        VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
        VariableInfo viY1 = vd1.variableInfo("y");
        assertEquals("y∈this.ys,y←this.ys[0],y→this.ys[1]", viY1.linkedVariables().toString());
        assertEquals("[] --> method∈this.ys*,method←this.ys*[0]*,method→this.ys*[1]", mlv.toString());
        assertEquals("this, this.ys, this.ys[0]", mlv.sortedModifiedString());
    }
}
