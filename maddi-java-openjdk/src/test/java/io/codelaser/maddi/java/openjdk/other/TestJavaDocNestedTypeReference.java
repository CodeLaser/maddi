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

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A nested type named through its ENCLOSING type — {@code {@link Outer.Nested}} — which is legal wherever
 * {@code Outer} itself is in scope and is how nested types are usually written in javadoc.
 * <p>
 * ⛔ It resolved to NOTHING. {@code resolveType} tried the name as an FQN, as a simple name in the current
 * package, against each import ("does any import end on {@code .Outer.Nested}" — none does), and against the
 * current type's own members. The one thing it never tried was the way Java actually reads it: resolve the
 * HEAD, then walk down.
 * <p>
 * ⚠ A MISSING resolution is invisible in a way a wrong one is not. The tag simply carries no reference and no
 * source token, so every consumer sees a javadoc comment that mentions nothing. OpenSearch 2026-08-19:
 * {@code HttpRequest.method()} documents its return as {@code {@link RestRequest.Method}} over a plain
 * {@code import ….rest.RestRequest}; promoting {@code RestRequest.Method} to a top-level {@code RestMethod}
 * rewrote the whole codebase and left that one line untouched, because no javadoc pass could see it.
 * {@code :server:compileJava} runs doclint, so it was a build failure — see
 * {@code TestPromoteNestedTypeToTopLevel#javaDocFollowsARenamingPromotion} for the consumer end of it.
 */
public class TestJavaDocNestedTypeReference extends CommonTest {

    @Language("java")
    String aOuter = """
            package a;
            public class Outer {
                public static class Nested {
                    public static class Deep {}
                }
                public void method() {}
            }
            """;

    @Language("java")
    String bN = """
            package b;
            import a.Outer;
            public class N {
                /**
                 * see {@link Outer.Nested}
                 */
                public void m() {}
                /**
                 * see {@link Outer}
                 */
                public void m2() {}
                /**
                 * see {@link Outer.Nested.Deep}
                 */
                public void m3() {}
                /**
                 * see {@link a.Outer.Nested}
                 */
                public void m4() {}
                /**
                 * see {@link Outer.NoSuchNested}
                 */
                public void m5() {}
                /**
                 * see {@link Outer.Nested#toString()}
                 */
                public void m6() {}
            }
            """;

    private JavaDoc.Tag tagOf(TypeInfo n, String methodName) {
        return n.findUniqueMethod(methodName, 0).javaDoc().tags().getFirst();
    }

    /**
     * ⛔ THE LOAD-BEARING ONE. The token must span the WHOLE written chain, not just its last segment: a
     * consumer that relocates {@code Nested} replaces that token with the type's new name, and a token
     * covering only {@code Nested} would leave the dead {@code Outer.} prefix in front of it.
     */
    @DisplayName("a nested type written through its enclosing type resolves, with the whole chain as its token")
    @Test
    public void nestedTypeThroughEnclosingType() {
        Map<String, TypeInfo> pr = scan(false, "a.Outer", aOuter, "b.N", bN);
        TypeInfo n = pr.get("b.N");
        TypeInfo nested = pr.get("a.Outer").findSubType("Nested", true);

        JavaDoc.Tag tag = tagOf(n, "m");
        assertEquals(nested, tag.resolvedReference());
        DetailedSources ds = tag.source().detailedSources();
        assertNotNull(ds);
        // "     * see {@link Outer.Nested}" — 'Outer.Nested' occupies columns 19..30
        assertEquals("5-19:5-30", ds.detail(nested).compact2());
        // the enclosing type is stamped over its own prefix, so a consumer can rewrite Outer independently
        assertEquals("5-19:5-23", ds.detail(pr.get("a.Outer")).compact2());
        // ... and NOT as a fully qualified reference: the package is not written, so nothing may be keyed on it.
        // ⚠ The key must be the very String INSTANCE the TypeInfo hands out: DetailedSources is an
        // IdentityHashMap, so a literal "a" would answer null here whatever the builder stamped, and the
        // assertion would pass for the wrong reason. writtenAsFqn asks it exactly this way.
        assertNull(ds.detail(pr.get("a.Outer").packageName()), "the package is not part of the written name");
    }

    @DisplayName("CONTROL: the plain enclosing type is unaffected")
    @Test
    public void plainTypeStillResolves() {
        Map<String, TypeInfo> pr = scan(false, "a.Outer", aOuter, "b.N", bN);
        TypeInfo outer = pr.get("a.Outer");
        JavaDoc.Tag tag = tagOf(pr.get("b.N"), "m2");
        assertEquals(outer, tag.resolvedReference());
        assertEquals("9-19:9-23", tag.source().detailedSources().detail(outer).compact2());
    }

    /** Two levels down: the walk continues for as many segments as are written. */
    @DisplayName("a nested type of a nested type resolves too")
    @Test
    public void twoLevelsOfNesting() {
        Map<String, TypeInfo> pr = scan(false, "a.Outer", aOuter, "b.N", bN);
        TypeInfo deep = pr.get("a.Outer").findSubType("Nested", true).findSubType("Deep", true);
        JavaDoc.Tag tag = tagOf(pr.get("b.N"), "m3");
        assertEquals(deep, tag.resolvedReference());
        assertEquals("13-19:13-35", tag.source().detailedSources().detail(deep).compact2());
    }

    /**
     * CONTROL, and the one that pins the difference the consumers act on: a FULLY qualified nested reference
     * keys a detail on the PACKAGE name, which is how {@code MoveType} tells "written as an FQN" from "written
     * as a name that an import or the package keeps alive". The new step must not blur that.
     */
    @DisplayName("CONTROL: a fully qualified nested reference still stamps its package")
    @Test
    public void fullyQualifiedNestedReferenceKeepsItsPackageToken() {
        Map<String, TypeInfo> pr = scan(false, "a.Outer", aOuter, "b.N", bN);
        TypeInfo nested = pr.get("a.Outer").findSubType("Nested", true);
        JavaDoc.Tag tag = tagOf(pr.get("b.N"), "m4");
        assertEquals(nested, tag.resolvedReference());
        DetailedSources ds = tag.source().detailedSources();
        assertNotNull(ds.detail(pr.get("a.Outer").packageName()), "a written FQN must key its package");
        assertEquals("17-19:17-32", ds.detail(nested).compact2());
    }

    /**
     * ⚠ THE NEGATIVE CONTROL. The walk must yield NOTHING when a segment does not exist, rather than falling
     * back to the head — resolving {@code Outer.NoSuchNested} to {@code Outer} would make a broken reference
     * look live and hand consumers a token spanning text they must not rewrite.
     */
    @DisplayName("CONTROL: an unknown segment resolves to nothing, not to the enclosing type")
    @Test
    public void unknownSegmentResolvesToNothing() {
        Map<String, TypeInfo> pr = scan(false, "a.Outer", aOuter, "b.N", bN);
        assertNull(tagOf(pr.get("b.N"), "m5").resolvedReference());
    }

    /** The member-reference arm reaches resolveType with the same name, so it gains the same repair. */
    @DisplayName("a MEMBER of a nested type written through its enclosing type resolves as well")
    @Test
    public void memberOfNestedTypeThroughEnclosingType() {
        Map<String, TypeInfo> pr = scan(false, "a.Outer", aOuter, "b.N", bN);
        TypeInfo nested = pr.get("a.Outer").findSubType("Nested", true);
        JavaDoc.Tag tag = tagOf(pr.get("b.N"), "m6");
        assertNotNull(tag.resolvedReference(), "the member reference lost its type part");
        assertEquals("25-19:25-30", tag.source().detailedSources().detail(nested).compact2());
    }
}
