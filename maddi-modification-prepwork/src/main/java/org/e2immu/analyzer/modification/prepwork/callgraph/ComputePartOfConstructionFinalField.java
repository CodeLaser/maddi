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

package org.e2immu.analyzer.modification.prepwork.callgraph;

import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfoContainer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.V;
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ComputePartOfConstructionFinalField {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputePartOfConstructionFinalField.class);
    private static final TimedLogger TIMED_LOGGER = new TimedLogger(LOGGER, 1000);

    public static final Value.SetOfInfo EMPTY_PART_OF_CONSTRUCTION = new ValueImpl.SetOfInfoImpl(Set.of());
    public static final Property PART_OF_CONSTRUCTION = new PropertyImpl("partOfConstructionType", EMPTY_PART_OF_CONSTRUCTION);

    private final boolean parallel;
    private final AtomicInteger count = new AtomicInteger();

    public ComputePartOfConstructionFinalField(boolean parallel) {
        this.parallel = parallel;
    }

    /*
    We must have all constructors and methods, also those in anonymous types. Extracting them from the call graph
    is cheaper than visiting
     */
    public void go(G<Info> cg) {
        if (parallel) {
            cg.vertices().parallelStream()
                    .filter(v -> v.t() instanceof MethodInfo)
                    .map(v -> (MethodInfo) v.t())
                    .collect(Collectors.groupingByConcurrent(MethodInfo::primaryType, Collectors.toList()))
                    .forEach((primaryType, methodsAndConstructors)
                            -> internalGo(primaryType, methodsAndConstructors, cg));
        } else {
            cg.vertices().stream()
                    .filter(v -> v.t() instanceof MethodInfo)
                    .map(v -> (MethodInfo) v.t())
                    .collect(Collectors.groupingBy(MethodInfo::primaryType, Collectors.toList()))
                    .forEach((primaryType, methodsAndConstructors)
                            -> internalGo(primaryType, methodsAndConstructors, cg));
        }
    }

    private void internalGo(TypeInfo typeInfo, List<MethodInfo> constructorsAndMethodsOfPrimaryType, G<Info> callGraph) {
        TIMED_LOGGER.info("Done {}", count);
        count.incrementAndGet();

        typeInfo.subTypes().forEach(st -> internalGo(st, constructorsAndMethodsOfPrimaryType, callGraph));

        Value.SetOfInfo setOfInfo = typeInfo.analysis().getOrNull(PART_OF_CONSTRUCTION, ValueImpl.SetOfInfoImpl.class);
        if (setOfInfo != null) {
            return; // we're going to assume that all FINAL_FIELDS are set as well
        }

        Set<MethodInfo> partOfConstruction = computePartOfConstruction(typeInfo, constructorsAndMethodsOfPrimaryType, callGraph);
        typeInfo.analysis().set(PART_OF_CONSTRUCTION, new ValueImpl.SetOfInfoImpl(partOfConstruction));
        Map<FieldInfo, Boolean> effectivelyFinalFieldMap = computeEffectivelyFinalFields(typeInfo, callGraph,
                partOfConstruction, constructorsAndMethodsOfPrimaryType);
        for (Map.Entry<FieldInfo, Boolean> entry : effectivelyFinalFieldMap.entrySet()) {
            FieldInfo fieldInfo = entry.getKey();
            if (!fieldInfo.analysis().haveAnalyzedValueFor(PropertyImpl.FINAL_FIELD)) {
                boolean isEffectivelyFinal = entry.getValue();
                fieldInfo.analysis().set(PropertyImpl.FINAL_FIELD, ValueImpl.BoolImpl.from(isEffectivelyFinal));
            }
        }
    }

    private Map<FieldInfo, Boolean> computeEffectivelyFinalFields(TypeInfo typeInfo, G<Info> callGraph,
                                                                  Set<MethodInfo> partOfConstruction,
                                                                  List<MethodInfo> constructorsAndMethodsOfPrimaryType) {
        Map<FieldInfo, Boolean> effectivelyFinalFieldMap = new HashMap<>();
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            assert fieldInfo.access() != null : "Field " + fieldInfo + " has null access.";
            boolean isFinal = fieldInfo.isPropertyFinal() || fieldInfo.access().isPrivate();
            Boolean prev = effectivelyFinalFieldMap.put(fieldInfo, isFinal);
            assert prev == null;

            // edges from field to method: these exist for methods declared directly in the field's owner
            // (see ComputeCallGraph.handleFieldAccess)
            V<Info> v = callGraph.vertex(fieldInfo);
            if (v != null) {
                Map<V<Info>, Long> edges = callGraph.edges(v);
                if (edges != null) {
                    for (Map.Entry<V<Info>, Long> entry : edges.entrySet()) {
                        if (entry.getKey().t() instanceof MethodInfo methodInfo
                            && notInConstructionOfSameStaticType(methodInfo, fieldInfo, partOfConstruction)) {
                            // so methodInfo references toField... check whether that is an assignment, or simply a read
                            if (isAssigned(methodInfo, fieldInfo)) {
                                effectivelyFinalFieldMap.put(fieldInfo, false);
                            }
                        }
                    }
                }
            }

            // a field can also be assigned from a method declared in a lambda / anonymous / local type enclosed
            // by the field's owner; such a method has no field->method edge (its typeInfo is not the owner), so
            // we check it directly. isAssigned returns false when the method does not assign the field.
            if (effectivelyFinalFieldMap.get(fieldInfo)) {
                for (MethodInfo methodInfo : constructorsAndMethodsOfPrimaryType) {
                    TypeInfo methodType = methodInfo.typeInfo();
                    if (methodType != fieldInfo.owner()
                        && methodType.isEnclosedIn(fieldInfo.owner())
                        && notInConstructionOfSameStaticType(methodInfo, fieldInfo, partOfConstruction)
                        && isAssigned(methodInfo, fieldInfo)) {
                        effectivelyFinalFieldMap.put(fieldInfo, false);
                        break;
                    }
                }
            }
        }
        return effectivelyFinalFieldMap;
    }

    /*
    only in the part of construction of the same static type, we can ignore assignments.
     */
    private boolean notInConstructionOfSameStaticType(MethodInfo methodInfo, FieldInfo toField, Set<MethodInfo> partOfConstruction) {
        if (!partOfConstruction.contains(methodInfo)) return true;
        TypeInfo firstStaticOfMethod = firstStatic(methodInfo.typeInfo());
        TypeInfo firstStaticOfField = firstStatic(toField.owner());
        return firstStaticOfField != firstStaticOfMethod;
    }

    private static TypeInfo firstStatic(TypeInfo typeInfo) {
        if (typeInfo.isPrimaryType() || typeInfo.isStatic()) return typeInfo;
        if (typeInfo.enclosingMethod() != null) return firstStatic(typeInfo.enclosingMethod().typeInfo());
        return firstStatic(typeInfo.compilationUnitOrEnclosingType().getRight());
    }

    private boolean isAssigned(MethodInfo methodInfo, FieldInfo fieldInfo) {
        Statement lastStatement = methodInfo.methodBody().lastStatement();
        if (lastStatement == null || lastStatement.isSynthetic()) return false;
        VariableData vd = VariableDataImpl.of(lastStatement);
        if (vd == null) return false; // body not analyzed (e.g. doNotRecurseIntoAnonymous): nothing to inspect
        return vd.variableInfoContainerStream()
                .filter(vic -> vic.variable() instanceof FieldReference fr && fr.fieldInfo() == fieldInfo)
                .map(VariableInfoContainer::best)
                .anyMatch(vi -> !vi.assignments().isEmpty());
    }

    private Set<MethodInfo> computePartOfConstruction(TypeInfo typeInfo,
                                                      List<MethodInfo> constructorsAndMethodsOfPrimaryType,
                                                      G<Info> callGraph) {
        Set<MethodInfo> calledFromConstruction = new HashSet<>();
        Set<MethodInfo> calledFromOutside = new HashSet<>();

        boolean changes = true;
        while (changes) {
            changes = false;
            for (MethodInfo methodInfo : constructorsAndMethodsOfPrimaryType) {
                if (methodInfo.isConstructor()) {
                    changes |= calledFromConstruction.add(methodInfo);
                } else if (methodInfo.access() == null || !methodInfo.access().isPrivate()) {
                    // a null access is only possible for a method left partially built by a dropped (fault-
                    // isolated) compilation unit; treat it conservatively as non-private (called from outside)
                    changes |= calledFromOutside.add(methodInfo);
                }
                boolean isCalledFromConstruction = calledFromConstruction.contains(methodInfo);
                boolean isCalledFromOutside = calledFromOutside.contains(methodInfo);
                V<Info> v = callGraph.vertex(methodInfo);
                Map<V<Info>, Long> edges = callGraph.edges(v);
                if (edges != null) {
                    for (Map.Entry<V<Info>, Long> entry : edges.entrySet()) {
                        if (entry.getKey().t() instanceof MethodInfo toMethod) {
                            if (isCalledFromConstruction) {
                                changes |= calledFromConstruction.add(toMethod);
                            }
                            if (isCalledFromOutside && !toMethod.isConstructor()) {
                                changes |= calledFromOutside.add(toMethod);
                            }
                        }
                    }
                }
            }
        }
        Set<MethodInfo> candidates = typeInfo.constructorAndMethodStream()
                .filter(this::canBePartOfConstruction).collect(Collectors.toCollection(HashSet::new));
        candidates.removeAll(calledFromOutside);
        candidates.retainAll(calledFromConstruction);
        return Set.copyOf(candidates);
    }

    private boolean canBePartOfConstruction(MethodInfo mi) {
        // a null access (only from a method left partially built by a dropped compilation unit) is treated as
        // non-private, i.e. not part of construction
        return mi.isConstructor()
               || mi.access() != null && mi.access().isPrivate() && mi.typeInfo().enclosingMethod() == null
               || mi.typeInfo().enclosingMethod() != null && canBePartOfConstruction(mi.typeInfo().enclosingMethod());
    }
}
