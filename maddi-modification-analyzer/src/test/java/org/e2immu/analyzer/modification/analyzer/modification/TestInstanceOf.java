package org.e2immu.analyzer.modification.analyzer.modification;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestInstanceOf extends CommonTest {

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class X {
                interface I { }
                record R(Object o) implements I {}
                void method(I i, String s) {
                    if(i instanceof R(Object o) && o instanceof Set set) {
                        set.add(s);
                    }
                }
            }
            """;

    @DisplayName("links of instanceof with record pattern")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> ao = prepAnalyzer.doPrimaryType(X);
        analyzer.go(ao);

        MethodInfo method = X.findUniqueMethod("method", 2);
        ParameterInfo i = method.parameters().getFirst();
        assertTrue(i.isModified());
        MethodLinkedVariables mlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:i≥1:s, 1:s≤0:i] --> -", mlv.toString());
        // there is no 'i.o', so we cannot have a modified components parameter
        Value.VariableBooleanMap map = i.analysis().getOrDefault(PropertyImpl.MODIFIED_COMPONENTS_PARAMETER,
                ValueImpl.VariableBooleanMapImpl.EMPTY);
        assertTrue(map.isEmpty());

    }

}
