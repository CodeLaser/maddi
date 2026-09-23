package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The order of variables is one order, by fully qualified name. Parameters of one method used to compare by index
 * instead, which agrees with the name order up to nine parameters and contradicts it from ten on: "m:10:x" sorts
 * before "m:2:y" by name, and the array access "m:10:x[i]" sits between them by name, so p2 < p10 < p10[i] < p2.
 * TimSort reported that cycle as "Comparison method violates its general contract" while sorting the links of a
 * method with ten or more parameters (an isolate of the closed-core corpus, 2026-09-22).
 */
public class TestParameterOrder {

    @DisplayName("ten or more parameters: the index order and the name order cannot both hold")
    @Test
    public void test() {
        Runtime runtime = new RuntimeImpl();
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.foo").build();
        TypeInfo ti = runtime.newTypeInfo(cu, "Test");
        cu.setTypes(List.of(ti));
        MethodInfo mi = runtime.newMethod(ti, "method", runtime.methodTypeMethod());
        List<ParameterInfo> parameters = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            parameters.add(mi.builder().addParameter("p" + (char) ('a' + i),
                    i == 10 ? runtime.newParameterizedType(ti, 1) : ti.asSimpleParameterizedType()));
        }
        mi.builder().commitParameters().commit();
        ParameterInfo p2 = parameters.get(2);
        ParameterInfo p10 = parameters.get(10);
        assertEquals("com.foo.Test.method(com.foo.Test,com.foo.Test,com.foo.Test,com.foo.Test,com.foo.Test,"
                     + "com.foo.Test,com.foo.Test,com.foo.Test,com.foo.Test,com.foo.Test,com.foo.Test[]):10:pk",
                p10.fullyQualifiedName());
        DependentVariable p10f = runtime.newDependentVariable(runtime.newVariableExpression(p10),
                runtime.newVariableExpression(parameters.getFirst()));
        assertTrue(p10f.fullyQualifiedName().startsWith(p10.fullyQualifiedName() + "["), p10f.fullyQualifiedName());

        // by name: p10 < p10[p0] < p2
        assertTrue(p10.compareTo(p10f) < 0);
        assertTrue(p10f.compareTo(p2) < 0);
        // so, transitively, p10 < p2: the index order (p2 < p10) cannot hold as well
        assertTrue(p10.compareTo(p2) < 0);
        assertTrue(p2.compareTo(p10) > 0);

        // the whole parameter list sorts as its names do, without an exception, in either starting order
        List<Variable> variables = new ArrayList<>(parameters);
        variables.add(p10f);
        variables.sort(Variable::compareTo);
        List<Variable> reversed = new ArrayList<>(variables.reversed());
        reversed.sort(Variable::compareTo);
        assertEquals(variables, reversed);
        // ":10:pk" < ":1:pb" as well: '0' sorts before ':'
        assertEquals(List.of(parameters.get(0), p10, p10f, parameters.get(1), p2), variables.subList(0, 5));
    }
}
