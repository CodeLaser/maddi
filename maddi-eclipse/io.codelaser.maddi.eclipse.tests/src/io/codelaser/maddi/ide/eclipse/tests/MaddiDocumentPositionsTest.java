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

package org.e2immu.analyzer.ide.eclipse.tests;

import org.e2immu.analyzer.ide.eclipse.MaddiDocumentPositions;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The coordinate translation behind the inline hints. maddi counts lines and columns from 1, Eclipse
 * addresses documents by offset, and the document keeps being edited after the analysis that produced the
 * positions — so the interesting cases are all the ones where the two no longer agree.
 */
public class MaddiDocumentPositionsTest {

    /** Lines: "package x;" (0..9), "class A {" (11..19), "}" (21). */
    private static IDocument document() {
        return new Document("package x;\nclass A {\n}\n");
    }

    @DisplayName("line/column are 1-based and map onto the document offset")
    @Test
    public void mapsOneBasedLineAndColumn() {
        Position first = MaddiDocumentPositions.position(document(), 1, 1);
        assertEquals(0, first.getOffset(), "line 1, column 1 is offset 0");

        Position secondLine = MaddiDocumentPositions.position(document(), 2, 1);
        assertEquals(11, secondLine.getOffset(), "line 2 starts after 'package x;\\n'");

        Position withinLine = MaddiDocumentPositions.position(document(), 2, 7);
        assertEquals(17, withinLine.getOffset(), "column 7 of line 2 is 6 characters in");
    }

    @DisplayName("a column past the end of the line is clamped onto it, not left dangling")
    @Test
    public void clampsColumnPastEndOfLine() {
        // the user deleted text after the analysis: column 80 no longer exists on this line
        Position position = MaddiDocumentPositions.position(document(), 2, 80);
        // line 2 is "class A {" at offset 11, so its last real character is the '{' at 19 — NOT the
        // newline at 20, which is where clamping against the delimiter-inclusive line length would land
        assertEquals(19, position.getOffset(), "clamped onto the last character of 'class A {'");
    }

    @DisplayName("a line beyond the document yields no position rather than an exception")
    @Test
    public void lineBeyondDocumentIsDropped() {
        assertNull(MaddiDocumentPositions.position(document(), 99, 1), "the file shrank since the analysis");
    }

    @DisplayName("missing or nonsensical coordinates are dropped, never guessed")
    @Test
    public void missingCoordinatesAreDropped() {
        assertNull(MaddiDocumentPositions.position(document(), null, 1));
        assertNull(MaddiDocumentPositions.position(document(), 0, 1));
        assertNull(MaddiDocumentPositions.position(null, 1, 1));
        // a null column is legitimate: it means "start of the line"
        assertEquals(11, MaddiDocumentPositions.position(document(), 2, null).getOffset());
    }

    @DisplayName("maddi's file:/// URIs become paths comparable with Eclipse's file:/ ones")
    @Test
    public void fileUriBecomesPath() {
        assertEquals(Path.of("/tmp/x/Example.java"), MaddiDocumentPositions.toPath("file:///tmp/x/Example.java"));
        // the same file as Eclipse spells it: unequal as strings, equal as paths
        assertEquals(MaddiDocumentPositions.toPath("file:/tmp/x/Example.java"),
                MaddiDocumentPositions.toPath("file:///tmp/x/Example.java"));
    }

    @DisplayName("a non-file or malformed URI is dropped")
    @Test
    public void badUriIsDropped() {
        assertNull(MaddiDocumentPositions.toPath(null));
        assertNull(MaddiDocumentPositions.toPath("not a uri"));
        assertNull(MaddiDocumentPositions.toPath("http://example.com/X.java"));
    }
}
