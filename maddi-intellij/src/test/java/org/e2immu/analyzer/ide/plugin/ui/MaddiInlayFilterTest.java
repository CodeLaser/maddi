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
import org.e2immu.analyzer.ide.plugin.analysis.MaddiAnalysisService;
import org.e2immu.analyzer.ide.plugin.model.AnalysisModel;
import org.e2immu.analyzer.ide.plugin.settings.InlineHintsMode;
import org.e2immu.analyzer.ide.plugin.settings.MaddiSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Live inlay-filter test: renders {@link MaddiInlayProvider} through the real declarative-inlay pass and
 * asserts that the {@link InlineHintsMode} filter decides which annotations appear inline. A canned result
 * (three annotations spanning the polarity/context-default axes) is injected into {@link MaddiAnalysisService}
 * via the daemon-free {@code applyResult} seam; the gutter is unaffected (it always shows the full set).
 */
public class MaddiInlayFilterTest extends DeclarativeInlayHintsProviderTestCase {

    // Clean source; the fixture is configured with this, then hints are dumped into it and compared.
    private static final String SOURCE = """
            public class Box {
                public int get() { return 0; }
                public void set(Mutable m) { }
            }
            """;

    /** @Container on the type — POSITIVE, informative (proven, not context-implied). */
    private static final AnalysisModel.Annotation CONTAINER =
            new AnalysisModel.Annotation("@Container", "POSITIVE", false);
    /** @NotModified on a read-only method — POSITIVE but a context default (implied by @Container). */
    private static final AnalysisModel.Annotation NOT_MODIFIED =
            new AnalysisModel.Annotation("@NotModified", "POSITIVE", true);
    /** @Modified on the parameter — NEGATIVE and NOT a context default (the signal, always worth showing). */
    private static final AnalysisModel.Annotation MODIFIED =
            new AnalysisModel.Annotation("@Modified", "NEGATIVE", false);

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return LightJavaCodeInsightFixtureTestCase.JAVA_LATEST;
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            MaddiSettings.getInstance().getState().inlineHintsMode = InlineHintsMode.HIDE_CONTEXT_DEFAULTS;
        } finally {
            super.tearDown();
        }
    }

    public void testAllShowsEverything() {
        doFilterTest(InlineHintsMode.ALL, """
                public class Box/*<# @Container #>*/ {
                    public int get/*<# @NotModified #>*/() { return 0; }
                    public void set(Mutable m/*<# @Modified #>*/) { }
                }
                """);
    }

    public void testHideContextDefaultsDropsImpliedPositive() {
        // @NotModified (context default) is dropped; the informative @Container and the @Modified signal stay.
        doFilterTest(InlineHintsMode.HIDE_CONTEXT_DEFAULTS, """
                public class Box/*<# @Container #>*/ {
                    public int get() { return 0; }
                    public void set(Mutable m/*<# @Modified #>*/) { }
                }
                """);
    }

    public void testPositiveOnlyDropsTheModifiedSignal() {
        doFilterTest(InlineHintsMode.POSITIVE_ONLY, """
                public class Box/*<# @Container #>*/ {
                    public int get/*<# @NotModified #>*/() { return 0; }
                    public void set(Mutable m) { }
                }
                """);
    }

    public void testNegativeOnlyKeepsOnlyTheModifiedSignal() {
        doFilterTest(InlineHintsMode.NEGATIVE_ONLY, """
                public class Box {
                    public int get() { return 0; }
                    public void set(Mutable m/*<# @Modified #>*/) { }
                }
                """);
    }

    private void doFilterTest(InlineHintsMode mode, String expected) {
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
        AnalysisModel.Result result = new AnalysisModel.Result(
                "test", List.of(), List.of(type, method, param), List.of(), 0, 0, 0);
        MaddiAnalysisService.getInstance(getProject()).applyResult(result);

        MaddiSettings.getInstance().getState().inlineHintsMode = mode;

        doTestProviderWithConfigured(SOURCE, expected, new MaddiInlayProvider(), Map.of(), null, false,
                DeclarativeInlayHintsProviderTestCase.ProviderTestMode.SIMPLE);
    }
}
