package io.codelaser.maddi.modification.link.impl.basics;

import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.link.impl.LinkComputerImpl;
import io.codelaser.maddi.modification.prepwork.variable.Links;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.variable.Stage;
import io.codelaser.maddi.modification.prepwork.variable.VariableData;
import io.codelaser.maddi.modification.prepwork.variable.VariableInfo;
import io.codelaser.maddi.modification.prepwork.variable.impl.VariableDataImpl;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStringBuilder extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            public class X<T> {
                public String method(List<T> list) {
                    StringBuilder sb = new StringBuilder();
                    T t = list.getFirst();
                    sb.append(t);
                    return sb.toString();
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        Statement newSb = method.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(newSb);
        VariableInfo sb = vd0.variableInfoContainerOrNull("sb").best(Stage.EVALUATION);
        Links sbLinks = sb.linkedVariablesOrEmpty();
        assertEquals("-", sbLinks.toString());

        Statement assignT = method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(assignT);
        VariableInfo t = vd1.variableInfoContainerOrNull("t").best(Stage.EVALUATION);
        Links tLinks = t.linkedVariablesOrEmpty();
        assertEquals("t∈0:list.§ts", tLinks.toString());

        Statement append = method.methodBody().statements().get(2);
        VariableData vd2 = VariableDataImpl.of(append);
        VariableInfo sb2 = vd2.variableInfoContainerOrNull("sb").best(Stage.EVALUATION);
        Links sb2Links = sb2.linkedVariablesOrEmpty();
        assertEquals("-", sb2Links.toString());
    }


}
