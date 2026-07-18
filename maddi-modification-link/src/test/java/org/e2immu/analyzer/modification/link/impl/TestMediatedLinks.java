package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Task #39 step 1 (mediation provenance): the pattern-binding link 'o → ii' is produced MEDIATED
 * (the binding re-mediates the declared type). This test asserts the flag survives into the STORED
 * statement-level links — the place the future declared-type consumer reads. If reconstruction drops
 * it, this test is the precise pin of the remaining work.
 */
public class TestMediatedLinks extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b.ii;
            class C1 {
                interface II { void method1(String s); }
                void m(Object o, String s) {
                    if (o instanceof II ii) ii.method1(s);
                }
            }
            """;

    @DisplayName("pattern-binding link is stored MEDIATED")
    @Test
    public void test() {
        TypeInfo C1 = javaInspector.parse("a.b.ii.C1", INPUT);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(C1);
        LinkComputer lc = new LinkComputerImpl(javaInspector);
        MethodInfo m = C1.findUniqueMethod("m", 2);
        m.analysis().getOrCreate(METHOD_LINKS, () -> lc.doMethod(m));

        VariableData vd = VariableDataImpl.of(m.methodBody().statements().getFirst());
        List<Link> allLinks = StreamSupport.stream(vd.variableInfoIterable(
                        org.e2immu.analyzer.modification.prepwork.variable.Stage.EVALUATION).spliterator(), false)
                .flatMap(vi -> vi.linkedVariables() == null ? Stream.of()
                        : vi.linkedVariables().stream())
                .toList();
        // the o<->ii pair, in whichever direction it was stored
        List<Link> patternLinks = allLinks.stream()
                .filter(l -> ("o".equals(l.from().simpleName()) && "ii".equals(l.to().simpleName()))
                             || ("ii".equals(l.from().simpleName()) && "o".equals(l.to().simpleName())))
                .filter(l -> !l.from().simpleName().contains("§") && !l.to().simpleName().contains("§"))
                .toList();
        assertFalse(patternLinks.isEmpty(), "the pattern-binding pair must be linked: " + allLinks);
        assertTrue(patternLinks.stream().anyMatch(Link::mediated),
                "the stored pattern link must carry the MEDIATED flag: " + patternLinks
                + " — if this fails, reconstruction drops the flag (task #39 remaining work)");
    }
}
