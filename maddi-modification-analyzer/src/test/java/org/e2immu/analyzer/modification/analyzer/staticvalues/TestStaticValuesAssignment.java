package org.e2immu.analyzer.modification.analyzer.staticvalues;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

public class TestStaticValuesAssignment extends CommonTest {


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Set;
            record X(int j, int k) {
            
                class Builder {
                    int j;
                    int k;
                    Builder setJ(int jp) {
                        j=jp;
                        return this;
                    }
                    Builder setK(int kp) {
                        k=kp;
                        return this;
                    }
                    Builder setJK(int jp, int kp) {
                        j=jp;
                        k=kp;
                        return this;
                    }
                    X build() {
                        return new X(j, k);
                    }
                }
                static X justJ4(int jp) {
                    Builder b = new Builder().setJK(jp, 4);
                    return b.build();
                }
                static X justJ(int jp) {
                    Builder b = new Builder().setJ(jp);
                    return b.build();
                }
                static X setJK(int jp, int kp) {
                    Builder b = new Builder().setJ(jp).setK(kp);
                    return b.build();
                }
            }
            """;

    @DisplayName("from builder to built class")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        analyzer.go(prepWork(X));

        TypeInfo builder = X.findSubType("Builder");
        MethodInfo build = builder.findUniqueMethod("build", 0);
        MethodLinkedVariables mlv = build.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[] --> build.j←this.j,build.k←this.k", mlv.toString());

        MethodInfo builderSetJK = builder.findUniqueMethod("setJK", 2);
        assertTrue(builderSetJK.isFluent());

        MethodInfo builderSetJ = builder.findUniqueMethod("setJ", 1);
        assertTrue(builderSetJ.isFluent());
        assertSame(builder.getFieldByName("j", true), builderSetJ.getSetField().field());

        MethodInfo builderSetK = builder.findUniqueMethod("setK", 1);
        assertTrue(builderSetK.isFluent());
        assertSame(builder.getFieldByName("k", true), builderSetK.getSetField().field());

        {
            MethodInfo justJ = X.findUniqueMethod("justJ", 1);
            Statement s0 = justJ.methodBody().statements().getFirst();
            VariableInfo vi0B = VariableDataImpl.of(s0).variableInfo("b");
            assertEquals("b.j←0:jp", vi0B.linkedVariables().toString());

            MethodLinkedVariables mlvJJ = justJ.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("[-] --> justJ.j←0:jp", mlvJJ.toString());
        }

        {
            MethodInfo justJ4 = X.findUniqueMethod("justJ4", 1);
            VariableData vd0 = VariableDataImpl.of(justJ4.methodBody().statements().getFirst());
            VariableInfo vi0B = vd0.variableInfo("b");
            assertEquals("b.j←0:jp,b.k←$_ce5", vi0B.linkedVariables().toString());

            MethodLinkedVariables mlvJJ = justJ4.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("[-] --> justJ4.j←0:jp,justJ4.k←$_ce5", mlvJJ.toString());
            // NOTE: k == 4 is lost here, not transferring constants a t m
        }

        {
            MethodInfo setJK = X.findUniqueMethod("setJK", 2);
            assertFalse(setJK.isFluent());

            VariableData vd0 = VariableDataImpl.of(setJK.methodBody().statements().getFirst());
            VariableInfo vi0B = vd0.variableInfo("b");
            assertEquals("b.j←0:jp,b.k←1:kp", vi0B.linkedVariables().toString());

            MethodLinkedVariables mlvSetJK = setJK.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("[-, -] --> setJK.j←0:jp,setJK.k←1:kp", mlvSetJK.toString());
        }
    }

}
