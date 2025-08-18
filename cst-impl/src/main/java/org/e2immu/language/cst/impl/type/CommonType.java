package org.e2immu.language.cst.impl.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.*;

public class CommonType {
    private final Predefined runtime;

    public CommonType(Predefined runtime) {
        this.runtime = runtime;
    }

    public ParameterizedType commonType(ParameterizedType pt1, ParameterizedType pt2) {
        assert pt1 != null && pt2 != null;

        if (pt1.equals(pt2)) return pt1;

        TypeInfo bestType = pt1.bestTypeInfo();
        TypeInfo pt2BestType = pt2.bestTypeInfo();
        boolean isPrimitive = pt1.isPrimitiveExcludingVoid();
        boolean pt2IsPrimitive = pt2.isPrimitiveExcludingVoid();
        if (isPrimitive && pt2IsPrimitive) {
            return runtime.widestType(pt1, pt2);
        }
        boolean isBoxed = pt1.isBoxedExcludingVoid();
        boolean pt2IsBoxed = pt2.isBoxedExcludingVoid();

        if ((isPrimitive || isBoxed) && pt2 == ParameterizedTypeImpl.NULL_CONSTANT) {
            if (isBoxed) return pt1;
            return runtime.boxed(bestType).asParameterizedType();
        }
        if ((pt2IsPrimitive || pt2IsBoxed) && pt1 == ParameterizedTypeImpl.NULL_CONSTANT) {
            if (pt2IsBoxed) return pt2;
            return runtime.boxed(pt2BestType).asParameterizedType();
        }
        if (isPrimitive || pt2IsPrimitive) {
            /* one is boxed, the pt2 is not. The result must be boxed (see e.g.
            org.e2immu.analyser.model.value.TestEqualsConstantInline.test17)
             */
            if (isPrimitive && pt2IsBoxed) {
                TypeInfo pt2Unboxed = runtime.unboxed(pt2BestType);
                ParameterizedType pt2UnboxedPt = pt2Unboxed.asSimpleParameterizedType();
                if (pt1.equals(pt2UnboxedPt)) return runtime.boxed(bestType).asParameterizedType();
                if (runtime.isAssignableFromTo(pt1, pt2UnboxedPt, true) >= 0 ||
                    runtime.isAssignableFromTo(pt2UnboxedPt, pt1, true) >= 0) {
                    return runtime.boxed(runtime.widestType(pt1, pt2UnboxedPt).typeInfo()).asSimpleParameterizedType();
                }
            }
            if (pt2IsPrimitive && isBoxed) {
                TypeInfo unboxed = runtime.unboxed(bestType);
                ParameterizedType unboxedPt = unboxed.asSimpleParameterizedType();
                if (unboxedPt.equals(pt2)) return pt1;
                if (runtime.isAssignableFromTo(pt2, unboxedPt, true) >= 0 ||
                    runtime.isAssignableFromTo(unboxedPt, pt2, true) >= 0) {
                    return runtime.boxed(runtime.widestType(pt2, unboxedPt).typeInfo()).asSimpleParameterizedType();
                }
            }
            return runtime.objectParameterizedType(); // no common type
        }
        if (pt2 == ParameterizedTypeImpl.NULL_CONSTANT) return pt1;
        if (pt1 == ParameterizedTypeImpl.NULL_CONSTANT) return pt2;

        if (bestType == null || pt2BestType == null) {
            return runtime.objectParameterizedType(); // no common type
        }
        if (pt1.isAssignableFrom(runtime, pt2)) {
            return pt1;
        }
        if (pt2.isAssignableFrom(runtime, pt1)) {
            return pt2;
        }
        // go into the hierarchy
        Map<TypeInfo, Integer> hierarchy = makeHierarchy(bestType);
        Map<TypeInfo, Integer> pt2Hierarchy = makeHierarchy(pt2BestType);
        List<TypeInfo> common = new ArrayList<>(hierarchy.keySet());
        common.retainAll(pt2Hierarchy.keySet());
        if (common.isEmpty()) {
            return runtime.objectParameterizedType();
        }
        if (common.size() > 1) {
            common.sort(Comparator.comparingInt(hierarchy::get));
        }
        TypeInfo commonSuperType = common.get(0);
        if (commonSuperType.equals(bestType)) {
            return pt1;
        }
        if (commonSuperType.equals(pt2BestType)) {
            return pt2;
        }
        ParameterizedType result = commonSuperType.asParameterizedType();
        if (!commonSuperType.typeParameters().isEmpty()) {
            ParameterizedType concrete = pt1.concreteSuperType(result);
            ParameterizedType concretept2 = pt2.concreteSuperType(result);
            List<ParameterizedType> updatedParameters = new ArrayList<>(commonSuperType.typeParameters().size());
            int i = 0;
            for (ParameterizedType parameter : concrete.parameters()) {
                ParameterizedType pt2Parameter = concretept2.parameters().get(i++);
                ParameterizedType commonParameter;
                if (pt1.equals(parameter) && pt2.equals(pt2Parameter)) {
                    // common situation when the types implement Comparable; must avoid infinite recursion
                    commonParameter = runtime.objectParameterizedType();
                } else {
                    commonParameter = commonType(parameter, pt2Parameter);
                }
                updatedParameters.add(commonParameter);
            }
            return new ParameterizedTypeImpl(commonSuperType, null, updatedParameters, result.arrays(),
                    null);
        }
        return result;
    }

    public Map<TypeInfo, Integer> makeHierarchy(TypeInfo typeInfo) {
        Map<TypeInfo, Integer> map = new HashMap<>();
        makeHierarchy(map, typeInfo, 0);
        return Map.copyOf(map);
    }

    private void makeHierarchy(Map<TypeInfo, Integer> map, TypeInfo start, int distance) {
        map.merge(start, distance, Integer::min);
        if (start.parentClass() != null && !start.parentClass().isJavaLangObject()) {
            TypeInfo parent = start.parentClass().typeInfo();
            if (!map.containsKey(parent)) {
                makeHierarchy(map, parent, distance + 1);
            }
        }
        for (ParameterizedType interfaceImplemented : start.interfacesImplemented()) {
            if (!map.containsKey(interfaceImplemented.typeInfo())) {
                makeHierarchy(map, interfaceImplemented.typeInfo(), distance + 100);
            }
        }
    }
}
