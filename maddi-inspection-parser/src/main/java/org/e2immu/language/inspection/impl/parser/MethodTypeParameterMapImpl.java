package org.e2immu.language.inspection.impl.parser;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.MethodTypeParameterMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// use GenericsHelper to construct
class MethodTypeParameterMapImpl implements MethodTypeParameterMap {

    private final MethodInfo methodInfo;
    private final Map<NamedType, ParameterizedType> concreteTypes;

    public MethodTypeParameterMapImpl(MethodInfo methodInfo, @NotNull Map<NamedType, ParameterizedType> concreteTypes) {
        this.methodInfo = methodInfo; // can be null, for SAMs
        this.concreteTypes = Map.copyOf(concreteTypes);
    }

    @Override
    public MethodInfo methodInfo() {
        return methodInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        return o instanceof MethodTypeParameterMapImpl m && methodInfo.equals(m.methodInfo);
    }

    @Override
    public int hashCode() {
        return methodInfo.hashCode();
    }

    @Override
    public boolean isSingleAbstractMethod() {
        return methodInfo != null;
    }

    @Override
    public ParameterizedType getConcreteReturnType(Runtime runtime) {
        if (!isSingleAbstractMethod())
            throw new UnsupportedOperationException("Can only be called on a single abstract method");
        ParameterizedType returnType = methodInfo.returnType();
        return returnType.applyTranslation(runtime, concreteTypes);
    }

    @Override
    public ParameterizedType getConcreteTypeOfParameter(Runtime runtime, int i) {
        if (!isSingleAbstractMethod())
            throw new UnsupportedOperationException("Can only be called on a single abstract method");
        int n = methodInfo.parameters().size();
        int index;
        if (i >= n) {
            // varargs
            index = n - 1;
        } else {
            index = i;
        }

        ParameterizedType parameterizedType = methodInfo.parameters().get(index).parameterizedType();
        return parameterizedType.applyTranslation(runtime, concreteTypes);
    }

    @Override
    public List<ParameterizedType> getConcreteTypeOfParameters(Runtime runtime) {
        return IntStream.range(0, methodInfo.parameters().size())
                .mapToObj(i -> getConcreteTypeOfParameter(runtime, i))
                .toList();
    }

    @Override
    public MethodTypeParameterMap expand(Runtime runtime, TypeInfo primaryType,
                                         Map<NamedType, ParameterizedType> mapExpansion) {
        Map<NamedType, ParameterizedType> join = new HashMap<>(concreteTypes);
        mapExpansion.forEach((k, v) -> join.merge(k, v, (v1, v2) -> v1.mostSpecific(runtime, primaryType, v2)));
        return new MethodTypeParameterMapImpl(methodInfo, Map.copyOf(join));
    }

    @Override
    public String toString() {
        return (isSingleAbstractMethod()
                ? ("method " + methodInfo.fullyQualifiedName())
                : "No method") + ", map " + concreteTypes;
    }

    @Override
    public ParameterizedType inferFunctionalType(Runtime runtime,
                                                 List<ParameterizedType> types,
                                                 ParameterizedType inferredReturnType) {
        Objects.requireNonNull(inferredReturnType);
        Objects.requireNonNull(types);
        if (!isSingleAbstractMethod())
            throw new UnsupportedOperationException("Can only be called on a single abstract method");

        List<ParameterizedType> parameters = typeParametersComputed(runtime, methodInfo, types, inferredReturnType);
        return runtime.newParameterizedType(methodInfo.typeInfo(), parameters);
    }

    /**
     * Example: methodInfo = R apply(T t); typeInfo = Function&lt;T, R&gt;; types: one value: the concrete type for
     * parameter #0 in apply; inferredReturnType: the concrete type for R, the return type.
     *
     * @param runtime            to access inspection
     * @param methodInfo         the SAM (e.g. accept, test, apply)
     * @param types              as provided by ParseMethodReference, or ParseLambdaExpr. They represent the concrete
     *                           types of the SAM
     * @param inferredReturnType the return type of the real method
     * @return a list of type parameters for the functional type
     */


    private static List<ParameterizedType> typeParametersComputed(
            Runtime runtime,
            MethodInfo methodInfo,
            List<ParameterizedType> types,
            ParameterizedType inferredReturnType) {
        TypeInfo typeInfo = methodInfo.typeInfo();
        if (typeInfo.typeParameters().isEmpty()) return List.of();
        // Function<T, R> -> loop over T and R, and see where they appear in the apply method.
        // If they appear as a parameter, then take the type from "types" which agrees with that parameter
        // If it appears as the return type, then return "inferredReturnType"
        return typeInfo.typeParameters().stream()
                .map(typeParameter -> {
                    int cnt = 0;
                    for (ParameterInfo parameterInfo : methodInfo.parameters()) {
                        if (parameterInfo.parameterizedType().typeParameter() == typeParameter) {
                            return types.get(cnt); // this is one we know!
                        }
                        cnt++;
                    }
                    if (methodInfo.returnType().typeParameter() == typeParameter)
                        return inferredReturnType;
                    return runtime.newParameterizedType(typeParameter, 0, null);
                })
                .map(pt -> pt.ensureBoxed(runtime))
                .collect(Collectors.toList());
    }


    @Override
    public boolean isAssignableFrom(MethodTypeParameterMap other) {
        if (!isSingleAbstractMethod() || !other.isSingleAbstractMethod()) throw new UnsupportedOperationException();
        if (methodInfo.equals(other.methodInfo())) return true;
        if (methodInfo.parameters().size() != other.methodInfo().parameters().size())
            return false;
        /*
        int i = 0;
        for (ParameterInfo pi : methodInspection.getParameters()) {
            ParameterInfo piOther = other.methodInspection.getParameters().get(i);
            i++;
        }
        // TODO
         */
        return methodInfo.returnType().isVoidOrJavaLangVoid() ==
               other.methodInfo().returnType().isVoidOrJavaLangVoid();
    }
/*
    // used in TypeInfo.convertMethodReferenceIntoLambda
    public MethodInfo.Builder buildCopy(Runtime runtime,
                                        TypeInfo typeInfo) {
        String methodName = methodInfo.name();
        MethodInfo copy = runtime.newMethod(typeInfo, methodName, methodInfo.methodType());
        MethodInfo.Builder copyBuilder = copy.builder();
        copyBuilder.addMethodModifier(runtime.methodModifierPUBLIC());

        for (ParameterInfo p : methodInfo.parameters()) {
            ParameterInspection.Builder newParameterBuilder = copy.newParameterInspectionBuilder(
                    p.identifier,
                    getConcreteTypeOfParameter(runtime.getPrimitives(), p.index), p.name, p.index);
            if (p.parameterInspection.get().isVarArgs()) {
                newParameterBuilder.setVarArgs(true);
            }
            copy.addParameter(newParameterBuilder);
        }
        copy.setReturnType(getConcreteReturnType(runtime.getPrimitives()));
        copy.readyToComputeFQN(runtime);
        return copy;
    }*/

    @Override
    public MethodTypeParameterMap translate(TranslationMap translationMap) {
        return new MethodTypeParameterMapImpl(methodInfo, concreteTypes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> translationMap.translateType(e.getValue()))));
    }

    @Override
    public ParameterizedType parameterizedType(int pos) {
        List<ParameterInfo> parameters = methodInfo.parameters();
        if (pos < parameters.size()) return parameters.get(pos).parameterizedType();
        ParameterInfo lastOne = parameters.get(parameters.size() - 1);
        if (!lastOne.isVarArgs()) throw new UnsupportedOperationException();
        return lastOne.parameterizedType().copyWithOneFewerArrays();
    }

    /*
    CT = concreteTypes

    CT:  T in Function -> AL<LL<S>>
    F2C: T in Function -> Coll<E>
    result: E in Coll -> LL<S>

    CT:  R in Function -> Stream<? R in flatMap>
    F2C: R in Function -> Stream<E in Coll>
    result: E in Coll = R in flatMap (is of little value here)
     */
    @Override
    public Map<NamedType, ParameterizedType> formalOfSamToConcreteTypes(MethodInfo actualMethod, Runtime runtime) {
        Map<NamedType, ParameterizedType> result = new HashMap<>(concreteTypes);

        TypeInfo functionType = this.methodInfo.typeInfo();
        MethodInfo sam = functionType.singleAbstractMethod();
        // match types of actual method inspection to type parameters of sam
        if (sam.returnType().isTypeParameter()) {
            NamedType f2cFrom = sam.returnType().typeParameter();
            ParameterizedType f2cTo = actualMethod.returnType();
            ParameterizedType ctTo = concreteTypes.get(f2cFrom);
            match(runtime, f2cTo, ctTo, result);
        }
        if (!actualMethod.isStatic() && !functionType.typeParameters().isEmpty()) {
            NamedType f2cFrom = functionType.typeParameters().get(0);
            ParameterizedType f2cTo = actualMethod.typeInfo().asParameterizedType();
            ParameterizedType ctTo = concreteTypes.get(f2cFrom);
            match(runtime, f2cTo, ctTo, result);
        }
        // TODO for-loop: make an equivalent with more type parameters MethodReference_2
        return result;
    }

    /*
    f2cFrom = T in function
    fc2To = Coll<E>
    ctTo = ArrayList<LinkedList<String>>

     */
    private void match(Runtime runtime,
                       ParameterizedType f2cTo,
                       ParameterizedType ctTo,
                       Map<NamedType, ParameterizedType> result) {
        if (f2cTo.isAssignableFrom(runtime, ctTo)) {
            ParameterizedType concreteSuperType = ctTo.concreteSuperType(f2cTo);
            int i = 0;
            for (ParameterizedType pt : f2cTo.parameters()) {
                if (pt.isTypeParameter()) {
                    result.put(pt.typeParameter(), concreteSuperType.parameters().get(i));
                }
                i++;
            }
        }
    }

    @Override
    public Map<NamedType, ParameterizedType> concreteTypes() {
        return concreteTypes;
    }
}
