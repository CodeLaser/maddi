package org.e2immu.analyzer.modification.linkedvariables.fluent;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.StaticValues;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.junit.jupiter.api.Assertions.*;

public class TestFluent extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class B {
                private int i;
                B setI(int i) { this.i = i; return this; }
                B setI2(int i, boolean condition) {
                    if (condition) {
                        System.out.println("true, i = "+i);
                        return this;
                    } else {
                        System.out.println("false, i = "+i);
                        B b = this;
                        return b;
                    }
                }
            }
            """;

    @DisplayName("builder with fluent methods")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        FieldInfo iField = B.getFieldByName("i", true);
        {
            // @GetSet @Fluent
            MethodInfo setI = B.findUniqueMethod("setI", 1);
            assertSame(FALSE, setI.analysis().getOrDefault(PropertyImpl.IDENTITY_METHOD, FALSE));
            assertSame(TRUE, setI.analysis().getOrDefault(PropertyImpl.FLUENT_METHOD, FALSE));
            assertTrue(setI.isFluent());
            Value.FieldValue fvI = setI.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD, ValueImpl.GetSetValueImpl.EMPTY);
            assertSame(iField, fvI.field());
        }
        {
            // @Fluent, not @GetSet
            MethodInfo setI2 = B.findUniqueMethod("setI2", 2);

            IfElseStatement ifElse = (IfElseStatement) setI2.methodBody().statements().get(0);
            {
                Statement s001 = ifElse.block().statements().get(1);
                VariableData vd001 = VariableDataImpl.of(s001);
                VariableInfo viRv001 = vd001.variableInfo(setI2.fullyQualifiedName());
                assertEquals("E=this", viRv001.staticValues().toString());
            }
            {
                Statement s012 = ifElse.elseBlock().statements().get(2);
                VariableData vd012 = VariableDataImpl.of(s012);
                VariableInfo viRv012 = vd012.variableInfo(setI2.fullyQualifiedName());
                assertEquals("E=this", viRv012.staticValues().toString());
            }
            {
                VariableData vd0 = VariableDataImpl.of(ifElse);
                VariableInfo viRv0 = vd0.variableInfo(setI2.fullyQualifiedName());
                assertEquals("E=this", viRv0.staticValues().toString());
            }

            assertSame(FALSE, setI2.analysis().getOrDefault(PropertyImpl.IDENTITY_METHOD, FALSE));
            Value.FieldValue fvI = setI2.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD, ValueImpl.GetSetValueImpl.EMPTY);
            assertSame(ValueImpl.GetSetValueImpl.EMPTY, fvI);

            StaticValues sv = setI2.analysis().getOrDefault(StaticValuesImpl.STATIC_VALUES_METHOD, StaticValuesImpl.NONE);
            assertEquals("E=this", sv.toString());

            assertSame(TRUE, setI2.analysis().getOrDefault(PropertyImpl.FLUENT_METHOD, FALSE));
            assertTrue(setI2.isFluent());
        }
    }
}
