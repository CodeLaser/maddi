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

package org.e2immu.analyzer.ide.plugin.settings;

import org.e2immu.analyzer.ide.client.AnalysisModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure logic: the inline-hints mode decides per annotation what to show. No IntelliJ platform needed. */
public class InlineHintsModeTest {

    private static final AnalysisModel.Annotation POS_CTX =
            new AnalysisModel.Annotation("@NotModified", "POSITIVE", true);   // container's param: implied
    private static final AnalysisModel.Annotation POS =
            new AnalysisModel.Annotation("@Immutable", "POSITIVE", false);    // proven on the type: informative
    private static final AnalysisModel.Annotation NEG =
            new AnalysisModel.Annotation("@Modified", "NEGATIVE", false);     // a modified param: the signal
    private static final AnalysisModel.Annotation NEG_CTX =
            new AnalysisModel.Annotation("@Mutable", "NEGATIVE", true);       // baseline clutter
    private static final AnalysisModel.Annotation NEUTRAL =
            new AnalysisModel.Annotation("@GetSet", "NEUTRAL", false);

    @Test
    public void all() {
        for (AnalysisModel.Annotation a : new AnalysisModel.Annotation[]{POS_CTX, POS, NEG, NEG_CTX, NEUTRAL}) {
            assertTrue(InlineHintsMode.ALL.shows(a));
            assertFalse(InlineHintsMode.NONE.shows(a));
        }
    }

    @Test
    public void positiveOnly() {
        assertTrue(InlineHintsMode.POSITIVE_ONLY.shows(POS));
        assertTrue(InlineHintsMode.POSITIVE_ONLY.shows(POS_CTX));
        assertTrue(InlineHintsMode.POSITIVE_ONLY.shows(NEUTRAL));  // neutrals kept
        assertFalse(InlineHintsMode.POSITIVE_ONLY.shows(NEG));
        assertFalse(InlineHintsMode.POSITIVE_ONLY.shows(NEG_CTX));
    }

    @Test
    public void negativeOnly() {
        assertTrue(InlineHintsMode.NEGATIVE_ONLY.shows(NEG));
        assertTrue(InlineHintsMode.NEGATIVE_ONLY.shows(NEG_CTX));
        assertTrue(InlineHintsMode.NEGATIVE_ONLY.shows(NEUTRAL));  // neutrals kept
        assertFalse(InlineHintsMode.NEGATIVE_ONLY.shows(POS));
        assertFalse(InlineHintsMode.NEGATIVE_ONLY.shows(POS_CTX));
    }

    @Test
    public void hideContextDefaults() {
        assertTrue(InlineHintsMode.HIDE_CONTEXT_DEFAULTS.shows(POS));      // informative positive kept
        assertTrue(InlineHintsMode.HIDE_CONTEXT_DEFAULTS.shows(NEG));      // the @Modified signal kept
        assertTrue(InlineHintsMode.HIDE_CONTEXT_DEFAULTS.shows(NEUTRAL));
        assertFalse(InlineHintsMode.HIDE_CONTEXT_DEFAULTS.shows(POS_CTX)); // implied positive hidden
        assertFalse(InlineHintsMode.HIDE_CONTEXT_DEFAULTS.shows(NEG_CTX)); // baseline negative hidden
    }
}
