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

package org.e2immu.language.cst.print.kotlin;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.MethodPrinter;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.output.*;

/**
 * Prints a {@link MethodInfo} as a Kotlin function (or secondary constructor): `[vis] [override|abstract] fun
 * [<T>] name(p: T, …)[: ReturnType] body`. The return type is omitted for `Unit`/void and for constructors.
 * `override` is emitted when the method overrides a supertype method. The body reuses the shared block/expression
 * printing (Kotlin accepts the Java-style `{ … }` block; idiomatic expression bodies `= expr` are a refinement).
 */
public record KotlinMethodPrinter(TypeInfo typeInfo, MethodInfo methodInfo, boolean formatter2) implements MethodPrinter {

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder b = new OutputBuilderImpl();
        KotlinModifiers.visibility(methodInfo.access()).ifPresent(v -> b.add(v).add(SpaceEnum.ONE));
        if (!methodInfo.overrides().isEmpty()) {
            b.add(KotlinKeyword.OVERRIDE).add(SpaceEnum.ONE);
        } else if (methodInfo.isAbstract() && !typeInfo.isInterface()) {
            b.add(KeywordImpl.ABSTRACT).add(SpaceEnum.ONE);
        }

        if (methodInfo.isConstructor()) {
            b.add(KotlinKeyword.CONSTRUCTOR);
        } else {
            b.add(KotlinKeyword.FUN).add(SpaceEnum.ONE);
            if (!methodInfo.typeParameters().isEmpty()) {
                b.add(SymbolEnum.LEFT_ANGLE_BRACKET);
                b.add(methodInfo.typeParameters().stream()
                        .map(tp -> new OutputBuilderImpl().add(new TextImpl(tp.simpleName())))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
                b.add(SymbolEnum.RIGHT_ANGLE_BRACKET).add(SpaceEnum.ONE);
            }
            b.add(new TextImpl(methodInfo.name()));
        }

        if (methodInfo.parameters().isEmpty()) {
            b.add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        } else {
            b.add(methodInfo.parameters().stream()
                    .map(this::parameter)
                    .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                            SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration())));
        }

        if (!methodInfo.isConstructor()) {
            ParameterizedType rt = methodInfo.returnType();
            if (rt != null && !rt.isVoidOrJavaLangVoid()) {
                b.add(SymbolEnum.COLON_LABEL).add(new TextImpl(KotlinTypeName.of(rt)));
            }
        }

        if (!methodInfo.isAbstract()) {
            b.add(SpaceEnum.ONE).add(methodInfo.methodBody().print(qualification));
        }
        return b;
    }

    private OutputBuilder parameter(ParameterInfo pi) {
        OutputBuilder ob = new OutputBuilderImpl();
        if (pi.isVarArgs()) ob.add(new TextImpl("vararg")).add(SpaceEnum.ONE);
        ParameterizedType type = pi.isVarArgs() ? pi.parameterizedType().copyWithArrays(0) : pi.parameterizedType();
        ob.add(new TextImpl(pi.name())).add(SymbolEnum.COLON_LABEL)
                .add(new TextImpl(KotlinTypeName.of(type)));
        return ob;
    }
}
