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

import com.intellij.testFramework.utils.inlays.declarative.DeclarativeInlayHintsProviderTestCase;
import io.codelaser.maddi.ide.client.AnalysisModel;
import io.codelaser.maddi.ide.plugin.analysis.MaddiAnalysisService;
import io.codelaser.maddi.ide.plugin.settings.HintPlacement;
import io.codelaser.maddi.ide.plugin.settings.InlineHintsMode;
import io.codelaser.maddi.ide.plugin.settings.MaddiSettings;

import java.util.List;
import java.util.Map;

import static io.codelaser.maddi.ide.plugin.kotlin.MaddiKotlinSurfaceTest.element;
import static io.codelaser.maddi.ide.plugin.kotlin.MaddiKotlinSurfaceTest.result;

/** Inline hints on Kotlin declarations, through the real declarative-inlay pass. */
public class MaddiKotlinInlayTest extends DeclarativeInlayHintsProviderTestCase {

    private static final String SOURCE = """
            class Point(val x: Int, y: Int) {
                val sum: Int = x + y
                fun plus(other: Point): Point = Point(x + other.x, 0)
            }
            """;

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

    /**
     * Every kind lands after its name. The constructor's {@code val x} is a parameter AND a field over the same
     * range; the parameter's verdict is the one shown, since the name sits in the parameter list.
     */
    public void testInline() {
        doTest(HintPlacement.INLINE, """
                class Point/*<# @Immutable #>*/(val x/*<# @NotModified #>*/: Int, y: Int) {
                    val sum/*<# @Final #>*/: Int = x + y
                    fun plus/*<# @Independent #>*/(other/*<# @Unmodified #>*/: Point): Point = Point(x + other.x, 0)
                }
                """);
    }

    public void testAboveDeclaration() {
        doTest(HintPlacement.ABOVE_DECLARATION, """
                /*<# block [@Immutable] #>*/
                class Point(val x/*<# @NotModified #>*/: Int, y: Int) {
                    /*<# block [@Final] #>*/
                    val sum: Int = x + y
                    /*<# block [@Independent] #>*/
                    fun plus(other/*<# @Unmodified #>*/: Point): Point = Point(x + other.x, 0)
                }
                """);
    }

    /** An extension function's receiver: one hint, on the first name of the receiver type. */
    public void testExtensionReceiver() {
        String source = """
                fun List<String>.firstOr(d: String): String = firstOrNull() ?: d
                """;
        myFixture.configureByText("Ext.kt", source);
        String path = myFixture.getFile().getVirtualFile().getPath();
        MaddiAnalysisService.getInstance(getProject()).applyResult(result(List.of(), List.of(
                element(path, 1, 5, 1, 16, "PARAMETER", "ExtKt.firstOr(java.util.List,String):0:$receiver", "@NotModified"),
                element(path, 1, 26, 1, 34, "PARAMETER", "ExtKt.firstOr(java.util.List,String):1:d", "@Unmodified"))));
        MaddiSettings.State state = MaddiSettings.getInstance().getState();
        state.inlineHintsMode = InlineHintsMode.ALL;
        state.hintPlacement = HintPlacement.INLINE;
        doTestProviderWithConfigured(source, """
                fun List/*<# @NotModified #>*/<String>.firstOr(d/*<# @Unmodified #>*/: String): String = firstOrNull() ?: d
                """, new MaddiKotlinInlayProvider(), Map.of(), null, false,
                DeclarativeInlayHintsProviderTestCase.ProviderTestMode.SIMPLE);
    }

    private void doTest(HintPlacement placement, String expected) {
        myFixture.configureByText("Point.kt", SOURCE);
        String path = myFixture.getFile().getVirtualFile().getPath();
        MaddiAnalysisService.getInstance(getProject()).applyResult(result(List.of(), List.of(
                element(path, 1, 1, 4, 1, "TYPE", "Point", "@Immutable"),
                element(path, 1, 13, 1, 22, "PARAMETER", "Point.<init>(int,int):0:x", "@NotModified"),
                element(path, 1, 13, 1, 22, "FIELD", "Point.x", "@Final"),
                element(path, 2, 5, 2, 24, "FIELD", "Point.sum", "@Final"),
                element(path, 3, 5, 3, 57, "METHOD", "Point.plus(Point)", "@Independent"),
                element(path, 3, 14, 3, 25, "PARAMETER", "Point.plus(Point):0:other", "@Unmodified"))));

        MaddiSettings.State state = MaddiSettings.getInstance().getState();
        state.inlineHintsMode = InlineHintsMode.ALL;
        state.hintPlacement = placement;

        doTestProviderWithConfigured(SOURCE, expected, new MaddiKotlinInlayProvider(), Map.of(), null, false,
                DeclarativeInlayHintsProviderTestCase.ProviderTestMode.SIMPLE);
    }
}
