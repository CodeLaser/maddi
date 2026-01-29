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

import org.e2immu.analyzer.modification.analyzer.CycleBreakingStrategy;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.TypeImmutableAnalyzer;
import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;

/*
Phase 4.2 Primary type immutable

 */
public class TypeImmutableAnalyzerImpl extends CommonAnalyzerImpl implements TypeImmutableAnalyzer {
    private final AnalysisHelper analysisHelper = new AnalysisHelper();

    public TypeImmutableAnalyzerImpl(IteratingAnalyzer.Configuration configuration,
                                     AtomicInteger propertiesChanged) {
        super(configuration, propertiesChanged);
    }

    @Override
    public void go(TypeInfo typeInfo, boolean activateCycleBreaking) {
        Immutable currentImmutable = typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE);
        if (currentImmutable.isImmutable()) {
            return; // nothing to be gained
        }
        Independent independent = typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT);
        Immutable immutable = computeImmutableType(typeInfo, independent, activateCycleBreaking);
        if (immutable != null) {
            if (typeInfo.analysis().setAllowControlledOverwrite(IMMUTABLE_TYPE, immutable)) {
                DECIDE.debug("TI: Decide immutable of type {} = {}", typeInfo, immutable);
                propertyChanges.incrementAndGet();
            }
        } else {
            UNDECIDED.debug("TI: Immutable of type {} undecided", typeInfo);
        }
    }

    private Immutable computeImmutableType(TypeInfo typeInfo, Independent independent, boolean activateCycleBreaking) {
        boolean fieldsAssignable = typeInfo.fields().stream().anyMatch(fi -> !fi.isPropertyFinal());
        if (fieldsAssignable) return MUTABLE;
        // sometimes, we have annotated setters on synthetic fields, which do not have the "final" property
        boolean haveSetters = typeInfo.methodStream()
                .anyMatch(fi -> fi.getSetField() != null && fi.getSetField().setter());
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
                        immutableSuperBroken = IMMUTABLE_HC; // cannot be IMMUTABLE, because we're deriving from it
                    } else {
                        return FINAL_FIELDS;
                    }
                } else {
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
        if (independent.isIndependentHc()) return IMMUTABLE_HC;
        return typeInfo.isExtensible() ? IMMUTABLE_HC : IMMUTABLE;
    }

    private Immutable immutableSuper(TypeInfo typeInfo) {
        Immutable immutable = typeInfo.analysis().getOrNull(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
        if (immutable != null || !typeInfo.isAbstract()) return immutable;
        Boolean immFromFieldNonAbstract = loopOverFieldsAndMethods(typeInfo, false);
        if (immFromFieldNonAbstract == null) return null;
        if (!immFromFieldNonAbstract) return FINAL_FIELDS;
        return IMMUTABLE_HC; // abstract type is extensible
    }

    private static boolean isNotSelf(FieldInfo fieldInfo) {
        TypeInfo bestType = fieldInfo.type().bestTypeInfo();
        return bestType == null || !bestType.equals(fieldInfo.owner());
    }

    private Boolean loopOverFieldsAndMethods(TypeInfo typeInfo, boolean abstractMethods) {
        // fields should be private, or immutable for the type to be immutable
        // fields should not be @Modified nor assigned to
        // fields should not be @Dependent

        Boolean isImmutable = true;
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            if (!fieldInfo.access().isPrivate()) {
                if (isNotSelf(fieldInfo)) {
                    Immutable immutable = analysisHelper.typeImmutableNullIfUndecided(fieldInfo.type());
                    if (immutable == null) {
                        isImmutable = null;
                    } else if (!immutable.isAtLeastImmutableHC()) {
                        return false;
                    }
                }
            }
            Bool fieldUnmodified = fieldInfo.analysis().getOrNull(UNMODIFIED_FIELD, ValueImpl.BoolImpl.class);
            if (fieldUnmodified == null) {
                isImmutable = null;
            } else if (fieldUnmodified.isFalse()) {
                return false;
            }
        }
        for (MethodInfo methodInfo : typeInfo.methods()) {
            if (methodInfo.isAbstract() == abstractMethods) {
                Bool nonModifying = methodInfo.analysis().getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
                if (nonModifying == null) {
                    isImmutable = null;
                } else if (nonModifying.isFalse()) {
                    return false;
                }
            }
        }
        if (isImmutable == null) {
            return null;
        }
        return true;
    }
}
