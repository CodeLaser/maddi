package org.e2immu.analyzer.modification.analyzer.shadow;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.analyzer.impl.ModAnalyzerForTesting;
import org.e2immu.analyzer.modification.analyzer.impl.SingleIterationAnalyzerImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PLAN-modification-reachability phase 1: validate the shadow pass. Two obligations:
 * (1) on the known frozen-saturation case (the deep capture chain of TestDeepCaptureChain), the
 * shadow must flag exactly the prematurely frozen TRUEs as divergences — and nothing else;
 * (2) on correctly-analyzed code (the functional-capture shapes), the shadow must agree with the
 * frozen properties in both directions.
 * <p>
 * P3 (primitive seeding, docs/handoff-verification-residue.md §7.5): reverse divergences are no
 * longer categorically "a bug in the pass" — with summary-fold seeding gone, they are the class
 * of frozen pessimism the cutover repairs (recursion through abstract declarations, stale early-
 * iteration conservatism the monotone write discipline cannot revisit). Each pinned reverse below
 * is individually justified.
 */
public class TestShadowModificationPass extends CommonTest {

    private ModAnalyzerForTesting trackingAnalyzer() {
        // LINKED_VARIABLES_ARGUMENTS (E1's food) is only written under trackObjectCreations
        return new SingleIterationAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setTrackObjectCreations(true).build());
    }

    @Language("java")
    private static final String DEEP_CHAIN = """
            package a.b;
            import java.util.List;
            class X {
                static class FC1 { List<Integer> f; FC1(List<Integer> in) { this.f = in; } void pass() { new FC2(this.f).pass(); } }
                static class FC2 { List<Integer> f; FC2(List<Integer> in) { this.f = in; } void pass() { new FC3(this.f).pass(); } }
                static class FC3 { List<Integer> f; FC3(List<Integer> in) { this.f = in; } void pass() { new FC4(this.f).pass(); } }
                static class FC4 { List<Integer> f; FC4(List<Integer> in) { this.f = in; } void pass() { new FC5(this.f).pass(); } }
                static class FC5 { List<Integer> f; FC5(List<Integer> in) { this.f = in; } void pass() { SINK.receive(this.f); } }
                static class SINK { static void receive(List<Integer> r) { r.removeFirst(); } }
            }
            """;

    @DisplayName("shadow flags exactly the frozen-TRUE downgrades of the deep capture chain")
    @Test
    public void testDeepChain() {
        TypeInfo X = javaInspector.parse("a.b.X", DEEP_CHAIN);
        List<Info> ao = prepWork(X);
        ModAnalyzerForTesting analyzer = trackingAnalyzer();
        analyzer.go(ao, 7);

        ShadowModificationPass.Report report = new ShadowModificationPass().go(ao);
        System.out.println("SHADOW " + report.summary());
        report.divergences().forEach(d -> System.out.println("SHADOW DIV " + d + " || " + report.explain(d.info())));
        report.reverseDivergences().forEach(d -> System.out.println("SHADOW REV " + d));

        assertEquals(List.of(), report.reverseDivergences(), "reverse divergences are shadow-pass bugs");
        assertEquals(0, report.callSitesWithoutArgumentLinks());
        // the frozen saturation boundary under trackObjectCreations: only FC5 (adjacent to the
        // sink) is decided correctly; FC1..FC4 freeze TRUE — one level shallower than the depth-2
        // saturation measured under the default configuration (the PLAN §1 repro). The shadow must
        // flag each frozen level's constructor parameter, field, and pass(), and nothing else.
        // Every cause chain traces back to the single true seed, FC5.pass() modifying this.f.
        assertEquals(List.of(
                "nonModifyingMethod a.b.X.FC1.pass()",
                "nonModifyingMethod a.b.X.FC2.pass()",
                "nonModifyingMethod a.b.X.FC3.pass()",
                "nonModifyingMethod a.b.X.FC4.pass()",
                "unmodifiedField a.b.X.FC1.f",
                "unmodifiedField a.b.X.FC2.f",
                "unmodifiedField a.b.X.FC3.f",
                "unmodifiedField a.b.X.FC4.f",
                "unmodifiedParameter a.b.X.FC1.<init>(java.util.List):0:in",
                "unmodifiedParameter a.b.X.FC2.<init>(java.util.List):0:in",
                "unmodifiedParameter a.b.X.FC3.<init>(java.util.List):0:in",
                "unmodifiedParameter a.b.X.FC4.<init>(java.util.List):0:in"
        ), report.sortedDivergenceStrings());
    }

    @Language("java")
    private static final String FUNCTIONAL = """
            package a.b;
            class X {
                interface Op { int apply(String s); }
                int j;
                int go(String in) {
                    return run(in, t -> { j = t.length(); return j; });
                }
                int run(String s, Op op) {
                    return op.apply(s);
                }
            }
            """;

    @DisplayName("shadow agrees with the frozen properties on the functional-capture shape")
    @Test
    public void testFunctional() {
        TypeInfo X = javaInspector.parse("a.b.X", FUNCTIONAL);
        List<Info> ao = prepWork(X);
        ModAnalyzerForTesting analyzer = trackingAnalyzer();
        analyzer.go(ao);

        ShadowModificationPass.Report report = new ShadowModificationPass().go(ao);
        System.out.println("SHADOW " + report.summary());
        report.divergences().forEach(d -> System.out.println("SHADOW DIV " + d + " || " + report.explain(d.info())));
        report.reverseDivergences().forEach(d -> System.out.println("SHADOW REV " + d));

        assertEquals(List.of(), report.reverseDivergences(), "reverse divergences are shadow-pass bugs");
        assertEquals(List.of(), report.sortedDivergenceStrings(),
                "correctly-analyzed code must diff clean");
    }

    @Language("java")
    private static final String BUILDER_CALLBACK = """
            package a.b;
            import java.util.HashSet;
            import java.util.Set;
            public class X {
                @FunctionalInterface
                public interface ThrowingFunction {
                    void apply(TryData o) throws Throwable;
                }
                public interface TryData {
                    ThrowingFunction throwingFunction();
                }
                public static class TryDataImpl implements TryData {
                    private final ThrowingFunction throwingFunction;
                    public TryDataImpl(ThrowingFunction throwingFunction) {
                        this.throwingFunction = throwingFunction;
                    }
                    @Override
                    public ThrowingFunction throwingFunction() {
                        return throwingFunction;
                    }
                }
                public static void run(TryData td) {
                    try {
                        td.throwingFunction().apply(td);
                    } catch (Throwable e) {
                        throw new UnsupportedOperationException();
                    }
                }
                public static void outer(TryData td) {
                    run(td);
                }
                private Set<Integer> someSet = new HashSet<>();
                public void method() {
                    TryData td = new TryDataImpl(this::methodBody);
                    outer(td);
                }
                private void methodBody(TryData tryData) {
                    this.someSet.add(5);
                }
            }
            """;

    @DisplayName("shadow agrees on the forwarding-hop callback shape (E7 via creation-site seeds)")
    @Test
    public void testBuilderCallback() {
        TypeInfo X = javaInspector.parse("a.b.X", BUILDER_CALLBACK);
        List<Info> ao = prepWork(X);
        ModAnalyzerForTesting analyzer = trackingAnalyzer();
        analyzer.go(ao, 7);

        ShadowModificationPass.Report report = new ShadowModificationPass().go(ao);
        System.out.println("SHADOW " + report.summary());
        report.divergences().forEach(d -> System.out.println("SHADOW DIV " + d + " || " + report.explain(d.info())));
        report.reverseDivergences().forEach(d -> System.out.println("SHADOW REV " + d));

        // P3 re-pin: the engine's own frozen state is internally inconsistent here —
        // ThrowingFunction.apply:0 aggregates unmodified=TRUE from its sole implementation
        // (methodBody never touches tryData), yet run:td / outer:td keep the stale FALSE written
        // while apply was still undecided (the monotone discipline never revisits it). td is
        // genuinely unmodified; these two reverses are exactly the class the cutover upgrades.
        assertEquals(List.of(
                        "unmodifiedParameter a.b.X.run(a.b.X.TryData):0:td",
                        "unmodifiedParameter a.b.X.outer(a.b.X.TryData):0:td"),
                report.reverseDivergences().stream().map(Object::toString).toList());
        assertEquals(List.of(), report.sortedDivergenceStrings(),
                "correctly-analyzed code must diff clean");
    }
}
