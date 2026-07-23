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

package org.e2immu.analyzer.modification.analyzer.eventual;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.impl.EventualCluster;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Greatest-fixpoint, step 1: {@link EventualCluster} <em>witnesses</em> every optimistic decision. When
 * {@code treatAsEventuallyImmutable} answers {@code true} only because of the seed (the candidate is a cluster
 * member whose own verdict is not yet proven), it records the edge <em>member &rarr; candidate</em> in
 * {@code assumptions()} — the ledger the contraction pass will walk. Recording changes no verdict; these tests
 * pin that the ledger is built (and stays empty when the assumption is not optimistic, or the gate is off).
 */
public class TestEventualClusterAssumptions extends CommonTest {

    // one compilation unit: C is a direct cluster candidate (a @Mark method gives it eventual intent), with its
    // own type-level EVENTUALLY_IMMUTABLE_TYPE deliberately left unset so treating it as eventual is optimistic;
    // nested M is a plain type used as the assuming member.
    @Language("java")
    private static final String SRC = """
            package a.b;
            public class C {
                private boolean f;
                public void commit() { this.f = true; }
                static class M { }
            }
            """;

    private record Parsed(TypeInfo candidate, TypeInfo member) { }

    private Parsed parse() {
        TypeInfo c = javaInspector.parse("a.b.C", SRC);
        // make C a *direct* candidate without proving its type-level verdict: a @Mark method is enough
        c.findUniqueMethod("commit", 0).analysis()
                .set(PropertyImpl.EVENTUAL_METHOD, ValueImpl.EventualImpl.mark("f"));
        return new Parsed(c, c.findSubType("M"));
    }

    @DisplayName("an optimistic treat-as-eventual records the member -> candidate assumption edge")
    @Test
    public void testOptimismIsWitnessed() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = true;
        try {
            Parsed p = parse();
            EventualCluster ec = new EventualCluster();
            ec.noteCandidate(p.candidate()); // (not required for a direct candidate, but this is how the analyzer feeds it)

            boolean treated = ec.treatAsEventuallyImmutable(p.member(), p.candidate(),
                    ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
            assertTrue(treated, "a cluster candidate is optimistically eventual under the gate");
            assertEquals(Set.of(p.candidate()), ec.assumptions().getOrDefault(p.member(), Set.of()),
                    "the member->candidate edge must be recorded for the contraction pass");
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    @DisplayName("a genuinely-proven candidate is not an assumption; the gate off records nothing")
    @Test
    public void testProvenAndGateOffRecordNothing() {
        boolean saved = EventualCluster.ENABLED;
        try {
            Parsed p = parse();

            // proven on its own merits (actual.isEventual()): true, but no assumption edge
            EventualCluster.ENABLED = true;
            EventualCluster ec1 = new EventualCluster();
            assertTrue(ec1.treatAsEventuallyImmutable(p.member(), p.candidate(),
                    new ValueImpl.EventuallyImmutableImpl("f", ValueImpl.ImmutableImpl.IMMUTABLE_HC)));
            assertTrue(ec1.assumptions().isEmpty(), "a proven verdict is not an optimistic assumption");

            // gate off: the optimistic branch never fires, nothing is treated or recorded
            EventualCluster.ENABLED = false;
            EventualCluster ec2 = new EventualCluster();
            assertFalse(ec2.treatAsEventuallyImmutable(p.member(), p.candidate(),
                    ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL));
            assertTrue(ec2.assumptions().isEmpty(), "the gate off records nothing");
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }
}
