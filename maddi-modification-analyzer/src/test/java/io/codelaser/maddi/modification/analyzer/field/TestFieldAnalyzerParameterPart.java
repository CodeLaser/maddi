package io.codelaser.maddi.modification.analyzer.field;

import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import io.codelaser.maddi.modification.prepwork.variable.impl.LinksImpl;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFieldAnalyzerParameterPart extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.pp;
            import java.util.List;
            class Holder {
                private final List<String> list = new java.util.ArrayList<>();
                List<String> getList() {
                    return list;
                }
            }
            class S {
                private final List<String> element;
                S(Holder holder) {
                    this.element = holder.getList();
                }
                void add(String s) {
                    element.add(s);
                }
            }
            """;

    @DisplayName("a field holding a part of a constructor parameter depends on that parameter")
    @Test
    public void test() {
        TypeInfo S = javaInspector.parse("a.pp.S", INPUT);
        List<Info> ao = prepWork(S);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build());
        analyzer.analyze(ao);
        // activemq's SchedulerBroker.systemUsage = brokerService.getSystemUsage(): the field holds the caller's
        // object, reachable through the parameter, so a modification through the field is visible to the caller.
        // Only links TO a parameter counted; a link to a part of one ('this.element ← 0:holder.list') did not
        FieldInfo element = S.getFieldByName("element", true);
        String links = String.valueOf(element.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class));
        Value.Independent independent = element.analysis().getOrNull(PropertyImpl.INDEPENDENT_FIELD,
                ValueImpl.IndependentImpl.class);
        assertTrue(links.contains("0:holder") && independent != null && independent.isDependent(),
                "links=" + links + " independent=" + independent);
    }
}
