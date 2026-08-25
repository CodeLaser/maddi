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

package io.codelaser.maddi.ide.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How an eventual verdict is rendered where there is only one line for it.
 * <p>
 * The {@code after=} roster is a union of marks and is unbounded — 16 on one of maddi's own types, and
 * {@code dogfood/expected-eventual-survivors.txt} records {@code Element} at 60. What a reader needs beside a
 * declaration is that the verdict is eventual and roughly how far off; the roster belongs in the gutter
 * tooltip, which renders the text unabbreviated.
 */
public class TestAbbreviateEventual {

    @DisplayName("a long roster collapses to a count")
    @Test
    public void longRosterCollapses() {
        String real = "@Immutable(hc=true,after=\"annotationClass,autoCloseable,biConsumer,consumer,"
                      + "enumTypeInfo,exception,function,hashtable,math,override,printStream,runtime,"
                      + "runtimeException,sourceSet,suppressWarnings,system\")";
        assertEquals("@Immutable(hc=true,after=16)", AnalysisModel.abbreviate(real));
    }

    /**
     * ⚠ A SHORT ROSTER IS THE USEFUL CASE AND IS LEFT ALONE. {@code after="runtime"} names the mark, which is
     * the whole content of the verdict; replacing it with a count would abbreviate away the information.
     */
    @DisplayName("a short roster is left verbatim")
    @Test
    public void shortRosterSurvives() {
        assertEquals("@NotModified(after=\"runtime\")",
                AnalysisModel.abbreviate("@NotModified(after=\"runtime\")"));
        assertEquals("@Immutable(after=\"a,b,c\")", AnalysisModel.abbreviate("@Immutable(after=\"a,b,c\")"));
        assertEquals("@Immutable(after=4)", AnalysisModel.abbreviate("@Immutable(after=\"a,b,c,d\")"));
    }

    @DisplayName("an annotation with no roster is untouched")
    @Test
    public void noRosterUntouched() {
        assertEquals("@Immutable", AnalysisModel.abbreviate("@Immutable"));
        assertEquals("@Container", AnalysisModel.abbreviate("@Container"));
        assertEquals("@Immutable(hc=true)", AnalysisModel.abbreviate("@Immutable(hc=true)"));
    }

    /** Malformed input must come back exactly as it arrived, never half-rewritten. */
    @DisplayName("an unterminated roster is not rewritten")
    @Test
    public void malformedUntouched() {
        assertEquals("@Immutable(after=\"a,b,c,d", AnalysisModel.abbreviate("@Immutable(after=\"a,b,c,d"));
        assertEquals("@Immutable(after=\"\")", AnalysisModel.abbreviate("@Immutable(after=\"\")"));
        assertEquals(null, AnalysisModel.abbreviate(null));
    }

    /** The gutter needs to know an element carries an eventual verdict, whatever else it carries. */
    @DisplayName("isEventual finds the eventual annotation among the others")
    @Test
    public void isEventualDetects() {
        assertTrue(AnalysisModel.isEventual(element(
                new AnalysisModel.Annotation("@Independent", "POSITIVE", false),
                new AnalysisModel.Annotation("@Immutable(hc=true,after=\"a,b\")", "EVENTUAL", false),
                new AnalysisModel.Annotation("@Mutable", "NEGATIVE", true))));
        assertFalse(AnalysisModel.isEventual(element(
                new AnalysisModel.Annotation("@Independent", "POSITIVE", false),
                new AnalysisModel.Annotation("@Immutable", "POSITIVE", false))));
        assertFalse(AnalysisModel.isEventual(null));
    }

    private static AnalysisModel.ElementAnnotation element(AnalysisModel.Annotation... annotations) {
        return new AnalysisModel.ElementAnnotation("file:///X.java", 1, 1, 2, 1, "TYPE", "X",
                List.of(), List.of(annotations), Map.of());
    }
}
