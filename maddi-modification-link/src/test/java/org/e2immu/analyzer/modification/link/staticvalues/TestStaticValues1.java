package org.e2immu.analyzer.modification.link.staticvalues;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.Variable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStaticValues1 extends CommonTest {

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class X {
                int j;
                int k;
                X setJ(int jp) {
                    j=jp;
                    return this;
                }
                X setJK(int jp, int kp) {
                    j=jp;
                    k=kp;
                    return this;
                }
                static X method(int i) {
                    X x = new X().setJ(i);
                    return x;
                }
                static X method2(int a, int b) {
                    X x = new X().setJK(a, b);
                    return x;
                }
            }
            """;

    @DisplayName("assignment to field in builder")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo setJ = X.findUniqueMethod("setJ", 1);
        MethodLinkedVariables mlvSetJ = setJ.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(setJ));
        FieldInfo fieldJ = X.getFieldByName("j", true);
        {
            Statement s0 = setJ.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0J = vd0.variableInfo(runtime.newFieldReference(fieldJ));
            assertEquals("this.j==0:jp",
                    vi0J.linkedVariables().toString());
        }
        assertEquals("[0:jp==this.j] --> setJ==this", mlvSetJ.toString());

        MethodInfo setJK = X.findUniqueMethod("setJK", 2);
        MethodLinkedVariables mlvSetJK = setJK.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(setJK));
        FieldInfo fieldK = X.getFieldByName("k", true);
        {
            Statement s0 = setJK.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0J = vd0.variableInfo(runtime.newFieldReference(fieldJ));
            assertEquals("this.j==0:jp", vi0J.linkedVariables().toString());
        }
        assertEquals("[0:jp==this.j, 1:kp==this.k] --> setJK==this", mlvSetJK.toString());


        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        {
            VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
            assertEquals("a.b.X.j#scope16-15:16-21, a.b.X.method(int):0:i, x", vd0.knownVariableNamesToString());
            VariableInfo viX = vd0.variableInfo("x");
            assertEquals("x.j==0:i", viX.linkedVariables().toString());

            assertEquals("[-] --> method.j==0:i", mlv.toString());
        }
        MethodInfo method2 = X.findUniqueMethod("method2", 2);
        MethodLinkedVariables mlv2 = method2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method2));
        {
            VariableData vd0 = VariableDataImpl.of(method2.methodBody().statements().getFirst());
            assertEquals("a.b.X.method2(int,int):0:a, a.b.X.method2(int,int):1:b, x", vd0.knownVariableNamesToString());
            VariableInfo viX = vd0.variableInfo("x");
            assertEquals("x.j==0:a,x.k==1:b", viX.linkedVariables().toString());
            assertEquals("a, b", viX.linkedVariables().primaryAssigned().stream().map(Variable::simpleName)
                    .sorted().collect(Collectors.joining(", ")));
            assertEquals("[-, -] --> method2.j==0:a,method2.k==1:b", mlv2.toString());
        }

    }
}
