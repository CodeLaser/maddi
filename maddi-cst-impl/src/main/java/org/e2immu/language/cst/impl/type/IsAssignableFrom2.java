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

package org.e2immu.language.cst.impl.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.HashSet;
import java.util.Set;


public record IsAssignableFrom2(Predefined runtime) {

    // a (to, from) pair currently being tested, used to break recursion on self-referential generics
    private record Pair(ParameterizedType to, ParameterizedType from) {
    }

    public boolean test(ParameterizedType to, ParameterizedType from) {
        return test(to, from, null);
    }

    private boolean test(ParameterizedType to, ParameterizedType from, Set<Pair> visited) {
        if (to.equals(from)) return true;

        boolean primitiveWidening = testPrimitiveWidening(to, from);
        if (primitiveWidening) return true;

        // the null constant is assignable to any reference type, including array types
        if ((to.isReferenceType() || to.arrays() > 0) && from == ParameterizedTypeImpl.NULL_CONSTANT) return true;

        // guard against infinite recursion on self-referential (f-bounded) type parameters, e.g.
        // <T extends Parent<T>>; a pair that is already on the current recursion stack is assumed
        // compatible (cf. v1's IN_RECURSION -> EQUALS). The pair is removed again once its subtree is
        // done, so independent sibling subproblems are still evaluated on their own merits.
        Set<Pair> visitedNotNull = visited == null ? new HashSet<>() : visited;
        Pair key = new Pair(to, from);
        if (!visitedNotNull.add(key)) return true;
        try {
            if (testBoxUnbox(to, from, visitedNotNull)) return true;
            if (testArrayAssignability(to, from, visitedNotNull)) return true;
            if (testHierarchy(to, from, visitedNotNull)) return true;
            if (testTypeParameter(to, from, visitedNotNull)) return true;
            return typeIntersection(to, from, visitedNotNull);
        } finally {
            visitedNotNull.remove(key);
        }
    }

    private boolean typeIntersection(ParameterizedType to, ParameterizedType from, Set<Pair> visited) {
        if (from.wildcard() == WildcardEnum.EXTENDS_INTERSECTION && from.arrays() == 0) {
            return from.parameters().stream().anyMatch(p -> test(to, p, visited));
        }
        if (to.wildcard() == WildcardEnum.EXTENDS_INTERSECTION && to.arrays() == 0) {
            // 'from' is assignable to an intersection A & B only if it is assignable to every component
            return to.parameters().stream().allMatch(p -> test(p, from, visited));
        }
        return false;
    }

    private boolean testTypeParameter(ParameterizedType to, ParameterizedType from, Set<Pair> visited) {
        if (from.arrays() == 0 && from.typeParameter() != null && from.wildcard() != WildcardEnum.EXTENDS_INTERSECTION) {
            var bounds = from.typeParameter().typeBounds();
            if (bounds.isEmpty()) {
                return test(to, runtime.objectParameterizedType(), visited);
            }
            // the type parameter is assignable to 'to' if any of its (upper) bounds is
            return bounds.stream().anyMatch(b -> test(to, b, visited));
        }
        if (to.arrays() == 0 && to.typeParameter() != null && to.wildcard() != WildcardEnum.EXTENDS_INTERSECTION) {
            if (from.arrays() == 0 && from.wildcard() == WildcardEnum.SUPER) {
                ParameterizedType lowerBound;
                if (from.typeParameter() != null) {
                    lowerBound = from.typeParameter().typeBounds().isEmpty() ? runtime.objectParameterizedType()
                            : from.typeParameter().typeBounds().getFirst();
                } else {
                    lowerBound = from.withWildcard(null); // the actual type, but without the wildcard
                }
                return test(lowerBound, to, visited);
            }
        }
        return false;
    }

    private boolean testHierarchy(ParameterizedType to, ParameterizedType from, Set<Pair> visited) {
        if (to.typeInfo() != null && to.arrays() == 0 && from.typeInfo() != null && from.arrays() == 0) {
            if (to.isJavaLangObject()) return true;
            // find the supertype of 'from' that has the same erasure as 'to', with its type arguments
            // substituted (e.g. ArrayList<String> -> Iterable<String> when to == Iterable<?>).
            // concreteSuperType returns 'from' itself when the erasures already coincide, and null when
            // 'to' is not a supertype of 'from'.
            ParameterizedType concreteSuper = from.concreteSuperType(to);
            if (concreteSuper != null) {
                return testTypeArgumentCompatibility(to, concreteSuper, visited);
            }
        }
        return false;
    }

    private boolean testTypeArgumentCompatibility(ParameterizedType to,
                                                  ParameterizedType from,
                                                  Set<Pair> visited) {
        if (to.parameters().isEmpty() || from.parameters().isEmpty()) return true;
        assert to.parameters().size() == from.parameters().size()
                : """
                We want to know when this occurs, and whether
                it is legitimate rather than obscuring errors elsewhere
                """;
        int i = 0;
        for (ParameterizedType toParameter : to.parameters()) {
            ParameterizedType fromParameter = from.parameters().get(i++);
            if (toParameter.isUnboundWildcard()) continue; // always good
            if (toParameter.wildcard() == WildcardEnum.EXTENDS) {
                if (fromParameter.wildcard() == WildcardEnum.EXTENDS) {
                    if (!test(toParameter, fromParameter, visited)) return false;
                } else if (fromParameter.wildcard() == WildcardEnum.SUPER) {
                    return false;
                } else { // concrete type
                    if (!test(toParameter, fromParameter, visited)) return false;
                }
            } else if (toParameter.wildcard() == WildcardEnum.SUPER) {
                if (fromParameter.wildcard() == WildcardEnum.SUPER) {
                    if (!test(fromParameter, toParameter, visited)) return false;
                } else if (fromParameter.wildcard() == WildcardEnum.EXTENDS) {
                    return false;
                } else { // concrete type
                    if (!test(fromParameter, toParameter, visited)) return false;
                }
            } else if (toParameter.typeParameter() != null) {
                // the target carries its declaration's own formal type parameter, e.g. Parent<T> as
                // produced by TypeInfo.asParameterizedType(). Such a position behaves like the raw type:
                // accept when the actual argument satisfies the type parameter's (upper) bounds.
                var bounds = toParameter.typeParameter().typeBounds();
                if (!bounds.isEmpty() && bounds.stream().noneMatch(b -> test(b, fromParameter, visited))) {
                    return false;
                }
            } else {
                if (!toParameter.equals(fromParameter)) return false;
                // invariant, must match exactly: List<Object> <- List<String> is not possible
            }
        }
        return true;
    }

    private static final Set<String> OCS = Set.of("java.lang.Cloneable", "java.io.Serializable", "java.lang.Object");

    private boolean testArrayAssignability(ParameterizedType to,
                                           ParameterizedType from,
                                           Set<Pair> visited) {
        if (from.arrays() > 0) {
            if (to.arrays() == 0 && to.typeInfo() != null && OCS.contains(to.typeInfo().fullyQualifiedName())) {
                return true;
            }
            if (to.arrays() > 0) {
                ParameterizedType toComponent = to.copyWithOneFewerArrays();
                ParameterizedType fromComponent = from.copyWithOneFewerArrays();
                // arrays are covariant for reference component types only; primitive component types
                // are invariant (int[] is not assignable to long[]), so require identity there.
                if (toComponent.arrays() == 0 && fromComponent.arrays() == 0
                    && (toComponent.isPrimitiveExcludingVoid() || fromComponent.isPrimitiveExcludingVoid())) {
                    return toComponent.equals(fromComponent);
                }
                return test(toComponent, fromComponent, visited);
            }
        }
        return false;
    }

    private boolean testBoxUnbox(ParameterizedType to, ParameterizedType from, Set<Pair> visited) {
        TypeInfo t = to.typeInfo();
        TypeInfo f = from.typeInfo();
        if (t == null || f == null || to.arrays() != 0 || from.arrays() != 0) return false;
        // boxing conversion, optionally followed by a widening reference conversion (e.g. Number <- int)
        if (f.isPrimitiveExcludingVoid() && !t.isPrimitiveExcludingVoid()) {
            return test(to, runtime.boxed(f).asSimpleParameterizedType(), visited);
        }
        // unboxing conversion, optionally followed by a widening primitive conversion (e.g. long <- Integer)
        if (t.isPrimitiveExcludingVoid() && f.isBoxedExcludingVoid()) {
            return test(to, runtime.unboxed(f).asSimpleParameterizedType(), visited);
        }
        return false;
    }

    private boolean testPrimitiveWidening(ParameterizedType to, ParameterizedType from) {
        if (to.isPrimitiveExcludingVoid() && from.isPrimitiveExcludingVoid()) {
            return runtime.isPrimitiveWidening(to.typeInfo(), from.typeInfo());
        }
        return false;
    }

}