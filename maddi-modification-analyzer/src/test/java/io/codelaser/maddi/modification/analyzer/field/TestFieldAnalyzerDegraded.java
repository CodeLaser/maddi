package io.codelaser.maddi.modification.analyzer.field;

import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import io.codelaser.maddi.modification.link.impl.LinkComputerImpl;
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

public class TestFieldAnalyzerDegraded extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.dg;
            import java.util.List;
            class W {
                private final List<String> list;
                W(List<String> list) {
                    this.list = list;
                }
                public List<String> getList() {
                    return list;
                }
                public int init() {
                    return list.size();
                }
            }
            """;

    @DisplayName("a method that degraded does not make the fields it refers to independent")
    @Test
    public void test() {
        TypeInfo W = javaInspector.parse("a.dg.W", INPUT);
        List<Info> ao = prepWork(W);
        LinkComputerImpl.DEGRADE_FOR_TESTING = mi -> mi.name().equals("init");
        try {
            IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(javaInspector,
                    new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build());
            analyzer.analyze(ao);
        } finally {
            LinkComputerImpl.DEGRADE_FOR_TESTING = null;
        }
        // fernflower's ClassWrapper.classStruct: init() tripped the work ceiling, its variable data kept no links,
        // the field's links stayed undecided until cycle breaking wrote them EMPTY, and the field -- stored from a
        // constructor parameter, handed out by a getter -- read as @Independent
        FieldInfo list = W.getFieldByName("list", true);
        Value.Independent independent = list.analysis().getOrNull(PropertyImpl.INDEPENDENT_FIELD,
                ValueImpl.IndependentImpl.class);
        String report = "degraded init: " + W.findUniqueMethod("init", 0).analysis()
                .getOrNull(PropertyImpl.DEGRADED_ANALYSIS_METHOD, ValueImpl.BoolImpl.class)
                        + ", links: " + list.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class)
                        + ", independent: " + independent;
        assertTrue(independent != null && independent.isDependent(), report);
    }
}
