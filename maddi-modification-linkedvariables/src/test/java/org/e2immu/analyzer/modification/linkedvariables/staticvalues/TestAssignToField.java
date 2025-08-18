package org.e2immu.analyzer.modification.linkedvariables.staticvalues;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAssignToField extends CommonTest {
    @Language("java")
    private static final String INPUT = """
            package a.b;

            import org.e2immu.annotation.NotModified;
            import java.util.ArrayList;
            import java.util.List;

            public class X {

                record A(List<String> list1, List<String> list2) {
                }

                public String method1(String in1, String in2) {
                    A a = new A(new ArrayList<>(), new ArrayList<>());
                    a.list1.add(in1);
                    int i1 = a.list1.size();

                    a.list2.add(in2);
                    int i2 = a.list2.size();

                    return in1.substring(i1) + in2.substring(i2);
                }

                public String method2(String in1, String in2) {
                    A a = new A(new ArrayList<>(), new ArrayList<>());
                    a.list2.add(in2);
                    int i2 = a.list2.size();

                    a.list1.add(in1);
                    int i1 = a.list1.size();

                    return in1.substring(i1) + in2.substring(i2);
                }

                public String method3(String in1, String in2) {
                    A a = new A(new ArrayList<>(), new ArrayList<>());
                    a.list2.add(in2);
                    a.list1.add(in1);

                    int i1 = a.list1.size();
                    int i2 = a.list2.size();

                    return in1.substring(i1) + in2.substring(i2);
                }

                public String method4(String in1, String in2) {
                    A a = new A(new ArrayList<>(), new ArrayList<>());
                    a.list1.add(in1);
                    a.list2.add(in2);

                    return in1.substring(a.list1.size()) + in2.substring(a.list2.size());
                }
            }
            """;

    @DisplayName("direct assignment")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        TypeInfo A = X.findSubType("A");
        MethodInfo constructorA = A.findConstructor(2);
        ParameterInfo cA0 = constructorA.parameters().get(0);
        assertEquals("E=this.list1", cA0.analysis().getOrDefault(StaticValuesImpl.STATIC_VALUES_PARAMETER,
                StaticValuesImpl.NONE).toString());
        assertEquals("[a.b.X.A.list1]", cA0.assignedToField().fields().toString());
    }

}
