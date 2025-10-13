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

package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

public interface Lambda extends Expression {
    MethodInfo methodInfo();

    default Block methodBody() {
        return methodInfo().methodBody();
    }

    default List<ParameterInfo> parameters() {
        return methodInfo().parameters();
    }

    default TypeInfo abstractFunctionalTypeInfo() {
        return concreteFunctionalType().typeInfo();
    }

    default ParameterizedType concreteFunctionalType() {
        return methodInfo().typeInfo().interfacesImplemented().get(0);
    }

    default ParameterizedType parameterizedType() {
        return implementation();
    }

    default ParameterizedType implementation() {
        return methodInfo().typeInfo().asSimpleParameterizedType();
    }

    default ParameterizedType concreteReturnType() {
        return methodInfo().returnType();
    }

    List<OutputVariant> outputVariants();

    Lambda withMethodInfoAndMethodBody(MethodInfo methodInfo);

    interface OutputVariant {
        boolean isEmpty();

        boolean isTyped();

        boolean isVar();

        OutputBuilder print(ParameterInfo parameterInfo, Qualification qualification);
    }

    interface Builder extends Expression.Builder<Builder> {

        @Fluent
        Builder setMethodInfo(MethodInfo methodInfo);

        @Fluent
        Builder setOutputVariants(List<OutputVariant> outputVariants);

        Lambda build();
    }

    String NAME = "lambda";

    @Override
    default String name() {
        return NAME;
    }
}
