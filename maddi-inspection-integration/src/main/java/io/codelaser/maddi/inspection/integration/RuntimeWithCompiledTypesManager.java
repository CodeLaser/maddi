/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.integration;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.cst.impl.expression.eval.EvalOptions;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

public class RuntimeWithCompiledTypesManager extends RuntimeImpl {
    private final CompiledTypesManager compiledTypesManager;
    private final SourceSet sourceSetOfInternal;

    public RuntimeWithCompiledTypesManager(CompiledTypesManager compiledTypesManager) {
        this(compiledTypesManager, EvalOptions.DEFAULT);
    }

    public RuntimeWithCompiledTypesManager(CompiledTypesManager compiledTypesManager, EvalOptions evalOptions) {
        super(evalOptions);
        this.compiledTypesManager = compiledTypesManager;
        this.sourceSetOfInternal = new SourceSetImpl.Builder().setName("_internal_")
                .setUri(URI.create("file:/"))
                .setLibrary(true)
                .setExternalLibrary(true)
                .setPartOfJdk(true)
                .setModule(true)
                .build();
    }

    @Override
    public TypeInfo getFullyQualified(String name, boolean complain) {
        return getFullyQualified(name, complain, compiledTypesManager.javaBase());
    }

    @Override
    public TypeInfo getFullyQualified(String name, boolean complain, SourceSet sourceSetOfRequest) {
        TypeInfo typeInfo = compiledTypesManager.type(name, sourceSetOfRequest);
        if (typeInfo == null && complain) {
            throw new UnsupportedOperationException("Cannot find " + name
                                                    + "; request by source set " + sourceSetOfRequest.name());
        }
        return typeInfo;
    }

    @Override
    public java.util.List<TypeInfo> loadedCopies(String fullyQualifiedName) {
        return compiledTypesManager.typesIfLoaded(fullyQualifiedName);
    }

    private static final String SYNTHETIC_FUNCTION = "SyntheticFunction";
    private static final String SYNTHETIC_CONSUMER = "SyntheticConsumer";

    @Override
    public synchronized TypeInfo syntheticFunctionalType(int inputParameters, boolean hasReturnValue) {
        Class<?> clazz;
        if (hasReturnValue) {
            clazz = switch (inputParameters) {
                case 0 -> Supplier.class;
                case 1 -> Function.class;
                case 2 -> BiFunction.class;
                default -> null;
            };
        } else {
            clazz = switch (inputParameters) {
                case 0 -> Runnable.class;
                case 1 -> Consumer.class;
                case 2 -> BiConsumer.class;
                default -> null;
            };
        }
        if (clazz != null) return compiledTypesManager.type(clazz);
        return generateSyntheticFunction(inputParameters, hasReturnValue);
    }

    private TypeInfo generateSyntheticFunction(int inputParameters, boolean hasReturnValue) {
        String name = hasReturnValue ? SYNTHETIC_FUNCTION : SYNTHETIC_CONSUMER;
        String fqn = "_internal_." + name;
        TypeInfo get = compiledTypesManager.typeIfLoaded(fqn, null);
        if (get != null) return get;

        CompilationUnit cu = newCompilationUnitBuilder()
                .setSourceSet(sourceSetOfInternal)
                .setPackageName("_internal_")
                .setURIString("predefined://java/util/function").build();
        TypeInfo typeInfo = newTypeInfo(cu, name);
        TypeInfo.Builder builder = typeInfo.builder();
        builder.setParentClass(objectParameterizedType());
        builder.setTypeNature(typeNatureInterface());

        List<TypeParameter> typeParameters = new ArrayList<>();
        for (int i = 0; i < inputParameters + (hasReturnValue ? 1 : 0); i++) {
            TypeParameter tp = newTypeParameter(i, "P" + i, typeInfo);
            tp.builder().setTypeBounds(List.of()).commit();
            builder.addOrSetTypeParameter(tp);
            typeParameters.add(tp);
        }
        builder.setAccess(accessPublic());
        builder.addTypeModifier(typeModifierPublic());
        builder.addAnnotation(functionalInterfaceAnnotationExpression());
        ParameterizedType returnType = hasReturnValue
                ? newParameterizedType(typeParameters.get(typeParameters.size() - 1), 0, null)
                : voidParameterizedType();
        String methodName = methodNameOfFunctionalInterface(inputParameters, hasReturnValue,
                returnType.isBooleanOrBoxedBoolean());
        MethodInfo sam = newMethod(typeInfo, methodName, methodTypeAbstractMethod());
        sam.analysis().set(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE);
        sam.builder()
                .setSynthetic(true)
                .setSource(noSource())
                .setAccess(accessPublic())
                .addMethodModifier(methodModifierPublic())
                .setReturnType(returnType);
        for (int i = 0; i < inputParameters; i++) {
            ParameterizedType type = newParameterizedType(typeParameters.get(i), 0, null);
            ParameterInfo pi = sam.builder().addParameter("p" + i, type);
            pi.builder().setVarArgs(false).commit();
        }
        sam.builder().commit();
        builder.addMethod(sam).setSingleAbstractMethod(sam).commit();
        return typeInfo;
    }

    private String methodNameOfFunctionalInterface(int numberOfParameters, boolean hasReturn, boolean isPredicate) {
        if (!hasReturn) return "accept";
        if (numberOfParameters == 0) return "get";
        if (isPredicate) return "test";
        return "apply";
    }
}
