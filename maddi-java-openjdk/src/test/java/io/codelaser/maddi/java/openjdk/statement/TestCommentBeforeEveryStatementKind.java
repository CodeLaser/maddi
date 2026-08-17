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
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A comment written above a statement must reach that statement, whatever kind of statement it is.
 *
 * <h2>Why one test for every kind, rather than one per kind</h2>
 * Because the defect was an <b>omission</b>, and an omission is invisible from inside the case that has it. Each
 * statement kind is built by its own {@code visit*} method in {@code ScanCompilationUnit}, each ending in its own
 * builder chain, and nine of them simply never called {@code addComments}. {@code visitEnhancedForLoop} does, and
 * sits four lines above {@code visitForLoop}, which did not — the two are adjacent in the file and differed in
 * this for as long as both existed. A test per kind would have been written for the kind someone was working on;
 * only a test that walks the whole list makes the gap in the list visible.
 *
 * <h2>Why it matters beyond a lost comment</h2>
 * A dropped comment does not fail a build, so this stayed invisible until something downstream needed comments to
 * be complete rather than decorative. Anything that asks "where are the comments in this method" gets a wrong
 * answer, and no exception: a code-moving refactoring loses the user's comment, and a consumer that subtracts
 * comment text from source text (deciding which types a moved body really names) silently keeps everything a
 * comment happens to mention. The trigger there was a type name inside a disabled statement in front of a
 * {@code for} loop.
 *
 * <h2>The other front end was right all along</h2>
 * {@code maddi-java-parser} attaches comments to every statement kind, because it computes them ONCE per
 * statement — {@code ParseStatement} does {@code comments(statement)} before it branches — and every branch then
 * passes the same list on. This front end computes them per branch, which is what makes an omission possible at
 * all. That the two front ends disagreed is the part worth remembering: neither is checked against the other, so
 * a defect can live in one indefinitely while tests of the other stay green.
 */
public class TestCommentBeforeEveryStatementKind extends CommonTest {

    /**
     * One comment per statement, each naming the kind it belongs to, so a misattached comment is as visible as a
     * missing one — the assertion reads the text back, not just the count.
     */
    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class C {
              static int method(int n, List<String> names) {
                // localVariable
                List<String> copy = new ArrayList<>();
                // expression
                copy.add("a");
                // for
                for(int i = 0; i < n; i++) {
                  copy.add("x");
                }
                // while
                while(n > 100) {
                  // break
                  break;
                }
                // do
                do {
                  // continue
                  continue;
                } while(n > 100);
                // forEach
                for(String a: names) {
                  copy.add(a);
                }
                // if
                if(n > 1) {
                  copy.add("many");
                }
                // try
                try {
                  copy.add("t");
                } catch(RuntimeException re) {
                  // throw
                  throw new UnsupportedOperationException();
                }
                // synchronized
                synchronized(copy) {
                  copy.add("s");
                }
                // assert
                assert n >= 0;
                // return
                return copy.size();
              }
            }
            """;

    /** The kinds in the order the body declares them; the comment text is the kind's name. */
    private static final List<String> TOP_LEVEL = List.of("localVariable", "expression", "for", "while", "do",
            "forEach", "if", "try", "synchronized", "assert", "return");

    @DisplayName("a comment above a statement reaches that statement, for every kind of statement")
    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        List<Statement> statements = method.methodBody().statements();
        assertEquals(TOP_LEVEL.size(), statements.size(),
                () -> "fixture and expectation have drifted: " + describe(statements));

        List<String> missing = new ArrayList<>();
        for (int i = 0; i < statements.size(); i++) {
            Statement s = statements.get(i);
            String expected = TOP_LEVEL.get(i);
            if (s.comments().size() != 1 || !s.comments().getFirst().comment().contains(expected)) {
                missing.add(expected + " (" + s.getClass().getSimpleName() + ", comments="
                            + s.comments().stream().map(c -> c.comment().strip()).toList() + ")");
            }
        }
        assertTrue(missing.isEmpty(), () -> "these statements did not receive their own comment: " + missing);
    }

    /**
     * The nested ones, which travel a different path: they are built while their enclosing block is being parsed.
     * {@code break} and {@code continue} sit inside a loop and {@code throw} inside a catch clause, so none of
     * them can be reached from the top-level list above — and all three are built by a {@code visit*} of their
     * own, which is exactly where the omission lived.
     */
    @DisplayName("...including the kinds that only occur nested: break, continue, throw")
    @Test
    public void testNested() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        List<String> found = new ArrayList<>();
        method.methodBody().visit(element -> {
            if (element instanceof Statement s) {
                s.comments().forEach(c -> found.add(c.comment().strip()));
            }
            return true;
        });
        for (String nested : List.of("break", "continue", "throw")) {
            assertTrue(found.contains(nested),
                    () -> "no statement carries the '" + nested + "' comment; found " + found);
        }
    }

    private static String describe(List<Statement> statements) {
        return statements.stream().map(s -> s.getClass().getSimpleName()).toList().toString();
    }
}
