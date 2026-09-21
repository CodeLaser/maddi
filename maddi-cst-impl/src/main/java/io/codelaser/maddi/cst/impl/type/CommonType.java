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

package io.codelaser.maddi.cst.impl.type;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Predefined;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.*;

public class CommonType {
    private final Predefined runtime;

    public CommonType(Predefined runtime) {
        this.runtime = runtime;
    }

    /**
     * The join of two types. Entry point; the recursion over type arguments is bounded by the set of pairs
     * already being resolved — see {@link #commonType(ParameterizedType, ParameterizedType, Set)}.
     */
    public ParameterizedType commonType(ParameterizedType pt1, ParameterizedType pt2) {
        return commonType(pt1, pt2, new HashSet<>());
    }

    /**
     * <b>⛔ A recursive generic used to recurse until the stack ran out.</b> The guard below ("common
     * situation when the types implement Comparable") only fires when a type argument is EXACTLY the type
     * being joined, which catches `C implements Comparable&lt;C&gt;` and nothing else: one level of nesting
     * between the two, or a pair that alternates, walks forever. Measured on the detekt corpus, where it
     * surfaced as a {@code StackOverflowError} in three tests the moment more expressions carried a real type
     * (the Kotlin front end's parenthesized-expression conversion).
     * <p>
     * {@code seen} holds the pairs whose join is still being computed. Re-entering one means the join is
     * defined in terms of itself, and {@code Object} is the answer that terminates — the same answer the
     * narrow guard already gave for the case it did catch. ⚠ It changes nothing on any input that terminated
     * before: a pair is only in the set while it is on the stack above you.
     */
    private ParameterizedType commonType(ParameterizedType pt1, ParameterizedType pt2,
                                         Set<List<ParameterizedType>> seen) {
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
                if (runtime.isAssignableFromToForPrimitives(pt1, pt2UnboxedPt, true) >= 0 ||
                    runtime.isAssignableFromToForPrimitives(pt2UnboxedPt, pt1, true) >= 0) {
                    return runtime.boxed(runtime.widestType(pt1, pt2UnboxedPt).typeInfo()).asSimpleParameterizedType();
                }
            }
            if (pt2IsPrimitive && isBoxed) {
                TypeInfo unboxed = runtime.unboxed(bestType);
                ParameterizedType unboxedPt = unboxed.asSimpleParameterizedType();
                if (unboxedPt.equals(pt2)) return pt1;
                if (runtime.isAssignableFromToForPrimitives(pt2, unboxedPt, true) >= 0 ||
                    runtime.isAssignableFromToForPrimitives(unboxedPt, pt2, true) >= 0) {
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
        if (runtime.isAssignableFrom(pt1, pt2)) {
            return pt1;
        }
        if (runtime.isAssignableFrom(pt2, pt1)) {
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
        TypeInfo commonSuperType = common.getFirst();
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
            // A raw type on either side (or any type-argument arity mismatch) means we cannot unify type
            // arguments position by position -- e.g. commonType(List<String>, raw List). Fall back to the raw
            // common supertype rather than indexing past the shorter argument list.
            if (concrete.parameters().size() == concretept2.parameters().size()) {
                List<ParameterizedType> updatedParameters = new ArrayList<>(commonSuperType.typeParameters().size());
                int i = 0;
                for (ParameterizedType parameter : concrete.parameters()) {
                    ParameterizedType pt2Parameter = concretept2.parameters().get(i++);
                    ParameterizedType commonParameter;
                    // Arrays.asList, not List.of: it tolerates a null argument, where List.of throws --
                    // and a guard that can throw where the old code merely recursed is not a guard
                    List<ParameterizedType> pair = Arrays.asList(parameter, pt2Parameter);
                    if (pt1.equals(parameter) && pt2.equals(pt2Parameter) || !seen.add(pair)) {
                        // the join is defined in terms of itself (a recursive generic such as
                        // `C implements Comparable<C>`, or a pair that alternates): Object terminates it
                        commonParameter = runtime.objectParameterizedType();
                    } else {
                        commonParameter = commonType(parameter, pt2Parameter, seen);
                        seen.remove(pair); // strictly "on the stack above you": a sibling may ask again
                    }
                    updatedParameters.add(commonParameter);
                }
                return new ParameterizedTypeImpl(commonSuperType, null, updatedParameters, result.arrays(),
                        null);
            }
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
