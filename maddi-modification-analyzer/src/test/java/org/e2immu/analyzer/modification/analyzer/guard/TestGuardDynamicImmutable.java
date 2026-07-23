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

package org.e2immu.analyzer.modification.analyzer.guard;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.impl.GuardAnalyzerImpl;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Policing the dynamic-immutability contract now that it decides verdicts.
 * <p>
 * {@code SourceContractMaterializer} (part 1) writes {@code @Immutable} on a source field into
 * {@code analysis()}, and {@code DynamicImmutability} (part 3) consumes it: the field, its accessor and often
 * the whole type are lifted out of {@code @Dependent} on the strength of that annotation alone. A promise that
 * moves verdicts has to be checked, or a wrong one manufactures an immutability result out of nothing.
 * <p>
 * {@code guardDynamicImmutableFields} checks it against every assignment the type itself makes to the field.
 * That is a <b>local</b> check, and the tests below pin exactly how far it reaches:
 * <ul>
 *   <li>a refutable lie — storing a freshly constructed mutable object — is an error;</li>
 *   <li>a proven contract — {@code List.copyOf} — is silent;</li>
 *   <li>{@code this.items = items} from a parameter is <em>unverifiable locally</em> and is reported at lower
 *       severity in its own category, because the true and the false version are syntactically identical and
 *       only the call sites can tell them apart. That is part 2's job.</li>
 * </ul>
 * The last test pins that contracts the guard already policed are still enforced, i.e. none of this disarmed
 * anything.
 */
public class TestGuardDynamicImmutable extends CommonTest {

    private record Run(List<Message> messages, TypeInfo typeInfo) {
        List<Message> violations() {
            return messages.stream().filter(m -> GuardAnalyzerImpl.CONTRACT_VIOLATION.equals(m.category())).toList();
        }

        List<Message> unverifiable() {
            return messages.stream().filter(m -> GuardAnalyzerImpl.CONTRACT_UNVERIFIABLE.equals(m.category())).toList();
        }
    }

    private Run analyzeWithGuard(String fqn, String source) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse(fqn, source);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).setGuardContracts(true).build());
        analyzer.analyze(analysisOrder);
        return new Run(analyzer.messages(), typeInfo);
    }

    /**
     * A demonstrably false dynamic-immutability contract: the accessor promises its return is immutable while
     * handing out the very list the constructor stored, which the caller still holds.
     */
    @Language("java")
    private static final String FALSE_CONTRACT = """
            package a.b;
            import java.util.List;
            import org.e2immu.annotation.Immutable;

            public class X {
                @Immutable(hc = true)
                private final List<String> items;

                public X(List<String> items) {
                    this.items = items;
                }

                @Immutable(hc = true)
                public List<String> items() {
                    return items;
                }
            }
            """;

    @DisplayName("storing a parameter is UNVERIFIABLE locally: warned, not accused -- and the promise is trusted")
    @Test
    public void testParameterAssignmentIsUnverifiable() throws IOException {
        Run run = analyzeWithGuard("a.b.X", FALSE_CONTRACT);
        MethodInfo items = run.typeInfo().findUniqueMethod("items", 0);

        // part 1 did its job on both sides
        assertNotNull(items.analysis().getOrNull(PropertyImpl.IMMUTABLE_METHOD, ValueImpl.ImmutableImpl.class),
                "IMMUTABLE_METHOD materialized");
        assertNotNull(run.typeInfo().fields().getFirst().analysis()
                        .getOrNull(PropertyImpl.IMMUTABLE_FIELD, ValueImpl.ImmutableImpl.class),
                "IMMUTABLE_FIELD materialized");

        // LIMITATION, stated loudly because it is the case that matters: this contract is FALSE -- the
        // constructor stores the caller's list and the accessor hands it straight back -- and the guard does
        // NOT call it out as a violation. It cannot: `this.items = items` is character-for-character what
        // TypeInspectionImpl does with a true contract (its callers pass List.copyOf(...)), so refuting it here
        // would accuse correct code. The guard says what it honestly can -- "I am trusting this, not checking
        // it" -- and part 2, which follows the argument to the call site, is what turns this into a verdict.
        assertTrue(run.violations().isEmpty(),
                "the parameter case must not be reported as a violation, have: "
                + run.violations().stream().map(Message::message).toList());
        assertEquals(1, run.unverifiable().size(),
                "exactly one unverifiable finding, have: "
                + run.unverifiable().stream().map(Message::message).toList());
        assertTrue(run.unverifiable().getFirst().message().contains("items"),
                run.unverifiable().getFirst().message());
    }

    /**
     * The same parameter shape, but with the constructor PRIVATE and every call site inside the type passing a
     * defensive copy. That is the one configuration a sound analysis can settle, so
     * {@code DynamicImmutabilityInference} follows the argument to the call site, proves the promise, and the
     * warning above disappears. The guard and the inference share one judgement of "produces an immutable
     * object" precisely so this cannot drift into warning about a promise the analyzer itself would make.
     */
    @Language("java")
    private static final String TRUE_CONTRACT_VIA_CALL_SITES = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            import org.e2immu.annotation.Immutable;

            public class X {
                @Immutable(hc = true)
                private final List<String> items;

                private X(List<String> items) {
                    this.items = items;
                }

                public List<String> items() {
                    return items;
                }

                public static class Builder {
                    private final List<String> collected = new ArrayList<>();
                    public X commit() { return new X(List.copyOf(collected)); }
                }
            }
            """;

    @DisplayName("a private constructor whose callers all copy: proven, so neither warned nor accused")
    @Test
    public void testProvenThroughCallSites() throws IOException {
        Run run = analyzeWithGuard("a.b.X", TRUE_CONTRACT_VIA_CALL_SITES);
        assertTrue(run.violations().isEmpty(),
                "a true contract must not be accused, have: "
                + run.violations().stream().map(Message::message).toList());
        assertTrue(run.unverifiable().isEmpty(),
                "part 2 can enumerate the callers here, so the contract is verified rather than trusted, have: "
                + run.unverifiable().stream().map(Message::message).toList());
    }

    /** The same promise, refutable: the constructor stores a freshly built mutable list. */
    @Language("java")
    private static final String REFUTABLE_LIE = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            import org.e2immu.annotation.Immutable;

            public class X {
                @Immutable(hc = true)
                private final List<String> items;

                public X(List<String> items) {
                    this.items = new ArrayList<>(items);
                }

                public List<String> items() {
                    return items;
                }
            }
            """;

    @DisplayName("a refutable lie -- storing a freshly built mutable list -- IS reported")
    @Test
    public void testRefutableLieIsCaught() throws IOException {
        Run run = analyzeWithGuard("a.b.X", REFUTABLE_LIE);
        assertEquals(1, run.violations().size(),
                "the false @Immutable on the field must be reported, have: "
                + run.violations().stream().map(Message::message).toList());
        assertTrue(run.violations().getFirst().message().contains("mutable object"),
                run.violations().getFirst().message());
    }

    /** The promise kept: List.copyOf really does copy, and the AAPI says so. */
    @Language("java")
    private static final String TRUE_CONTRACT = """
            package a.b;
            import java.util.List;
            import org.e2immu.annotation.Immutable;

            public class X {
                @Immutable(hc = true)
                private final List<String> items;

                public X(List<String> items) {
                    this.items = List.copyOf(items);
                }

                public List<String> items() {
                    return items;
                }
            }
            """;

    @DisplayName("a proven contract is silent -- no violation, and nothing to warn about either")
    @Test
    public void testTrueContractIsSilent() throws IOException {
        Run run = analyzeWithGuard("a.b.X", TRUE_CONTRACT);
        assertTrue(run.violations().isEmpty(), "no violation, have: "
                                               + run.violations().stream().map(Message::message).toList());
        assertTrue(run.unverifiable().isEmpty(), "nothing unverifiable either, have: "
                                                 + run.unverifiable().stream().map(Message::message).toList());
    }

    /**
     * The same file, plus a contract the guard <em>does</em> police: {@code @Immutable} on a type whose field is
     * assigned after construction (rule 0).
     */
    @Language("java")
    private static final String FALSE_CONTRACT_PLUS_REAL_VIOLATION = """
            package a.b;
            import java.util.List;
            import org.e2immu.annotation.Immutable;

            public class X {
                @Immutable(hc = true)
                private final List<String> items;

                public X(List<String> items) {
                    this.items = items;
                }

                @Immutable(hc = true)
                public List<String> items() {
                    return items;
                }

                @Immutable
                static class NotReally {
                    private int count;

                    public void bump() {
                        ++count;
                    }

                    public int count() {
                        return count;
                    }
                }
            }
            """;

    @DisplayName("a contract the guard does police is still enforced, so materializing disarmed nothing")
    @Test
    public void testRealViolationStillCaught() throws IOException {
        Run run = analyzeWithGuard("a.b.X", FALSE_CONTRACT_PLUS_REAL_VIOLATION);
        assertFalse(run.violations().isEmpty(), "the @Immutable contract on NotReally must still be enforced");
        assertTrue(run.violations().stream().allMatch(m -> m.info().fullyQualifiedName().contains("NotReally")),
                "every violation concerns the genuinely-violating type, have: "
                + run.violations().stream().map(m -> m.info().fullyQualifiedName()).toList());
    }
}
