package org.e2immu.analyzer.modification.linkedvariables.modification;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestModificationViaFieldToParameter extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class X {
                static class M { int i; M(int i) { this.i = i; } void setI(int i) { this.i = i; } }
                static Go callGo(int i) {
                    M m = new M(i);
                    return new Go(m);
                }
                static class Go {
                    private M m;
                    Go(M m) {
                        this.m = m;
                    }
                    void inc() {
                        this.m.i++;
                    }
                }
                static class Go2 {
                    private M m;
                    Go2(M m) {
                        this.m = m;
                    }
                    int get() {
                        return this.m.i;
                    }
                }
            }
            """;

    @DisplayName("does the modification travel via the field?")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        {
            TypeInfo go = X.findSubType("Go");
            MethodInfo constructor = go.findConstructor(1);
            ParameterInfo p0 = constructor.parameters().getFirst();
            assertFalse(p0.isUnmodified());
        }
        {
            TypeInfo go = X.findSubType("Go2");
            MethodInfo constructor = go.findConstructor(1);
            ParameterInfo p0 = constructor.parameters().getFirst();
            assertTrue(p0.isUnmodified());
        }
    }

}
