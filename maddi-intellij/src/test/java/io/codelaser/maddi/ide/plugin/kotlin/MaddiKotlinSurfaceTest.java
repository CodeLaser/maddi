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
package io.codelaser.maddi.ide.plugin.kotlin;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import io.codelaser.maddi.ide.client.AnalysisModel;
import io.codelaser.maddi.ide.plugin.analysis.MaddiAnalysisService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The Java surfaces on a {@code .kt} file: a canned result (positions as the Kotlin front end reports them,
 * 1-based and end-inclusive) must land on Kotlin's declarations. No daemon.
 */
public class MaddiKotlinSurfaceTest extends LightJavaCodeInsightFixtureTestCase {

    public void testGutterOnClassAndFunction() {
        myFixture.configureByText("Point.kt", """
                class Po<caret>int(val x: Int) {
                    fun twice(): Int = 2 * x
                }
                """);
        String path = path();
        service().applyResult(result(List.of(), List.of(
                element(path, 1, 1, 3, 1, "TYPE", "Point", "@Immutable"),
                element(path, 2, 5, 2, 28, "METHOD", "Point.twice()", "@NotModified"))));

        assertTrue(tooltips().stream().anyMatch(t -> t.contains("@Immutable")));
        myFixture.getEditor().getCaretModel().moveToOffset(myFixture.getFile().getText().indexOf("twice") + 1);
        List<String> atFunction = tooltips();
        assertTrue("got " + atFunction, atFunction.stream().anyMatch(t -> t.contains("@NotModified")));
        assertFalse("the class's verdict must not leak onto its member; got " + atFunction,
                atFunction.stream().anyMatch(t -> t.contains("@Immutable")));
    }

    public void testPropertyGetsTheFieldVerdict() {
        myFixture.configureByText("Holder.kt", """
                class Holder {
                    val it<caret>ems = ArrayList<String>()
                }
                """);
        String path = path();
        service().applyResult(result(List.of(), List.of(
                element(path, 2, 5, 2, 38, "FIELD", "Holder.items", "@Final"))));
        assertTrue(tooltips().stream().anyMatch(t -> t.contains("@Final")));
    }

    public void testFindingIsHighlighted() {
        myFixture.configureByText("Demo.kt", """
                class Demo {
                    fun m(x: Int) { }
                }
                """);
        AnalysisModel.Finding finding = new AnalysisModel.Finding(path(), 2, 11, 2, 11, "ERROR",
                "contract-violation", "parameter x is modified", List.of());
        service().applyResult(result(List.of(finding), List.of()));
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        assertTrue(highlights.stream().anyMatch(h -> "parameter x is modified".equals(h.getDescription())
                                                     && h.getSeverity() == HighlightSeverity.ERROR));
    }

    private List<String> tooltips() {
        return myFixture.findGuttersAtCaret().stream().map(GutterMark::getTooltipText)
                .filter(Objects::nonNull).toList();
    }

    private MaddiAnalysisService service() {
        return MaddiAnalysisService.getInstance(getProject());
    }

    private String path() {
        return myFixture.getFile().getVirtualFile().getPath();
    }

    static AnalysisModel.ElementAnnotation element(String path, int bl, int bc, int el, int ec, String kind,
                                                   String fqn, String text) {
        AnalysisModel.Annotation annotation = new AnalysisModel.Annotation(text, "POSITIVE", false);
        return new AnalysisModel.ElementAnnotation(path, bl, bc, el, ec, kind, fqn, List.of(text),
                List.of(annotation), Map.of());
    }

    static AnalysisModel.Result result(List<AnalysisModel.Finding> findings,
                                       List<AnalysisModel.ElementAnnotation> annotations) {
        return new AnalysisModel.Result("test", findings, annotations, List.of(), 0, 0, 0,
                AnalysisModel.OUTCOME_CERTIFIED);
    }
}
