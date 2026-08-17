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

package org.e2immu.language.java.openjdk;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter2.Formatter2Impl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * A {@code //} comment runs to the end of its line, so a block containing one can never be inlined: whatever is
 * appended after it -- in practice the block's own closing brace -- would end up inside the comment and the
 * emitted source would not compile.
 * <pre>
 *     int m() { return 1; // why }
 * </pre>
 * <b>The formatter gets this right, and this test records that.</b> It was written to reproduce exactly that
 * output, seen from splitclass on a constructed method, and it does not: printing a parsed method whose body
 * carries a trailing line comment keeps the comment line-terminated at every width from 20 to 200. So the
 * collapse rule in {@code BlockPrinter.handleBlock} -- which decides on width alone and has no notion of a line
 * comment -- is NOT the cause there, and the invariant asserted here is the regression guard for it.
 */
public class TestLineCommentNotInlined extends CommonTest {

    private static final String SRC = """
            package a.b;
            class X {
                int m() {
                    return 1; // why it is one
                }
                int n() {
                    int j = 2; // first
                    return j; // second
                }
            }
            """;

    private String printAt(TypeInfo ti, int width) {
        OutputBuilder ob = runtime.newCompilationUnitPrinter(ti.compilationUnit(), true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(width).setSpacesInTab(4).build();
        return new Formatter2Impl(runtime, options).write(ob);
    }

    @DisplayName("no brace is ever swallowed by a trailing line comment, at any line width")
    @Test
    public void bracesNeverInsideAComment() {
        TypeInfo ti = scan("a.b.X", SRC);
        // none of the comment texts above contain a brace, so a brace after '//' can only be swallowed structure
        for (int width = 20; width <= 200; width++) {
            String out = printAt(ti, width);
            for (String line : out.split("\n", -1)) {
                int slashes = line.indexOf("//");
                if (slashes < 0) continue;
                String afterComment = line.substring(slashes + 2);
                if (afterComment.indexOf('}') >= 0 || afterComment.indexOf('{') >= 0) {
                    fail("width " + width + ": a brace is inside a line comment, so this does not compile:\n"
                         + line + "\n--- full output ---\n" + out);
                }
            }
        }
    }

    @DisplayName("every brace still present: the comment must not hide structure at any width")
    @Test
    public void braceCountIsStable() {
        TypeInfo ti = scan("a.b.X", SRC);
        for (int width = 20; width <= 200; width++) {
            String out = printAt(ti, width);
            long open = out.chars().filter(c -> c == '{').count();
            long close = out.chars().filter(c -> c == '}').count();
            if (open != 3 || close != 3) {
                fail("width " + width + ": expected 3 pairs of braces, got " + open + "/" + close + ":\n" + out);
            }
        }
    }
}
