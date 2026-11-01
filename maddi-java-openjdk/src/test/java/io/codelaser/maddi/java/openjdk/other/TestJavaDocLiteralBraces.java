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

import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * #166: a javadoc may hold {@code {n}} as text, a MessageFormat pattern in nacos. comment() took it for a tag
 * placeholder: {@code {0}} became tag 0, and {@code {1}}, with one tag only, threw IndexOutOfBoundsException.
 */
public class TestJavaDocLiteralBraces extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class Formats {
                /**
                 * Formats {@code key} as "{0}={1}".
                 */
                public static String format(String key) { return key; }
            }
            """;

    @DisplayName("literal {0} and {1} in a javadoc are kept as written")
    @Test
    public void test() {
        TypeInfo formats = scan("a.b.Formats", INPUT);
        JavaDoc javaDoc = formats.findUniqueMethod("format", 1).javaDoc();
        assertEquals(1, javaDoc.tags().size());
        String comment = javaDoc.comment();
        assertTrue(comment.contains("Formats {@code key} as \"{0}={1}\"."), comment);
    }
}
