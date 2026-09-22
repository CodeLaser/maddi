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
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PARAMETER half of {@link TestContractOnAbstractMethod}: {@code @NotModified} on a parameter of a BODILESS
 * method is decided by contract, exactly as the method-level property is.
 * <p>
 * The rationale is the same and so is the machinery. A bodiless method's parameter has nothing to compute from:
 * {@code AbstractMethodAnalyzerImpl.unmodified} folds {@code UNMODIFIED_PARAMETER} over the implementations'
 * parameter at the same index, and a single modifying implementation makes the fold write FALSE — a
 * reconstruction of the very declaration the author wrote on the interface. And because
 * {@code UNMODIFIED_PARAMETER} is one of the three properties the MODREACH cutover freezes, a seed that is not
 * also protected there is overwritten by {@code ShadowModificationPass.write} (which calls
 * {@code analysis.overwrite} directly, bypassing TolerantWrite) and can never be re-seeded.
 * <p>
 * ⛔ {@code UNMODIFIED_FIELD}, the third frozen property, deliberately gets NO equivalent. A field is never
 * bodiless — {@code FieldAnalyzerImpl} computes it from the code that touches the field — and
 * {@code SourceContractMaterializer.materialize(FieldInfo)} accordingly never materializes it on source. There
 * is nothing for the cutover to overwrite, and giving a field contract the "decided" treatment would assert
 * something the analyzer can check, which is what guard mode is for.
 */
public class TestContractOnAbstractParameter extends CommonTest {

    @Language("java")
    private static final String NOT_MODIFIED_ON_PARAMETER = """
            package a.b;
            import io.codelaser.maddi.annotation.NotModified;
            import java.util.List;

            public class X {

                interface Sink {
                    void accept(@NotModified List<String> items);
                }

                static class BadSink implements Sink {
                    @Override
                    public void accept(List<String> items) {
                        items.add("x"); // modifies the argument: violates the contract
                    }
                }

                static class GoodSink implements Sink {
                    private int size;

                    @Override
                    public void accept(List<String> items) {
                        size = items.size();
                    }
                }
            }
            """;

    /**
     * CONTROL. Without the annotation the same parameter must NOT come out unmodified — otherwise the test
     * above would pass whether or not anybody contracted anything.
     */
    @Language("java")
    private static final String NO_CONTRACT = check(NOT_MODIFIED_ON_PARAMETER
            .replace("import io.codelaser.maddi.annotation.NotModified;\n", "")
            .replace("@NotModified ", ""));

    private static String check(String noContract) {
        if (noContract.contains("@NotModified")) {
            throw new AssertionError("the control still carries @NotModified; it would prove nothing");
        }
        return noContract;
    }

    @DisplayName("CONTROL: without the annotation, the interface parameter is NOT unmodified")
    @Test
    public void controlWithoutContract() throws IOException {
        for (boolean modReach : new boolean[]{false, true}) {
            Run run = analyze("a.b.X", NO_CONTRACT, modReach);
            Value.Bool unmodified = accept(run).parameters().getFirst().analysis()
                    .getOrNull(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
            assertTrue(unmodified == null || unmodified.isFalse(),
                    "the control must NOT be unmodified, or the contract test proves nothing; modReach="
                    + modReach + ", have " + unmodified);
        }
    }

    @DisplayName("@NotModified on a bodiless method's parameter reaches analysis(), despite a modifying impl")
    @Test
    public void parameterContractIsHonoured() throws IOException {
        Run run = analyze("a.b.X", NOT_MODIFIED_ON_PARAMETER, false);
        assertContracted(run, "without MODREACH");
    }

    @DisplayName("... and the MODREACH cutover must not downgrade it either")
    @Test
    public void parameterContractSurvivesModificationReachability() throws IOException {
        Run run = analyze("a.b.X", NOT_MODIFIED_ON_PARAMETER, true);
        assertContracted(run, "with MODREACH");
    }

    /**
     * vavr's actual shape, and the case {@code ShallowMethodAnalyzer} does NOT cover: it reads the parameter's
     * OWN annotations, so an intermediate interface that re-declares the method abstract without repeating the
     * annotation gets nothing. On the corpus this is the difference between honouring 10 contracts and honouring
     * the 40 declarations they bind — see {@code TestContractOnAbstractMethod.contractThroughIntermediateAbstract}.
     */
    @Language("java")
    private static final String CONTRACT_THROUGH_INTERMEDIATE = """
            package a.b;
            import io.codelaser.maddi.annotation.NotModified;
            import java.util.List;

            public class W {

                interface Root {
                    void accept(@NotModified List<String> items);
                }

                interface Middle extends Root {
                    @Override
                    void accept(List<String> items);
                }

                static class Impl implements Middle {
                    @Override
                    public void accept(List<String> items) {
                        items.add("x");
                    }
                }
            }
            """;

    @DisplayName("the parameter contract binds an intermediate abstract re-declaration (vavr's shape)")
    @Test
    public void parameterContractThroughIntermediateAbstract() throws IOException {
        for (boolean modReach : new boolean[]{false, true}) {
            Run run = analyze("a.b.W", CONTRACT_THROUGH_INTERMEDIATE, modReach);
            for (String iface : new String[]{"Root", "Middle"}) {
                MethodInfo accept = run.typeInfo().findSubType(iface).methodStream()
                        .filter(m -> "accept".equals(m.name())).findFirst().orElseThrow();
                Value.Bool unmodified = accept.parameters().getFirst().analysis()
                        .getOrNull(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
                assertNotNull(unmodified, "the contract must reach " + iface + ".accept, modReach=" + modReach);
                assertTrue(unmodified.isTrue(), iface + ", modReach=" + modReach + ": have " + unmodified);
            }
        }
    }

    private void assertContracted(Run run, String what) {
        ParameterInfo items = accept(run).parameters().getFirst();
        Value.Bool unmodified = items.analysis()
                .getOrNull(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
        assertNotNull(unmodified, "the contract on a bodiless method's parameter must reach analysis(), " + what);
        assertTrue(unmodified.isTrue(), what + ": have " + unmodified);
    }

    private MethodInfo accept(Run run) {
        return run.typeInfo().findSubType("Sink").methodStream()
                .filter(m -> "accept".equals(m.name())).findFirst().orElseThrow();
    }

    private record Run(TypeInfo typeInfo, List<Message> messages) {
    }

    private Run analyze(String fqn, String source, boolean modificationViaReachability) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse(fqn, source);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).setGuardContracts(true)
                        .setModificationViaReachability(modificationViaReachability).build());
        analyzer.analyze(analysisOrder);
        return new Run(typeInfo, analyzer.messages());
    }
}
