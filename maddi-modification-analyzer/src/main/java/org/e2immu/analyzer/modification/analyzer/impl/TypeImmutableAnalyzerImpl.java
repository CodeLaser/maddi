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
import org.e2immu.analyzer.modification.common.util.TolerantWrite;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.List;
import java.util.Set;
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
                                     AtomicInteger propertiesChanged, List<Message> analyzerMessages) {
        super(configuration, propertiesChanged, analyzerMessages);
    }

    @Override
    public void go(TypeInfo typeInfo, boolean activateCycleBreaking) {
        Immutable currentImmutable = typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE);
        if (currentImmutable.isImmutable()) {
            return; // nothing to be gained
        }
        Independent independent = typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT);
        Immutable immutable = computeImmutableType(typeInfo, independent, activateCycleBreaking, Set.of());
        if (immutable != null) {
            if (TolerantWrite.setAllowControlledOverwrite(typeInfo.analysis(), IMMUTABLE_TYPE, immutable, typeInfo)) {
                DECIDE.debug("TI: Decide immutable of type {} = {}", typeInfo, immutable);
                propertyChanges.incrementAndGet();
            }
        } else {
            UNDECIDED.debug("TI: Immutable of type {} undecided", typeInfo);
        }
    }

    /**
     * The level this type would reach if the modification of {@code excusedFields} did not count -- i.e. the level
     * it reaches <em>after the mark</em>, when the eventually immutable fields it holds have been committed and can
     * no longer change (road to immutability §060). Returns null when undecided.
     * <p>
     * Deliberately the same computation as the unconditional verdict, with one relaxation, so the two can never
     * drift apart. Field <em>finality</em> is not relaxed: a type whose transition is a plain assignable flag needs
     * the precondition reasoning we are not reviving.
     */
    @Override
    public Immutable immutableIgnoringModificationOf(TypeInfo typeInfo, Set<FieldInfo> excusedFields) {
        Independent independent = typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT);
        return computeImmutableType(typeInfo, independent, false, excusedFields);
    }

    private Immutable computeImmutableType(TypeInfo typeInfo, Independent independent, boolean activateCycleBreaking,
                                           Set<FieldInfo> excusedFields) {
        boolean fieldsAssignable = typeInfo.fields().stream().anyMatch(fi -> !fi.isPropertyFinal());
        if (fieldsAssignable) return MUTABLE;
        // sometimes, we have annotated setters on synthetic fields, which do not have the "final" property
        boolean haveSetters = typeInfo.methodStream()
                .anyMatch(fi -> fi.getSetField() != null && fi.getSetField().setter());
        if (haveSetters) return MUTABLE;

        // hierarchy BEFORE the dependence cap: a mutable supertype makes the type MUTABLE regardless of its own
        // final fields. The 'isDependent -> FINAL_FIELDS' early return that stood here committed a premature
        // optimistic verdict which the hierarchy-aware 2nd type pass then tried (and was refused) to downgrade
        // (timefold bavet Group*CollectorBiNode extending mutable AbstractGroupBiNode).

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
        if (independent.isDependent()) {
            // an undecided supertype may still force MUTABLE: wait rather than conclude FINAL_FIELDS prematurely
            return stopExternal ? null : FINAL_FIELDS;
        }
        if (immFromHierarchy.isFinalFields()) return FINAL_FIELDS;

        if (stopExternal) {
            return null;
        }

        // fields and abstract methods (those annotated by hand)

        Boolean immFromField = loopOverFieldsAndMethods(typeInfo, true, excusedFields);
        if (immFromField == null) {
            // The 51% type-null cluster (elasticsearch first contact): a missing verdict here — a field's
            // UNMODIFIED undecided, a NON-PRIVATE field's type immutability-undecided, or an abstract
            // method's NON_MODIFYING undecided — roots in EXTERNAL unannotated types and CASCADES through
            // field types (nullness is transitively closed over 'has a field of a null type'). Under the
            // breaking pass, a verdict that will never arrive is pessimistic: the type is at most
            // FINAL_FIELDS (field finality was established at the top of this method); upgrades in later
            // verification passes remain allowed, downgrades are never needed. Without breaking: wait.
            return activateCycleBreaking ? FINAL_FIELDS : null;
        }
        if (!immFromField) return FINAL_FIELDS;
        if (independent.isIndependentHc() || typeInfo.isExtensible()) return IMMUTABLE_HC;
        // the hidden-content-free level requires every instance field's type to be deeply immutable itself:
        // a private, never-modified field of a mutable type (fernflower's records of Exprent/Statement) is
        // hidden content, not absence of content
        Boolean noHiddenContent = instanceFieldTypesDeeplyImmutable(typeInfo);
        if (noHiddenContent == null) return activateCycleBreaking ? IMMUTABLE_HC : null;
        return noHiddenContent ? IMMUTABLE : IMMUTABLE_HC;
    }

    private Boolean instanceFieldTypesDeeplyImmutable(TypeInfo typeInfo) {
        boolean undecided = false;
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            if (fieldInfo.isStatic()) continue; // instance content only; static state belongs to the class
            Immutable immutable = analysisHelper.typeImmutableNullIfUndecided(fieldInfo.type());
            if (immutable == null) {
                undecided = true;
            } else if (!immutable.isImmutable()) {
                return false; // includes NO_VALUE: no proof of deep immutability, no hc-free promotion
            }
        }
        return undecided ? null : true;
    }

    private Immutable immutableSuper(TypeInfo typeInfo) {
        Immutable immutable = typeInfo.analysis().getOrNull(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
        if (immutable != null || !typeInfo.isAbstract()) return immutable;
        // The abstract supertype's own immutability has not been decided yet. We must NOT estimate it from only its
        // non-abstract members: that ignores the abstract methods, which can make the abstract type mutable (e.g. a
        // modifying abstract method on an interface). Such an over-optimistic estimate gets committed onto a subtype,
        // and then -- once the abstract type is correctly decided lower -- the subtype's immutability would have to be
        // downgraded, tripping the monotonic-overwrite guard. Instead we wait: a null return leaves the subtype
        // undecided until the abstract supertype's own analysis (which does account for abstract methods) completes.
        return null;
    }

    private static boolean isNotSelf(FieldInfo fieldInfo) {
        TypeInfo bestType = fieldInfo.type().bestTypeInfo();
        return bestType == null || !bestType.equals(fieldInfo.owner());
    }

    private Boolean loopOverFieldsAndMethods(TypeInfo typeInfo, boolean abstractMethods,
                                             Set<FieldInfo> excusedFields) {
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
            if (excusedFields.contains(fieldInfo)) continue; // after the mark, this field can no longer change
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
