package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.info.ComputeMethodOverrides;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class ComputeMethodOverridesImpl implements ComputeMethodOverrides {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeMethodOverridesImpl.class);

    @Override
    public MethodInfo computeFunctionalInterface(TypeInfo typeInfo) {
        List<MethodInfo> abstractMethods = computeIsFunctionalInterface(typeInfo, new HashSet<>(), new HashMap<>());
        return abstractMethods.size() == 1 ? abstractMethods.getFirst() : null;
    }

    /*
   See Overload_3 for the necessity of the translationMap. Overload_4 and MethodCall_67 are good tests for this
   method.
  */
    private List<MethodInfo> computeIsFunctionalInterface(TypeInfo typeInfo,
                                                          Set<MethodInfo> overriddenByDefault,
                                                          Map<NamedType, ParameterizedType> translationMap) {
        try {
            if (!typeInfo.isInterface()) {
                return List.of(); // inspection is still going on, which means recursion... ignore!
            }
            List<MethodInfo> abstractMethods = new ArrayList<>();

            for (MethodInfo methodInfo : typeInfo.methods()) {
                // NOTE 20240710: I've removed the restriction of "!access().isPrivate()"
                boolean nonStaticNonDefault = !methodInfo.isStatic() && !methodInfo.isDefault()
                                              && !methodInfo.isOverloadOfJLOMethod();
                if (nonStaticNonDefault) {
                    if (overriddenByDefault.stream().noneMatch(override ->
                            isOverrideOf(methodInfo, override, translationMap))) {
                        abstractMethods.add(methodInfo);
                        overriddenByDefault.add(methodInfo);
                    } // is cancelled out, we have a default implementation for this method
                } else if (methodInfo.isDefault()) {
                    overriddenByDefault.add(methodInfo);
                }
            }
            // overridden needs to cancel out all of them, individually!
            for (ParameterizedType superInterface : typeInfo.interfacesImplemented()) {
                Map<NamedType, ParameterizedType> map = mapOfSuperType(superInterface);
                Map<NamedType, ParameterizedType> superMap = new HashMap<>(translationMap);
                superMap.putAll(map);
                assert !typeInfo.equals(superInterface.typeInfo()) : "Can never be the same type";
                abstractMethods.addAll(computeIsFunctionalInterface(superInterface.typeInfo(), overriddenByDefault,
                        superMap));
            }
            return abstractMethods;
        } catch (RuntimeException re) {
            LOGGER.error("Caught exception computing isFunctionalInterface, type {}", typeInfo);
            throw re;
        }
    }

    private boolean isOverrideOf(MethodInfo inSubType, MethodInfo inSuperType,
                                 Map<NamedType, ParameterizedType> map) {
        if (!inSubType.name().equals(inSuperType.name())) return false;
        return sameParameters(inSubType.parameters(), inSuperType.parameters(), map);
    }

    @Override
    public Set<MethodInfo> overrides(MethodInfo methodInfo) {
        return Set.copyOf(recursiveOverridesCall(methodInfo.typeInfo(), methodInfo, Map.of()));
    }

    private Set<MethodInfo> recursiveOverridesCall(TypeInfo typeToSearch,
                                                   MethodInfo targetMethod,
                                                   Map<NamedType, ParameterizedType> translationMap) {
        Set<MethodInfo> result = new HashSet<>();
        for (ParameterizedType superType : directSuperTypes(typeToSearch)) {
            if (superType == null) {
                throw new UnsupportedOperationException("Supertype of " + typeToSearch + " is null");
            }
            Map<NamedType, ParameterizedType> translationMapOfSuperType = mapOfSuperType(superType);
            translationMapOfSuperType.putAll(translationMap);
            assert superType.typeInfo() != null;
            MethodInfo override = findUniqueMethod(superType.typeInfo(), targetMethod, translationMapOfSuperType);
            if (override != null) {
                result.add(override);
            }
            if (!superType.typeInfo().isJavaLangObject()) {
                result.addAll(recursiveOverridesCall(superType.typeInfo(), targetMethod, translationMapOfSuperType));
            }
        }
        return result;
    }

    private MethodInfo findUniqueMethod(TypeInfo typeInfo, MethodInfo targetMethod,
                                        Map<NamedType, ParameterizedType> translationMap) {
        return Stream.concat(typeInfo.methods().stream(), typeInfo.constructors().stream())
                .filter(mi -> sameMethod(mi, targetMethod, translationMap))
                .findFirst().orElse(null);
    }

    private boolean sameMethod(MethodInfo base, MethodInfo targetMethod,
                               Map<NamedType, ParameterizedType> translationMap) {
        if (!base.name().equals(targetMethod.name())) return false;
        return sameParameters(base.parameters(), targetMethod.parameters(), translationMap);
    }

    @Override
    public Map<NamedType, ParameterizedType> mapOfSuperType(ParameterizedType superType) {
        Map<NamedType, ParameterizedType> translationMapOfSuperType = new HashMap<>();
        if (!superType.parameters().isEmpty()) {
            assert superType.typeInfo() != null;
            ParameterizedType formalType = superType.typeInfo().asParameterizedType();
            int index = 0;
            for (ParameterizedType parameter : formalType.parameters()) {
                ParameterizedType concreteParameter = superType.parameters().get(index);
                translationMapOfSuperType.put(parameter.typeParameter(), concreteParameter);
                index++;
                if (index >= superType.parameters().size()) break;
            }
        }
        return translationMapOfSuperType;
    }

    @Override
    public boolean sameParameters(List<ParameterInfo> parametersOfMyMethod,
                                  List<ParameterInfo> parametersOfTarget,
                                  Map<NamedType, ParameterizedType> translationMap) {
        try {
            if (parametersOfMyMethod.size() != parametersOfTarget.size()) return false;
            int i = 0;
            for (ParameterInfo parameterInfo : parametersOfMyMethod) {
                ParameterInfo p2 = parametersOfTarget.get(i);
                if (differentType(parameterInfo.parameterizedType(), p2.parameterizedType(), translationMap,
                        null, 0)) {
                    return false;
                }
                i++;
            }
            return true;
        } catch (RuntimeException re) {
            LOGGER.error("Caught exception in sameParameters:\n{}\nvs\n{}", parametersOfMyMethod, parametersOfTarget, re);
            throw re;
        }
    }

    /**
     * This method is NOT the same as <code>isAssignableFrom</code>, and it serves a different purpose.
     * We need to take care to ensure that overloads are different.
     * <p>
     * java.lang.Appendable.append(java.lang.CharSequence) and java.lang.AbstractStringBuilder.append(java.lang.String)
     * can exist together in one class. They are different, even if String is assignable to CharSequence.
     * <p>
     * On the other hand, int comparable(Value other) is the same method as int comparable(T) in Comparable.
     * This is solved by taking the concrete type when we move from concrete types to parameterized types.
     *
     * @param inSuperType    first type
     * @param inSubType      second type
     * @param translationMap a map from type parameters in the super type to (more) concrete types in the sub-type
     * @return true if the types are "different"
     */
    private static boolean differentType(
            ParameterizedType inSuperType,
            ParameterizedType inSubType,
            Map<NamedType, ParameterizedType> translationMap,
            Set<TypeParameter> visitedTypeParameters,
            int infiniteLoopDetector) {
        if (infiniteLoopDetector >= 20) {
            throw new UnsupportedOperationException("Caught infinite loop: " + inSuperType + " vs "
                                                    + inSubType + "; tm " + translationMap);
        }
        Objects.requireNonNull(inSuperType);
        Objects.requireNonNull(inSubType);
        if (inSuperType.isReturnTypeOfConstructor() && inSubType == inSuperType) return false;

        if (inSuperType.typeInfo() != null) {
            if (inSubType.typeInfo() != inSuperType.typeInfo()) return true;
            if (inSuperType.parameters().size() != inSubType.parameters().size()) return true;
            int i = 0;
            for (ParameterizedType param1 : inSuperType.parameters()) {
                ParameterizedType param2 = inSubType.parameters().get(i);
                if (differentType(param1, param2, translationMap, visitedTypeParameters,
                        infiniteLoopDetector + 1))
                    return true;
                i++;
            }
            return false;
        }
        TypeParameter superTp = inSuperType.typeParameter();
        if (superTp != null && inSubType.typeInfo() != null) {
            // check if we can go from the parameter to the concrete type
            ParameterizedType inMap = translationMap.get(superTp);
            if (inMap == null) return true;
            if (inMap.typeParameter() != superTp) {
                return differentType(inMap, inSubType, translationMap, visitedTypeParameters,
                        infiniteLoopDetector + 1);
            } // else: the map doesn't point us to some other place
        }
        TypeParameter subTp = inSubType.typeParameter();
        if (superTp == null && subTp == null) return false;
        if (superTp == null || subTp == null) return true;
        if (superTp.equals(subTp)) return false;
        // they CAN have different indices, example in BiFunction TestTestExamplesWithAnnotatedAPIs, AnnotationsOnLambdas

        ParameterizedType translated = translationMap.get(superTp);
        if (translated != null && translated.typeParameter() == subTp) return false;
        if (inSubType.isUnboundTypeParameter() && inSuperType.isUnboundTypeParameter()) return false;
        List<ParameterizedType> inSubTypeBounds = subTp.typeBounds();
        List<ParameterizedType> inSuperTypeBounds = superTp.typeBounds();
        if (inSubTypeBounds.size() != inSuperTypeBounds.size()) return true;
        if (visitedTypeParameters == null) visitedTypeParameters = new HashSet<>();
        if (!visitedTypeParameters.add(subTp) && !
                visitedTypeParameters.add(superTp)) {
            return false; // no more recursing, both are presen
        }
        int i = 0;
        for (ParameterizedType typeBound : subTp.typeBounds()) {
            boolean different = differentType(typeBound, inSuperTypeBounds.get(i), translationMap, visitedTypeParameters,
                    infiniteLoopDetector + 1);
            if (different) return true;
            i++;
        }
        return false;
    }

    @Override
    public List<ParameterizedType> directSuperTypes(TypeInfo typeInfo) {
        if (typeInfo.isJavaLangObject()) return List.of();
        List<ParameterizedType> list = new ArrayList<>();
        if (typeInfo.parentClass() == null) {
            throw new UnsupportedOperationException("Only java.lang.Object can have null as the parentClass. Problem with: "
                                                    + typeInfo.fullyQualifiedName());
        }
        list.add(typeInfo.parentClass());
        list.addAll(typeInfo.interfacesImplemented());
        return list;
    }
}
