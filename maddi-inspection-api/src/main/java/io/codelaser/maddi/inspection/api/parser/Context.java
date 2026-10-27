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

package io.codelaser.maddi.inspection.api.parser;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.NamedType;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.Map;

public interface Context {
    VariableContext dependentVariableContext();

    ForwardType emptyForwardType();

    MethodInfo enclosingMethod();

    TypeInfo enclosingType();

    ForwardType erasureForwardType();

    Info info();

    boolean isDetailedSources();

    boolean isLombok();

    Lombok lombok();

    MethodResolution methodResolution();

    Context newCompilationUnit(CompilationUnit compilationUnit);

    ForwardType newForwardType(ParameterizedType parameterizedType);

    ForwardType newForwardType(ParameterizedType parameterizedType,
                               boolean erasure, Map<NamedType,
                    ParameterizedType> typeParameterMap);

    Context newAnonymousClassBody(TypeInfo anonymousType);

    Context newLocalTypeDeclaration();

    Context newSubType(TypeInfo typeInfo);

    Context newTypeBody();

    Context newTypeContext();

    Context newVariableContext(String lambda);

    Context newVariableContextForMethodBlock(MethodInfo methodInfo, ForwardType forwardType);

    Resolver resolver();

    Runtime runtime();

    TypeContext typeContext();

    VariableContext variableContext();

    Summary summary();

    ParseHelper parseHelper();

    GenericsHelper genericsHelper();

    DetailedSources.Builder newDetailedSourcesBuilder();

    Context withEnclosingMethod(MethodInfo methodInfo);

    // to transfer trailing comments to the correct field

    void blockComment(Source source);

    boolean commentIsBlocked(Source source);

}
