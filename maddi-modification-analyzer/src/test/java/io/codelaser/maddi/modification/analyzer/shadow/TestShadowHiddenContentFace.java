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

package io.codelaser.maddi.modification.analyzer.shadow;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.link.impl.LinkNatureImpl;
import io.codelaser.maddi.modification.prepwork.variable.impl.LinksImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shadow pass follows an assigned-from link as "one end modified implies the other" only between two REAL faces,
 * the engine's own rule. vavr, 2026-09-23: {@code Future.sequence} builds {@code zero = successful(executor,
 * Stream.empty())} and hands it to {@code foldLeft(zero, f)}, whose {@code zero} is modified through the JDK hint on
 * {@code BiFunction.apply}. The local carried {@code zero.§tss ← None.INSTANCE.§tss} -- the two objects share hidden
 * content -- and stripping both virtual faces to their owners read it as {@code zero ← None.INSTANCE}, so the pass
 * marked {@code Option.None.INSTANCE} modified.
 * <p>
 * The corpus link does not form at unit scale (the fold summary there keeps no {@code return ← zero}), so the
 * decision is pinned on hand-built links.
 */
public class TestShadowHiddenContentFace extends CommonTest {

    @DisplayName("an assigned-from link carries whole-object modification only between real faces")
    @Test
    public void test() {
        TypeInfo x = javaInspector.parse("a.b.X", """
                package a.b;
                class X {
                    static Object NONE;
                    static Object ZERO;
                }
                """);
        FieldReference none = runtime.newFieldReference(x.getFieldByName("NONE", true));
        FieldReference zero = runtime.newFieldReference(x.getFieldByName("ZERO", true));
        FieldReference noneTss = hiddenContent(x, none);
        FieldReference zeroTss = hiddenContent(x, zero);

        assertTrue(ShadowModificationPass.carriesWholeObjectModification(
                new LinksImpl.LinkImpl(zero, LinkNatureImpl.IS_ASSIGNED_FROM, none)), "zero ← NONE");

        assertFalse(ShadowModificationPass.carriesWholeObjectModification(
                new LinksImpl.LinkImpl(zeroTss, LinkNatureImpl.IS_ASSIGNED_FROM, noneTss)),
                "zero.§tss ← NONE.§tss: shared hidden content, not an alias");
        assertFalse(ShadowModificationPass.carriesWholeObjectModification(
                new LinksImpl.LinkImpl(zeroTss, LinkNatureImpl.IS_ASSIGNED_FROM, none)), "one virtual face");
        assertFalse(ShadowModificationPass.carriesWholeObjectModification(
                new LinksImpl.LinkImpl(zero, LinkNatureImpl.IS_ELEMENT_OF, none)), "not an assignment");
    }

    private FieldReference hiddenContent(TypeInfo owner, FieldReference of) {
        FieldInfo tss = runtime.newFieldInfo("§tss", false, runtime.objectParameterizedType(), owner);
        return runtime.newFieldReference(tss, runtime.newVariableExpression(of), tss.type());
    }
}
