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

package io.codelaser.maddi.java.openjdk.method;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A line comment written at the end of a statement's line must not end up printed BEFORE that statement.
 * <p>
 * {@code SourceCodeScan.addComments} deliberately looks ahead: it takes the comments preceding the NEXT
 * sibling, keeps those that begin on the line the current node ends on, and records them against the CURRENT
 * node -- so {@code m2(); // c} is correctly understood as a comment about {@code m2}, not about {@code m3}.
 * The printing side has no matching notion: a node's {@code comments()} are all emitted before it, so the
 * comment comes out one statement too early, above the statement it was written after. A reader then attaches
 * it to the wrong call, which for a comment is the whole of its meaning.
 * <p>
 * Fixing it properly means the CST carrying the distinction -- a same-line trailing comment is not a leading
 * comment -- and the printers emitting such a comment after the element, on its line. Dropping the lookahead
 * instead would put the comment back between the two statements, textually where it was written, at the cost
 * of the attribution the scanner works to establish.
 * <p>
 * Reached jfocus splitclass, where two create-mechanics tests print a method whose body carries exactly this
 * shape (`TestCDISplitMethodClusters3d`/`3e`, parked on it).
 */
public class TestTrailingStatementComment extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
                void m1() { }
                void m2() { }
                void m3() { }
                void main() {
                    m1();
                    m2(); // about m3, or about m2?
                    m3();
                }
            }
            """;

    @Disabled("known defect: a same-line trailing comment is printed before its statement; see the javadoc")
    @DisplayName("a line comment after a statement stays after it")
    @Test
    public void test() {
        TypeInfo C = scan("a.b.C", INPUT);
        String printed = print2(C.compilationUnit());
        int posM2 = printed.indexOf("m2();");
        int posComment = printed.indexOf("// about m3");
        assertEquals(true, posM2 >= 0 && posComment >= 0, printed);
        assertEquals(true, posComment > posM2,
                "the comment must not be printed before the statement it follows:\n" + printed);
    }
}
