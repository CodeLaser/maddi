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
import io.codelaser.maddi.modification.prepwork.variable.impl.VariableInfoImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * #15: {@code Links} equality is primary-only, and {@code VariableInfoImpl.setLinkedVariables} used it for change
 * detection, so a recomputed value with the same primary but different CONTENT replaced nothing: a variable's
 * statement-level links stayed at the method's FIRST link computation for the rest of the run (EC O4:
 * {@code iterator ↦ -} kept over {@code iterator.§m ☷{remove} this.§m}). Since f747b2105 the last computation wins
 * when the content differs; a change of primary is still refused, as before.
 */
public class TestLinksLastWins extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                void m(Object a, Object b, Object c) { }
            }
            """;

    @DisplayName("a recomputed value with the same primary and other content replaces the first")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        MethodInfo m = X.findUniqueMethod("m", 3);
        Variable a = m.parameters().get(0);
        Variable b = m.parameters().get(1);
        Variable c = m.parameters().get(2);

        VariableInfoImpl vi = new VariableInfoImpl(a, null, null, false);
        Links first = new LinksImpl(a, List.of(new LinksImpl.LinkImpl(a, LinkNatureImpl.OBJECT_GRAPH_OVERLAPS, b)));
        vi.setLinkedVariables(first);
        assertSame(first, vi.linkedVariables());

        // same primary, richer content: equal by Links.equals, yet the later computation must win
        Links second = new LinksImpl(a, List.of(
                new LinksImpl.LinkImpl(a, LinkNatureImpl.OBJECT_GRAPH_OVERLAPS, b),
                new LinksImpl.LinkImpl(a, LinkNatureImpl.SHARES_FIELDS, c)));
        assertEquals(first, second, "the premise: Links equality is primary-only");
        vi.setLinkedVariables(second);
        assertSame(second, vi.linkedVariables());

        // same primary, same content in a fresh object: nothing to replace
        Links secondAgain = new LinksImpl(a, List.of(
                new LinksImpl.LinkImpl(a, LinkNatureImpl.OBJECT_GRAPH_OVERLAPS, b),
                new LinksImpl.LinkImpl(a, LinkNatureImpl.SHARES_FIELDS, c)));
        vi.setLinkedVariables(secondAgain);
        assertSame(second, vi.linkedVariables());

        // poorer content also wins: the LAST computation, not the richest, is the settled one
        vi.setLinkedVariables(first);
        assertSame(first, vi.linkedVariables());

        // a change of primary is refused, as before
        Links otherPrimary = new LinksImpl(b, List.of(new LinksImpl.LinkImpl(b, LinkNatureImpl.OBJECT_GRAPH_OVERLAPS, c)));
        assertThrows(UnsupportedOperationException.class, () -> vi.setLinkedVariables(otherPrimary));
        assertSame(first, vi.linkedVariables());
    }
}
