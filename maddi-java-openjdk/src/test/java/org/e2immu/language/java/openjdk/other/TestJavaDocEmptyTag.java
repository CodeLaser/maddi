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

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An EMPTY doc tag is legal input, and it must not throw.
 * <p>
 * {@code {@link }} compiles — javac emits a warning, not an error — and javac's own tree then reports a
 * {@code DCLink} whose {@code getReference()} is {@code null}. Dereferencing it threw
 * {@code NullPointerException: Cannot invoke "ReferenceTree.getSignature()"} out of the scanner, and because a
 * parse error anywhere refuses the whole {@code ParseResult}, ONE such comment in ONE file rejected an entire
 * 354-source-set Elasticsearch parse of 23,497 files. Found at
 * {@code x-pack/plugin/esql/compute/.../BucketedSortCommon.java:38}, which reads exactly {@code * See {@link }}.
 * <p>
 * {@code @throws} and {@code @see} with nothing after them are the same shape and are covered here too: the
 * bug was not "{@code @link} is special", it was "an empty tag has a null reference".
 */
public class TestJavaDocEmptyTag extends CommonTest {

    @Language("java")
    private static final String EMPTY_LINK = """
            package a.b;
            class X {
                /**
                 * See {@link }
                 */
                void method() {}
            }
            """;

    @Language("java")
    private static final String EMPTY_THROWS_AND_SEE = """
            package a.b;
            class Y {
                /**
                 * Text
                 * @throws
                 * @see
                 */
                void method() {}
            }
            """;

    @Test
    public void anEmptyLinkDoesNotThrow() {
        TypeInfo X = scan("a.b.X", EMPTY_LINK);
        JavaDoc javaDoc = X.findUniqueMethod("method", 0).javaDoc();
        assertNotNull(javaDoc, "the comment is still a comment");
        // the tag is kept, and its content is empty rather than absent: an empty link says nothing, and
        // dropping the tag would lose the fact that the author wrote one
        JavaDoc.Tag link = javaDoc.tags().stream()
                .filter(t -> t.identifier() == JavaDoc.TagIdentifier.LINK)
                .findFirst().orElseThrow(() -> new AssertionError("no LINK tag in " + javaDoc.tags()));
        assertEquals("", link.content());
        // ▶ MEASURED, not assumed: an empty reference resolves to the ENCLOSING type. My first version of this
        // test asserted null and was refuted — which is the useful half of writing it, because "an empty link
        // means this type" is a real behaviour a caller can rely on, and nothing recorded it before.
        assertEquals("a.b.X", ((TypeInfo) link.resolvedReference()).fullyQualifiedName());
    }

    @Test
    public void anEmptyThrowsAndSeeDoNotThrow() {
        TypeInfo Y = scan("a.b.Y", EMPTY_THROWS_AND_SEE);
        JavaDoc javaDoc = Y.findUniqueMethod("method", 0).javaDoc();
        assertNotNull(javaDoc);
        for (JavaDoc.Tag tag : javaDoc.tags()) {
            assertNotNull(tag.content(), "every tag has a content, even an empty one: " + tag.identifier());
        }
    }
}
