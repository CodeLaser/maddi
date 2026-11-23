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

import static org.junit.jupiter.api.Assertions.*;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;

/**
 * A block {@code @see} with a LABEL resolves to its reference.
 * <p>
 * javac hands {@code @see Ref label words} back as a reference followed by the label's text; joining them made the
 * tag's content {@code "Ref; label words"}, which resolves to nothing, so the tag carried no reference at all and
 * every consumer that judges references skipped it. Found on a module split, where an unreachable
 * {@code @see ProblemChange Use ProblemChange to ...} was left behind unquoted and javadoc failed with
 * "reference not found".
 */
public class TestJavaDocLabelledSee extends CommonTest {

    @Language("java")
    private static final String SEE = """
            package a.b;
            class X {
                /**
                 * Text.
                 *
                 * @see Y Use Y to change it.
                 * @see Y#run() run it
                 * @see "a quoted string"
                 */
                void method() {}
            }
            class Y {
                void run() {}
            }
            """;

    @Test
    public void aLabelledSeeResolvesToItsReference() {
        TypeInfo X = scan("a.b.X", SEE);
        JavaDoc javaDoc = X.findUniqueMethod("method", 0).javaDoc();
        var sees = javaDoc.tags().stream().filter(t -> t.identifier() == JavaDoc.TagIdentifier.SEE).toList();
        assertEquals(3, sees.size(), sees.toString());

        assertEquals("Y", sees.get(0).content());
        assertInstanceOf(TypeInfo.class, sees.get(0).resolvedReference(), sees.get(0).toString());
        assertEquals("a.b.Y", ((TypeInfo) sees.get(0).resolvedReference()).fullyQualifiedName());

        assertEquals("Y#run()", sees.get(1).content());
        assertNotNull(sees.get(1).resolvedReference(), sees.get(1).toString());

        // a quoted @see has no reference to resolve, and keeps its text
        assertNull(sees.get(2).resolvedReference());
    }
}
