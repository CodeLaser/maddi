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

import org.e2immu.analyzer.modification.common.util.TolerantWrite;
import org.e2immu.analyzer.modification.analyzer.AbstractMethodAnalyzer;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;

public class AbstractMethodAnalyzerImpl extends CommonAnalyzerImpl implements AbstractMethodAnalyzer {

    private final EventualCluster eventualCluster;

    public AbstractMethodAnalyzerImpl(IteratingAnalyzer.Configuration configuration, AtomicInteger propertiesChanged,
                                      List<Message> analyzerMessages, EventualCluster eventualCluster) {
        super(configuration, propertiesChanged, analyzerMessages);
        this.eventualCluster = eventualCluster;
    }

    @Override
    public void go(boolean firstIteration, List<MethodInfo> abstractMethods) {
        for (MethodInfo methodInfo : abstractMethods) {
            Value.SetOfMethodInfo implementations = methodInfo.analysis().getOrDefault(IMPLEMENTATIONS,
                    ValueImpl.SetOfMethodInfoImpl.EMPTY);
            if (implementations.isEmpty()) {
                if (firstIteration) doMethodWithoutImplementation(methodInfo);
            } else {
                Iterable<MethodInfo> concreteImplementations = implementations.methodInfoSet();
                for (ParameterInfo pi : methodInfo.parameters()) {
                    unmodified(concreteImplementations, pi);
                    independent(concreteImplementations, pi);
                    collectDowncast(concreteImplementations, pi);
                }
                methodNonModifying(concreteImplementations, methodInfo);
                methodIndependent(concreteImplementations, methodInfo);
                methodEventual(concreteImplementations, methodInfo);
                methodEventuallyNonModifying(concreteImplementations, methodInfo);
            }
        }
    }

    // no implementation means we can completely ignore it
    private void doMethodWithoutImplementation(MethodInfo methodInfo) {
        if (!methodInfo.analysis().haveAnalyzedValueFor(INDEPENDENT_METHOD)) {
            TolerantWrite.setOnce(methodInfo.analysis(), INDEPENDENT_METHOD, INDEPENDENT, methodInfo);
            DECIDE.debug("AMA: Decide independent method without implementations {}", methodInfo);
            propertyChanges.incrementAndGet();
        }
        if (!methodInfo.analysis().haveAnalyzedValueFor(IMMUTABLE_METHOD)) {
            TolerantWrite.setOnce(methodInfo.analysis(), IMMUTABLE_METHOD, ValueImpl.ImmutableImpl.IMMUTABLE, methodInfo);
            DECIDE.debug("AMA: Decide immutable method without implementations {}", methodInfo);
            propertyChanges.incrementAndGet();
        }
        for (ParameterInfo pi : methodInfo.parameters()) {
            if (!pi.analysis().haveAnalyzedValueFor(INDEPENDENT_PARAMETER)) {
                TolerantWrite.setOnce(pi.analysis(), INDEPENDENT_PARAMETER, INDEPENDENT, pi);
                DECIDE.debug("AMA: Decide independent parameter of method without implementations {}", pi);
                propertyChanges.incrementAndGet();
            }
            if (!pi.analysis().haveAnalyzedValueFor(UNMODIFIED_PARAMETER)) {
                TolerantWrite.setOnce(pi.analysis(), UNMODIFIED_PARAMETER, TRUE, pi);
                DECIDE.debug("AMA: Decide unmodified parameter of method without implementations {}", pi);
                propertyChanges.incrementAndGet();
            }
        }
    }

    private void collectDowncast(Iterable<MethodInfo> concreteImplementations, ParameterInfo pi) {
        Value.VariableToTypeInfoSet downcastsValue = pi.analysis().getOrNull(DOWNCAST_PARAMETER,
                ValueImpl.VariableToTypeInfoSetImpl.class);
        if (downcastsValue == null) {
            Map<Variable, Set<TypeInfo>> downcasts = new HashMap<>();
            for (MethodInfo implementation : concreteImplementations) {
                ParameterInfo pii = implementation.parameters().get(pi.index());
                Value.VariableToTypeInfoSet downcastsImplValue = pii.analysis().getOrNull(DOWNCAST_PARAMETER,
                        ValueImpl.VariableToTypeInfoSetImpl.class);
                if (downcastsImplValue == null) {
                    UNDECIDED.debug("AMA: Undecided downcast parameter {}", pi);
                    return;
                }
                downcastsImplValue.variableToTypeInfoSet().forEach((v, set) ->
                        downcasts.merge(v, set,
                                (s0, s1) -> Stream.concat(s0.stream(), s1.stream()).collect(Collectors.toUnmodifiableSet())));
            }
            ValueImpl.VariableToTypeInfoSetImpl value = new ValueImpl.VariableToTypeInfoSetImpl(Map.copyOf(downcasts));
            if (TolerantWrite.setAllowControlledOverwrite(pi.analysis(), DOWNCAST_PARAMETER, value, pi)) {
                DECIDE.debug("AMA: Decide downcast parameter {}: {}", pi, value.nice());
                propertyChanges.incrementAndGet();
            }
        }
    }

    private void independent(Iterable<MethodInfo> concreteImplementations, ParameterInfo pi) {
        Value.Independent independent = pi.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT);
        if (independent.isIndependent()) {
            return;
        }
        Value.Independent fromImplementations = INDEPENDENT;
        for (MethodInfo implementation : concreteImplementations) {
            ParameterInfo pii = implementation.parameters().get(pi.index());
            Value.Independent independentImpl = pii.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT);
            fromImplementations = fromImplementations.min(independentImpl);
            if (fromImplementations.isDependent()) break; // no need to try others
        }
        if (TolerantWrite.setAllowControlledOverwrite(pi.analysis(), INDEPENDENT_PARAMETER, fromImplementations, pi)) {
            DECIDE.debug("AMA: Decide independent of param {} = {}", pi, fromImplementations);
        }
    }

    private void methodNonModifying(Iterable<MethodInfo> concreteImplementations, MethodInfo methodInfo) {
        Value.Bool nonModifying = methodInfo.analysis().getOrDefault(NON_MODIFYING_METHOD, FALSE);
        if (nonModifying.isTrue()) {
            return;
        }
        Value.Bool fromImplementations = TRUE;
        for (MethodInfo implementation : concreteImplementations) {
            Value.Bool nonModImpl = implementation.analysis().getOrDefault(NON_MODIFYING_METHOD, FALSE);
            if (nonModImpl.isFalse()) {
                fromImplementations = FALSE;
                break;
            }
        }
        if (TolerantWrite.setAllowControlledOverwrite(methodInfo.analysis(), NON_MODIFYING_METHOD, fromImplementations, methodInfo)) {
            DECIDE.debug("AM: Decide non-modifying of method {} = {}", methodInfo, fromImplementations);
        }
    }

    /**
     * Eventual immutability travels from implementation to abstract method (road to immutability §060). An
     * interface such as {@code TypeInfo} declares {@code commit}; the fact that it marks a state transition is
     * only visible in {@code TypeInfoImpl}, which holds the eventually immutable field. Without this step the
     * interface stays plain mutable, and -- by the hierarchy rule -- so does every implementation of it.
     * <p>
     * All implementations must agree, on the side of the transition <em>and</em> on the mark label. Labels are
     * field names, and two implementations are free to name their state differently; that is a disagreement we
     * cannot merge, so we conclude nothing.
     */
    private void methodEventual(Iterable<MethodInfo> concreteImplementations, MethodInfo methodInfo) {
        if (methodInfo.analysis().haveAnalyzedValueFor(EVENTUAL_METHOD)) return;
        Value.Eventual fromImplementations = null;
        for (MethodInfo implementation : concreteImplementations) {
            Value.Eventual eventual = implementation.analysis().getOrDefault(EVENTUAL_METHOD,
                    ValueImpl.EventualImpl.NOT_EVENTUAL);
            if (!eventual.isEventual()) return; // one implementation without a mark: no promise to make
            if (fromImplementations == null) {
                fromImplementations = eventual;
            } else if (!fromImplementations.equals(eventual)) {
                return;
            }
        }
        if (fromImplementations != null) {
            methodInfo.analysis().set(EVENTUAL_METHOD, fromImplementations);
            DECIDE.debug("AM: Decide eventual of abstract method {} = {}", methodInfo, fromImplementations);
            propertyChanges.incrementAndGet();
        }
    }

    /**
     * Carry {@code @NotModified(after=)} up to the abstract method, the same way {@link #methodEventual} carries
     * {@code @Mark}/{@code @Only}. This is what makes {@code Info.access()} (and the rest of the read-through
     * accessors) non-modifying after the mark, so the interface can reach an eventual verdict at all.
     * <p>
     * Every implementation must be compatible: either eventually-non-modifying after the same label set, or
     * unconditionally non-modifying (which holds after any mark). One implementation that modifies with no
     * after-label -- or whose modification is still undecided -- is a promise we cannot make.
     */
    private void methodEventuallyNonModifying(Iterable<MethodInfo> concreteImplementations, MethodInfo methodInfo) {
        if (methodInfo.analysis().haveAnalyzedValueFor(EVENTUALLY_NON_MODIFYING_METHOD)) return;
        Set<String> agreed = null;
        for (MethodInfo implementation : concreteImplementations) {
            Value.SetOfStrings evNonMod = implementation.analysis()
                    .getOrDefault(EVENTUALLY_NON_MODIFYING_METHOD, ValueImpl.SetOfStringsImpl.EMPTY_SET);
            if (!evNonMod.set().isEmpty()) {
                // the label rests on whatever the implementation's excusals assumed (commitLabels leans on the
                // cluster seed): carry the provenance so the contraction can cascade a broken assumption here
                eventualCluster.noteLabelInheritance(methodInfo.typeInfo(), implementation.typeInfo());
                if (agreed == null) agreed = evNonMod.set();
                else if (!agreed.equals(evNonMod.set())) {
                    // EVENTUALCLUSTER: unlike a @Mark, whose label names THE transition, "non-modifying after L"
                    // weakens monotonically -- once the union has passed, each implementation's own subset
                    // certainly has. The union is the weakest common guarantee (ParameterInfoImpl says
                    // 'methodInfo' where MethodInfoImpl says 'inspection' for the same abstract accessor).
                    if (!EventualCluster.ENABLED) return; // implementations name different transitions
                    Set<String> union = new HashSet<>(agreed);
                    union.addAll(evNonMod.set());
                    agreed = union;
                }
            } else {
                Value.Bool nonMod = implementation.analysis().getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
                if (nonMod == null || nonMod.isFalse()) return; // modifies with no after-label, or undecided
            }
        }
        if (agreed != null) {
            methodInfo.analysis().set(EVENTUALLY_NON_MODIFYING_METHOD, new ValueImpl.SetOfStringsImpl(Set.copyOf(agreed)));
            DECIDE.debug("AM: Decide eventually-non-modifying of abstract method {} after {}", methodInfo, agreed);
            propertyChanges.incrementAndGet();
        }
    }

    private void methodIndependent(Iterable<MethodInfo> concreteImplementations, MethodInfo methodInfo) {
        Value.Independent independent = methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT);
        if (independent.isIndependent()) {
            return;
        }
        Value.Independent fromImplementations = INDEPENDENT;
        for (MethodInfo implementation : concreteImplementations) {
            Value.Independent independentImpl = implementation.analysis().getOrDefault(INDEPENDENT_METHOD,
                    DEPENDENT);
            fromImplementations = fromImplementations.min(independentImpl);
            if (fromImplementations.isDependent()) break;
        }
        if (TolerantWrite.setAllowControlledOverwrite(methodInfo.analysis(), INDEPENDENT_METHOD, fromImplementations, methodInfo)) {
            DECIDE.debug("AMA: Decide independent of method {} = {}", methodInfo, fromImplementations);
        }
    }

    private void unmodified(Iterable<MethodInfo> concreteImplementations, ParameterInfo pi) {
        Value.Bool unmodified = pi.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE);
        if (unmodified.isTrue()) {
            return;
        }
        Set<MethodInfo> waitFor = new HashSet<>();
        Value.Bool fromImplementations = TRUE;
        for (MethodInfo implementation : concreteImplementations) {
            ParameterInfo pii = implementation.parameters().get(pi.index());
            Value.Bool unmodifiedImpl = pii.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE);
            if (unmodifiedImpl.isFalse()) {
                fromImplementations = FALSE;
                break;
            }
        }
        if (TolerantWrite.setAllowControlledOverwrite(pi.analysis(), UNMODIFIED_PARAMETER, fromImplementations, pi)) {
            DECIDE.debug("Decide unmodified of param {} = {}", pi, fromImplementations);
        }
    }
}

