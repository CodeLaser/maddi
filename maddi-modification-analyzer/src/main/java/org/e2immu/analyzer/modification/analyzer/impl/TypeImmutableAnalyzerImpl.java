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

import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.TypeImmutableAnalyzer;
import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.common.AnalyzerException;
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

            Immutable immFromHierarchy = IMMUTABLE;
            boolean stopExternal = false;

            for (ParameterizedType superType : typeInfo.parentAndInterfacesImplemented()) {
                Immutable immutableSuper = immutableSuper(superType.typeInfo());
                Immutable immutableSuperBroken;
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
                Bool fieldUnmodified = fieldInfo.analysis().getOrNull(UNMODIFIED_FIELD, ValueImpl.BoolImpl.class);
                if (fieldUnmodified == null) {
                    internalWaitFor.add(fieldInfo);
                    isImmutable = null;
                } else if (fieldUnmodified.isFalse()) {
                    return false;
                }
            }
            for (MethodInfo methodInfo : typeInfo.methods()) {
                if (methodInfo.isAbstract() == abstractMethods) {
                    Bool nonModifying = methodInfo.analysis().getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
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
