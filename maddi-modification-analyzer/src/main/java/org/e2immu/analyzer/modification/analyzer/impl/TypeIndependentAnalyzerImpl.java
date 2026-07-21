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

    public TypeIndependentAnalyzerImpl(Runtime runtime, IteratingAnalyzer.Configuration configuration,
                                       AtomicInteger propertyChanges, List<Message> analyzerMessages) {
        super(configuration, propertyChanges, analyzerMessages);
        this.contractReader = new ContractReader(runtime);
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
            Independent independentSuper = independentSuper(superTypeInfo, afterMark);
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
            if (indyFromHierarchy.isDependent()) return DEPENDENT;
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

    private Independent independentSuper(TypeInfo superTypeInfo, TypeImmutableAnalyzer.AfterMark afterMark) {
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
        Independent independent = INDEPENDENT;
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            // AfterMark.fields() only ever holds fields whose own type is eventually immutable -- that is the
            // condition under which TypeEventualAnalyzerImpl puts them there -- so what such a field exposes has
            // itself become immutable at the mark, and exposing it afterwards is harmless.
            if (afterMark.fields().contains(fieldInfo)) continue;
            Independent fieldIndependent = fieldInfo.analysis().getOrNull(INDEPENDENT_FIELD,
                    ValueImpl.IndependentImpl.class);
            if (fieldIndependent == null) {
                independent = null;
            } else if (fieldIndependent.isDependent()) {
                return DEPENDENT;
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
                    if (!excused(beforeMarkOnly, methodInfo.returnType())) return DEPENDENT;
                } else if (independent != null) {
                    independent = independent.min(methodIndependent);
                }
                for (ParameterInfo pi : methodInfo.parameters()) {
                    Independent paramIndependent = pi.analysis().getOrNull(INDEPENDENT_PARAMETER,
                            ValueImpl.IndependentImpl.class);
                    if (paramIndependent == null) {
                        independent = null;
                    } else if (paramIndependent.isDependent()) {
                        if (!excused(beforeMarkOnly, pi.parameterizedType())) return DEPENDENT;
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
     */
    private boolean excused(boolean beforeMarkOnly, ParameterizedType exposed) {
        if (!beforeMarkOnly) return false;
        TypeInfo bestType = exposed.bestTypeInfo();
        return bestType != null && eventuallyImmutable(bestType).isEventual();
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

