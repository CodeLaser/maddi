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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link JavaDoc#commentWithPlaceholders()} keeps the comment's LINES: its line {@code k} is the javadoc's source
 * line {@code source().beginLine() + k}.
 * <p>
 * A consumer that places a word of the comment in the file relies on exactly that -- the text index does, and
 * through it rename.field, which renames a name's mentions in comments. A line holding only an HTML element
 * ({@code <p>}) produced no text and lost its line break, so every word below it came out one line too high per
 * such line: renaming ACTIVE_THREAD_COUNT_UNLIMITED in timefold's PartitionedSearchPhaseConfig, whose javadoc has
 * two {@code <p>} lines above the mention, inserted the new name two lines up, into a bare {@code *} line and into
 * the middle of a word (rename fuzz, slowTest 2026-09-24).
 */
public class TestJavaDocKeepsItsLines extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                public static final String AUTO = "most-cores";
                public static final String UNLIMITED = "all-cores";
                private String limit;

                /**
                 * The limit.
                 *
                 * <p>
                 * Defaults to {@value #AUTO} which consumes the majority
                 * but not all of the CPU cores.
                 *
                 * <p>
                 * Use {@value #UNLIMITED} to give it all CPU cores.
                 * This is useful if you're handling the CPU consumption on an OS level.
                 *
                 * @return null, a number, {@value #AUTO} or {@value #UNLIMITED}.
                 */
                public String getLimit() {
                    return limit;
                }
            }
            """;

    @DisplayName("a line holding only <p> keeps its place: every later line stays where the source has it")
    @Test
    public void linesSurviveHtmlOnlyLines() {
        TypeInfo x = scan("a.b.X", INPUT);
        JavaDoc javaDoc = x.findUniqueMethod("getLimit", 0).javaDoc();
        List<String> lines = List.of(javaDoc.commentWithPlaceholders().split("\n", -1));
        int begin = javaDoc.source().beginLine();
        // source lines: 8 'The limit.', 15 'Use {@value #UNLIMITED} ...', 16 'This is useful ...', 18 '@return ...'
        assertEquals(8, begin, javaDoc.source().toString());
        assertEquals(true, lines.get(15 - begin).contains("Use {@value #UNLIMITED}"), String.join("|", lines));
        assertEquals(true, lines.get(16 - begin).contains("This is useful"), String.join("|", lines));
        assertEquals(true, lines.get(18 - begin).startsWith("@return"), String.join("|", lines));
    }
}
