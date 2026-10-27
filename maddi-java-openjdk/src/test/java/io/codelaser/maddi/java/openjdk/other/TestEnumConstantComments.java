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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An enum constant carries the comments written for it, as a field declaration does: a comment above it, and a
 * single-line comment after it on its own line. Until this test the comment pre-scan had no case for an enum
 * constant, so both were recorded for nothing, and a consumer removing a constant could not tell which comment went
 * with it.
 * <p>
 * And a javadoc carries where it is WRITTEN, {@code /**} through {@code *&#47;}, as
 * {@link DetailedSources.WHOLE_COMMENT} on its source. The javadoc's own source spans its text only, so on a line
 * shared with code -- {@code /** the second *&#47; B,} -- nothing said where the comment began.
 */
public class TestEnumConstantComments extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public enum E {
                // above A
                A, // after A
                /* above B */
                B,
                /** before C */ C,
                /**
                 * Above D.
                 */
                D; // after D
            }
            """;

    @DisplayName("each enum constant has the comments written for it, and a javadoc knows where it is written")
    @Test
    public void test() {
        TypeInfo e = scan("a.b.E", INPUT);

        assertEquals("[SingleLineComment at 3:5, SingleLineComment at 4:8]", comments(e, "A"),
                "A: '// above A' on the line above it (line 3, column 5), then '// after A' after it on its own"
                + " line (line 4, column 8)");
        assertEquals("[MultiLineComment at 5:5]", comments(e, "B"),
                "B: '/* above B */' on the line above it (line 5, column 5), and not A's '// after A'");
        assertEquals("[JavaDoc at 7:9]", comments(e, "C"),
                "C: its javadoc, whose source starts at its text 'before C' (line 7, column 9)");
        assertEquals("[JavaDoc at 9:8, SingleLineComment at 11:8]", comments(e, "D"),
                "D: its javadoc block, whose source starts at its text 'Above D.' (line 9, column 8), then"
                + " '// after D' after the ';' on its line (line 11, column 8)");

        assertEquals("7:5-7:19", wholeComment(e, "C"),
                "C's javadoc as written: '/** before C */' runs from column 5 to column 19 of line 7");
        assertEquals("8:5-10:7", wholeComment(e, "D"),
                "D's javadoc as written: '/**' at line 8 column 5 through '*/' at line 10 columns 6-7");
    }

    /** Each comment of the constant as its kind and the line:column where its source begins. */
    private static String comments(TypeInfo e, String constant) {
        List<Comment> comments = e.getFieldByName(constant, true).comments();
        return comments.stream()
                .map(c -> c.getClass().getSimpleName().replace("Impl", "")
                          + " at " + c.source().beginLine() + ":" + c.source().beginPos())
                .toList().toString();
    }

    private static String wholeComment(TypeInfo e, String constant) {
        Source source = e.getFieldByName(constant, true).javaDoc().source();
        return position(source.detailedSources().detail(DetailedSources.WHOLE_COMMENT));
    }

    private static String position(Source s) {
        return s.beginLine() + ":" + s.beginPos() + "-" + s.endLine() + ":" + s.endPos();
    }
}
