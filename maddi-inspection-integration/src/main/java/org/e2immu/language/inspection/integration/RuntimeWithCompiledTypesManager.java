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

package org.e2immu.language.inspection.integration;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.resource.SourceSetImpl;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.*;

public class RuntimeWithCompiledTypesManager extends RuntimeImpl {
    private final CompiledTypesManager compiledTypesManager;
    private final SourceSet sourceSetOfInternal;

    public RuntimeWithCompiledTypesManager(CompiledTypesManager compiledTypesManager) {
        this.compiledTypesManager = compiledTypesManager;
        this.sourceSetOfInternal = new SourceSetImpl("_internal_", List.of(),
                URI.create("file:/"), StandardCharsets.UTF_8, false, true, true,
                true, true, Set.of(), Set.of());
    }

    @Override
    public TypeInfo getFullyQualified(String name, boolean complain) {
        return getFullyQualified(name, complain, compiledTypesManager.javaBase());
    }

    @Override
    public TypeInfo getFullyQualified(String name, boolean complain, SourceSet sourceSetOfRequest) {
        TypeInfo typeInfo = compiledTypesManager.getOrLoad(name, sourceSetOfRequest);
        if (typeInfo == null && complain) {
            throw new UnsupportedOperationException("Cannot find " + name
                                                    + "; request by source set " + sourceSetOfRequest.name());
        }
        return typeInfo;
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
        if (clazz != null) return compiledTypesManager.getOrLoad(clazz);
        return generateSyntheticFunction(inputParameters, hasReturnValue);
    }

    private TypeInfo generateSyntheticFunction(int inputParameters, boolean hasReturnValue) {
        String name = hasReturnValue ? SYNTHETIC_FUNCTION : SYNTHETIC_CONSUMER;
        String fqn = "_internal_." + name;
        TypeInfo get = compiledTypesManager.get(fqn, null);
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
