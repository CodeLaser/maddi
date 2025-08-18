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
        Map<FieldInfo, Boolean> effectivelyFinalFieldMap = computeEffectivelyFinalFields(typeInfo, callGraph, partOfConstruction);
        for (Map.Entry<FieldInfo, Boolean> entry : effectivelyFinalFieldMap.entrySet()) {
            FieldInfo fieldInfo = entry.getKey();
            if (!fieldInfo.analysis().haveAnalyzedValueFor(PropertyImpl.FINAL_FIELD)) {
                boolean isEffectivelyFinal = entry.getValue();
                fieldInfo.analysis().set(PropertyImpl.FINAL_FIELD, ValueImpl.BoolImpl.from(isEffectivelyFinal));
            }
        }
    }

    private Map<FieldInfo, Boolean> computeEffectivelyFinalFields(TypeInfo typeInfo, G<Info> callGraph, Set<MethodInfo> partOfConstruction) {
        Map<FieldInfo, Boolean> effectivelyFinalFieldMap = new HashMap<>();
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            boolean isFinal = fieldInfo.isPropertyFinal() || fieldInfo.access().isPrivate();
            Boolean prev = effectivelyFinalFieldMap.put(fieldInfo, isFinal);
            assert prev == null;

            // edges from field to method
            V<Info> v = callGraph.vertex(fieldInfo);
            if (v != null) {
                Map<V<Info>, Long> edges = callGraph.edges(v);
                if (edges != null) {
                    for (Map.Entry<V<Info>, Long> entry : edges.entrySet()) {
                        if (entry.getKey().t() instanceof MethodInfo methodInfo
                            && notInConstructionOfSameStaticType(methodInfo, fieldInfo, partOfConstruction)) {
                            // so methodInfo references toField... check whether that is an assignment, or simply a read
                            boolean isAssigned = isAssigned(methodInfo, fieldInfo);
                            if (isAssigned) {
                                effectivelyFinalFieldMap.put(fieldInfo, false);
                            }
                        }
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
        if (lastStatement == null) return false;
        VariableData vd = VariableDataImpl.of(lastStatement);
        assert vd != null;
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
                } else if (!methodInfo.access().isPrivate()) {
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
        return mi.isConstructor()
               || mi.access().isPrivate() && mi.typeInfo().enclosingMethod() == null
               || mi.typeInfo().enclosingMethod() != null && canBePartOfConstruction(mi.typeInfo().enclosingMethod());
    }
}
