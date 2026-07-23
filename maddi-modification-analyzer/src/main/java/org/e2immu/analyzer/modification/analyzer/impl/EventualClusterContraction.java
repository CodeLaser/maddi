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

package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The <em>contraction</em> half of the {@link EventualCluster} greatest fixpoint (step 2). The oracle supplies the
 * optimistic seed and {@link EventualCluster#assumptions() records} every edge {@code member -> candidate} where a
 * member concluded eventually immutable only by assuming an as-yet-unproven candidate. This pass runs once, after
 * the iterating analyzer has converged, and computes the largest self-consistent subset of the members that
 * obtained an eventual verdict: it drops any member that (transitively) relied on a candidate which did not itself
 * end up eventually immutable, then <b>retracts</b> the dropped members' {@code EVENTUALLY_IMMUTABLE_TYPE}.
 * <p>
 * <b>Why a separate post-pass, not an in-loop downgrade.</b> {@code EVENTUALLY_IMMUTABLE_TYPE} is written once
 * ({@code TypeEventualAnalyzerImpl.computeTypeLevel} bails if it is already set) and is not cleared by
 * {@code IteratingAnalyzerImpl.clearDerivedFamily}, and analysis() writes are monotone (a weakening is refused by
 * {@code TolerantWrite}). A retraction is a weakening, so it cannot happen inside the monotone loop; instead this
 * pass runs at the terminal certification point and <em>removes</em> the property outright. The seed only ever
 * influenced {@code EVENTUALLY_IMMUTABLE_TYPE} (the optimistic contribution fires solely in the after-mark branch
 * of {@code immutableSuper} and in {@code fieldHoldsCommittableContent}), so clearing that property is the whole
 * retraction — no derived-family recompute is needed.
 * <p>
 * <b>Soundness vs completeness.</b> The assumption ledger is a superset of the final structural dependencies (an
 * edge recorded while a candidate was unproven survives even if the candidate is later proven independently), so
 * the contraction is conservative: it never keeps an unsound verdict, but could in principle drop a verdict that
 * turned out justifiable. On a self-consistent cluster (maddi's own {@code Info} family) every assumed candidate
 * is retained, so the pass retracts nothing — that no-op is the evidence the seeded result was sound.
 * <p>
 * Gated on {@code EVENTUALCLUSTER}: off the gate the assumption ledger is empty and this pass is a no-op.
 */
public class EventualClusterContraction {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventualClusterContraction.class);

    private EventualClusterContraction() {
    }

    /**
     * The greatest-fixpoint contraction as pure set logic (generic, so it is unit-testable without the CST): the
     * members to <em>retract</em> — those in {@code haveVerdict} that are not in the largest subset closed under
     * "every candidate I assumed is also retained". A member is dropped when some candidate in
     * {@code assumptions.get(member)} is not retained; dropping cascades until a fixpoint.
     */
    public static <T> Set<T> membersToRetract(Set<T> haveVerdict, Map<T, ? extends Set<T>> assumptions) {
        Set<T> retained = new HashSet<>(haveVerdict);
        boolean changed = true;
        while (changed) {
            changed = false;
            Iterator<T> it = retained.iterator();
            while (it.hasNext()) {
                T member = it.next();
                Set<T> assumed = assumptions.get(member);
                if (assumed != null && !retained.containsAll(assumed)) {
                    it.remove();
                    changed = true;
                }
            }
        }
        Set<T> retract = new HashSet<>(haveVerdict);
        retract.removeAll(retained);
        return retract;
    }

    /**
     * Apply the contraction to a converged analysis: retract {@code EVENTUALLY_IMMUTABLE_TYPE} on every member
     * whose optimistic verdict did not survive the fixpoint. Returns the number of verdicts retracted (0 off the
     * gate, and 0 whenever the seeded cluster was self-consistent).
     */
    public static int retract(List<Info> analysisOrder, EventualCluster cluster) {
        if (!EventualCluster.ENABLED) return 0;
        // the ledger with label provenance folded in: an interface whose abstract methods inherited their
        // eventually-non-modifying labels also inherited the implementations' assumption edges
        Map<TypeInfo, Set<TypeInfo>> assumptions = cluster.effectiveAssumptions();
        if (assumptions.isEmpty()) return 0;

        // an assumption "treat candidate as eventually immutable" is discharged by an eventual verdict, or -- a
        // fortiori -- by the candidate ending up UNCONDITIONALLY at least immutable-hc (MethodInspection): the
        // optimism was only that the candidate's content would at some point stop changing
        Set<TypeInfo> haveEventual = new HashSet<>();
        Set<TypeInfo> discharged = new HashSet<>();
        for (Info info : analysisOrder) {
            if (info instanceof TypeInfo t) {
                if (t.analysis()
                        .getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE, ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL)
                        .isEventual()) {
                    haveEventual.add(t);
                    discharged.add(t);
                } else if (t.analysis()
                        .getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE)
                        .isAtLeastImmutableHC()) {
                    discharged.add(t);
                    // its own broken leans (if any) affected only method labels, which are vacuous on an
                    // unconditionally immutable type; do not let them cascade through the closure
                    assumptions.remove(t);
                }
            }
        }
        // an assumed candidate OUTSIDE the analysis order (java.lang.Record, pulled in as a record's
        // supertype) discharges through its preloaded unconditional verdict; it can never appear in the
        // analysis-order scan above
        for (Set<TypeInfo> assumed : assumptions.values()) {
            for (TypeInfo candidate : assumed) {
                if (!discharged.contains(candidate) && candidate.analysis()
                        .getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE)
                        .isAtLeastImmutableHC()) {
                    discharged.add(candidate);
                }
            }
        }
        Set<TypeInfo> retract = membersToRetract(discharged, assumptions);
        retract.retainAll(haveEventual); // only an eventual verdict can be retracted
        if (System.getenv("EC_RETRACT_DEBUG") != null) {
            for (TypeInfo t : retract) {
                Set<TypeInfo> assumed = assumptions.getOrDefault(t, Set.of());
                String broken = assumed.stream().filter(c -> !discharged.contains(c))
                        .map(TypeInfo::fullyQualifiedName).sorted().collect(java.util.stream.Collectors.joining(", "));
                System.out.println("ECRETRACT " + t.fullyQualifiedName() + " <- broken: ["
                        + broken + "] (cascade if empty)");
            }
        }
        for (TypeInfo t : retract) {
            t.analysis().removeIf(p -> PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE.key().equals(p.key()));
            LOGGER.debug("EC contraction: retract eventual verdict of {} (unproven assumption)", t);
        }
        if (!retract.isEmpty()) {
            LOGGER.info("EC contraction: retracted {} optimistic eventual verdict(s) that leaned on unproven candidates",
                    retract.size());
        }
        return retract.size();
    }
}
