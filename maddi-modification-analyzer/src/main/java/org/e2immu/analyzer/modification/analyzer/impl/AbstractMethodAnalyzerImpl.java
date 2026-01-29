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

import org.e2immu.analyzer.modification.analyzer.AbstractMethodAnalyzer;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;

public class AbstractMethodAnalyzerImpl extends CommonAnalyzerImpl implements AbstractMethodAnalyzer {

    public AbstractMethodAnalyzerImpl(IteratingAnalyzer.Configuration configuration, AtomicInteger propertiesChanged) {
        super(configuration, propertiesChanged);
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
            }
        }
    }

    // no implementation means we can completely ignore it
    private void doMethodWithoutImplementation(MethodInfo methodInfo) {
        if (!methodInfo.analysis().haveAnalyzedValueFor(INDEPENDENT_METHOD)) {
            methodInfo.analysis().set(INDEPENDENT_METHOD, INDEPENDENT);
            DECIDE.debug("AMA: Decide independent method without implementations {}", methodInfo);
            propertyChanges.incrementAndGet();
        }
        if (!methodInfo.analysis().haveAnalyzedValueFor(IMMUTABLE_METHOD)) {
            methodInfo.analysis().set(IMMUTABLE_METHOD, ValueImpl.ImmutableImpl.IMMUTABLE);
            DECIDE.debug("AMA: Decide immutable method without implementations {}", methodInfo);
            propertyChanges.incrementAndGet();
        }
        for (ParameterInfo pi : methodInfo.parameters()) {
            if (!pi.analysis().haveAnalyzedValueFor(INDEPENDENT_PARAMETER)) {
                pi.analysis().set(INDEPENDENT_PARAMETER, INDEPENDENT);
                DECIDE.debug("AMA: Decide independent parameter of method without implementations {}", pi);
                propertyChanges.incrementAndGet();
            }
            if (!pi.analysis().haveAnalyzedValueFor(UNMODIFIED_PARAMETER)) {
                pi.analysis().set(UNMODIFIED_PARAMETER, TRUE);
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
            if (pi.analysis().setAllowControlledOverwrite(DOWNCAST_PARAMETER, value)) {
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
        if (pi.analysis().setAllowControlledOverwrite(INDEPENDENT_PARAMETER, fromImplementations)) {
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
        if (methodInfo.analysis().setAllowControlledOverwrite(NON_MODIFYING_METHOD, fromImplementations)) {
            DECIDE.debug("AM: Decide non-modifying of method {} = {}", methodInfo, fromImplementations);
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
        if (methodInfo.analysis().setAllowControlledOverwrite(INDEPENDENT_METHOD, fromImplementations)) {
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
        if (pi.analysis().setAllowControlledOverwrite(UNMODIFIED_PARAMETER, fromImplementations)) {
            DECIDE.debug("Decide unmodified of param {} = {}", pi, fromImplementations);
        }
    }
}

