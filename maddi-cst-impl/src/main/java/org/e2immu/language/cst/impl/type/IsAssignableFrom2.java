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

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.Wildcard;
import org.e2immu.util.internal.util.ListUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IsAssignableFrom2 {
    private final Predefined runtime;
    private final ParameterizedType target;
    private final ParameterizedType from;
    private final Map<String, Integer> cache;
    // widening on primitives and references (sub-typing)
    private final boolean allowBoxingUnboxingWidening;

    public IsAssignableFrom2(Predefined runtime,
                             ParameterizedType target,
                             ParameterizedType from) {
        this(runtime, target, from, true, new HashMap<>());
    }

    public IsAssignableFrom2(Predefined runtime,
                             ParameterizedType target,
                             ParameterizedType from,
                             boolean allowBoxingUnboxingWidening) {
        this(runtime, target, from, allowBoxingUnboxingWidening, new HashMap<>());
    }

    private IsAssignableFrom2(Predefined runtime,
                              ParameterizedType target,
                              ParameterizedType from,
                              boolean allowBoxingUnboxingWidening,
                              Map<String, Integer> cache) {
        this.runtime = Objects.requireNonNull(runtime);
        this.target = Objects.requireNonNull(target);
        this.from = Objects.requireNonNull(from);
        this.allowBoxingUnboxingWidening = allowBoxingUnboxingWidening;
        this.cache = cache;
    }

    public boolean execute() {
        return execute(false, false, Mode.COVARIANT);
    }

    public enum Mode {
        INVARIANT, // everything has to be identical, there is no leeway with respect to hierarchy
        COVARIANT, // allow assignment of sub-types: Number <-- Integer; List<Integer> <-- IntegerList
        CONTRAVARIANT, // allow for super-types:  Integer <-- Number; IntegerList <-- List<Integer>
        ANY, // accept everything
        COVARIANT_ERASURE, // covariant, but ignore all type parameters
    }

    private static final int IN_RECURSION = -1;

    /**
     * @param ignoreArrays               do the comparison, ignoring array information
     * @param strictTypeParameterTargets set to true when deciding whether a cast is required in an assignment, or not.
     *                                   typical value is false, when used for method selection.
     * @param mode                       the comparison mode
     * @return a numeric "nearness", the lower, the better and the more specific
     */
    public Boolean execute(boolean ignoreArrays, boolean strictTypeParameterTargets, Mode mode) {
        String visitedString = from + "|" + target + "|" + mode + "|" + ignoreArrays + "|" + strictTypeParameterTargets;
        Integer cachedValue = cache.get(visitedString);
        if (cachedValue != null) return cachedValue == IN_RECURSION ? null : cachedValue == 1;
        cache.put(visitedString, IN_RECURSION);
        boolean accept = internalExecute(ignoreArrays, strictTypeParameterTargets, mode);
        cache.put(visitedString, accept ? 1 : 0);
        return accept;
    }

    private boolean internalExecute(boolean ignoreArrays, boolean strictTypeParameterTargets, Mode mode) {
        if (target == from || target.equals(from) || ignoreArrays && target.equalsIgnoreArrays(from)) return true;
        if (target.equalsFQN(from)) return true;

        // NULL
        if (from.isTypeOfNullConstant()) {
            return !target.isPrimitiveExcludingVoid();
        }

        // Assignment to Object: everything can be assigned to object via boxing/unboxing!
        if (ignoreArrays) {
            if (target.typeInfo() != null && target.typeInfo().isJavaLangObject()) {
                return allowBoxingUnboxingWidening;
            }
        } else if (target.isJavaLangObject()) {
            return allowBoxingUnboxingWidening || from.isJavaLangObject();
        }

        // TWO TYPES, POTENTIALLY WITH PARAMETERS, but not TYPE PARAMETERS
        // List<T> vs LinkedList; int vs double, but not T vs LinkedList
        if (target.typeInfo() != null && from.typeInfo() != null) {

            // arrays?
            if (!ignoreArrays) {
                if (target.arrays() != from.arrays()) {
                    if (target.arrays() < from.arrays() && target.typeInfo().isJavaLangObject()) {
                        return true;
                    }
                    // all arrays are serializable if there base object is serializable
                    // See MethodCall_50: always serializable, even if the base type does not extend Serializable
                    return target.arrays() == 0 && target.typeInfo().isJavaIoSerializable() && from.arrays() > 0;
                }
                if (target.arrays() > 0) {
                    // recurse without the arrays; target and from remain the same
                    // this changes the cache key
                    return execute(true, strictTypeParameterTargets, Mode.COVARIANT);
                }
            }

            // PRIMITIVES
            if (from.typeInfo().isPrimitiveExcludingVoid()) {
                if (target.typeInfo().isPrimitiveExcludingVoid()) {
                    // use a dedicated method in Primitives
                    int i = runtime.isAssignableFromToForPrimitives(from, target,
                            mode == Mode.COVARIANT || mode == Mode.COVARIANT_ERASURE);
                    return i == 0 || allowBoxingUnboxingWidening && i > 0;
                }
                return allowBoxingUnboxingWidening && checkBoxing(target, from);
            }
            if (target.typeInfo().isPrimitiveExcludingVoid()) {
                // the other one is not a primitive
                return checkUnboxing(target, from);
            }

            // two different types, so they must be in a hierarchy
            if (target.typeInfo() != from.typeInfo()) {
                return allowBoxingUnboxingWidening && differentNonNullTypeInfo(mode, strictTypeParameterTargets);
            }
            // identical base type, so look at type parameters
            return sameNoNullTypeInfo(mode, strictTypeParameterTargets);
        }

        if (target.typeInfo() != null && from.typeParameter() != null) {
            List<ParameterizedType> otherTypeBounds = from.typeParameter().typeBounds();
            if (otherTypeBounds.isEmpty()) {
                if (mode == Mode.COVARIANT_ERASURE) {
                    if (from.arrays() > target.arrays() && !target.typeInfo().isJavaLangObject()) {
                        // See MethodCall13,4: double <- T[]
                        return false;
                    }
                    // See MethodCall13,4: char[] <- T[]
                    return from.arrays() <= 0 || from.arrays() != target.arrays()
                           || !target.typeInfo().isPrimitiveExcludingVoid();// see e.g. Lambda_7, MethodCall_30,_31,_59
                }
                return target.typeInfo().isJavaLangObject();
            }
            return otherTypeBounds.stream()
                    .map(bound -> new IsAssignableFrom2(runtime, target, bound, allowBoxingUnboxingWidening, cache)
                            .execute(true, strictTypeParameterTargets, mode))
                    .allMatch(i -> i == null || i);
            // note: i == null means: IN_RECURSION
        }

        // I am a type parameter
        if (target.typeParameter() != null) {
            return targetIsATypeParameter(mode, strictTypeParameterTargets);
        }
        // if wildcard is unbound, I am <?>; anything goes
        return target.wildcard() == null || target.wildcard().isUnbound();
    }

    private boolean targetIsATypeParameter(Mode mode, boolean strictTypeParameterTargets) {
        assert target.typeParameter() != null;

        if (target.typeParameter().equals(from.typeParameter()) && target.arrays() != from.arrays()) {
            // T <- T[], T[] <- T, ...
            return false;
        }
        if (target.arrays() > 0 && from.arrays() < target.arrays()) {
            return !strictTypeParameterTargets && !from.isPrimitiveExcludingVoid();
        }

        List<ParameterizedType> targetTypeBounds = target.typeParameter().typeBounds();
        if (targetTypeBounds.isEmpty()) {
            int arrayDiff = from.arrays() - target.arrays();
            assert arrayDiff >= 0;
            if (strictTypeParameterTargets) {
                return false; // only when they are exactly the same, which was tested earlier
            }
            return allowBoxingUnboxingWidening;
        }
        if (target.arrays() > 0 && from.arrays() != target.arrays()) {
            return false;
        }
        // other is a type
        if (from.typeInfo() != null) {
            for (ParameterizedType typeBound : targetTypeBounds) {
                Boolean accept = new IsAssignableFrom2(runtime, typeBound, from, allowBoxingUnboxingWidening, cache)
                        .execute(true, strictTypeParameterTargets, mode);
                if (accept == null || accept) {
                    return true; // in recursion
                }
            }
            return false;
        }
        // other is a type parameter
        if (from.typeParameter() != null) {
            List<ParameterizedType> fromTypeBounds = from.typeParameter().typeBounds();
            if (fromTypeBounds.isEmpty()) {
                return !strictTypeParameterTargets && allowBoxingUnboxingWidening;
            }
            if (mode == Mode.INVARIANT && (isSelfReference(from) || isSelfReference(target))) {
                // see TestAssignableFromGenerics2
                return true;
            }
            // we both have type bounds; we go for the best combination
            for (ParameterizedType myBound : targetTypeBounds) {
                for (ParameterizedType otherBound : fromTypeBounds) {
                    Boolean accept = new IsAssignableFrom2(runtime, myBound, otherBound, allowBoxingUnboxingWidening, cache)
                            .execute(true, strictTypeParameterTargets, mode);
                    if (accept == null || accept) return true;
                }
            }
        }
        return false;
    }

    private boolean isSelfReference(ParameterizedType pt) {
        TypeParameter tp = pt.typeParameter();
        if (tp == null) return false;
        if (tp.getOwner().isRight()) return false;
        TypeInfo ti = tp.getOwner().getLeft();
        int i = tp.getIndex();
        return ti.typeParameters().size() > i && ti.typeParameters().get(i).equals(tp);
    }

    private boolean sameNoNullTypeInfo(Mode mode, boolean strictTypeParameterTargets) {
        if (mode == Mode.COVARIANT_ERASURE) return true;

        // List<E> <-- List<String>
        if (target.parameters().isEmpty()) {
            // ? extends Type <-- Type ; Type <- ? super Type; ...
            return compatibleWildcards(mode, target.wildcard(), from.wildcard());
        }
        return ListUtil.joinLists(target.parameters(), from.parameters())
                .allMatch(p ->
                        compatibleTypeParameter(mode, strictTypeParameterTargets, p));
    }

    private boolean compatibleTypeParameter(Mode mode,
                                            boolean strictTypeParameterTargets,
                                            ListUtil.Pair<ParameterizedType, ParameterizedType> p) {
        Mode newMode = mode == Mode.INVARIANT ? Mode.INVARIANT : Mode.COVARIANT;
        Boolean accept = new IsAssignableFrom2(runtime, p.k(), p.v(), allowBoxingUnboxingWidening, cache)
                .execute(true, strictTypeParameterTargets, newMode);
        return accept == null || accept;
    }

    private boolean differentNonNullTypeInfo(Mode mode, boolean strictTypeParameterTargets) {
        boolean accept = switch (mode) {
            case COVARIANT, COVARIANT_ERASURE -> hierarchy(strictTypeParameterTargets, target, from, mode);
            case CONTRAVARIANT -> hierarchy(strictTypeParameterTargets, from, target, Mode.COVARIANT);
            case INVARIANT -> false;
            case ANY -> throw new UnsupportedOperationException("?");
        };
        if (!accept && from.isFunctionalInterface() && target.isFunctionalInterface()) {
            // two functional interfaces, yet different TypeInfo objects
            return functionalInterface(mode);
        }
        return accept;
    }

    /*
    either COVARIANT_ERASURE, which means we simply have to test the number of parameters and isVoid,
    or INVARIANT... all type parameters identical
     */
    private boolean functionalInterface(Mode mode) {
        TypeInfo targetTi = target.typeInfo();
        MethodInfo targetMi = targetTi.singleAbstractMethod();
        TypeInfo fromTi = from.typeInfo();
        MethodInfo fromMi = fromTi.singleAbstractMethod();

        /*
         See call to 'method' in MethodCall_32 for this "if" statement. Both types I and J are functional interfaces,
         with the same return type and parameters. But they're not seen as assignable.
         */
        if (!targetMi.name().equals(fromMi.name())
            && !targetMi.isSynthetic()
            && !fromMi.isSynthetic()
            && !targetMi.isSAMOfStandardFunctionalInterface()
            && !fromMi.isSAMOfStandardFunctionalInterface()) {
            return false;
        }
        if (targetMi.parameters().size() != fromMi.parameters().size()) return false;

        boolean targetIsVoid = targetMi.returnType().isVoid();
        boolean fromIsVoid = fromMi.returnType().isVoid();

        // target void -> fromIsVoid is unimportant, we can assign a function to a consumer
        if (!targetIsVoid) {
            if (fromIsVoid) return false;
            // see TestMethodCall13,5 for an example where the return type plays a role
            Boolean erasedReturnType = new IsAssignableFrom2(runtime, targetMi.returnType(), fromMi.returnType())
                    .execute(false, false, mode);
            if (erasedReturnType != null && !erasedReturnType) return false;
        }

        if (mode == Mode.COVARIANT_ERASURE) {
            return true;
        }
        // now, ensure that all type parameters have equal values
        int i = 0;
        for (ParameterInfo t : targetMi.parameters()) {
            ParameterInfo f = fromMi.parameters().get(i);
            if (!t.parameterizedType().equals(f.parameterizedType())) return false;
            i++;
        }
        return targetMi.returnType().equals(fromMi.returnType());
    }

    private boolean hierarchy(boolean strictTypeParameterTargets, ParameterizedType target, ParameterizedType from, Mode mode) {
        TypeInfo other = from.typeInfo();
        for (ParameterizedType interfaceImplemented : other.interfacesImplemented()) {
            ParameterizedType concreteType = from.concreteDirectSuperType(interfaceImplemented);
            Boolean scoreInterface = new IsAssignableFrom2(runtime, target, concreteType, allowBoxingUnboxingWidening, cache)
                    .execute(true, strictTypeParameterTargets, mode);
            if (scoreInterface != null && scoreInterface) return true;
        }
        ParameterizedType parentClass = other.parentClass();
        if (parentClass != null && !parentClass.isJavaLangObject()) {
            ParameterizedType concreteType = from.concreteDirectSuperType(parentClass);
            Boolean scoreParent = new IsAssignableFrom2(runtime, target, concreteType, allowBoxingUnboxingWidening, cache)
                    .execute(true, strictTypeParameterTargets, mode);
            return scoreParent != null && scoreParent;
        }
        return false;
    }


    private boolean compatibleWildcards(Mode mode, Wildcard w1, Wildcard w2) {
        if (w1 == w2) return true;
        return mode != Mode.INVARIANT || w1 != null;
    }

    // int <- Integer, long <- Integer, double <- Long
    private boolean checkUnboxing(ParameterizedType primitiveTarget, ParameterizedType from) {
        if (from.isBoxedExcludingVoid()) {
            TypeInfo primitiveFrom = runtime.unboxed(from.typeInfo());
            if (primitiveFrom == primitiveTarget.typeInfo()) {
                return true;
            }
            ParameterizedType primitiveFromPt = primitiveFrom.asSimpleParameterizedType();
            int h = runtime.isAssignableFromToForPrimitives(primitiveFromPt, primitiveTarget, true);
            return h >= 0;
        }
        return false;
    }

    private boolean checkBoxing(ParameterizedType target, ParameterizedType primitiveType) {
        TypeInfo boxed = primitiveType.toBoxed(runtime);
        if (boxed == target.typeInfo()) {
            return true;
        }
        // check the hierarchy of boxed: e.g. Number
        ParameterizedType boxedPt = boxed.asSimpleParameterizedType();
        return hierarchy(false, target, boxedPt, Mode.COVARIANT);
    }
}