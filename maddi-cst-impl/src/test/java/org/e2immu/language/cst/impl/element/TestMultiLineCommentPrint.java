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

package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.impl.output.TextImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real-world sources carry trailing whitespace before the newline inside block comments (closed-core,
 * early-2000s code: 8 of 40 intake losses on one formatter assert). {@code multilinePrint} trims every
 * line except the FIRST — construction trims only the comment's two ends — and that surviving trailing
 * space tripped the formatter's Output invariant ("An output cannot end in a space",
 * {@code BlockPrinter$Output}). No printed Text element may end in a space.
 */
public class TestMultiLineCommentPrint {

    @DisplayName("trailing space on the first line of a multi-line block comment must not survive printing")
    @Test
    public void trailingSpaceFirstLine() {
        String comment = "allowed units + Trunc(extra_units  * partial/total) \n"
                         + "  = total allowed  \n"
                         + "  (whitespace shape reproduced from the closed-core corpus) ";
        MultiLineCommentImpl mlc = new MultiLineCommentImpl(null, comment, false);
        OutputBuilder ob = mlc.print(null);
        boolean sawText = false;
        for (var element : ob.list()) {
            if (element instanceof TextImpl text) {
                sawText = true;
                assertFalse(text.text().endsWith(" "),
                        "printed comment line ends in a space: '" + text.text() + "'");
            }
        }
        assertTrue(sawText, "expected at least one Text element from the comment");
    }
}
