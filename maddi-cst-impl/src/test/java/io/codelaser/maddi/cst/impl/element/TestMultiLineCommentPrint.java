/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.impl.element;

import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.impl.output.TextImpl;
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
