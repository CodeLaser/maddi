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

package io.codelaser.maddi.cst.print.csharp;

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
import io.codelaser.maddi.cst.impl.output.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Prints a {@link MethodInfo} as a C# method or constructor:
 * {@code [access] [static] [virtual|override|abstract] ReturnType Name<T>(T p, params U[] us) where T : … body}.
 * <ul>
 *   <li>a body that is a single {@code return} is an expression body, {@code => expr;};</li>
 *   <li>a constructor's {@code super(…)}/{@code this(…)} is its initializer, {@code : base(…)}/{@code : this(…)};</li>
 *   <li>a static initializer is the static constructor;</li>
 *   <li>a {@code synchronized} method is {@code [MethodImpl(MethodImplOptions.Synchronized)]};</li>
 *   <li>{@code throws} is dropped: C# has no checked exceptions.</li>
 * </ul>
 */
public record CSharpMethodPrinter(TypeInfo typeInfo, MethodInfo methodInfo, boolean formatter2) implements MethodPrinter {

    @Override
    public OutputBuilder print(Qualification q) {
        CSharpContext.pushMethod(methodInfo);
        CSharpContext.enterScope(java.util.Set.of()); // the parameters'
        try {
            return printMethod(q);
        } finally {
            CSharpContext.exitScope();
            CSharpContext.popMethod();
        }
    }

    private OutputBuilder printMethod(Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        if (methodInfo.isStaticInitializer()) {
            return b.add(new TextImpl("static " + CSharpNames.type(typeInfo))).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                    .add(SpaceEnum.ONE).add(CSharpStatementPrinter.block(methodInfo.methodBody(), q));
        }
        if (methodInfo.isInstanceInitializer()) {
            CSharpContext.message(CSharpPrintMessage.Code.INSTANCE_INITIALIZER, methodInfo, typeInfo.simpleName());
            return b;
        }
        if (methodInfo.isSynchronized()) {
            CSharpContext.using("System.Runtime.CompilerServices");
            b.add(new TextImpl("[MethodImpl(MethodImplOptions.Synchronized)]")).add(SpaceEnum.NEWLINE);
        }
        StringBuilder modifiers = new StringBuilder();
        String access = CSharpModifiers.access(methodInfo, typeInfo);
        // C#'s interface members are public; a private one says so
        if (access != null) modifiers.append(access).append(' ');
        if (methodInfo.isStatic()) modifiers.append("static ");
        String inheritance = CSharpModifiers.inheritance(methodInfo, typeInfo);
        if (inheritance != null) modifiers.append(inheritance).append(' ');

        if (methodInfo.isConstructor()) {
            b.add(new TextImpl(modifiers + CSharpNames.type(typeInfo))).add(parameters(methodInfo, q));
            constructorBody(b, q);
            return b;
        }
        String typeParameters = methodInfo.typeParameters().isEmpty() ? ""
                : methodInfo.typeParameters().stream().map(tp -> CSharpNames.name(tp.simpleName()))
                        .collect(Collectors.joining(", ", "<", ">"));
        b.add(new TextImpl(modifiers + CSharpTypeName.of(methodInfo.returnType(), q))).add(SpaceEnum.ONE)
                .add(new TextImpl(CSharpNames.method(methodInfo) + typeParameters));
        b.add(parameters(methodInfo, q));
        for (String constraint : CSharpTypeName.constraints(methodInfo.typeParameters(), q)) {
            b.add(SpaceEnum.ONE).add(new TextImpl(constraint));
        }
        Block body = methodInfo.methodBody();
        if (methodInfo.isAbstract() || body == null) return b.add(SymbolEnum.SEMICOLON);
        List<Statement> statements = body.statements().stream().filter(s -> !s.isSynthetic()).toList();
        if (statements.size() == 1 && statements.getFirst() instanceof ReturnStatement rs && !rs.hasNoValue()) {
            return b.add(SymbolEnum.binaryOperator("=>")).add(CSharpExpressionPrinter.print(rs.expression(), q))
                    .add(SymbolEnum.SEMICOLON);
        }
        return b.add(SpaceEnum.ONE).add(CSharpStatementPrinter.block(statements, q));
    }

    /** {@code : base(…)} or {@code : this(…)} from the first statement, then the rest of the body. */
    private void constructorBody(OutputBuilder b, Qualification q) {
        Block body = methodInfo.methodBody();
        List<Statement> statements = body == null ? List.of()
                : body.statements().stream().filter(s -> !s.isSynthetic()).toList();
        if (!statements.isEmpty() && statements.getFirst() instanceof ExplicitConstructorInvocation eci) {
            if (!eci.isSuper() || !eci.parameterExpressions().isEmpty()) {
                b.add(SymbolEnum.COLON).add(eci.isSuper() ? CSharpKeyword.BASE : KeywordImpl.THIS)
                        .add(CSharpExpressionPrinter.arguments(eci.parameterExpressions(), q));
            }
            statements = statements.subList(1, statements.size());
        }
        b.add(SpaceEnum.ONE).add(CSharpStatementPrinter.block(statements, q));
    }

    /** {@code (int a, params string[] rest)}. */
    static OutputBuilder parameters(MethodInfo methodInfo, Qualification q) {
        if (methodInfo.parameters().isEmpty()) return new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        return methodInfo.parameters().stream().map(p -> parameter(p, q))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                        SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration()));
    }

    private static OutputBuilder parameter(ParameterInfo p, Qualification q) {
        String type = CSharpTypeName.of(p.parameterizedType(), q);
        return new OutputBuilderImpl().add(new TextImpl((p.isVarArgs() ? "params " : "") + type + " "
                                                        + CSharpContext.declare(p.name())));
    }
}
