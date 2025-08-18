package org.e2immu.analyzer.modification.linkedvariables.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.analyzer.modification.linkedvariables.IteratingAnalyzer;
import org.e2immu.analyzer.modification.linkedvariables.TypeImmutableAnalyzer;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.*;

/*
Phase 4.2 Primary type immutable

 */
public class TypeImmutableAnalyzerImpl extends CommonAnalyzerImpl implements TypeImmutableAnalyzer {
    private final AnalysisHelper analysisHelper = new AnalysisHelper();

    public TypeImmutableAnalyzerImpl(IteratingAnalyzer.Configuration configuration) {
        super(configuration);
    }

    private record OutputImpl(List<AnalyzerException> analyzerExceptions,
                              Set<Info> internalWaitFor,
                              Set<TypeInfo> externalWaitFor) implements Output {
    }

    @Override
    public Output go(TypeInfo typeInfo, boolean activateCycleBreaking) {
        ComputeImmutable ci = new ComputeImmutable();
        List<AnalyzerException> analyzerExceptions = new LinkedList<>();
        try {
            ci.go(typeInfo, activateCycleBreaking);
        } catch (RuntimeException re) {
            if (configuration.storeErrors()) {
                if (!(re instanceof AnalyzerException)) {
                    analyzerExceptions.add(new AnalyzerException(typeInfo, re));
                }
            } else throw re;
        }
        return new OutputImpl(analyzerExceptions, ci.internalWaitFor, ci.externalWaitFor);
    }

    private class ComputeImmutable {
        Set<Info> internalWaitFor = new HashSet<>();
        Set<TypeInfo> externalWaitFor = new HashSet<>();

        void go(TypeInfo typeInfo, boolean activateCycleBreaking) {
            if (typeInfo.analysis().haveAnalyzedValueFor(IMMUTABLE_TYPE)) {
                return;
            }
            Independent independent = typeInfo.analysis().getOrNull(INDEPENDENT_TYPE, ValueImpl.IndependentImpl.class);
            if (independent == null) {
                UNDECIDED.debug("TI: Immutable of type {} undecided, wait for independent", typeInfo);
                return;
            }
            Immutable immutable = computeImmutableType(typeInfo, independent, activateCycleBreaking);
            if (immutable != null) {
                DECIDE.debug("TI: Decide immutable of type {} = {}", typeInfo, immutable);
                typeInfo.analysis().setAllowControlledOverwrite(IMMUTABLE_TYPE, immutable);
            } else {
                UNDECIDED.debug("TI: Immutable of type {} undecided, wait for internal {}, external {}", typeInfo,
                        internalWaitFor, externalWaitFor);
            }
        }

        private Immutable computeImmutableType(TypeInfo typeInfo, Independent independent, boolean activateCycleBreaking) {
            boolean fieldsAssignable = typeInfo.fields().stream().anyMatch(fi -> !fi.isPropertyFinal());
            if (fieldsAssignable) return MUTABLE;
            // sometimes, we have annotated setters on synthetic fields, which do not have the "final" property
            boolean haveSetters = typeInfo.methodStream().anyMatch(fi -> fi.getSetField() != null && fi.getSetField().setter());
            if (haveSetters) return MUTABLE;
            if (independent.isDependent()) return FINAL_FIELDS;

            // hierarchy

            Value.Immutable immFromHierarchy = IMMUTABLE;
            boolean stopExternal = false;

            for (ParameterizedType superType : typeInfo.parentAndInterfacesImplemented()) {
                Value.Immutable immutableSuper = immutableSuper(superType.typeInfo());
                Value.Immutable immutableSuperBroken;
                if (immutableSuper == null) {
                    if (activateCycleBreaking) {
                        if (configuration.cycleBreakingStrategy() == CycleBreakingStrategy.NO_INFORMATION_IS_NON_MODIFYING) {
                            immutableSuperBroken = HiddenContentTypes.hasHc(superType.typeInfo()) ? IMMUTABLE_HC : IMMUTABLE;
                        } else {
                            return FINAL_FIELDS;
                        }
                    } else {
                        externalWaitFor.add(superType.typeInfo());
                        immutableSuperBroken = MUTABLE; // not relevant
                        stopExternal = true;
                    }
                } else {
                    if (immutableSuper.isMutable()) return MUTABLE;
                    immutableSuperBroken = immutableSuper;
                }
                immFromHierarchy = immutableSuperBroken.min(immFromHierarchy);
            }
            if (immFromHierarchy.isFinalFields()) return FINAL_FIELDS;

            if (stopExternal) {
                return null;
            }

            // fields and abstract methods (those annotated by hand)

            Boolean immFromField = loopOverFieldsAndMethods(typeInfo, true);
            if (immFromField == null) return null;
            if (!immFromField) return FINAL_FIELDS;
            return HiddenContentTypes.hasHc(typeInfo) ? IMMUTABLE_HC : IMMUTABLE;
        }

        private Immutable immutableSuper(TypeInfo typeInfo) {
            Immutable immutable = typeInfo.analysis().getOrNull(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
            if (immutable != null || !typeInfo.isAbstract()) return immutable;
            Boolean immFromFieldNonAbstract = loopOverFieldsAndMethods(typeInfo, false);
            if (immFromFieldNonAbstract == null) return null;
            if (!immFromFieldNonAbstract) return FINAL_FIELDS;
            return HiddenContentTypes.hasHc(typeInfo) ? IMMUTABLE_HC : IMMUTABLE;
        }

        private static boolean isNotSelf(FieldInfo fieldInfo) {
            TypeInfo bestType = fieldInfo.type().bestTypeInfo();
            return bestType == null || !bestType.equals(fieldInfo.owner());
        }

        private Boolean loopOverFieldsAndMethods(TypeInfo typeInfo, boolean abstractMethods) {
            // fields should be private, or immutable for the type to be immutable
            // fields should not be @Modified nor assigned to
            // fields should not be @Dependent
            Set<TypeInfo> externalWaitFor = new HashSet<>();
            Set<Info> internalWaitFor = new HashSet<>();

            Boolean isImmutable = true;
            for (FieldInfo fieldInfo : typeInfo.fields()) {
                if (!fieldInfo.access().isPrivate()) {
                    if (isNotSelf(fieldInfo)) {
                        Immutable immutable = analysisHelper.typeImmutableNullIfUndecided(fieldInfo.type());
                        if (immutable == null) {
                            externalWaitFor.add(fieldInfo.type().bestTypeInfo());
                            isImmutable = null;
                        } else if (!immutable.isAtLeastImmutableHC()) {
                            return false;
                        }
                    }
                }
                Value.Bool fieldUnmodified = fieldInfo.analysis().getOrNull(UNMODIFIED_FIELD, ValueImpl.BoolImpl.class);
                if (fieldUnmodified == null) {
                    internalWaitFor.add(fieldInfo);
                    isImmutable = null;
                } else if (fieldUnmodified.isFalse()) {
                    return false;
                }
            }
            for (MethodInfo methodInfo : typeInfo.methods()) {
                if (methodInfo.isAbstract() == abstractMethods) {
                    Value.Bool nonModifying = methodInfo.analysis().getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
                    if (nonModifying == null) {
                        internalWaitFor.add(methodInfo);
                        isImmutable = null;
                    } else if (nonModifying.isFalse()) {
                        return false;
                    }
                }
            }
            if (isImmutable == null) {
                this.externalWaitFor.addAll(externalWaitFor);
                this.internalWaitFor.addAll(internalWaitFor);
                return null;
            }
            return true;
        }
    }
}
