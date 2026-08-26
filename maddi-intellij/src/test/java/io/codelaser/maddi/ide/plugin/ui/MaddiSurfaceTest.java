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

package io.codelaser.maddi.ide.plugin.ui;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import io.codelaser.maddi.ide.plugin.analysis.MaddiAnalysisService;
import io.codelaser.maddi.ide.client.AnalysisModel;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Surface tests: inject a canned {@link AnalysisModel.Result} into {@link MaddiAnalysisService} (via the
 * daemon-free {@code applyResult} seam) and assert the IDE surfaces render it. No daemon / JDK-25 needed.
 */
public class MaddiSurfaceTest extends LightJavaCodeInsightFixtureTestCase {

    /** A guard finding is highlighted at its range with the right severity and message. */
    public void testGuardFindingIsHighlighted() {
        myFixture.configureByText("Demo.java", """
                public class Demo {
                    void m(int x) { }
                }
                """);
        String path = filePath();
        AnalysisModel.Finding finding = new AnalysisModel.Finding(
                path, 2, 10, 2, 10, "ERROR", "contract-violation", "parameter x is modified", List.of());
        service().applyResult(result(List.of(finding), List.of()));

        List<HighlightInfo> highlights = myFixture.doHighlighting();
        boolean found = highlights.stream().anyMatch(h ->
                "parameter x is modified".equals(h.getDescription()) && h.getSeverity() == HighlightSeverity.ERROR);
        assertTrue("expected the guard finding to be highlighted as an error", found);
    }

    /** A nested type gets its OWN annotations, not the enclosing type's (whose range also contains it). */
    public void testNestedTypeAnnotationsDoNotLeak() {
        myFixture.configureByText("Outer.java", """
                public class Outer {
                    static class Inn<caret>er { void x() {} }
                }
                """);
        String path = filePath();
        // Outer's range spans the whole class (contains Inner); Inner has its own smaller range. Outer is
        // listed first, so a naive first-containing-match would wrongly give Inner the outer's @Container.
        AnalysisModel.ElementAnnotation outer = new AnalysisModel.ElementAnnotation(
                path, 1, 1, 3, 1, "TYPE", "Outer", List.of("@Container"),
                List.of(new AnalysisModel.Annotation("@Container", "POSITIVE", false)), Map.of());
        AnalysisModel.ElementAnnotation inner = new AnalysisModel.ElementAnnotation(
                path, 2, 5, 2, 40, "TYPE", "Outer.Inner", List.of("@Immutable"),
                List.of(new AnalysisModel.Annotation("@Immutable", "POSITIVE", false)), Map.of());
        service().applyResult(result(List.of(), List.of(outer, inner)));

        List<String> tooltips = myFixture.findGuttersAtCaret().stream()
                .map(GutterMark::getTooltipText).filter(Objects::nonNull).toList();
        assertTrue("Inner should get its own @Immutable gutter; got " + tooltips,
                tooltips.stream().anyMatch(t -> t.contains("@Immutable")));
        assertFalse("Inner must NOT inherit Outer's @Container; got " + tooltips,
                tooltips.stream().anyMatch(t -> t.contains("@Container")));
    }

    /**
     * ⛔ AN EVENTUAL VERDICT AND A PROVEN ONE MUST NOT LOOK THE SAME IN THE GUTTER. Polarity used to drive
     * filtering only, so {@code @Immutable} and {@code @Immutable(hc=true,after=…)} — a property that holds,
     * and one that holds only after a mark — arrived under the same icon, and telling them apart meant reading
     * the tooltip to its end. The tooltip keeps the FULL roster; the icon is what carries the distinction at a
     * glance.
     */
    public void testEventualVerdictGetsItsOwnGutterIcon() {
        myFixture.configureByText("Eventual.java", """
                public class Even<caret>tual {
                }
                """);
        String path = filePath();
        AnalysisModel.Annotation eventual = new AnalysisModel.Annotation(
                "@Immutable(hc=true,after=\"a,b,c,d,e,f\")", "EVENTUAL", false);
        AnalysisModel.ElementAnnotation type = new AnalysisModel.ElementAnnotation(
                path, 1, 1, 2, 1, "TYPE", "Eventual", List.of(eventual.text()), List.of(eventual), Map.of());
        service().applyResult(result(List.of(), List.of(type)));

        List<GutterMark> gutters = myFixture.findGuttersAtCaret();
        assertFalse("expected a maddi gutter mark", gutters.isEmpty());
        assertTrue("the eventual verdict must not use the plain annotation icon",
                gutters.stream().anyMatch(g -> g.getIcon() == com.intellij.icons.AllIcons.Nodes.Static));
        // ...and the roster the inline hint abbreviates is still readable here, in full
        List<String> tooltips = gutters.stream().map(GutterMark::getTooltipText)
                .filter(Objects::nonNull).toList();
        assertTrue("the gutter keeps the full roster; got " + tooltips,
                tooltips.stream().anyMatch(t -> t.contains("a,b,c,d,e,f")));
    }

    /** CONTROL: a proven (non-eventual) verdict keeps the plain icon, so the two are distinguishable. */
    public void testProvenVerdictKeepsThePlainGutterIcon() {
        myFixture.configureByText("Proven.java", """
                public class Pro<caret>ven {
                }
                """);
        String path = filePath();
        AnalysisModel.Annotation immutable = new AnalysisModel.Annotation("@Immutable", "POSITIVE", false);
        AnalysisModel.ElementAnnotation type = new AnalysisModel.ElementAnnotation(
                path, 1, 1, 2, 1, "TYPE", "Proven", List.of(immutable.text()), List.of(immutable), Map.of());
        service().applyResult(result(List.of(), List.of(type)));

        List<GutterMark> gutters = myFixture.findGuttersAtCaret();
        assertFalse("expected a maddi gutter mark", gutters.isEmpty());
        assertTrue("a proven verdict keeps the plain annotation icon",
                gutters.stream().anyMatch(g -> g.getIcon() == com.intellij.icons.AllIcons.Nodes.Annotationtype));
    }

    private MaddiAnalysisService service() {
        return MaddiAnalysisService.getInstance(getProject());
    }

    private String filePath() {
        return myFixture.getFile().getVirtualFile().getPath();
    }

    private static AnalysisModel.Result result(List<AnalysisModel.Finding> findings,
                                               List<AnalysisModel.ElementAnnotation> annotations) {
        return new AnalysisModel.Result("test", findings, annotations, List.of(), 0, 0, 0, AnalysisModel.OUTCOME_CERTIFIED);
    }
}
