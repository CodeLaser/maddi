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

package io.codelaser.maddi.modification.analyzer.modification;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A disclaimed ({@code @IgnoreModifications}) variable must not implicate its own node when it is handed to a
 * callee's {@code @Modified} parameter — the ARGUMENT site, the last of {@code MethodModification.go}'s four
 * modification-recording sites to honour the disclaimer (fix C, 2026-09-22).
 * <p>
 * The three other sites have always carried
 * {@code Util.variableAndScopes(...).filter(v -> !v.isIgnoreModifications())};
 * {@code handleModifiedParameter} had no filter, so {@code s.consume(l)} wrote
 * {@code unmodifiedParameter=false} onto {@code l} even though {@code l} declares
 * {@code ignoreModsParameter=true} — the analyzer contradicting the author within one parameter.
 * <p>
 * ⚠ <b>Choosing the observable took two attempts, and the first one proved nothing.</b> The obvious fixture —
 * a disclaimed FIELD handed to a modifying parameter, asserted on {@code UNMODIFIED_FIELD} — is vacuous: a
 * disclaimed field's {@code UNMODIFIED_FIELD} is already decided TRUE by the annotation through another path,
 * so that test passed with AND without the fix (verified by re-running it against an unfiltered
 * {@code handleModifiedParameter}). Worse, both such methods stay {@code nonModifyingMethod=false} either way —
 * a field disclaimer does not lift its enclosing method's verdict, which is a separate gap, not this one.
 * A PARAMETER is the shape where the contradiction is visible, because nothing else decides it.
 * <p>
 * ⚠ <b>Which half of the fix this test actually pins, measured by removing each:</b> with BOTH the engine
 * filter and the shadow mirror removed, the plain variant still PASSES and only the MODREACH variant FAILS.
 * The defect was therefore in {@code ShadowModificationPass.project()}, which filtered a disclaimed
 * {@code FieldReference} but added a disclaimed {@code ParameterInfo} node unconditionally; the engine
 * reaches TRUE here regardless, because a disclaimed parameter's {@code UNMODIFIED_PARAMETER} is decided for
 * it elsewhere. So the engine-side filter is a CONSISTENCY change with no effect this fixture can see — it
 * aligns the fourth site with the other three and matters, if anywhere, on scope chains and on the corpus.
 * Do not read a green run here as evidence about the engine half.
 * <p>
 * That asymmetry is the same one-sided-fix defect {@link TestIgnoreModificationsOnFunctionalParameter} was
 * written for — found the same way, in three seconds, with the roles of the two implementations reversed.
 */
public class TestIgnoreModificationsOnArgument extends CommonTest {

    @Language("java")
    private static final String SOURCE = """
            package a.b;
            import io.codelaser.maddi.annotation.rare.IgnoreModifications;
            import java.util.List;

            public class X {

                static class Sink {
                    void consume(List<String> list) {
                        list.add("x");
                    }
                }

                // CONTROL: an ordinary parameter handed to a modifying parameter IS modified
                static class Plain {
                    void go(List<String> l, Sink s) {
                        s.consume(l);
                    }
                }

                // the author declares that what anyone does to l is not this method's modification
                static class Disclaimed {
                    void go(@IgnoreModifications List<String> l, Sink s) {
                        s.consume(l);
                    }
                }
            }
            """;

    @DisplayName("a disclaimed argument must not be written modified by the callee's @Modified parameter")
    @Test
    public void disclaimedArgumentIsNotModified() throws IOException {
        run(false);
    }

    @DisplayName("... and the MODREACH cutover, the mirror implementation, must honour it identically")
    @Test
    public void disclaimedArgumentUnderModificationReachability() throws IOException {
        run(true);
    }

    private void run(boolean modificationViaReachability) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse("a.b.X", SOURCE);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10)
                        .setModificationViaReachability(modificationViaReachability).build());
        analyzer.analyze(analysisOrder);

        String what = " (modReach=" + modificationViaReachability + ")";
        // the control first: without it, the assertion below could pass because nothing taints anything
        assertEquals(ValueImpl.BoolImpl.FALSE, unmodifiedArgument(typeInfo, "Plain"),
                "the CONTROL must be MODIFIED — otherwise this test proves nothing" + what);
        assertEquals(ValueImpl.BoolImpl.TRUE, unmodifiedArgument(typeInfo, "Disclaimed"),
                "@IgnoreModifications on a parameter must survive that parameter being handed to a"
                + " @Modified parameter" + what);
    }

    private ValueImpl.BoolImpl unmodifiedArgument(TypeInfo typeInfo, String subType) {
        MethodInfo go = typeInfo.findSubType(subType).findUniqueMethod("go", 2);
        return go.parameters().getFirst().analysis()
                .getOrNull(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
    }
}
