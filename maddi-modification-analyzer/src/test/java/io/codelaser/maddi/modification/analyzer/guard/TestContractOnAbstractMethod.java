/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.modification.analyzer.guard;

import io.codelaser.maddi.cst.api.analysis.Message;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.GuardAnalyzerImpl;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An annotation on a method of an ABSTRACT type is a contract, and must reach {@code analysis()}.
 * <p>
 * A bodiless method has nothing to compute from: the value the analyzer derives for it is the disjunction over its
 * implementations, i.e. a reconstruction of the very declaration the author has already written down.
 * <p>
 * These three cases CHARACTERIZE what already works (they pass on the code as it stands, 2026-09-22) — the point
 * is the CONTROL, which shows the contract is load-bearing rather than the interface being independent anyway.
 * They are the baseline for the case that does NOT work, and is not yet expressed here:
 * <p>
 * <b>⛔ One undecided implementation suppresses the contract entirely.</b>
 * {@code AbstractMethodAnalyzerImpl.methodIndependent} starts from {@code analysis()}, folds the min over the
 * implementations, and returns WITHOUT WRITING as soon as one implementation is still undecided ("undecided is
 * not DEPENDENT — wait"). Nothing ever puts the author's contract into {@code analysis()} first, so the abstract
 * method stays undecided for good. Measured on vavr 1.0.1: {@code @Independent(hc=true)} on
 * {@code io.vavr.Value.iterator()} left all 133 type verdicts untouched and the method itself with no
 * {@code independentMethod} at all — of its 24 implementations, 23 were independent and exactly one
 * ({@code HashMap.iterator()}) was undecided. The guard still reported that one as a violation, because it reads
 * contracts from the CST ({@code ContractReader}) rather than from {@code analysis()} — which is also why
 * materializing the contract cannot blunt the guard's comparison.
 */
public class TestContractOnAbstractMethod extends CommonTest {

    @Language("java")
    private static final String INDEPENDENT_ON_INTERFACE = """
            package a.b;
            import io.codelaser.maddi.annotation.Independent;
            import java.util.ArrayList;
            import java.util.List;

            public class X {

                interface Source {
                    @Independent(hc = true)
                    List<String> items();
                }

                static class BadSource implements Source {
                    private final List<String> list = new ArrayList<>();

                    @Override
                    public List<String> items() {
                        return list; // dependent: violates the contract, and must still be reported
                    }
                }

                static class GoodSource implements Source {
                    private final List<String> list = new ArrayList<>();

                    @Override
                    public List<String> items() {
                        return List.copyOf(list);
                    }
                }
            }
            """;

    @Language("java")
    private static final String NOT_MODIFIED_ON_INTERFACE = """
            package a.b;
            import io.codelaser.maddi.annotation.NotModified;

            public class Y {

                interface Counter {
                    @NotModified
                    int count();
                }

                static class BadCounter implements Counter {
                    private int n;

                    @Override
                    public int count() {
                        return ++n; // modifying: violates the contract
                    }
                }

                static class GoodCounter implements Counter {
                    private final int n = 3;

                    @Override
                    public int count() {
                        return n;
                    }
                }
            }
            """;

    /**
     * CONTROL. The same source with the annotation removed. Without this, the test above proves nothing: a
     * field-less interface can reach {@code @Independent} on its own, in which case {@code items()} would be
     * independent whether or not anyone contracted it, and the assertion would pass vacuously.
     */
    @Language("java")
    private static final String NO_CONTRACT = check(INDEPENDENT_ON_INTERFACE
            .replace("import io.codelaser.maddi.annotation.Independent;\n", "")
            .replace("        @Independent(hc = true)\n", ""));

    /** the control is built by text substitution: fail loudly if a rename ever leaves the annotation in place */
    private static String check(String noContract) {
        if (noContract.contains("@Independent")) {
            throw new AssertionError("the control still carries @Independent; it would prove nothing");
        }
        return noContract;
    }

    @DisplayName("CONTROL: without the annotation, the same interface method is NOT independent")
    @Test
    public void controlWithoutContract() throws IOException {
        Run run = analyzeWithGuard("a.b.X", NO_CONTRACT);
        MethodInfo items = run.typeInfo().findSubType("Source").methodStream()
                .filter(m -> "items".equals(m.name())).findFirst().orElseThrow();
        Value.Independent independent = items.analysis()
                .getOrNull(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.class);
        assertTrue(independent == null || !independent.isAtLeastIndependentHc(),
                "the control must NOT be independent, or the contract test proves nothing; have " + independent);
    }

    @DisplayName("@Independent on an interface method reaches analysis(), despite a dependent implementation")
    @Test
    public void independentContractIsHonoured() throws IOException {
        Run run = analyzeWithGuard("a.b.X", INDEPENDENT_ON_INTERFACE);

        MethodInfo items = run.typeInfo().findSubType("Source").methodStream()
                .filter(m -> "items".equals(m.name())).findFirst().orElseThrow();
        Value.Independent independent = items.analysis()
                .getOrNull(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.class);
        assertNotNull(independent, "the contract on a bodiless method must reach analysis()");
        assertTrue(independent.isAtLeastIndependentHc(),
                "@Independent contracted on a.b.X.Source.items(); computed from implementations it would be "
                + "DEPENDENT, but a bodiless method has nothing to compute from: have " + independent);

        // the other half: trusting the declaration must NOT silence the implementation that breaks it
        Message violation = onlyViolation(run.messages(), "a.b.X.BadSource.items()");
        assertTrue(violation.message().contains("dependent"), violation.message());
        assertTrue(violation.message().contains("@Independent contract on a.b.X.Source.items()"),
                violation.message());
    }

    @DisplayName("@NotModified on an interface method reaches analysis(), despite a modifying implementation")
    @Test
    public void notModifiedContractIsHonoured() throws IOException {
        Run run = analyzeWithGuard("a.b.Y", NOT_MODIFIED_ON_INTERFACE);

        MethodInfo count = run.typeInfo().findSubType("Counter").methodStream()
                .filter(m -> "count".equals(m.name())).findFirst().orElseThrow();
        Value.Bool nonModifying = count.analysis()
                .getOrNull(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
        assertNotNull(nonModifying, "the contract on a bodiless method must reach analysis()");
        assertTrue(nonModifying.isTrue(), "@NotModified contracted on a.b.Y.Counter.count(): have " + nonModifying);

        Message violation = onlyViolation(run.messages(), "a.b.Y.BadCounter.count()");
        assertTrue(violation.message().contains("modifying"), violation.message());
    }

    /**
     * Contract vs CONTRACT. {@code Narrow} inherits {@code @Independent} and re-declares the same method
     * {@code @Independent(absent = true)} — two authored statements, and nothing in the analyzer can rank them.
     * Neither is applied: the property is left to computation, and the disagreement is reported.
     */
    @Language("java")
    private static final String CONFLICTING_CONTRACTS = """
            package a.b;
            import io.codelaser.maddi.annotation.Independent;
            import java.util.ArrayList;
            import java.util.List;

            public class Z {

                interface Source {
                    @Independent
                    List<String> items();
                }

                interface Narrow extends Source {
                    @Override
                    @Independent(absent = true)
                    List<String> items();
                }

                static class Impl implements Narrow {
                    private final List<String> list = new ArrayList<>();

                    @Override
                    public List<String> items() {
                        return list;
                    }
                }
            }
            """;

    @DisplayName("a contract weaker than the one it inherits is reported, and decides nothing")
    @Test
    public void conflictingContractsDecideNothing() throws IOException {
        Run run = analyzeWithGuard("a.b.Z", CONFLICTING_CONTRACTS);

        List<Message> conflicts = run.messages().stream()
                .filter(m -> GuardAnalyzerImpl.CONTRACT_CONFLICT.equals(m.category())).toList();
        assertEquals(1, conflicts.size(), "expected one contract-conflict, have: "
                                          + run.messages().stream().map(Message::message).toList());
        Message conflict = conflicts.getFirst();
        assertEquals("a.b.Z.Narrow.items()", conflict.info().fullyQualifiedName(), conflict.message());
        assertTrue(conflict.level().isError());
        assertTrue(conflict.message().contains("never weaken"), conflict.message());

        // and nothing propagates from it: the weakened declaration must NOT have been seeded as independent
        MethodInfo narrowed = run.typeInfo().findSubType("Narrow").methodStream()
                .filter(m -> "items".equals(m.name())).findFirst().orElseThrow();
        Value.Independent independent = narrowed.analysis()
                .getOrNull(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.class);
        assertTrue(independent == null || !independent.isAtLeastIndependentHc(),
                "an unadjudicable statement must not propagate; have " + independent);
    }

    private record Run(TypeInfo typeInfo, List<Message> messages) {
    }

    private Run analyzeWithGuard(String fqn, String source) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse(fqn, source);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).setGuardContracts(true).build());
        analyzer.analyze(analysisOrder);
        return new Run(typeInfo, analyzer.messages());
    }

    private Message onlyViolation(List<Message> messages, String fqn) {
        List<Message> violations = messages.stream()
                .filter(m -> GuardAnalyzerImpl.CONTRACT_VIOLATION.equals(m.category())).toList();
        assertEquals(1, violations.size(), "expected exactly one violation, have: "
                                           + violations.stream().map(Message::message).toList());
        Message violation = violations.getFirst();
        assertEquals(fqn, violation.info().fullyQualifiedName(), violation.message());
        assertTrue(violation.level().isError());
        return violation;
    }
}
