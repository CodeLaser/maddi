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

package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.MethodPrinter;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.List;

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
        if (methodInfo.isStaticInitializer() || methodInfo.isInstanceInitializer()) {
            // a static initializer belongs in the companion object (step 3); until then it reads as an `init`
            return b.add(new TextImpl("init")).add(SpaceEnum.ONE)
                    .add(KotlinStatementPrinter.block(methodInfo.methodBody(), qualification));
        }
        KotlinModifiers.visibility(methodInfo.access()).ifPresent(v -> b.add(v).add(SpaceEnum.ONE));
        if (!methodInfo.overrides().isEmpty()) {
            b.add(KotlinKeyword.OVERRIDE).add(SpaceEnum.ONE);
        } else if (methodInfo.isAbstract() && !typeInfo.isInterface()) {
            b.add(KeywordImpl.ABSTRACT).add(SpaceEnum.ONE);
        } else if (isOpen()) {
            b.add(KotlinKeyword.OPEN).add(SpaceEnum.ONE);
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
            b.add(new TextImpl(KotlinNames.name(methodInfo.name())));
        }

        if (methodInfo.parameters().isEmpty()) {
            b.add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        } else {
            b.add(methodInfo.parameters().stream()
                    .map(pi -> parameter(pi, qualification))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                            SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration())));
        }

        if (!methodInfo.isConstructor()) {
            ParameterizedType rt = methodInfo.returnType();
            if (rt != null && !rt.isVoidOrJavaLangVoid()) {
                b.add(SymbolEnum.COLON_LABEL).add(new TextImpl(KotlinTypeName.of(rt, qualification)));
            }
        }

        if (!methodInfo.isAbstract()) {
            Block body = methodInfo.methodBody();
            if (methodInfo.isConstructor()) {
                constructorBody(b, body, qualification);
                return b;
            }
            Expression expressionBody = expressionBody(body);
            if (expressionBody != null) {
                b.add(SpaceEnum.ONE).add(KotlinSymbols.assignment("=")).add(SpaceEnum.ONE)
                        .add(KotlinExpressionPrinter.print(expressionBody, qualification));
            } else {
                b.add(SpaceEnum.ONE).add(KotlinStatementPrinter.block(body, qualification));
            }
        }
        return b;
    }

    /**
     * A Java {@code super(…)}/{@code this(…)} first statement is Kotlin's delegation in the header:
     * {@code constructor(x: Int) : super(x) { … }}. A class without a primary constructor must have every secondary
     * one delegate to the superclass, so an implicit {@code super()} is written out when the class has a parent.
     */
    private void constructorBody(OutputBuilder b, Block body, Qualification q) {
        List<Statement> statements = body == null ? List.of()
                : body.statements().stream().filter(s -> !s.isSynthetic()).toList();
        ExplicitConstructorInvocation eci = !statements.isEmpty()
                                            && statements.getFirst() instanceof ExplicitConstructorInvocation e ? e : null;
        if (eci != null) {
            b.add(SpaceEnum.ONE).add(SymbolEnum.COLON).add(SpaceEnum.ONE)
                    .add(eci.isSuper() ? KeywordImpl.SUPER : KeywordImpl.THIS)
                    .add(eci.parameterExpressions().isEmpty() ? new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                            : eci.parameterExpressions().stream().map(x -> KotlinExpressionPrinter.print(x, q))
                            .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                                    SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.defaultGuideGenerator())));
            statements = statements.subList(1, statements.size());
        } else if (KotlinTypePrinter.hasWrittenSuperclass(typeInfo)) {
            b.add(SpaceEnum.ONE).add(SymbolEnum.COLON).add(SpaceEnum.ONE).add(KeywordImpl.SUPER)
                    .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        }
        if (!statements.isEmpty()) {
            b.add(SpaceEnum.ONE).add(KotlinStatementPrinter.block(statements, q));
        }
    }

    /** A Java method that can be overridden must say so in Kotlin, where {@code final} is the default. */
    private boolean isOpen() {
        return !methodInfo.isFinal() && !methodInfo.isStatic() && !methodInfo.isConstructor()
               && !methodInfo.access().isPrivate() && !typeInfo.isInterface()
               && KotlinTypePrinter.isOpen(typeInfo);
    }

    /** The expression of a single-`return` body (Kotlin `fun … = expr`), or {@code null} for a block body. */
    private static Expression expressionBody(Block body) {
        List<Statement> statements = body.statements().stream().filter(s -> !s.isSynthetic()).toList();
        if (statements.size() == 1 && statements.getFirst() instanceof ReturnStatement rs
            && !rs.hasNoValue()) {
            return rs.expression();
        }
        return null;
    }

    private OutputBuilder parameter(ParameterInfo pi, Qualification q) {
        OutputBuilder ob = new OutputBuilderImpl();
        if (pi.isVarArgs()) ob.add(new TextImpl("vararg")).add(SpaceEnum.ONE);
        ParameterizedType type = pi.isVarArgs() ? pi.parameterizedType().copyWithArrays(0) : pi.parameterizedType();
        ob.add(new TextImpl(KotlinNames.name(pi.name()))).add(SymbolEnum.COLON_LABEL)
                .add(new TextImpl(KotlinTypeName.of(type, q)));
        return ob;
    }
}
