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

package io.codelaser.maddi.modification.analyzer;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When the analyzer is done, nothing is left undecided — and that is what makes an ABSENT property in the
 * written analysis results readable.
 * <p>
 * Undecided is a first-class state <em>during</em> iteration: the folds in {@code AbstractMethodAnalyzerImpl}
 * branch on {@code getOrNull(...) == null} ("undecided is not DEPENDENT — wait"). But
 * {@code WriteAnalysisResults} streams {@code analysis().propertyValueStream()} and then drops every value for
 * which {@code isDefault()} holds, so a property that is undecided and one that is decided at its default
 * (modifying, DEPENDENT, MUTABLE) are INDISTINGUISHABLE in the JSON. Reading an absent property as "the
 * analyzer could not decide" is therefore wrong, and it is a mistake that is easy to make when comparing runs.
 * <p>
 * This pins the invariant that makes the reading safe: after a complete analysis every method carries a
 * written value for both modification properties, so an absent one in the results means DEFAULT, never
 * undecided. The sample deliberately mixes the shapes that reach a default by different routes — an abstract
 * method whose implementations disagree (one modifying, one not), an accessor that exposes a field
 * (DEPENDENT), and one that returns a copy.
 */
public class TestNoUndecidedAtEnd extends CommonTest {

    @Language("java")
    private static final String SRC = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;

            public class P {
                interface Source {
                    List<String> items();
                    int count();
                }
                static class A implements Source {
                    private final List<String> list = new ArrayList<>();
                    @Override public List<String> items() { return list; }
                    @Override public int count() { return list.size(); }
                }
                static class B implements Source {
                    private int n;
                    @Override public List<String> items() { return List.of(); }
                    @Override public int count() { return ++n; }
                }
            }
            """;

    @DisplayName("after a complete analysis no modification property is left unwritten")
    @Test
    public void nothingIsUndecidedWhenTheAnalyzerIsDone() throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse("a.b.P", SRC);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build());
        analyzer.analyze(analysisOrder);

        List<String> unwritten = new ArrayList<>();
        int methods = 0;
        for (Info info : analysisOrder) {
            if (!(info instanceof MethodInfo mi)) continue;
            methods++;
            if (!mi.analysis().haveAnalyzedValueFor(PropertyImpl.NON_MODIFYING_METHOD)) {
                unwritten.add(mi.fullyQualifiedName() + ":nonModifyingMethod");
            }
            if (!mi.analysis().haveAnalyzedValueFor(PropertyImpl.INDEPENDENT_METHOD)) {
                unwritten.add(mi.fullyQualifiedName() + ":independentMethod");
            }
        }
        assertTrue(methods >= 9, "expected the whole sample to be analysed, have " + methods + " methods");
        assertTrue(unwritten.isEmpty(), "undecided survived to the end, so an absent property in the written"
                                        + " results would be ambiguous: " + unwritten);
    }
}
