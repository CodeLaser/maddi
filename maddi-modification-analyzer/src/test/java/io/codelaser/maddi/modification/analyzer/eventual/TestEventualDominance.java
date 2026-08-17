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

package io.codelaser.maddi.modification.analyzer.eventual;

import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.impl.EventualCluster;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The DOMINANCE discipline of {@code computeEventual}'s propagation step ({@code SideWalk}): a sided call
 * classifies its caller only when it witnesses the method's ENTRY state — on the spine, before any live
 * early exit, untainted by a possible earlier transition. The one-sided visitor stamped {@code @Only(after)}
 * on both-sides bodies ({@code isSet() ? get() : compute} — the {@code MethodInspectionImpl.Builder.
 * fullyQualifiedName} shape), a false contract the decorator ships to the IDE and the type level cannot
 * excuse. Off the gate, the old visitor is verbatim, pinned by the gate-off twin.
 */
public class TestEventualDominance extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            import io.codelaser.maddi.support.SetOnce;

            public class D {
              static class T {
                private final SetOnce<String> value = new SetOnce<>();
                public void set(String s) { value.set(s); }
                public String get() { return value.get(); }
                public String getOrCompute() { return value.isSet() ? value.get() : "pending"; }
                public String getGuarded() { if (value.isSet()) return value.get(); return "pending"; }
                public String ensure(String s) { if (!value.isSet()) value.set(s); return value.get(); }
                public String setThenGet(String s) { value.set(s); return value.get(); }
              }
            }
            """;

    private Value.Eventual eventual(TypeInfo t, String method, int params) {
        return t.findUniqueMethod(method, params).analysis()
                .getOrDefault(PropertyImpl.EVENTUAL_METHOD, ValueImpl.EventualImpl.NOT_EVENTUAL);
    }

    @DisplayName("gate on: only entry-state witnesses classify; both-sides bodies conclude no side")
    @Test
    public void testDominance() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = true;
        try {
            TypeInfo D = javaInspector.parse("D", INPUT);
            analyzer.go(prepWork(D));
            TypeInfo T = D.findSubType("T");

            // the honest, dominating forwards keep their classification
            Value.Eventual set = eventual(T, "set", 1);
            assertTrue(set.isMark());
            assertEquals(Set.of("value"), set.fields());
            Value.Eventual get = eventual(T, "get", 0);
            assertTrue(get.isOnly());
            assertEquals(Boolean.TRUE, get.after());

            // the guarded-fallback shapes are callable on BOTH sides: no classification --
            // stamping @Only(after) here was the MethodInspection.fullyQualifiedName bug
            assertFalse(eventual(T, "getOrCompute", 0).isEventual(), "ternary fallback: both sides");
            assertFalse(eventual(T, "getGuarded", 0).isEventual(), "early-return fallback: both sides");

            // ensure-then-read: the conditional set() taints 'value', so the unconditional get() no
            // longer witnesses the entry state -- no (false) @Only(after)
            assertFalse(eventual(T, "ensure", 1).isEventual(), "ensure-then-read: tainted");

            // spine set-then-get: the @Mark contributes AND taints, so the get() does not mix the sides
            Value.Eventual stg = eventual(T, "setThenGet", 1);
            assertTrue(stg.isMark(), "set-then-get is the transition, not mixed sides");
            assertEquals(Set.of("value"), stg.fields());
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    @DisplayName("gate off: the dominance discipline is UNGATED (2026-08-01) -- same witnesses, no false contracts")
    @Test
    public void testGateOff() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = false;
        try {
            TypeInfo D = javaInspector.parse("D", INPUT);
            analyzer.go(prepWork(D));
            TypeInfo T = D.findSubType("T");

            assertTrue(eventual(T, "set", 1).isMark());
            assertTrue(eventual(T, "get", 0).isOnly());
            // ungated: the guarded-fallback and ensure-then-read shapes conclude no side on either gate --
            // the historical one-sided visitor stamped @Only(after) here, a false contract
            assertFalse(eventual(T, "getOrCompute", 0).isEventual());
            assertFalse(eventual(T, "getGuarded", 0).isEventual());
            assertFalse(eventual(T, "ensure", 1).isEventual());
            // and the spine set-then-get is the transition on either gate
            assertTrue(eventual(T, "setThenGet", 1).isMark());
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }
}
