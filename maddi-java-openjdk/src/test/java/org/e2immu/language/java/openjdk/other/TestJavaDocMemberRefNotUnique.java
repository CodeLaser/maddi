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

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A javadoc member reference whose MEMBER cannot be pinned down — {@code {@link X#write}} where {@code write} is
 * overloaded, or names something that does not exist — used to resolve to nothing at all: {@code resolveReference}
 * returned null, and the caller then threw away the detailed sources it had already collected for the TYPE part.
 * The type part is certain even when the member is not, and consumers need its source token: a refactoring that
 * moves the referring file has to rewrite {@code X} to X's new location, or the simple name stops resolving.
 * (ES server-base carve, StreamOutputHelper: {@code {@link StreamOutput#write}}.)
 */
public class TestJavaDocMemberRefNotUnique extends CommonTest {

    @Language("java")
    String aOver = """
            package a;
            public class Over {
                public void write(int i) {}
                public void write(byte[] b) {}
                public void unique() {}
                public int field;
            }
            """;

    @Language("java")
    String bN = """
            package b;
            import a.Over;
            public class N {
                /**
                 * see {@link Over#write}
                 */
                public void m() {}
                /**
                 * see {@link Over#unique}
                 */
                public void m2() {}
                /**
                 * see {@link Over#field}
                 */
                public void m3() {}
                /**
                 * see {@link Over#noSuchMember}
                 */
                public void m4() {}
            }
            """;

    @DisplayName("an OVERLOADED member reference still resolves to the type, and keeps the type's source token")
    @Test
    public void testOverloadedMemberReference() {
        Map<String, TypeInfo> pr = scan(false, "a.Over", aOver, "b.N", bN);
        TypeInfo N = pr.get("b.N");
        TypeInfo over = pr.get("a.Over");

        JavaDoc.Tag tag = N.findUniqueMethod("m", 0).javaDoc().tags().getFirst();
        // 'write' is overloaded, so no single member can be named; the reference is to the type
        assertEquals(over, tag.resolvedReference());
        DetailedSources detailedSources = tag.source().detailedSources();
        assertNotNull(detailedSources, "detailed sources of the type part were discarded");
        // the token is the TYPE part alone ("Over"), not the whole reference ("Over#write"): a consumer rewrites
        // only the type when the referring file moves away from Over's package
        assertEquals("5-19:5-28", tag.sourceOfReference().compact2());
        assertEquals("5-19:5-22", detailedSources.detail(over).compact2());
    }

    @DisplayName("an unambiguous member reference is unaffected: it still resolves to the member itself")
    @Test
    public void testUniqueMemberReferenceUnchanged() {
        Map<String, TypeInfo> pr = scan(false, "a.Over", aOver, "b.N", bN);
        TypeInfo N = pr.get("b.N");
        TypeInfo over = pr.get("a.Over");

        JavaDoc.Tag method = N.findUniqueMethod("m2", 0).javaDoc().tags().getFirst();
        assertEquals(over.findUniqueMethod("unique", 0), method.resolvedReference());

        JavaDoc.Tag field = N.findUniqueMethod("m3", 0).javaDoc().tags().getFirst();
        assertEquals(over.getFieldByName("field", true), field.resolvedReference());
    }

    @DisplayName("a member that does not exist falls back to the type rather than resolving to nothing")
    @Test
    public void testUnknownMemberReference() {
        Map<String, TypeInfo> pr = scan(false, "a.Over", aOver, "b.N", bN);
        TypeInfo N = pr.get("b.N");
        TypeInfo over = pr.get("a.Over");

        JavaDoc.Tag tag = N.findUniqueMethod("m4", 0).javaDoc().tags().getFirst();
        assertEquals(over, tag.resolvedReference());
        assertNotNull(tag.source().detailedSources());
        assertNotNull(tag.source().detailedSources().detail(over));
    }
}
