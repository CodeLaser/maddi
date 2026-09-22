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
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
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
    private final Map<Info, Map<Property, Outcome>> cache = new ConcurrentHashMap<>();

    public ContractResolution(Runtime runtime) {
        this.contractReader = new ContractReader(runtime);
    }

    public Outcome resolve(MethodInfo methodInfo, Property property) {
        return cached(methodInfo, property, _ -> {
            Value own = contractOf(methodInfo, property);
            Inherited inherited = strongestInherited(methodInfo, property,
                    parent -> contractOf(parent, property));
            return adjudicate(own, inherited);
        });
    }

    /**
     * The PARAMETER arm, same rule and same reasons. A bodiless method's parameter has nothing to compute from
     * either: {@code AbstractMethodAnalyzerImpl.unmodified} folds {@code UNMODIFIED_PARAMETER} over the
     * implementations' parameter at the same index, so one modifying implementation reconstructs — and
     * contradicts — the declaration the author wrote on the interface. The inherited contract is the one on the
     * overridden method's parameter at the SAME INDEX.
     */
    public Outcome resolve(ParameterInfo parameterInfo, Property property) {
        return cached(parameterInfo, property, _ -> {
            Value own = contractOf(parameterInfo, property);
            Inherited inherited = strongestInherited(parameterInfo.methodInfo(), property, parent -> {
                // a bridge/erasure override can differ in arity; index out of range means "not this declaration"
                if (parameterInfo.index() >= parent.parameters().size()) return null;
                return contractOf(parent.parameters().get(parameterInfo.index()), property);
            });
            return adjudicate(own, inherited);
        });
    }

    private Outcome cached(Info info, Property property, java.util.function.Function<Property, Outcome> compute) {
        return cache.computeIfAbsent(info, _ -> new ConcurrentHashMap<>()).computeIfAbsent(property, compute);
    }

    /** the strongest contract among the overridden declarations, and which one it came from */
    private record Inherited(Value value, MethodInfo source) {
        static final Inherited NONE = new Inherited(null, null);
    }

    private Inherited strongestInherited(MethodInfo methodInfo, Property property,
                                         java.util.function.Function<MethodInfo, Value> contractOfParent) {
        Inherited best = Inherited.NONE;
        for (MethodInfo parent : methodInfo.overrides()) {
            Value fromParent = contractOfParent.apply(parent);
            if (fromParent == null) continue;
            if (best.value() == null || stronger(fromParent, best.value())) {
                best = new Inherited(fromParent, parent);
            }
        }
        return best;
    }

    private static Outcome adjudicate(Value own, Inherited inherited) {
        if (own == null) {
            // "a contract on a declaration binds every override" — the same inheritance the shallow analyzer
            // gives jar methods, and which SourceContractMaterializer already applies to @IgnoreModifications
            return inherited.value() == null ? Outcome.NONE : new Outcome(inherited.value(), null);
        }
        if (inherited.value() == null || own.equals(inherited.value()) || stronger(own, inherited.value())) {
            return new Outcome(own, null);
        }
        return new Outcome(null, inherited.source());
    }

    private Value contractOf(Info info, Property property) {
        // most elements carry no annotation at all, and of those that do @Override is by far the most common;
        // skip before paying for the CST walk
        if (info.annotations().isEmpty()) return null;
        Map<Property, Value> contracts = contractReader.contracts(info);
        return contracts.get(property);
    }

    /** {@code overwriteAllowed} is the lattice's own "may be refined to": b may be refined to a ⇒ a is stronger */
    private static boolean stronger(Value a, Value b) {
        return b.overwriteAllowed(a);
    }
}
