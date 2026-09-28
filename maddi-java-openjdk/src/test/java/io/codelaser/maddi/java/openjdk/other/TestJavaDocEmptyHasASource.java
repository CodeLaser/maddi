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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * #31: javac positions a doc comment by its body, so an empty {@code /** *&#47;} had no position and the JavaDoc
 * read as line 0. A consumer placing a cut relative to "the earliest comment" widened it to line 0 and died.
 * The comment's own {@code /**} token knows its line.
 */
public class TestJavaDocEmptyHasASource extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class Utils {
                /** */
                public static int kept(int x) { return x; }

                /** Real documentation. */
                public static int documented(int x) { return x; }
            }
            """;

    @DisplayName("an empty javadoc is positioned at its /** token")
    @Test
    public void test() {
        TypeInfo utils = scan("a.b.Utils", INPUT);
        JavaDoc empty = utils.findUniqueMethod("kept", 1).javaDoc();
        assertEquals(3, empty.source().beginLine(), empty.source().toString());
        assertEquals(5, empty.source().beginPos(), empty.source().toString());
        assertEquals(3, empty.source().endLine(), empty.source().toString());
        assertEquals("", empty.commentWithPlaceholders().strip());
        JavaDoc real = utils.findUniqueMethod("documented", 1).javaDoc();
        assertEquals(6, real.source().beginLine(), real.source().toString());
    }
}
