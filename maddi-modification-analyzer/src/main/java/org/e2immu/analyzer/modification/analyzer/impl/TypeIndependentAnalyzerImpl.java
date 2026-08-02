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

import org.e2immu.analyzer.modification.common.defaults.ContractReader;
import org.e2immu.analyzer.modification.common.util.TolerantWrite;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.TypeImmutableAnalyzer;
import org.e2immu.analyzer.modification.analyzer.TypeIndependentAnalyzer;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.analyzer.CycleBreakingStrategy.NO_INFORMATION_IS_NON_MODIFYING;
import static org.e2immu.language.cst.api.analysis.Value.Independent;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;

/*
Phase 4.1 Primary type independent

 */
public class TypeIndependentAnalyzerImpl extends CommonAnalyzerImpl implements TypeIndependentAnalyzer {
    private final ContractReader contractReader;
    // as in TypeEventualAnalyzerImpl: the support classes are consulted from every consumer, and a compiled type
    // is shallow-analyzed lazily, so the contract fallback is hit constantly. Concurrent: types may run in parallel.
    private final Map<TypeInfo, Value.EventuallyImmutable> eventuallyImmutableCache = new ConcurrentHashMap<>();

    private final EventualCluster eventualCluster;

    // log-only diagnostic gate, shared name with TypeEventualAnalyzerImpl / TypeImmutableAnalyzerImpl
    private static final String EC_TYPE_DEBUG = System.getenv("EC_TYPE_DEBUG");

    private static boolean ecTypeDebug(TypeInfo typeInfo) {
        if (EC_TYPE_DEBUG == null) return false;
        for (String part : EC_TYPE_DEBUG.split(",")) {
            if (!part.isBlank() && typeInfo.fullyQualifiedName().contains(part)) return true;
        }
        return false;
    }

    public TypeIndependentAnalyzerImpl(Runtime runtime, IteratingAnalyzer.Configuration configuration,
                                       AtomicInteger propertyChanges, List<Message> analyzerMessages,
                                       EventualCluster eventualCluster) {
        super(configuration, propertyChanges, analyzerMessages);
        this.contractReader = new ContractReader(runtime);
        this.eventualCluster = eventualCluster;
    }

    @Override
    public void go(TypeInfo typeInfo, boolean activateCycleBreaking) {

        Independent typeIndependent = typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT);
        if (typeIndependent.isIndependent()) return; // nothing to be gained
        Independent independent = computeIndependentType(typeInfo, activateCycleBreaking,
                TypeImmutableAnalyzer.AfterMark.NONE);
        if (independent != null) {
            if (TolerantWrite.setAllowControlledOverwrite(typeInfo.analysis(), INDEPENDENT_TYPE, independent, typeInfo)) {
                DECIDE.debug("Ti: Decide independent of type {} = {}", typeInfo, independent);
                propertyChanges.incrementAndGet();
            }
        } else if (activateCycleBreaking) {
            boolean write = TolerantWrite.setAllowControlledOverwrite(typeInfo.analysis(), INDEPENDENT_TYPE, INDEPENDENT, typeInfo);
            assert write;
            propertyChanges.incrementAndGet();
            DECIDE.info("Ti: Decide independent of type {} = INDEPENDENT by {}", typeInfo, CYCLE_BREAKING);
        } else {
            UNDECIDED.debug("Ti: Independent of type {} undecided", typeInfo);
        }
    }

    @Override
    public Independent independentAfterMark(TypeInfo typeInfo, TypeImmutableAnalyzer.AfterMark afterMark,
                                            boolean activateCycleBreaking) {
        return computeIndependentType(typeInfo, activateCycleBreaking, afterMark);
    }

    private Independent computeIndependentType(TypeInfo typeInfo, boolean activateCycleBreaking,
                                               TypeImmutableAnalyzer.AfterMark afterMark) {
        Independent indyFromHierarchy = INDEPENDENT;

        // hierarchy

        boolean stopExternal = false;
        for (ParameterizedType superType : typeInfo.parentAndInterfacesImplemented()) {
            TypeInfo superTypeInfo = superType.typeInfo();
            Independent independentSuper = independentSuper(typeInfo, superTypeInfo, afterMark);
            Independent independentSuperBroken;
            if (independentSuper == null) {
                if (activateCycleBreaking) {
                    if (configuration.cycleBreakingStrategy() == NO_INFORMATION_IS_NON_MODIFYING) {
                        independentSuperBroken = INDEPENDENT;
                    } else {
                        return DEPENDENT;
                    }
                } else {
                    independentSuperBroken = INDEPENDENT; // not relevant
                }
                stopExternal = true;
            } else {
                independentSuperBroken = independentSuper;
            }
            indyFromHierarchy = independentSuperBroken.min(indyFromHierarchy);
            if (indyFromHierarchy.isDependent()) {
                if (!afterMark.isNone() && ecTypeDebug(typeInfo)) {
                    System.out.println("ECTYPE " + typeInfo.fullyQualifiedName()
                                       + " DEPENDENT: super " + superTypeInfo.fullyQualifiedName());
                }
                return DEPENDENT;
            }
        }
        if (stopExternal) {
            return null;
        }
        assert indyFromHierarchy.isAtLeastIndependentHc();

        Independent fromFieldsAndAbstractMethods = loopOverFieldsAndAbstractMethods(typeInfo, afterMark);
        if (fromFieldsAndAbstractMethods == null && !afterMark.isNone()) {
            // Undecided, and in after-mark mode that must not be read as INDEPENDENT the way min(null) does for
            // the unconditional verdict. The unconditional value is revised as inputs settle (TolerantWrite lets
            // it improve), but the eventual verdict is written once and never revisited, so a promotion made on a
            // not-yet-copied abstract INDEPENDENT_METHOD would stick. Wait for the next iteration instead.
            return null;
        }
        return indyFromHierarchy.min(fromFieldsAndAbstractMethods);
    }

    private Independent independentSuper(TypeInfo member, TypeInfo superTypeInfo,
                                         TypeImmutableAnalyzer.AfterMark afterMark) {
        if (!afterMark.isNone()) {
            // mirrors immutableSuper: after OUR mark the supertype has been marked too -- the transition belongs
            // to the object, not to one type -- so it is independent to the degree its after-mark immutability
            // implies. Never worse than what was computed unconditionally, hence the max.
            Value.EventuallyImmutable ev = superTypeInfo.analysis()
                    .getOrDefault(EVENTUALLY_IMMUTABLE_TYPE, ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
            if (ev.isEventual()) {
                Independent fromMark = ev.immutableAfterMark().toCorrespondingIndependent();
                Independent plain = superTypeInfo.analysis().getOrNull(INDEPENDENT_TYPE,
                        ValueImpl.IndependentImpl.class);
                return plain == null ? fromMark : fromMark.max(plain);
            }
            // EVENTUALCLUSTER: the supertype's own verdict is still circular (SumImpl waiting on
            // BinaryOperatorImpl, which forms-and-retracts on the wider ledger) -- the immutableSuper seed,
            // independence side: contribute independent-hc optimistically, witnessed for the contraction.
            // Without this the sub-impls' after-mark independence fell to the super's honest unconditional
            // @Dependent through the hierarchy min, and the dependence cap froze them at MUTABLE.
            if (eventualCluster.treatAsEventuallyImmutable(member, superTypeInfo, ev)) {
                Independent plain = superTypeInfo.analysis().getOrNull(INDEPENDENT_TYPE,
                        ValueImpl.IndependentImpl.class);
                Independent optimistic = ValueImpl.IndependentImpl.INDEPENDENT_HC;
                return plain == null ? optimistic : optimistic.max(plain);
            }
        }
        Independent ofType = superTypeInfo.analysis().getOrNull(INDEPENDENT_TYPE, ValueImpl.IndependentImpl.class);
        if (ofType != null || !superTypeInfo.isAbstract()) return ofType;
        Independent ofMethods = INDEPENDENT;
        for (MethodInfo methodInfo : superTypeInfo.constructorsAndMethods()) {
            if (!methodInfo.isAbstract()) {
                Independent ofMethod = methodInfo.analysis().getOrNull(INDEPENDENT_METHOD,
                        ValueImpl.IndependentImpl.class);
                if (ofMethod == null) return null;
                if (ofMethod.isDependent()) return DEPENDENT;
                ofMethods = ofMethods.min(ofMethod);
            }
        }
        return ofMethods;
    }

    private Independent loopOverFieldsAndAbstractMethods(TypeInfo typeInfo,
                                                         TypeImmutableAnalyzer.AfterMark afterMark) {
        boolean afterMarkMode = !afterMark.isNone();
        Independent independent = INDEPENDENT;
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            // AfterMark.fields() originally held only fields whose own TYPE is eventually immutable -- what
            // such a field exposes has itself become immutable at the mark. Since the container ride-along,
            // it also holds RAW container fields (a final List of committable content): committed content,
            // but a wrapper frozen by no mark -- if that wrapper ESCAPES through a dependent accessor, the
            // caller mutates our state post-mark. So the skip re-checks the original premise; a ride-along
            // container falls through to the dependent-exposure checks below (TestEventualPropagation.test7,
            // surfaced the day the cluster ran default-on).
            if (afterMark.fields().contains(fieldInfo)) {
                TypeInfo fieldType = fieldInfo.type().bestTypeInfo();
                boolean typeCommits = fieldType != null
                        && (eventuallyImmutable(fieldType).isEventual()
                            || immutableOf(fieldType).isAtLeastImmutableHC()
                            || eventualCluster.treatAsEventuallyImmutable(typeInfo, fieldType,
                                eventuallyImmutable(fieldType)));
                // a ride-along container is skippable when its wrapper is PROVABLY an immutable copy
                // (every write a copyOf/of-family call -- the cst-impl constructor discipline): no escape
                // can mutate such a wrapper, contract or no contract. The verification arm of
                // docs/eventual-design-improvements.md §4, syntactic and cheap.
                if (typeCommits || fieldWrapperProvablyImmutable(fieldInfo)) continue;
            }
            // an @IgnoreModifications field is manual hidden content (road §050): what is reachable through
            // it is disclaimed, so its independence verdict does not bear on the type's -- the twin of the
            // ungated skip in TypeImmutableAnalyzerImpl.loopOverFieldsAndMethods, and a no-op wherever no
            // field carries the annotation (the StatementImpl.propertyValueMap store held the entire
            // statement family at FinalFields-after-mark through this loop)
            if (fieldInfo.isIgnoreModifications()) continue;
            Independent fieldIndependent = fieldInfo.analysis().getOrNull(INDEPENDENT_FIELD,
                    ValueImpl.IndependentImpl.class);
            if (fieldIndependent == null) {
                independent = null;
            } else if (fieldIndependent.isDependent()) {
                if (!excused(typeInfo, afterMarkMode, false, fieldInfo.type())) {
                    if (afterMarkMode && ecTypeDebug(typeInfo)) {
                        System.out.println("ECTYPE " + typeInfo.fullyQualifiedName()
                                           + " DEPENDENT: field " + fieldInfo.name());
                    }
                    return DEPENDENT;
                }
            } else if (independent != null) {
                independent = independent.min(fieldIndependent);
            }
        }
        for (MethodInfo methodInfo : typeInfo.methods()) {
            if (methodInfo.isAbstract()) {
                boolean beforeMarkOnly = afterMark.methods().contains(methodInfo);
                Independent methodIndependent = methodInfo.analysis().getOrNull(INDEPENDENT_METHOD,
                        ValueImpl.IndependentImpl.class);
                if (methodIndependent == null) {
                    independent = null;
                } else if (methodIndependent.isDependent()) {
                    if (!ignoreModificationsAccessor(methodInfo)
                        && !contractedIndependentHc(methodInfo)
                        && !excused(typeInfo, afterMarkMode, beforeMarkOnly, methodInfo.returnType())) {
                        if (afterMarkMode && ecTypeDebug(typeInfo)) {
                            System.out.println("ECTYPE " + typeInfo.fullyQualifiedName()
                                               + " DEPENDENT: method " + methodInfo.name()
                                               + " returns " + methodInfo.returnType());
                        }
                        return DEPENDENT;
                    }
                } else if (independent != null) {
                    independent = independent.min(methodIndependent);
                }
                for (ParameterInfo pi : methodInfo.parameters()) {
                    Independent paramIndependent = pi.analysis().getOrNull(INDEPENDENT_PARAMETER,
                            ValueImpl.IndependentImpl.class);
                    if (paramIndependent == null) {
                        independent = null;
                    } else if (paramIndependent.isDependent()) {
                        if (!excused(typeInfo, afterMarkMode, beforeMarkOnly, pi.parameterizedType())) {
                            if (afterMarkMode && ecTypeDebug(typeInfo)) {
                                System.out.println("ECTYPE " + typeInfo.fullyQualifiedName()
                                                   + " DEPENDENT: parameter " + pi.fullyQualifiedName());
                            }
                            return DEPENDENT;
                        }
                    } else if (independent != null) {
                        independent = independent.min(paramIndependent);
                    }
                }
            }
        }
        return independent;
    }

    /**
     * Whether one dependent exposure may be discounted after the mark. BOTH conditions must hold, and the second
     * is the whole point of the exercise:
     * <ol>
     * <li>the method can only run before the mark ({@code @Mark} or {@code @Only(before=)}, i.e. it is in
     * {@code AfterMark.methods()}), so it cannot be called to leak anything once the mark has been passed;</li>
     * <li>the type it exposes is <em>itself</em> eventually immutable.</li>
     * </ol>
     * The second condition is not belt-and-braces. A reference handed out <em>before</em> the mark survives it --
     * the caller keeps it -- so "cannot be called afterwards" alone would be unsound: the content would have
     * escaped while it was still mutable, and stay mutable. It is sound only when the escaped object is itself
     * frozen by a mark of its own, which is exactly the {@code TypeInfo.builder() -> TypeInspection.Builder}
     * shape: committing the builder makes further mutation throw. A method failing this keeps the type dependent,
     * after the mark as much as before.
     * <p>
     * EVENTUALCLUSTER, the wider after-mark form: under the cluster's joint transition, condition 2 is the
     * load-bearing one for ANY exposure, not just a before-mark-only method's. A dependent accessor callable
     * after the mark ({@code ConstantExpression.rewire()} exposing {@code Expression}) shares accessible
     * content that is committed once the exposed type's own marks have passed — a pre-mark leak survives the
     * mark as a reference to a now-frozen object, exactly the argument of clause 2. The exposed type may
     * itself still be circular, so the check accepts a cluster candidate through the witnessed seed, as
     * {@code immutableSuper} does; the contraction retracts if the candidate never proves. This was the last
     * cap of the constant-expression ring: their after-mark independence stayed {@code @Dependent}, which
     * the dependence cap turned into FINAL_FIELDS-after-mark, which {@code isMutable(@FinalFields)} then
     * spread to every sub-interface. Off the gate, only the original two-condition rule runs.
     */
    private boolean excused(TypeInfo member, boolean afterMarkMode, boolean beforeMarkOnly,
                            ParameterizedType exposed) {
        // a PURE type-parameter exposure (ConstantExpression.constant() returning T) is hidden content by
        // definition -- exactly what an immutable-hc verdict permits to be shared -- whatever the
        // over-conservative dependent verdict upstream says
        if (afterMarkMode && EventualCluster.ENABLED && exposed.arrays() == 0 && exposed.typeParameter() != null) {
            return true;
        }
        TypeInfo bestType = exposed.bestTypeInfo();
        if (bestType == null) return false;
        Value.EventuallyImmutable ev = eventuallyImmutable(bestType);
        if (beforeMarkOnly && ev.isEventual()) return true; // the original, ungated rule
        if (afterMarkMode && EventualCluster.ENABLED) {
            if (ev.isEventual()) return true;
            return eventualCluster.treatAsEventuallyImmutable(member, bestType, ev);
            // NB a "container of committable content" clause stood here briefly (2026-08-01) and was
            // removed the same day: it promoted a type leaking a raw mutable ArrayList (the wrapper
            // itself is frozen by no mark) -- TestEventualPropagation.test7 caught it the moment the
            // cluster ran default-on. The Set.copyOf-backed exposures it was written for are the
            // TRUSTED-LEAF case instead: see contractedIndependentHc.
        }
        return false;
    }

    /** {@code IMMUTABLE_TYPE} with the {@link ContractReader} fallback for a jar type whose contract was
     *  never materialised into {@code analysis()} -- as in {@code TypeEventualAnalyzerImpl.immutableOf}. */
    private final Map<TypeInfo, Value.Immutable> immutableCache = new ConcurrentHashMap<>();

    private Value.Immutable immutableOf(TypeInfo typeInfo) {
        Value.Immutable fromAnalysis = typeInfo.analysis().getOrNull(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
        if (fromAnalysis != null) return fromAnalysis;
        return immutableCache.computeIfAbsent(typeInfo, ti ->
                contractReader.contracts(ti).get(IMMUTABLE_TYPE) instanceof Value.Immutable i
                        ? i : ValueImpl.ImmutableImpl.MUTABLE);
    }

    /**
     * A getter handing out an {@code @IgnoreModifications} store ({@code FieldInspection.analysisOfInitializer()}
     * returning the {@code PropertyValueMap} overlay): the value is manual hidden content (road §050), so
     * exposing it is hidden-content sharing, not dependence — the independence twin of the eventual walk's
     * {@code isIgnoreModificationsAccessor} and the field loops' skips. An abstract accessor carries no getset
     * mark of its own, so the IMPLEMENTATIONS are consulted, as in {@code EventualCluster.hasSetters}.
     * Annotation-driven: a no-op wherever no field carries the annotation.
     */
    private boolean ignoreModificationsAccessor(MethodInfo methodInfo) {
        if (isIgnoreModAccessor(methodInfo)) return true;
        for (MethodInfo im : methodInfo.analysis()
                .getOrDefault(IMPLEMENTATIONS, ValueImpl.SetOfMethodInfoImpl.EMPTY).methodInfoSet()) {
            if (isIgnoreModAccessor(im)) return true;
        }
        return false;
    }

    /**
     * A hand-written {@code @Independent(hc=true)} on the accessor — the TRUSTED-LEAF compromise
     * (docs/eventual-design-improvements.md §4): the runtime immutability of a {@code Set.copyOf}-backed
     * exposure ({@code FieldInspection.fieldModifiers()}) is not computable from the declared type, so the
     * contract states it, and contracts win — read through the {@link ContractReader}, as everywhere.
     */
    private final Map<MethodInfo, Independent> contractedIndependentCache = new ConcurrentHashMap<>();

    private boolean contractedIndependentHc(MethodInfo methodInfo) {
        return contractedIndependentCache.computeIfAbsent(methodInfo, mi ->
                        contractReader.contracts(mi).get(INDEPENDENT_METHOD) instanceof Independent i ? i : DEPENDENT)
                .isAtLeastIndependentHc();
    }

    // the verification arm: every write to the field (initializer + constructor assignments) is an
    // immutable-copy expression, so the wrapper the accessors hand out cannot be mutated by any caller
    private final Map<FieldInfo, Boolean> wrapperImmutableCache = new ConcurrentHashMap<>();

    private boolean fieldWrapperProvablyImmutable(FieldInfo fieldInfo) {
        return wrapperImmutableCache.computeIfAbsent(fieldInfo, f -> {
            java.util.List<org.e2immu.language.cst.api.expression.Expression> writes = new java.util.ArrayList<>();
            org.e2immu.language.cst.api.expression.Expression init = f.initializer();
            if (init != null && !init.isEmpty()) writes.add(init);
            for (MethodInfo ctor : f.owner().constructors()) {
                if (ctor.methodBody().isEmpty()) continue;
                ctor.methodBody().visit(e -> {
                    if (e instanceof org.e2immu.language.cst.api.expression.Assignment a
                        && a.variableTarget() instanceof org.e2immu.language.cst.api.variable.FieldReference fr
                        && fr.scopeIsThis() && f.equals(fr.fieldInfo())) {
                        writes.add(a.value());
                    }
                    return true;
                });
            }
            return !writes.isEmpty() && writes.stream()
                    .allMatch(TypeIndependentAnalyzerImpl::immutableCopyExpression);
        });
    }

    private static boolean immutableCopyExpression(org.e2immu.language.cst.api.expression.Expression expr) {
        if (expr instanceof org.e2immu.language.cst.api.expression.NullConstant) {
            return true; // a null wrapper cannot be mutated; the null-tolerant copyOf ternary shape
        }
        if (expr instanceof org.e2immu.language.cst.api.expression.Cast c) {
            return immutableCopyExpression(c.expression());
        }
        if (expr instanceof org.e2immu.language.cst.api.expression.EnclosedExpression ee) {
            return immutableCopyExpression(ee.inner());
        }
        if (expr instanceof org.e2immu.language.cst.api.expression.InlineConditional ic) {
            return immutableCopyExpression(ic.ifTrue()) && immutableCopyExpression(ic.ifFalse());
        }
        if (expr instanceof org.e2immu.language.cst.api.expression.MethodCall mc) {
            MethodInfo mi = mc.methodInfo();
            String name = mi.name();
            if (("copyOf".equals(name) || "of".equals(name)) && mi.isStatic()) {
                String owner = mi.typeInfo().fullyQualifiedName();
                return "java.util.List".equals(owner) || "java.util.Set".equals(owner)
                       || "java.util.Map".equals(owner);
            }
            if ("requireNonNull".equals(name) && !mc.parameterExpressions().isEmpty()) {
                return immutableCopyExpression(mc.parameterExpressions().getFirst());
            }
        }
        return false;
    }

    private static boolean isIgnoreModAccessor(MethodInfo methodInfo) {
        Value.FieldValue fieldValue = methodInfo.getSetField();
        return fieldValue != null && fieldValue.field() != null && !fieldValue.setter()
               && fieldValue.field().analysis()
                       .getOrDefault(IGNORE_MODIFICATIONS_FIELD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    /** As {@code TypeEventualAnalyzerImpl.eventuallyImmutable}: analysis first, hand-written contract as fallback. */
    private Value.EventuallyImmutable eventuallyImmutable(TypeInfo typeInfo) {
        Value.EventuallyImmutable fromAnalysis = typeInfo.analysis()
                .getOrDefault(EVENTUALLY_IMMUTABLE_TYPE, ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
        if (fromAnalysis.isEventual()) return fromAnalysis;
        return eventuallyImmutableCache.computeIfAbsent(typeInfo, ti ->
                contractReader.contracts(ti).get(EVENTUALLY_IMMUTABLE_TYPE) instanceof Value.EventuallyImmutable e
                        ? e : ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
    }
}

