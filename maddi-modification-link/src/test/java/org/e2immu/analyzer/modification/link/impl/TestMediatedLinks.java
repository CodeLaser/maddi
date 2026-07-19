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

    @DisplayName("pattern-binding link is stored MEDIATED (engine-edge path)")
    @Test
    public void test() {
        TypeInfo C1 = javaInspector.parse("a.b.ii.C1", INPUT);
        List<Link> pairLinks = mediatedPairLinks(C1, 0);
        assertTrue(pairLinks.stream().anyMatch(Link::mediated),
                "the stored pattern link must carry the MEDIATED flag: " + pairLinks
                + " — if this fails, reconstruction drops the flag (task #39 remaining work)");
    }

    @Language("java")
    private static final String INPUT_CAST = """
            package a.b.ii;
            class C2 {
                interface II { void method1(String s); }
                void m(Object o, String s) {
                    II ii = (II) o;
                    ii.method1(s);
                }
            }
            """;

    @DisplayName("cast-initializer link is stored MEDIATED (shared-variable collapse path)")
    @Test
    public void testCast() {
        TypeInfo C2 = javaInspector.parse("a.b.ii.C2", INPUT_CAST);
        List<Link> pairLinks = mediatedPairLinks(C2, 0);
        assertTrue(pairLinks.stream().anyMatch(Link::mediated),
                "the stored cast link must carry the MEDIATED flag: " + pairLinks);
    }

    @Language("java")
    private static final String INPUT_PLAIN = """
            package a.b.ii;
            class C3 {
                interface II { void method1(String s); }
                void m(II o, String s) {
                    II ii = o;
                    ii.method1(s);
                }
            }
            """;

    @DisplayName("a plain, unmediated assignment must NOT be flagged")
    @Test
    public void testPlain() {
        TypeInfo C3 = javaInspector.parse("a.b.ii.C3", INPUT_PLAIN);
        List<Link> pairLinks = mediatedPairLinks(C3, 0);
        assertFalse(pairLinks.isEmpty(), "the plain assignment pair must be linked");
        assertTrue(pairLinks.stream().noneMatch(Link::mediated),
                "a plain assignment carries no mediation: " + pairLinks);
    }

    @Language("java")
    private static final String INPUT_CHAIN = """
            package a.b.ii;
            class C4 {
                // two abstract methods: NOT a functional interface (FI values ride the Λ machinery
                // and have no plain return summary)
                interface II { void method1(String s); void method2(); }
                II m(Object o) {
                    II ii = (II) o;
                    II jj = ii;
                    return jj;
                }
            }
            """;

    @DisplayName("a chain across a mediated hop taints the summary link (return<-jj<-ii<-(cast)o)")
    @Test
    public void testChain() {
        Link l = returnToParameterLink("a.b.ii.C4", INPUT_CHAIN);
        assertTrue(l.mediated(), "every path from the return to 0:o crosses the cast hop: " + l);
    }

    @Language("java")
    private static final String INPUT_CHAIN_PLAIN = """
            package a.b.ii;
            class C5 {
                interface II { void method1(String s); void method2(); }
                II m(II o) {
                    II ii = o;
                    II jj = ii;
                    return jj;
                }
            }
            """;

    @DisplayName("an unmediated chain stays unmediated at the summary")
    @Test
    public void testChainPlain() {
        Link l = returnToParameterLink("a.b.ii.C5", INPUT_CHAIN_PLAIN);
        assertFalse(l.mediated(), "no hop is mediated, the summary link must not be: " + l);
    }

    private Link returnToParameterLink(String fqn, String input) {
        TypeInfo typeInfo = javaInspector.parse(fqn, input);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(typeInfo);
        LinkComputer lc = new LinkComputerImpl(javaInspector);
        MethodInfo m = typeInfo.findUniqueMethod("m", 1);
        var mlv = m.analysis().getOrCreate(METHOD_LINKS, () -> lc.doMethod(m));
        return mlv.ofReturnValue().stream()
                .filter(l -> "o".equals(l.to().simpleName()))
                .findFirst()
                .orElseThrow(() -> {
                    var lastVd = VariableDataImpl.of(m.methodBody().statements().getLast());
                    StringBuilder sb = new StringBuilder();
                    lastVd.variableInfoIterable(org.e2immu.analyzer.modification.prepwork.variable.Stage.MERGE)
                            .forEach(vi -> sb.append(vi.variable().simpleName()).append("=")
                                    .append(vi.linkedVariables()).append("; "));
                    return new AssertionError("expected a return<-0:o summary link; mlv=" + mlv
                            + "; last statement links: " + sb);
                });
    }

    // stored statement-level links between 'o' and 'ii' at the given statement, in whichever direction
    private List<Link> mediatedPairLinks(TypeInfo typeInfo, int statementIndex) {
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(typeInfo);
        LinkComputer lc = new LinkComputerImpl(javaInspector);
        MethodInfo m = typeInfo.findUniqueMethod("m", 2);
        m.analysis().getOrCreate(METHOD_LINKS, () -> lc.doMethod(m));

        VariableData vd = VariableDataImpl.of(m.methodBody().statements().get(statementIndex));
        List<Link> allLinks = StreamSupport.stream(vd.variableInfoIterable(
                        org.e2immu.analyzer.modification.prepwork.variable.Stage.EVALUATION).spliterator(), false)
                .flatMap(vi -> vi.linkedVariables() == null ? Stream.of()
                        : vi.linkedVariables().stream())
                .toList();
        List<Link> pairLinks = allLinks.stream()
                .filter(l -> ("o".equals(l.from().simpleName()) && "ii".equals(l.to().simpleName()))
                             || ("ii".equals(l.from().simpleName()) && "o".equals(l.to().simpleName())))
                .filter(l -> !l.from().simpleName().contains("§") && !l.to().simpleName().contains("§"))
                .toList();
        assertFalse(pairLinks.isEmpty(), "the o/ii pair must be linked: " + allLinks);
        return pairLinks;
    }
}
