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

public class TestFieldAnalyzerReadInCondition extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.rc;
            import java.util.List;
            class S {
                private List<String> element;
                void set(List<String> o) {
                    this.element = o;
                }
                List<String> get() {
                    return element;
                }
                Object pick(Object other) {
                    if (element == null) {
                        return other;
                    } else {
                        return this;
                    }
                }
            }
            """;

    @DisplayName("a field only read in the condition of a method's last statement keeps its links")
    @Test
    public void test() {
        TypeInfo S = javaInspector.parse("a.rc.S", INPUT);
        List<Info> ao = prepWork(S);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build());
        analyzer.analyze(ao);
        // guava's MoreCollectors.ToOptionalState.element: in pick(), the field is read in the condition of the
        // if/else that is the method's last statement, and linked in neither branch. The merged variable data then
        // kept no links at all (null, undecided) instead of empty links, the field's links stayed undecided until
        // cycle breaking wrote them EMPTY, and the field -- stored from a parameter, handed out by a getter -- read
        // as @Independent
        FieldInfo element = S.getFieldByName("element", true);
        String links = String.valueOf(element.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class));
        Value.Independent independent = element.analysis().getOrNull(PropertyImpl.INDEPENDENT_FIELD,
                ValueImpl.IndependentImpl.class);
        assertTrue(links.contains("this.element←0:o") && independent != null && independent.isDependent(),
                "links=" + links + " independent=" + independent);
    }
}
