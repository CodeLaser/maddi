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

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.MethodPrinter;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.List;

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
            CSharpContext.message(CSharpPrintCode.INSTANCE_INITIALIZER, methodInfo, typeInfo.simpleName());
            return b;
        }
        b.add(CSharpAttributes.uses(methodInfo.annotations(), methodInfo.isConstructor() ? LibraryMapping.Target.CONSTRUCTOR
                : LibraryMapping.Target.METHOD, q, true));
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
            boolean outer = CSharpInner.withOuter(typeInfo);
            b.add(new TextImpl(modifiers + CSharpNames.type(typeInfo)))
                    .add(outer ? outerParameters(methodInfo, q) : parameters(methodInfo, q));
            constructorBody(b, outer, q);
            return b;
        }
        CSharpWildcards.Captured captured = CSharpWildcards.captured(methodInfo, q);
        // <J> J mapping(), which a method that is not generic overrides: J erased to its bound (CSharpProgram)
        boolean erased = CSharpContext.program().erasedGeneric(methodInfo);
        List<String> allTypeParameters = new java.util.ArrayList<>(erased ? List.of() : methodInfo.typeParameters().stream()
                .map(CSharpNames::typeParameter).toList());
        allTypeParameters.addAll(captured.typeParameters());
        String typeParameters = allTypeParameters.isEmpty() ? "" : "<" + String.join(", ", allTypeParameters) + ">";
        MethodInfo overridden = CSharpTypePrinter.interfaceMethodDefaulted(typeInfo, methodInfo);
        if (overridden != null) {
            // a default method for an inherited interface's method implements it explicitly
            ParameterizedType owner = CSharpTypePrinter.implementedAs(typeInfo, overridden.typeInfo());
            java.util.Map<TypeParameter, ParameterizedType> map = CSharpTypePrinter.typeArguments(owner);
            b.add(new TextImpl(CSharpTypeName.of(CSharpTypePrinter.substitute(overridden.returnType(), map), q)))
                    .add(SpaceEnum.ONE)
                    .add(new TextImpl(CSharpTypeName.of(owner, q) + "." + CSharpNames.method(overridden)));
        } else {
            String property = CSharpContext.program().computedProperty(methodInfo);
            if (property != null) return property(b, modifiers.toString(), property, q);
            b.add(new TextImpl(modifiers + returnType(q))).add(SpaceEnum.ONE)
                    .add(new TextImpl(CSharpNames.method(methodInfo) + typeParameters));
        }
        b.add(parameters(methodInfo, captured, q));
        List<String> constraints = new java.util.ArrayList<>(erased ? List.of()
                : CSharpTypeName.constraints(methodInfo.typeParameters(), q));
        constraints.addAll(captured.constraints());
        for (String constraint : constraints) {
            b.add(SpaceEnum.ONE).add(new TextImpl(constraint));
        }
        Block body = methodInfo.methodBody();
        if (methodInfo.isAbstract() || body == null) return b.add(SymbolEnum.SEMICOLON);
        List<Statement> statements = CstQueries.own(body.statements());
        Expression returnedValue = CstQueries.returnedValue(statements);
        if (returnedValue != null) {
            return b.add(SymbolEnum.binaryOperator("=>")).add(CSharpExpressionPrinter.returned(returnedValue, q))
                    .add(SymbolEnum.SEMICOLON);
        }
        return b.add(SpaceEnum.ONE).add(CSharpStatementPrinter.block(statements, q));
    }

    /**
     * A method that is a property without a field ({@link CSharpProperties}): {@code T Name { get; }} when abstract,
     * {@code T Name => expr;} for a returned expression, {@code T Name { get { … } }} for any other body.
     */
    private OutputBuilder property(OutputBuilder b, String modifiers, String name, Qualification q) {
        b.add(new TextImpl(modifiers + returnType(q))).add(SpaceEnum.ONE).add(new TextImpl(name));
        Block body = methodInfo.methodBody();
        if (methodInfo.isAbstract() || body == null) return b.add(new TextImpl("{ get; }"));
        List<Statement> statements = CstQueries.own(body.statements());
        Expression returnedValue = CstQueries.returnedValue(statements);
        if (returnedValue != null) {
            return b.add(SymbolEnum.binaryOperator("=>")).add(CSharpExpressionPrinter.returned(returnedValue, q))
                    .add(SymbolEnum.SEMICOLON);
        }
        return b.add(SpaceEnum.ONE).add(SymbolEnum.LEFT_BRACE).add(new TextImpl("get")).add(SpaceEnum.ONE)
                .add(CSharpStatementPrinter.block(statements, q)).add(SymbolEnum.RIGHT_BRACE);
    }

    /** The return type, {@code T?} when the method may return null. */
    private String returnType(Qualification q) {
        if (CSharpContext.program().erasedGeneric(methodInfo)) {
            return CSharpTypeName.rawArgumentOf(methodInfo.returnType().typeParameter(), q);
        }
        io.codelaser.maddi.cst.api.type.NullableState nullable = CSharpContext.program().facts().nullable(methodInfo);
        String type = CSharpTypeName.declared(methodInfo.returnType(), false, nullable, q);
        return nullableTypeParameter(methodInfo.returnType(), methodInfo, nullable) ? type + "?" : type;
    }

    /** {@code : base(…)} or {@code : this(…)} from the first statement, then the rest of the body. */
    private void constructorBody(OutputBuilder b, boolean outer, Qualification q) {
        Block body = methodInfo.methodBody();
        List<Statement> statements = body == null ? List.of()
                : CstQueries.own(body.statements());
        boolean chained = false;
        ParameterizedType parent = typeInfo.parentClass();
        boolean outerParent = outer && parent != null && CSharpInner.withOuter(parent.typeInfo());
        if (!statements.isEmpty() && statements.getFirst() instanceof ExplicitConstructorInvocation eci) {
            chained = !eci.isSuper();
            // an inner class passes its enclosing instance on: this(outer, …), base(outer, …)
            boolean passOuter = outer && (chained || outerParent);
            if (passOuter) {
                b.add(SymbolEnum.COLON).add(eci.isSuper() ? CSharpKeyword.BASE : KeywordImpl.THIS)
                        .add(CSharpExpressionPrinter.argumentsAfter(new OutputBuilderImpl().add(new TextImpl("outer")), eci.parameterExpressions(),
                                eci.methodInfo(), q));
            } else if (!eci.isSuper() || !eci.parameterExpressions().isEmpty()) {
                b.add(SymbolEnum.COLON).add(eci.isSuper() ? CSharpKeyword.BASE : KeywordImpl.THIS)
                        .add(CSharpExpressionPrinter.arguments(eci.parameterExpressions(), eci.methodInfo(), q));
            }
            statements = statements.subList(1, statements.size());
        } else if (outerParent) {
            b.add(SymbolEnum.COLON).add(CSharpKeyword.BASE).add(CSharpExpressionPrinter.argumentsAfter(
                    new OutputBuilderImpl().add(new TextImpl("outer")), List.of(), null, q));
        }
        if (!chained && (outer || CSharpInner.initializedInConstructor(typeInfo))) {
            List<OutputBuilder> first = new java.util.ArrayList<>();
            if (outer) first.add(new OutputBuilderImpl().add(new TextImpl("this.outer = outer;")));
            first.addAll(CSharpInner.fieldInitializers(typeInfo, q));
            b.add(SpaceEnum.ONE).add(CSharpStatementPrinter.block(statements, first, q));
        } else {
            b.add(SpaceEnum.ONE).add(CSharpStatementPrinter.block(statements, q));
        }
    }

    /** An inner class's constructor's parameters: its enclosing instance first, {@code (Outer outer, …)}. */
    private static OutputBuilder outerParameters(MethodInfo methodInfo, Qualification q) {
        OutputBuilder first = new OutputBuilderImpl().add(new TextImpl(CSharpInner.outerTypeName(methodInfo.typeInfo())
                                                                      + " outer"));
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(first), methodInfo.parameters().stream()
                        .map(p -> parameter(p, null, q)))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                        SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration()));
    }

    /** {@code (int a, params string[] rest)}. */
    static OutputBuilder parameters(MethodInfo methodInfo, Qualification q) {
        return parameters(methodInfo, CSharpWildcards.Captured.NONE, q);
    }

    private static OutputBuilder parameters(MethodInfo methodInfo, CSharpWildcards.Captured captured, Qualification q) {
        if (methodInfo.parameters().isEmpty()) return new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        int token = CSharpContext.program().typeToken(methodInfo);
        // a type token, Class<T> type, is the type argument T
        if (token >= 0 && methodInfo.parameters().size() == 1) {
            return new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        }
        return methodInfo.parameters().stream().filter(p -> p.index() != token)
                .map(p -> parameter(p, captured.names().get(p.index()), q))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                        SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration()));
    }

    /**
     * A nullable parameter or result of a method's own type parameter's type is {@code T?} in a static method, as
     * {@link CSharpTypeName#declared} has a class's type parameter: C# infers {@code T} from an argument that may be
     * null as the type without {@code ?}, so that {@code EnsureNotNull<T>(T? o, …)} returns a non-null {@code T}. In a
     * method that may override or be overridden, C# reads that {@code T?} as {@code Nullable<T>} unless the override
     * says {@code where T : default}.
     */
    private static boolean nullableTypeParameter(ParameterInfo p, io.codelaser.maddi.cst.api.type.NullableState nullable) {
        return !p.isVarArgs() && nullableTypeParameter(p.parameterizedType(), p.methodInfo(), nullable);
    }

    /** A nullable parameter or result of {@code method} whose type is a type parameter: as above, {@code T?}. */
    private static boolean nullableTypeParameter(ParameterizedType type, MethodInfo method,
                                                 io.codelaser.maddi.cst.api.type.NullableState nullable) {
        return nullable == io.codelaser.maddi.cst.api.type.NullableState.NULLABLE && type.isTypeParameter()
               && type.arrays() == 0 && type.typeParameter().isMethodTypeParameter()
               && (method.isStatic() || method.isConstructor());
    }

    private static OutputBuilder parameter(ParameterInfo p, List<String> wildcardNames, Qualification q) {
        CSharpProgram program = CSharpContext.program();
        String type = wildcardNames != null ? CSharpWildcards.type(p.parameterizedType(), wildcardNames, q)
                : CSharpTypeName.declared(p.parameterizedType(), program.readOnly(p), program.facts().nullable(p), q);
        if (wildcardNames == null && nullableTypeParameter(p, program.facts().nullable(p))) type += "?";
        return new OutputBuilderImpl().add(CSharpAttributes.uses(p.annotations(), LibraryMapping.Target.PARAMETER, q, false)).add(new TextImpl((p.isVarArgs() ? "params " : "") + type + " "
                                                        + CSharpContext.declare(p.name())));
    }
}
