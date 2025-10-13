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

package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;

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
}
