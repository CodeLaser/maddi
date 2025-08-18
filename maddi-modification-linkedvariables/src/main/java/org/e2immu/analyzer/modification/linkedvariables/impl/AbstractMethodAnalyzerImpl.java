package org.e2immu.analyzer.modification.linkedvariables.impl;

import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.analyzer.modification.linkedvariables.AbstractMethodAnalyzer;
import org.e2immu.analyzer.modification.linkedvariables.IteratingAnalyzer;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;

public class AbstractMethodAnalyzerImpl extends CommonAnalyzerImpl implements AbstractMethodAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMethodAnalyzerImpl.class);

    // given an abstract method, which are its concrete implementations?
    final Map<MethodInfo, Set<MethodInfo>> concreteImplementationsOfAbstractMethods = new HashMap<>();
    final List<MethodInfo> abstractMethodsWithoutImplementation;

    public AbstractMethodAnalyzerImpl(IteratingAnalyzer.Configuration configuration, Set<TypeInfo> primaryTypes) {
        super(configuration);
        primaryTypes.stream().flatMap(TypeInfo::recursiveSubTypeStream).forEach(typeInfo ->
                typeInfo.methodStream().filter(mi -> !mi.isAbstract()).forEach(mi ->
                        mi.overrides().stream()
                                // override of the non-abstract method must be in our source code, and must be abstract itself
                                .filter(MethodInfo::isAbstract)
                                .filter(ov -> primaryTypes.contains(ov.typeInfo().primaryType()))
                                .forEach(ov ->
                                        concreteImplementationsOfAbstractMethods
                                                .computeIfAbsent(ov, m -> new HashSet<>()).add(mi))));
        abstractMethodsWithoutImplementation = primaryTypes.stream().flatMap(TypeInfo::recursiveSubTypeStream)
                .flatMap(ti -> ti.methods().stream())
                .filter(mi -> mi.isAbstract() && !concreteImplementationsOfAbstractMethods.containsKey(mi))
                .toList();
    }

    private record OutputImpl(List<AnalyzerException> analyzerExceptions,
                              Set<MethodInfo> waitForMethods) implements Output {
    }

    @Override
    public Output go(boolean firstIteration) {
        if (firstIteration) {
            for (MethodInfo methodInfo : abstractMethodsWithoutImplementation) {
                if (!methodInfo.analysis().haveAnalyzedValueFor(INDEPENDENT_METHOD)) {
                    methodInfo.analysis().set(INDEPENDENT_METHOD, DEPENDENT);
                }
                if (!methodInfo.analysis().haveAnalyzedValueFor(IMMUTABLE_METHOD)) {
                    methodInfo.analysis().set(IMMUTABLE_METHOD, ValueImpl.ImmutableImpl.MUTABLE);
                }
                for (ParameterInfo pi : methodInfo.parameters()) {
                    if (!pi.analysis().haveAnalyzedValueFor(INDEPENDENT_PARAMETER)) {
                        pi.analysis().set(INDEPENDENT_PARAMETER, DEPENDENT);
                    }
                    if (!pi.analysis().haveAnalyzedValueFor(UNMODIFIED_PARAMETER)) {
                        pi.analysis().set(UNMODIFIED_PARAMETER, FALSE);
                    }
                }
            }
        }
        Set<MethodInfo> waitForMethods = new HashSet<>();
        Iterator<Map.Entry<MethodInfo, Set<MethodInfo>>> iterator = concreteImplementationsOfAbstractMethods.entrySet().iterator();
        List<AnalyzerException> analyzerExceptions = new LinkedList<>();
        while (iterator.hasNext()) {
            Map.Entry<MethodInfo, Set<MethodInfo>> entry = iterator.next();
            MethodInfo methodInfo = entry.getKey();
            try {
                if (!methodInfo.analysis().haveAnalyzedValueFor(INDEPENDENT_METHOD)
                    || !methodInfo.analysis().haveAnalyzedValueFor(NON_MODIFYING_METHOD)
                    || methodInfo.parameters().stream().anyMatch(pi ->
                        !pi.analysis().haveAnalyzedValueFor(INDEPENDENT_PARAMETER)
                        || !pi.analysis().haveAnalyzedValueFor(UNMODIFIED_PARAMETER))) {
                    Set<MethodInfo> waitForOfMethod = resolve(methodInfo, entry.getValue());
                    if (waitForOfMethod.isEmpty()) {
                        iterator.remove();
                        LOGGER.debug("Removing {} from waitFor, have left: {}", methodInfo,
                                concreteImplementationsOfAbstractMethods.keySet());
                    } else {
                        LOGGER.debug("Adding {} to waitFor, have left: {}", waitForOfMethod,
                                concreteImplementationsOfAbstractMethods.keySet());
                        waitForMethods.addAll(waitForOfMethod);
                    }
                } else {
                    iterator.remove();
                }
            } catch (RuntimeException re) {
                if (configuration.storeErrors()) {
                    if (!(re instanceof AnalyzerException)) {
                        analyzerExceptions.add(new AnalyzerException(methodInfo, re));
                    }
                } else throw re;
            }
        }
        return new OutputImpl(analyzerExceptions, waitForMethods);
    }

    private Set<MethodInfo> resolve(MethodInfo methodInfo, Set<MethodInfo> concreteImplementations) {
        Set<MethodInfo> waitFor = new HashSet<>();
        for (ParameterInfo pi : methodInfo.parameters()) {
            waitFor.addAll(unmodified(concreteImplementations, pi));
            waitFor.addAll(independent(concreteImplementations, pi));
            waitFor.addAll(collectDowncast(concreteImplementations, pi));
        }
        waitFor.addAll(methodNonModifying(concreteImplementations, methodInfo));
        waitFor.addAll(methodIndependent(concreteImplementations, methodInfo));
        return waitFor;
    }

    private static Set<MethodInfo> collectDowncast(Set<MethodInfo> concreteImplementations, ParameterInfo pi) {
        Value.SetOfTypeInfo downcastsValue = pi.analysis().getOrNull(DOWNCAST_PARAMETER, Value.SetOfTypeInfo.class);
        if (downcastsValue == null) {
            Set<MethodInfo> waitFor = new HashSet<>();
            Set<TypeInfo> downcasts = new HashSet<>();
            for (MethodInfo implementation : concreteImplementations) {
                ParameterInfo pii = implementation.parameters().get(pi.index());
                Value.SetOfTypeInfo downcastsImplValue = pii.analysis().getOrNull(DOWNCAST_PARAMETER,
                        Value.SetOfTypeInfo.class);
                if (downcastsImplValue == null) {
                    waitFor.add(implementation);
                } else {
                    downcasts.addAll(downcastsImplValue.typeInfoSet());
                }
            }
            if (!waitFor.isEmpty()) {
                return waitFor;
            }
            pi.analysis().set(DOWNCAST_PARAMETER, new ValueImpl.SetOfTypeInfoImpl(Set.copyOf(downcasts)));
        }
        return Set.of();
    }

    private static Set<MethodInfo> independent(Set<MethodInfo> concreteImplementations, ParameterInfo pi) {
        Value.Independent independent = pi.analysis().getOrNull(INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.class);
        if (independent == null) {
            Set<MethodInfo> waitFor = new HashSet<>();
            Value.Independent fromImplementations = INDEPENDENT;
            for (MethodInfo implementation : concreteImplementations) {
                ParameterInfo pii = implementation.parameters().get(pi.index());
                Value.Independent independentImpl = pii.analysis().getOrNull(INDEPENDENT_PARAMETER,
                        ValueImpl.IndependentImpl.class);
                if (independentImpl == null) {
                    waitFor.add(implementation);
                    fromImplementations = null;
                } else if (fromImplementations != null) {
                    fromImplementations = fromImplementations.min(independentImpl);
                    if (fromImplementations.isDependent()) break;
                }
            }
            if (fromImplementations != null) {
                pi.analysis().set(INDEPENDENT_PARAMETER, fromImplementations);
                DECIDE.debug("AM: Decide independent of param {} = {}", pi, fromImplementations);
            } else {
                UNDECIDED.debug("AM: Independent of param {} undecided, wait for {}", pi, waitFor);
            }
            return waitFor;
        }
        return Set.of();
    }


    private Set<MethodInfo> methodNonModifying(Set<MethodInfo> concreteImplementations, MethodInfo methodInfo) {
        Value.Bool nonModifying = methodInfo.analysis().getOrNull(NON_MODIFYING_METHOD,
                ValueImpl.BoolImpl.class);
        if (nonModifying == null) {
            Set<MethodInfo> waitFor = new HashSet<>();
            Value.Bool fromImplementations = ValueImpl.BoolImpl.TRUE;
            for (MethodInfo implementation : concreteImplementations) {
                Value.Bool nonModImpl = implementation.analysis().getOrNull(NON_MODIFYING_METHOD,
                        ValueImpl.BoolImpl.class);
                if (nonModImpl == null) {
                    waitFor.add(implementation);
                    fromImplementations = null;
                } else if (nonModImpl.isFalse() && fromImplementations != null) {
                    fromImplementations = FALSE;
                    break;
                }
            }
            if (fromImplementations != null) {
                methodInfo.analysis().set(NON_MODIFYING_METHOD, fromImplementations);
                DECIDE.debug("AM: Decide non-modifying of method {} = {}", methodInfo, fromImplementations);
            } else {
                UNDECIDED.debug("AM: Non-modifying method {} undecided, wait for {}", methodInfo, waitFor);
            }
            return waitFor;
        }
        return Set.of();
    }


    private Set<MethodInfo> methodIndependent(Set<MethodInfo> concreteImplementations, MethodInfo methodInfo) {
        Value.Independent independent = methodInfo.analysis().getOrNull(INDEPENDENT_METHOD,
                ValueImpl.IndependentImpl.class);
        if (independent == null) {
            Set<MethodInfo> waitFor = new HashSet<>();
            Value.Independent fromImplementations = INDEPENDENT;
            for (MethodInfo implementation : concreteImplementations) {
                Value.Independent independentImpl = implementation.analysis().getOrNull(INDEPENDENT_METHOD,
                        ValueImpl.IndependentImpl.class);
                if (independentImpl == null) {
                    waitFor.add(implementation);
                    fromImplementations = null;
                } else if (fromImplementations != null) {
                    fromImplementations = fromImplementations.min(independentImpl);
                    if (fromImplementations.isDependent()) break;
                }
            }
            if (fromImplementations != null) {
                methodInfo.analysis().set(INDEPENDENT_METHOD, fromImplementations);
                DECIDE.debug("AM: Decide independent of method {} = {}", methodInfo, fromImplementations);
            } else {
                UNDECIDED.debug("AM: Independent of method {} undecided, wait for {}", methodInfo, waitFor);
            }
            return waitFor;
        }
        return Set.of();
    }

    private static Set<MethodInfo> unmodified(Set<MethodInfo> concreteImplementations, ParameterInfo pi) {
        Value.Bool unmodified = pi.analysis().getOrNull(UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
        if (unmodified == null) {
            Set<MethodInfo> waitFor = new HashSet<>();
            Value.Bool fromImplementations = ValueImpl.BoolImpl.TRUE;
            for (MethodInfo implementation : concreteImplementations) {
                ParameterInfo pii = implementation.parameters().get(pi.index());
                Value.Bool unmodifiedImpl = pii.analysis().getOrNull(UNMODIFIED_PARAMETER,
                        ValueImpl.BoolImpl.class);
                if (unmodifiedImpl == null) {
                    waitFor.add(implementation);
                    fromImplementations = null;
                } else if (unmodifiedImpl.isFalse() && fromImplementations != null) {
                    fromImplementations = FALSE;
                    break;
                }
            }
            if (fromImplementations != null) {
                pi.analysis().set(UNMODIFIED_PARAMETER, fromImplementations);
                DECIDE.debug("Decide unmodified of param {} = {}", pi, fromImplementations);
            } else {
                UNDECIDED.debug("Non-modifying of param {} undecided, wait for {}", pi, waitFor);
            }
            return waitFor;
        }
        return Set.of();
    }
}
