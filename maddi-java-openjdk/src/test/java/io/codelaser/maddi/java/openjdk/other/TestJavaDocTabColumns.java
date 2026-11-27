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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;

/**
 * Javadoc positions count CHARACTERS, as code positions do -- also in a tab-indented file.
 * <p>
 * javac's {@code LineMap.getColumnNumber} expands a tab to the next multiple of 8. The javadoc scanner used it, the
 * code scanner did not, so in a file indented with tabs a javadoc tag's source pointed past the tag, by 7 columns
 * per leading tab. A refactoring editing the tag at that position (a {@code {@link}} downgraded to
 * {@code {@code}}) wrote into the wrong characters, in the middle of the referenced name.
 */
public class TestJavaDocTabColumns extends CommonTest {

    // one tab before the field's javadoc lines; the line '\t * {@link Target}' has '{' at character 5
    private static final String INPUT = "package a.b;\n"
                                        + "public class C {\n"
                                        + "\t/**\n"
                                        + "\t * {@link Target}\n"
                                        + "\t */\n"
                                        + "\tString s;\n"
                                        + "}\n"
                                        + "class Target {\n"
                                        + "}\n";

    @DisplayName("a javadoc tag in a tab-indented line has character columns, not tab-expanded ones")
    @Test
    public void test() {
        TypeInfo c = scan("a.b.C", INPUT);
        FieldInfo s = c.getFieldByName("s", true);
        JavaDoc javaDoc = s.javaDoc();
        JavaDoc.Tag tag = javaDoc.tags().getFirst();
        Source source = tag.source();
        // '\t * {@link Target}': '{' is character 5, 'T' of Target character 12, '}' character 18
        assertEquals(4, source.beginLine(), source.toString());
        assertEquals("{@link Target}", "\t * {@link Target}".substring(source.beginPos() - 1, source.endPos()),
                "the tag's source must cover the tag's own characters: " + source);
        Source whole = javaDoc.source().detailedSources() == null ? null
                : javaDoc.source().detailedSources().detail(DetailedSources.WHOLE_COMMENT);
        if (whole != null) {
            assertEquals(3, whole.beginLine(), whole.toString());
            assertEquals(2, whole.beginPos(), "'/**' follows one tab, at character 2: " + whole);
        }
    }
}
