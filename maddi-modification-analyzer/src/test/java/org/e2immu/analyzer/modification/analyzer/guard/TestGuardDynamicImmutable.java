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
 * What materializing a dynamic-immutability contract does, and does not, do to guard mode.
 * <p>
 * {@code SourceContractMaterializer} writes {@code @Immutable} on a source field or method into
 * {@code analysis()}. Materializing a contract means the analyzer stops deriving a value and starts trusting
 * one, so the standing question for any such change is whether it blunts the guard: if "computed" becomes
 * trivially equal to "contracted", the guard can no longer catch a user who promised something untrue.
 * <p>
 * For these two properties it cannot, because the guard never reads them. {@code guardMethod} polices
 * {@code NON_MODIFYING_METHOD} and {@code INDEPENDENT_METHOD} on abstract methods;
 * {@code immutableFieldFailingRule} reads {@code FINAL_FIELD}, {@code UNMODIFIED_FIELD}, the field's
 * <em>declared</em> type and {@code INDEPENDENT_FIELD}. Neither {@code IMMUTABLE_METHOD} nor
 * {@code IMMUTABLE_FIELD} appears anywhere in {@code GuardAnalyzerImpl}. The first test below pins that as the
 * measured reality; the second pins that a contract the guard <em>does</em> police is still enforced, i.e. the
 * materializer did not quietly disarm it.
 */
public class TestGuardDynamicImmutable extends CommonTest {

    private record Run(List<Message> messages, TypeInfo typeInfo) {
        List<Message> violations() {
            return messages.stream().filter(m -> GuardAnalyzerImpl.CONTRACT_VIOLATION.equals(m.category())).toList();
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

    @DisplayName("a false dynamic-immutability contract is materialized, and the guard does not police it")
    @Test
    public void testFalseContractNotPoliced() throws IOException {
        Run run = analyzeWithGuard("a.b.X", FALSE_CONTRACT);
        MethodInfo items = run.typeInfo().findUniqueMethod("items", 0);

        // part 1 did its job on both sides
        assertNotNull(items.analysis().getOrNull(PropertyImpl.IMMUTABLE_METHOD, ValueImpl.ImmutableImpl.class),
                "IMMUTABLE_METHOD materialized");
        assertNotNull(run.typeInfo().fields().getFirst().analysis()
                        .getOrNull(PropertyImpl.IMMUTABLE_FIELD, ValueImpl.ImmutableImpl.class),
                "IMMUTABLE_FIELD materialized");

        // LIMITATION, not desired behaviour: the promise is false -- the constructor stores the caller's list
        // and the accessor hands it straight back -- yet nothing reports it, because GuardAnalyzerImpl reads
        // neither property. Materializing therefore cannot have blunted a check: there was none to blunt.
        // Whoever wires up consumption (part 3) should add the matching guard check at the same time.
        assertTrue(run.violations().isEmpty(),
                "no violation is reported for a false dynamic-immutability contract, have: "
                + run.violations().stream().map(Message::message).toList());
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
