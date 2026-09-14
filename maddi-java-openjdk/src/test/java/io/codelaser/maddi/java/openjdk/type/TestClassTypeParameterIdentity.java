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

package io.codelaser.maddi.java.openjdk.type;

import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A CLASS type parameter must be ONE object, whichever order the compilation units are scanned in: the type's own
 * {@code typeParameters()}, every occurrence in its signatures, and the key its written positions are filed under
 * in {@code DetailedSources} (an identity map).
 * <p>
 * The third route of docs/method-type-parameter-source-loss.md. When the CALLER is scanned first, resolving
 * {@code list.add(t)} loads {@code A} from its symbol: that builds a {@code T} with no source and the bound widened
 * to {@code ? extends Tuple}, and builds {@code add} and {@code next} on it. The source scan of {@code A} then
 * created a second {@code T} and put it in place with {@code addOrSetTypeParameter}, which reaches the type's list
 * and its fields, but not the signatures already built. Rename then found {@code T}'s positions under the declared
 * instance and asked for them with the other one: {@code add(T tuple)} was left unrenamed.
 * <p>
 * Unlike the method route, a replace was possible here, and was exactly the problem: the fix fills the symbol-built
 * instances in from the declaration instead of replacing them.
 */
public class TestClassTypeParameterIdentity extends CommonTest {

    @Language("java")
    private static final String TUPLE = """
            package a;
            interface Tuple {
                <X> X getStore(int i);
                void setStore(int i, Object v);
            }
            """;

    @Language("java")
    private static final String A = """
            package a;
            class A<T extends Tuple> {
                private T first;

                public void add(T tuple) {
                    tuple.setStore(0, first);
                    first = tuple;
                }

                public T next(T tuple) {
                    return tuple.getStore(1);
                }
            }
            """;

    /** The caller. Resolving its calls creates {@code add} and {@code next} from their symbols. */
    @Language("java")
    private static final String USER = """
            package a;
            class User {
                void use(A<Tuple> list, Tuple t) {
                    list.add(t);
                    Tuple n = list.next(t);
                }
            }
            """;

    // one scan per test: the harness registers its types in a shared InfoByFqn
    private TypeInfo a(boolean callerFirst) {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("a.Tuple", TUPLE);
        if (callerFirst) sources.put("a.User", USER);
        sources.put("a.A", A);
        if (!callerFirst) sources.put("a.User", USER);
        return scan(false, sources).primaryTypes().stream()
                .filter(t -> "a.A".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
    }

    @DisplayName("declaration scanned first: one T everywhere")
    @Test
    public void testDeclarationFirst() {
        assertOneT(a(false));
    }

    @DisplayName("caller scanned first: still one T, with the declaration's source and bound")
    @Test
    public void testCallerFirst() {
        assertOneT(a(true));
    }

    private static void assertOneT(TypeInfo a) {
        assertEquals(1, a.typeParameters().size());
        TypeParameter t = a.typeParameters().getFirst();
        assertNotNull(t.source(), "the declaration's own source");
        assertEquals("2-9:2-23", t.source().compact2());
        assertEquals("[Type a.Tuple]", t.typeBounds().toString(), "the bound as written, not '? extends'");

        assertOccurrence(t, "field first", a.getFieldByName("first", true).type(),
                a.getFieldByName("first", true).source());
        MethodInfo add = a.findUniqueMethod("add", 1);
        assertOccurrence(t, "add(T tuple)", add.parameters().getFirst().parameterizedType(),
                add.parameters().getFirst().source());
        MethodInfo next = a.findUniqueMethod("next", 1);
        assertOccurrence(t, "next(T tuple)", next.parameters().getFirst().parameterizedType(),
                next.parameters().getFirst().source());
        assertOccurrence(t, "T next(..)", next.returnType(), next.source());
    }

    /** The occurrence holds the type's own instance, and its position is found by asking with that instance. */
    private static void assertOccurrence(TypeParameter t, String where, ParameterizedType type, Source source) {
        assertSame(t, type.typeParameter(), where + ": the type's own T, not a second instance");
        assertNotNull(source, where);
        assertFalse(source.detailedSources().details(t).isEmpty(), where + ": the position of T, keyed by T");
    }
}
