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

package org.e2immu.analyzer.ide.plugin.ui;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.utils.inlays.declarative.DeclarativeInlayHintsProviderTestCase;
import org.e2immu.analyzer.ide.client.AnalysisModel;
import org.e2immu.analyzer.ide.plugin.analysis.MaddiAnalysisService;
import org.e2immu.analyzer.ide.plugin.settings.HintPlacement;
import org.e2immu.analyzer.ide.plugin.settings.InlineHintsMode;
import org.e2immu.analyzer.ide.plugin.settings.MaddiSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Where the hints land. The default ({@link HintPlacement#ABOVE_DECLARATION}) gives each declaration a line
 * of its own, so the result reads like hand-written annotated API source; a parameter's annotations stay
 * inline in both modes, because a parameter has no line of its own to use.
 * <p>
 * Renders through the real declarative-inlay pass: a block hint above a line and an inline one after a name
 * are different {@code InlayPosition} types, and only the platform's own rendering shows which was produced.
 */
public class MaddiInlayPlacementTest extends DeclarativeInlayHintsProviderTestCase {

    private static final String SOURCE = """
            public class Box {
                public int get() { return 0; }
                public void set(Mutable m) { }
            }
            """;

    private static final AnalysisModel.Annotation CONTAINER =
            new AnalysisModel.Annotation("@Container", "POSITIVE", false);
    private static final AnalysisModel.Annotation NOT_MODIFIED =
            new AnalysisModel.Annotation("@NotModified", "POSITIVE", false);
    private static final AnalysisModel.Annotation MODIFIED =
            new AnalysisModel.Annotation("@Modified", "NEGATIVE", false);

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return LightJavaCodeInsightFixtureTestCase.JAVA_LATEST;
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            MaddiSettings.State state = MaddiSettings.getInstance().getState();
            state.inlineHintsMode = InlineHintsMode.HIDE_CONTEXT_DEFAULTS;
            state.hintPlacement = HintPlacement.ABOVE_DECLARATION;
        } finally {
            super.tearDown();
        }
    }

    public void testAboveDeclarationPutsTypeAndMethodHintsOnTheirOwnLine() {
        // note the indentation of the method's hint: it lines up with the declaration it describes, which is
        // what AboveLineIndentedPosition buys over a plain block inlay. The parameter stays inline.
        doPlacementTest(HintPlacement.ABOVE_DECLARATION, """
                /*<# block [@Container] #>*/
                public class Box {
                    /*<# block [@NotModified] #>*/
                    public int get() { return 0; }
                    public void set(Mutable m/*<# @Modified #>*/) { }
                }
                """);
    }

    public void testInlinePutsEveryHintAfterTheName() {
        doPlacementTest(HintPlacement.INLINE, """
                public class Box/*<# @Container #>*/ {
                    public int get/*<# @NotModified #>*/() { return 0; }
                    public void set(Mutable m/*<# @Modified #>*/) { }
                }
                """);
    }

    private void doPlacementTest(HintPlacement placement, String expected) {
        myFixture.configureByText("Box.java", SOURCE);
        String path = myFixture.getFile().getVirtualFile().getPath();

        AnalysisModel.ElementAnnotation type = new AnalysisModel.ElementAnnotation(
                path, 1, 1, 4, 2, "TYPE", "Box",
                List.of(CONTAINER.text()), List.of(CONTAINER), Map.of());
        AnalysisModel.ElementAnnotation method = new AnalysisModel.ElementAnnotation(
                path, 2, 1, 2, 40, "METHOD", "Box.get",
                List.of(NOT_MODIFIED.text()), List.of(NOT_MODIFIED), Map.of());
        AnalysisModel.ElementAnnotation param = new AnalysisModel.ElementAnnotation(
                path, 3, 1, 3, 40, "PARAMETER", "Box.set.m",
                List.of(MODIFIED.text()), List.of(MODIFIED), Map.of());
        MaddiAnalysisService.getInstance(getProject()).applyResult(new AnalysisModel.Result(
                "test", List.of(), List.of(type, method, param), List.of(), 0, 0, 0,
                AnalysisModel.OUTCOME_CERTIFIED));

        MaddiSettings.State state = MaddiSettings.getInstance().getState();
        state.inlineHintsMode = InlineHintsMode.ALL; // placement is the variable here, not the filter
        state.hintPlacement = placement;

        doTestProviderWithConfigured(SOURCE, expected, new MaddiInlayProvider(), Map.of(), null, false,
                DeclarativeInlayHintsProviderTestCase.ProviderTestMode.SIMPLE);
    }
}
