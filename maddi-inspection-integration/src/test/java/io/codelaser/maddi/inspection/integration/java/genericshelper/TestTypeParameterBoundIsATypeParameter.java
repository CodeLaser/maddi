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

package io.codelaser.maddi.inspection.integration.java.genericshelper;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code initialTypeParameterMap} replaces a {@code ?} by {@code ? extends X}, with X the type parameter's
 * first bound — and it read that bound's {@code typeInfo()} without checking it.
 * <p>
 * ⛔ <b>A BOUND THAT IS ITSELF A TYPE PARAMETER HAS NO {@code typeInfo()}.</b> {@code Pair<T, U extends T>} is
 * ordinary Java, and used at a wildcard site it produced
 * {@code NullPointerException: Cannot invoke "TypeInfo.isJavaLangObject()" because "bound" is null}.
 * <p>
 * ⚠ THE COST IS WHAT MAKES IT WORTH A ROW RATHER THAN A ONE-LINE FIX. It surfaced only on a corpus-wide
 * member sweep — {@code qualifiedElements=["**.**$*.*(**)"]} — which reached it after <b>28 minutes</b> over
 * 38,618 types and then wrote nothing. One type anywhere in the corpus with this shape loses the whole run,
 * and a corpus-wide sweep is exactly what a caller reaches for once the glob surface tells them how
 * (see {@code #181}).
 * <p>
 * ▶ THE FIX KEEPS THE INTENT: the bound still becomes {@code ? extends <bound>} — a bound that is a type
 * parameter is still a bound worth carrying. Only the route changes, from "build a type from its TypeInfo"
 * to "take the bound and put a wildcard on it", because the first route cannot express a type parameter.
 */
public class TestTypeParameterBoundIsATypeParameter extends CommonTest2 {

    @Language("java")
    private static final String PAIR = """
            package a.b;
            public class Pair<T, U extends T> {
                T first;
                U second;
            }
            """;

    /** ⚠ The wildcard site is what triggers the substitution; a raw or fully-applied use never reaches it. */
    @Language("java")
    private static final String USER = """
            package a.b;
            public class User {
                Pair<?, ?> unknown;
                Pair<String, String> known;
            }
            """;

    private ParameterizedType fieldType(ParseResult pr, String field) {
        TypeInfo user = pr.findType("a.b.User");
        FieldInfo fieldInfo = user.fields().stream().filter(f -> field.equals(f.name())).findFirst().orElseThrow();
        return fieldInfo.type();
    }

    /**
     * ⛔⛔ THE HEADLINE. Before the fix this threw an NPE out of {@code initialTypeParameterMap}; the assertion
     * is on the MAP rather than merely on "it did not throw", so the fix has to keep the substitution's
     * meaning and not just stop crashing.
     */
    @DisplayName("#179: a type parameter whose bound is another type parameter does not NPE")
    @Test
    public void boundIsATypeParameter() throws IOException {
        ParseResult pr = init(Map.of("a.b.Pair", PAIR, "a.b.User", USER));
        ParameterizedType unknown = fieldType(pr, "unknown");
        assertEquals("Type a.b.Pair<?,?>", unknown.toString());

        Map<?, ?> map = assertDoesNotThrow(unknown::initialTypeParameterMap,
                "a bound that is a type parameter has no typeInfo(), and that used to be an NPE");

        // T is unbounded, so it stays a bare wildcard; U extends T keeps its bound, as the code intends
        assertEquals(2, map.size(), "" + map);
        assertTrue(map.toString().contains("T="), "" + map);
        assertTrue(map.toString().contains("U="), "" + map);
    }

    /**
     * ⚠ CONTROL: the ordinary case must be unchanged. A fix that returned a bare wildcard for EVERY bound
     * would pass the headline and quietly drop every real bound in the corpus — which is worse than the
     * crash, because nothing would say so.
     */
    @DisplayName("#179 CONTROL: a bound that IS a type keeps producing `? extends <that type>`")
    @Test
    public void boundIsAnOrdinaryType() throws IOException {
        @Language("java") String bounded = """
                package a.b;
                public class Bounded<T extends Number> {
                    T value;
                }
                """;
        @Language("java") String user = """
                package a.b;
                public class UserOfBounded {
                    Bounded<?> unknown;
                }
                """;
        ParseResult pr = init(Map.of("a.b.Bounded", bounded, "a.b.UserOfBounded", user));
        TypeInfo u = pr.findType("a.b.UserOfBounded");
        ParameterizedType unknown = u.fields().getFirst().type();

        Map<?, ?> map = unknown.initialTypeParameterMap();
        assertTrue(map.toString().contains("Number"),
                "the bound must survive as `? extends Number`, not be flattened to `?`: " + map);
    }

    /** ⚠ CONTROL: a fully applied use has no wildcard, so it never enters the substitution at all. */
    @DisplayName("#179 CONTROL: a fully applied type is unaffected")
    @Test
    public void fullyApplied() throws IOException {
        ParseResult pr = init(Map.of("a.b.Pair", PAIR, "a.b.User", USER));
        Map<?, ?> map = fieldType(pr, "known").initialTypeParameterMap();
        assertEquals(2, map.size(), "" + map);
        assertTrue(map.toString().contains("String"), "" + map);
    }
}
