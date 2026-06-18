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

    public boolean test(ParameterizedType to, ParameterizedType from) {
        return test(to, from, null);
    }

    private boolean test(ParameterizedType to, ParameterizedType from, Set<ParameterizedType> visited) {
        if (to.equals(from)) return true;

        boolean primitiveWidening = testPrimitiveWidening(to, from);
        if (primitiveWidening) return true;

        if (to.isReferenceType() && from == ParameterizedTypeImpl.NULL_CONSTANT) return true;

        boolean boxUnbox = testBoxUnbox(to, from);
        if (boxUnbox) return true;

        boolean array = testArrayAssignability(to, from, visited);
        if (array) return true;

        boolean hierarchy = testHierarchy(to, from, visited);
        if (hierarchy) return true;

        boolean typeParameter = testTypeParameter(to, from, visited);
        if (typeParameter) return true;

        return typeIntersection(to, from, visited);
    }

    private boolean typeIntersection(ParameterizedType to, ParameterizedType from, Set<ParameterizedType> visited) {
        if (from.wildcard() == WildcardEnum.EXTENDS_INTERSECTION && from.arrays() == 0) {
            return from.parameters().stream().anyMatch(p -> test(to, p, visited));
        }
        if (to.wildcard() == WildcardEnum.EXTENDS_INTERSECTION && to.arrays() == 0) {
            return to.parameters().stream().anyMatch(p -> test(p, from, visited));
        }
        return false;
    }

    private boolean testTypeParameter(ParameterizedType to, ParameterizedType from, Set<ParameterizedType> visited) {
        if (from.arrays() == 0 && from.typeParameter() != null && from.wildcard() != WildcardEnum.EXTENDS_INTERSECTION) {
            ParameterizedType upperBound = from.typeParameter().typeBounds().isEmpty()
                    ? runtime.objectParameterizedType()
                    : from.typeParameter().typeBounds().getFirst();
            return test(to, upperBound, visited);
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

    private boolean testHierarchy(ParameterizedType to, ParameterizedType from, Set<ParameterizedType> visited) {
        if (to.typeInfo() != null && to.arrays() == 0 && from.typeInfo() != null && from.arrays() == 0) {
            if (to.typeInfo().isInterface()) {
                if (!from.typeInfo().interfacesImplemented().isEmpty()) {
                    for (ParameterizedType fromI : from.typeInfo().interfacesImplemented()) {
                        if (fromI.typeInfo() == to.typeInfo()) {
                            return testTypeArgumentCompatibility(to, fromI, visited);
                        }
                    }
                    Set<ParameterizedType> visitedNotNull = visited == null ? new HashSet<>() : visited;
                    for (ParameterizedType fromI : from.typeInfo().interfacesImplemented()) {
                        if (visitedNotNull.add(fromI)) {
                            boolean recursive = test(to, fromI, visitedNotNull);
                            if (recursive) return true;
                        }
                    }
                }
            } else {
                if (to.isJavaLangObject()) return true;
                ParameterizedType current = from;
                while (!current.isJavaLangObject()) {
                    if (current.typeInfo() == to.typeInfo()) {
                        return testTypeArgumentCompatibility(to, current, visited);
                    }
                    current = current.typeInfo().parentClass();
                }
            }
        }
        return false;
    }

    private boolean testTypeArgumentCompatibility(ParameterizedType to,
                                                  ParameterizedType from,
                                                  Set<ParameterizedType> visited) {
        if (to.parameters().isEmpty() || from.parameters().isEmpty()) return true;
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
                                           Set<ParameterizedType> visited) {
        if (from.arrays() > 0) {
            if (to.arrays() == 0 && to.typeInfo() != null && OCS.contains(to.typeInfo().fullyQualifiedName())) {
                return true;
            }
            if (to.arrays() > 0) {
                return test(to.copyWithOneFewerArrays(), from.copyWithOneFewerArrays(), visited);
            }
        }
        return false;
    }

    private boolean testBoxUnbox(ParameterizedType to, ParameterizedType from) {
        TypeInfo t = to.typeInfo();
        TypeInfo f = from.typeInfo();
        if (t != null && f != null && to.arrays() == 0 && from.arrays() == 0) {
            if (t.isPrimitiveExcludingVoid()) return f == runtime.boxed(t);
            if (f.isPrimitiveExcludingVoid()) return t == runtime.boxed(f);
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