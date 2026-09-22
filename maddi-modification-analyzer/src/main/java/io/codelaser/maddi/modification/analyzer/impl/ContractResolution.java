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

package io.codelaser.maddi.modification.analyzer.impl;

import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.modification.common.defaults.ContractReader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adjudicates the AUTHORED statements bearing on one method + property, across the method itself and the methods
 * it overrides. The single place that answers "is this decided by contract, and to what value".
 *
 * <h2>Why a contract on a bodiless method is decided</h2>
 * A method with no body has nothing to compute from: the value the analyzer derives for it is a fold over its
 * implementations, i.e. a reconstruction of the declaration the author has already written down. Folding over
 * implementations to second-guess a declaration is not verification — it is guessing at something that was
 * stated. Worse, both folds in {@link AbstractMethodAnalyzerImpl} let a single implementation suppress the
 * declaration outright, by opposite routes: {@code methodIndependent} WAITS while any implementation is
 * undecided and so writes nothing at all, while {@code methodNonModifying} defaults an undecided implementation
 * to modifying. Measured on vavr 1.0.1: {@code @Independent(hc=true)} on {@code io.vavr.Value.iterator()}
 * changed nothing — 23 of its 24 implementations were independent and one ({@code HashMap.iterator()}) was
 * undecided, so the contract never reached {@code analysis()} and none of the 133 type verdicts moved.
 * <p>
 * Deciding is what makes the contract visible: every consumer — type independence, the exposure cap on
 * immutability, {@code LinkMethodCall} at each call site, the codec, the IDE daemon — reads {@code analysis()}
 * and nothing else. Seeding without also SKIPPING the fold would leave the fold writing over it every pass;
 * the write is refused ({@code overwriteAllowed} is upgrade-only), but a refused downgrade is exactly the
 * certification blind spot {@code TolerantWrite} counts, so every contracted method would generate one forever.
 *
 * <h2>Contract vs computed, and contract vs contract — two different things</h2>
 * A contract that disagrees with COMPUTED evidence (an implementation that is dependent where the interface
 * promised independence) is trusted and propagates: the analyzer cannot adjudicate an author's promise against
 * its own inference, and {@code GuardAnalyzerImpl.CONTRACT_VIOLATION} reports the offending implementation. That
 * is the trust-and-report trade the analysis-hints path already makes for jar types.
 * <p>
 * A contract that disagrees with ANOTHER CONTRACT is different in kind: two authored statements, and nothing in
 * the analyzer can rank them. Such a property is NOT decided — {@link Outcome#conflict()} is reported
 * ({@code CONTRACT_CONFLICT}) and nothing is seeded, so an unadjudicable statement never propagates. Computation
 * then decides it, exactly as if neither annotation had been written.
 * <p>
 * The one shape that counts as a conflict here is an own contract WEAKER than one it inherits: an override may
 * strengthen a promise, never weaken it. Two supertypes contracting differently is not a conflict — an
 * implementation must satisfy both, so the strongest is the floor.
 */
public class ContractResolution {

    /**
     * @param value      the adjudicated contract, or {@code null} when nothing is contracted or the statements conflict
     * @param weakerThan when non-null, the overridden method whose stronger contract this one contradicts
     */
    public record Outcome(Value value, MethodInfo weakerThan) {
        public static final Outcome NONE = new Outcome(null, null);

        /** decided by contract: seed it, and skip the fold */
        public boolean decided() {
            return value != null;
        }

        /** two authored statements disagree: report, decide nothing */
        public boolean conflict() {
            return value == null && weakerThan != null;
        }
    }

    private final ContractReader contractReader;
    // resolve() is called from both the materializer and the fold, for every abstract method, every pass; the
    // reader re-derives from the CST on each call by design (that is what lets the guard compare contract with
    // analysis()), so the repeated walk is memoized rather than repeated.
    private final Map<MethodInfo, Map<Property, Outcome>> cache = new ConcurrentHashMap<>();

    public ContractResolution(Runtime runtime) {
        this.contractReader = new ContractReader(runtime);
    }

    public Outcome resolve(MethodInfo methodInfo, Property property) {
        return cache.computeIfAbsent(methodInfo, _ -> new ConcurrentHashMap<>())
                .computeIfAbsent(property, p -> compute(methodInfo, p));
    }

    private Outcome compute(MethodInfo methodInfo, Property property) {
        Value own = contractOf(methodInfo, property);
        Value inherited = null;
        MethodInfo inheritedSource = null;
        for (MethodInfo parent : methodInfo.overrides()) {
            Value fromParent = contractOf(parent, property);
            if (fromParent == null) continue;
            if (inherited == null || stronger(fromParent, inherited)) {
                inherited = fromParent;
                inheritedSource = parent;
            }
        }
        if (own == null) {
            // "a contract on a declaration binds every override" — the same inheritance the shallow analyzer
            // gives jar methods, and which SourceContractMaterializer already applies to @IgnoreModifications
            return inherited == null ? Outcome.NONE : new Outcome(inherited, null);
        }
        if (inherited == null || own.equals(inherited) || stronger(own, inherited)) {
            return new Outcome(own, null);
        }
        return new Outcome(null, inheritedSource);
    }

    private Value contractOf(MethodInfo methodInfo, Property property) {
        // most methods carry no annotation at all, and of those that do @Override is by far the most common;
        // skip before paying for the CST walk
        if (methodInfo.annotations().isEmpty()) return null;
        Map<Property, Value> contracts = contractReader.contracts(methodInfo);
        return contracts.get(property);
    }

    /** {@code overwriteAllowed} is the lattice's own "may be refined to": b may be refined to a ⇒ a is stronger */
    private static boolean stronger(Value a, Value b) {
        return b.overwriteAllowed(a);
    }
}
