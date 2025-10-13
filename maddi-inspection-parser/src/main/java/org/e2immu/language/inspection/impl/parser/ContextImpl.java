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

package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.*;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class ContextImpl implements Context {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextImpl.class);

    private record Data(Runtime runtime, Summary summary, MethodResolution methodResolution,
                        boolean detailedSources, Lombok lombok) {
    }

    private final Data data;
    private final TypeInfo enclosingType;
    private final MethodInfo enclosingMethod;
    private final Resolver resolver;
    private final TypeContext typeContext;
    private final VariableContext variableContext;
    private final ForwardType typeOfEnclosingSwitchExpression;

    public static Context create(Runtime runtime,
                                 CompiledTypesManager compiledTypesManager,
                                 Summary summary,
                                 Resolver resolver,
                                 TypeContext typeContext,
                                 boolean detailedSources,
                                 boolean isLombok) {
        MethodResolutionImpl methodResolution = new MethodResolutionImpl(runtime);
        Lombok lombok = isLombok ? new LombokImpl(runtime, compiledTypesManager) : null;
        Data d = new Data(runtime, summary, methodResolution, detailedSources, lombok);
        return new ContextImpl(d, null, null, resolver,
                typeContext, new VariableContextImpl(), null);
    }

    private ContextImpl(Data data, TypeInfo enclosingType,
                        MethodInfo enclosingMethod,
                        Resolver resolver,
                        TypeContext typeContext,
                        VariableContext variableContext,
                        ForwardType typeOfEnclosingSwitchExpression) {
        this.data = data;
        this.enclosingType = enclosingType;
        this.enclosingMethod = enclosingMethod;
        this.typeContext = typeContext;
        this.variableContext = variableContext;
        this.typeOfEnclosingSwitchExpression = typeOfEnclosingSwitchExpression;
        this.resolver = resolver;
    }

    public Info info() {
        if (enclosingMethod != null) return enclosingMethod;
        return enclosingType;
    }

    @Override
    public Runtime runtime() {
        return data.runtime;
    }

    @Override
    public MethodResolution methodResolution() {
        return data.methodResolution;
    }

    @Override
    public Resolver resolver() {
        return resolver;
    }

    @Override
    public TypeInfo enclosingType() {
        return enclosingType;
    }

    @Override
    public MethodInfo enclosingMethod() {
        return enclosingMethod;
    }

    @Override
    public TypeContext typeContext() {
        return typeContext;
    }

    @Override
    public VariableContext variableContext() {
        return variableContext;
    }

    @Override
    public Summary summary() {
        return data.summary;
    }

    @Override
    public VariableContext dependentVariableContext() {
        return variableContext.newVariableContext();
    }

    @Override
    public ContextImpl newCompilationUnit(CompilationUnit compilationUnit) {
        TypeContext typeContext = typeContext().newCompilationUnit(compilationUnit);
        return new ContextImpl(data, null, null, resolver,
                typeContext, variableContext.newEmpty(), null);
    }

    @Override
    public Context newVariableContext(String reason) {
        LOGGER.debug("Creating a new variable context for {}", reason);
        VariableContext newVariableContext = variableContext.newVariableContext();
        return new ContextImpl(data, enclosingType, enclosingMethod, resolver,
                typeContext, newVariableContext, typeOfEnclosingSwitchExpression);
    }

    @Override
    public Context newVariableContextForMethodBlock(MethodInfo methodInfo, ForwardType forwardType) {
        return new ContextImpl(data, methodInfo.typeInfo(), methodInfo, resolver,
                typeContext, dependentVariableContext(), forwardType);
    }

    public ContextImpl newAnonymousClassBody(TypeInfo baseType) {
        TypeContext newTypeContext = typeContext.newAnonymousClassBody(baseType);
        VariableContext newVariableContext = variableContext.newVariableContext();
        return new ContextImpl(data, baseType, null, resolver.newEmpty(), newTypeContext,
                newVariableContext, typeOfEnclosingSwitchExpression);
    }

    @Override
    public Context newLocalTypeDeclaration() {
        assert enclosingType != null;
        assert enclosingMethod != null;
        TypeContext newTypeContext = typeContext.newAnonymousClassBody(enclosingType);
        VariableContext newVariableContext = variableContext.newVariableContext();
        return new ContextImpl(data, enclosingType, enclosingMethod, resolver.newEmpty(), newTypeContext,
                newVariableContext, typeOfEnclosingSwitchExpression);
    }

    public ContextImpl newSubType(TypeInfo subType) {
        return new ContextImpl(data, subType, null, resolver,
                typeContext.newTypeContext(), variableContext, null);
    }

    @Override
    public ForwardType newForwardType(ParameterizedType parameterizedType) {
        return new ForwardTypeImpl(parameterizedType, false, TypeParameterMap.EMPTY);
    }

    @Override
    public ForwardType newForwardType(ParameterizedType parameterizedType,
                                      boolean erasure,
                                      Map<NamedType, ParameterizedType> typeParameterMap) {
        return new ForwardTypeImpl(parameterizedType, erasure, new TypeParameterMap(typeParameterMap));
    }

    @Override
    public Context newTypeBody() {
        VariableContext newVariableContext = variableContext.newVariableContext();
        return new ContextImpl(data, enclosingType, enclosingMethod, resolver, typeContext,
                newVariableContext, typeOfEnclosingSwitchExpression);
    }

    @Override
    public Context newTypeContext() {
        TypeContext newTypeContext = typeContext.newTypeContext();
        return new ContextImpl(data, enclosingType, enclosingMethod, resolver, newTypeContext,
                variableContext, typeOfEnclosingSwitchExpression);
    }

    @Override
    public Context withEnclosingMethod(MethodInfo methodInfo) {
        return new ContextImpl(data, enclosingType, methodInfo, resolver, typeContext, variableContext,
                typeOfEnclosingSwitchExpression);
    }

    @Override
    public ForwardType erasureForwardType() {
        return new ForwardTypeImpl(null, true, TypeParameterMap.EMPTY);
    }

    public ForwardType emptyForwardType() {
        return new ForwardTypeImpl(null, false, TypeParameterMap.EMPTY);
    }

    @Override
    public ParseHelper parseHelper() {
        return resolver().parseHelper();
    }

    @Override
    public GenericsHelper genericsHelper() {
        return data.methodResolution.genericsHelper();
    }

    @Override
    public DetailedSources.Builder newDetailedSourcesBuilder() {
        if (data.detailedSources) return runtime().newDetailedSourcesBuilder();
        return null;
    }

    @Override
    public boolean isDetailedSources() {
        return data.detailedSources;
    }

    @Override
    public boolean isLombok() {
        return data.lombok != null;
    }

    @Override
    public Lombok lombok() {
        return data.lombok;
    }
}
