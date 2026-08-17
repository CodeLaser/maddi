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

package io.codelaser.maddi.java.openjdk.statement;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.statement.SwitchStatementNewStyle;
import io.codelaser.maddi.cst.api.statement.SwitchStatementOldStyle;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A comment above a {@code switch} belongs to the switch, and must not be claimed by the block the old-style form
 * synthesises for its case statements.
 *
 * <h2>Why the switch needed a test of its own</h2>
 * Every other statement kind simply had no comments (see {@code TestCommentBeforeEveryStatementKind}). The switch
 * looked the same from the statement's side — {@code comments()} empty — but the comment was not lost: it had been
 * attached to the switch's inner {@link Block}. So the two defects need different fixes, and fixing the switch the
 * way the others were fixed would have produced the comment TWICE.
 *
 * <h2>How it got there</h2>
 * The old-style form has no block of its own in the javac tree: the cases hang off the switch, and a {@code Block}
 * is synthesised to hold their statements. It was synthesised with <em>the switch's</em> source, and the block
 * builder ends with {@code addComments(commentsForNode(source))} — a lookup that answers "the comments preceding
 * this position". Given the switch's position, that is the switch's own leading comment. Every other caller passes
 * the block's own source, where the same lookup is right.
 *
 * <h2>What it cost</h2>
 * The comment disappeared from the output. Not misplaced — printing the method emitted the comment before the
 * {@code if} that followed and nothing at all for the switch, because the old-style switch printer does not print
 * its block's leading comments. So a caller that reprints a method silently dropped the user's comment, which is
 * why {@link #testItPrints()} asserts on the printed text and not only on the tree.
 */
public class TestCommentBeforeSwitch extends CommonTest {

    @Language("java")
    private static final String OLD_STYLE = """
            package a.b;
            class C {
              static int method(int n) {
                // before the switch
                switch(n) {
                  case 1: n++; break;
                  default: break;
                }
                // before the if
                if(n > 0) {
                  n++;
                }
                return n;
              }
            }
            """;

    @DisplayName("an old-style switch owns its comment, and its synthesised block does not also hold it")
    @Test
    public void testOldStyle() {
        TypeInfo typeInfo = scan("a.b.C", OLD_STYLE);
        MethodInfo method = typeInfo.findUniqueMethod("method", 1);
        Statement first = method.methodBody().statements().getFirst();
        assertTrue(first instanceof SwitchStatementOldStyle, () -> "expected an old-style switch, got " + first);

        assertEquals(List.of("before the switch"), comments(first),
                () -> "the switch should own the comment written above it");

        // and exactly once: the synthesised block must not hold a second copy, or a printer that learns to emit
        // block comments will emit this one twice
        SwitchStatementOldStyle sw = (SwitchStatementOldStyle) first;
        assertEquals(List.of(), comments(sw.block()),
                () -> "the synthesised block must not claim the switch's comment");
    }

    @Language("java")
    private static final String NEW_STYLE = """
            package a.b;
            class C {
              static int method(int n) {
                // before the switch
                switch(n) {
                  case 1 -> n++;
                  default -> {}
                }
                return n;
              }
            }
            """;

    @DisplayName("...and so does a new-style switch, which never had the block to blame")
    @Test
    public void testNewStyle() {
        TypeInfo typeInfo = scan("a.b.C", NEW_STYLE);
        MethodInfo method = typeInfo.findUniqueMethod("method", 1);
        Statement first = method.methodBody().statements().getFirst();
        assertTrue(first instanceof SwitchStatementNewStyle, () -> "expected a new-style switch, got " + first);
        assertEquals(List.of("before the switch"), comments(first),
                () -> "the switch should own the comment written above it");
    }

    /**
     * The tree being right is not the point on its own; the comment has to come back out. This is the assertion the
     * defect would have failed most visibly: before the fix the printed method carried {@code // before the if} and
     * no trace of {@code // before the switch}.
     */
    @DisplayName("the comment survives a round trip through the printer")
    @Test
    public void testItPrints() {
        TypeInfo typeInfo = scan("a.b.C", OLD_STYLE);
        MethodInfo method = typeInfo.findUniqueMethod("method", 1);
        String printed = method.print(runtime.qualificationQualifyFromPrimaryType()).toString();
        assertTrue(printed.contains("// before the switch"),
                () -> "the switch's comment did not survive printing:\n" + printed);
        // the neighbour is the control: it was never affected, so if it is missing too the failure is elsewhere
        assertTrue(printed.contains("// before the if"), () -> "control comment missing:\n" + printed);
        // once, not twice
        assertFalse(printed.indexOf("// before the switch") != printed.lastIndexOf("// before the switch"),
                () -> "the switch's comment was printed more than once:\n" + printed);
    }

    private static List<String> comments(io.codelaser.maddi.cst.api.element.Element element) {
        return element.comments().stream().map(c -> c.comment().strip()).toList();
    }
}
