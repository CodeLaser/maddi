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

package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.prepwork.variable.Links;
import io.codelaser.maddi.modification.prepwork.variable.impl.LinksImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code Links.Builder.addAllDistinct} merges a variable's links over the sub-blocks of one statement. Its
 * distinctness is Link's (from,to)-only equality: a second nature on a pair already present is NOT added, the
 * first one stays. The check was a linear scan of the builder per incoming link, and on timefold's selector
 * factories it held one thread for 39 minutes; it goes through a hash index now, with the same semantics, kept
 * across adds and rebuilt after a removal.
 */
public class TestLinksBuilderAddAllDistinct extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                void m(Object a, Object b, Object c, Object d) { }
            }
            """;

    @DisplayName("distinct by (from,to): a second nature on a known pair is skipped, before and after a removal")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        MethodInfo m = X.findUniqueMethod("m", 4);
        Variable a = m.parameters().get(0);
        Variable b = m.parameters().get(1);
        Variable c = m.parameters().get(2);
        Variable d = m.parameters().get(3);

        LinksImpl.Builder builder = new LinksImpl.Builder(a);
        builder.add(LinkNatureImpl.OBJECT_GRAPH_OVERLAPS, b);

        Links sub1 = new LinksImpl(a, List.of(
                new LinksImpl.LinkImpl(a, LinkNatureImpl.SHARES_FIELDS, b),   // same pair, other nature: skipped
                new LinksImpl.LinkImpl(a, LinkNatureImpl.SHARES_FIELDS, c))); // new pair: added
        builder.addAllDistinct(sub1);
        assertEquals("a∩b,a≈c", render(builder));

        // the index follows a plain add
        builder.add(LinkNatureImpl.OBJECT_GRAPH_OVERLAPS, d);
        Links sub2 = new LinksImpl(a, List.of(
                new LinksImpl.LinkImpl(a, LinkNatureImpl.SHARES_FIELDS, d),   // known since the add: skipped
                new LinksImpl.LinkImpl(a, LinkNatureImpl.SHARES_FIELDS, c))); // known since sub1: skipped
        builder.addAllDistinct(sub2);
        assertEquals("a∩b,a≈c,a∩d", render(builder));

        // a removal invalidates the index; the pair is then free again
        builder.removeIf(l -> l.to().equals(c));
        assertEquals("a∩b,a∩d", render(builder));
        builder.addAllDistinct(sub2);
        assertEquals("a∩b,a∩d,a≈c", render(builder));
    }

    private static String render(LinksImpl.Builder builder) {
        return builder.build().stream()
                .map(l -> l.from().simpleName() + l.linkNature() + l.to().simpleName())
                .collect(Collectors.joining(","));
    }
}
