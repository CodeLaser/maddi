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
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.MethodPrinter;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.output.*;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

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
        CSharpContext.enterScope(Set.of()); // the parameters'
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
        // a record's equals(Object) is C#'s Equals(R? other): a record synthesizes Equals(object) (CS0111), which
        // calls it with the other record, or with null
        boolean recordEquals = recordEquals(methodInfo);
        String inheritance = recordEquals ? null : CSharpModifiers.inheritance(methodInfo, typeInfo);
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
        List<String> allTypeParameters = new ArrayList<>(erased ? List.of() : methodInfo.typeParameters().stream()
                .map(CSharpNames::typeParameter).toList());
        allTypeParameters.addAll(captured.typeParameters());
        String typeParameters = allTypeParameters.isEmpty() ? "" : "<" + String.join(", ", allTypeParameters) + ">";
        MethodInfo overridden = CSharpHierarchy.interfaceMethodDefaulted(typeInfo, methodInfo);
        if (overridden != null) {
            // a default method for an inherited interface's method implements it explicitly
            ParameterizedType owner = CSharpHierarchy.implementedAs(typeInfo, overridden.typeInfo());
            Map<TypeParameter, ParameterizedType> map = CSharpHierarchy.typeArguments(owner);
            b.add(new TextImpl(CSharpTypeName.of(CSharpHierarchy.substitute(overridden.returnType(), map), q)))
                    .add(SpaceEnum.ONE)
                    .add(new TextImpl(CSharpTypeName.of(owner, q) + "." + (CSharpNames.translated(overridden.typeInfo())
                            ? CSharpNames.method(overridden) : CSharpNames.method(methodInfo))));
        } else {
            String property = CSharpContext.program().computedProperty(methodInfo);
            if (property != null) return property(b, modifiers.toString(), property, q);
            b.add(new TextImpl(modifiers + returnType(q))).add(SpaceEnum.ONE)
                    .add(new TextImpl(CSharpNames.method(methodInfo) + typeParameters));
        }
        b.add(recordEquals ? recordEqualsParameter(methodInfo, q) : parameters(methodInfo, captured, q));
        // an override and an explicit implementation inherit the constraints, and may not repeat them (CS0460)
        boolean inherited = overridden != null || inheritance != null && inheritance.contains("override");
        List<String> constraints = new ArrayList<>(erased || inherited ? List.of()
                : CSharpTypeName.constraints(methodInfo.typeParameters(), q));
        if (!inherited) constraints.addAll(captured.constraints());
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
        if (CSharpContext.program().erasedGeneric(methodInfo) && methodInfo.returnType().isTypeParameter()) {
            return CSharpTypeName.rawArgument(methodInfo.returnType().typeParameter(), q);
        }
        if (boxedTypeArgument(methodInfo.returnType(), methodInfo, MethodInfo::returnType)) {
            return CSharpTypeName.argument(methodInfo.returnType(), q);
        }
        NullableState nullable = CSharpContext.program().facts().nullable(methodInfo);
        String type = CSharpTypeName.declared(methodInfo.returnType(), false, nullable, q);
        return nullableTypeParameter(methodInfo.returnType(), methodInfo, nullable) ? type + "?" : type;
    }

    /**
     * A boxed type, an enum or a struct ({@link CSharpTypeName#valueReference}) where a method it overrides has a type
     * parameter of its class: the overridden method's
     * {@code T Describe()} in {@code Holder<int>} is {@code int Describe()}, so {@code Integer describe()} is
     * {@code int Describe()}, not {@code int?} (CS0508), and a parameter likewise (CS0115).
     */
    /** A library interface's method a superclass of the program declares abstract: S Next() of IJavaIterator<S>. */
    private static boolean redeclaredAbove(MethodInfo o, TypeInfo owner) {
        return CstQueries.superclasses(owner).stream().anyMatch(s -> CSharpHierarchy.leftAbstract(s).stream()
                .anyMatch(l -> l.method().equals(o)));
    }

    /** {@code method}'s result is printed as the value type itself, an override of {@code T Get()}. */
    static boolean plainResult(MethodInfo method) {
        return boxedTypeArgument(method.returnType(), method, MethodInfo::returnType);
    }

    /** {@code method}'s parameter {@code i} is printed as the value type itself, an override of {@code Set(T t)}. */
    static boolean plainParameter(MethodInfo method, int i) {
        if (i >= method.parameters().size()) return false;
        return boxedTypeArgument(method.parameters().get(i).parameterizedType(), method,
                o -> o.parameters().size() > i ? o.parameters().get(i).parameterizedType() : null);
    }

    private static boolean boxedTypeArgument(ParameterizedType pt, MethodInfo method,
                                             Function<MethodInfo, ParameterizedType> inOverridden) {
        if (!CSharpTypeName.valueReference(pt)) return false;
        // a type of the program: a library's mapping may keep int? as the argument (IJavaEntry<int?, V>); an enum's
        // or a struct's is the value type (IComparer<E>)
        boolean boxed = pt.isBoxedExcludingVoid();
        return method.overrides().stream().filter(o -> o != method && (!boxed || CSharpNames.translated(o.typeInfo())
                                                                       || redeclaredAbove(o, method.typeInfo())))
                .map(inOverridden)
                .anyMatch(o -> o != null && o.arrays() == 0 && o.isTypeParameter() && o.typeParameter() != null
                               && !o.typeParameter().isMethodTypeParameter());
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
            b.add(SpaceEnum.ONE).add(CSharpStatementPrinter.block(statements, CSharpInner.prologue(typeInfo, outer, q), q));
        } else {
            b.add(SpaceEnum.ONE).add(CSharpStatementPrinter.block(statements, q));
        }
    }

    /** An inner class's constructor's parameters: its enclosing instance first, {@code (Outer outer, …)}. */
    private static OutputBuilder outerParameters(MethodInfo methodInfo, Qualification q) {
        OutputBuilder first = new OutputBuilderImpl().add(new TextImpl(CSharpInner.outerTypeName(methodInfo.typeInfo())
                                                                      + " outer"));
        return Stream.concat(Stream.of(first), methodInfo.parameters().stream()
                        .map(p -> parameter(p, null, q)))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                        SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration()));
    }

    private static boolean recordEquals(MethodInfo m) {
        return m.typeInfo().typeNature().isRecord() && !m.isStatic() && "equals".equals(m.name())
               && m.parameters().size() == 1 && m.parameters().getFirst().parameterizedType().isJavaLangObject();
    }

    /** {@code (R? o)}, the record's own type: {@code ?} in a nullable context. */
    private static OutputBuilder recordEqualsParameter(MethodInfo m, Qualification q) {
        TypeInfo record = m.typeInfo();
        String type = CSharpNames.type(record) + (record.typeParameters().isEmpty() ? "" : "<" + String.join(", ",
                record.typeParameters().stream().map(CSharpNames::typeParameter).toList()) + ">");
        String nullable = CSharpContext.program().facts().nullabilityAnalysed() ? "?" : "";
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new TextImpl(type + nullable + " " + CSharpContext.declare(m.parameters().getFirst().name())))
                .add(SymbolEnum.RIGHT_PARENTHESIS);
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
    private static boolean nullableTypeParameter(ParameterInfo p, NullableState nullable) {
        return !p.isVarArgs() && nullableTypeParameter(p.parameterizedType(), p.methodInfo(), nullable);
    }

    /** A nullable parameter or result of {@code method} whose type is a type parameter: as above, {@code T?}. */
    private static boolean nullableTypeParameter(ParameterizedType type, MethodInfo method,
                                                 NullableState nullable) {
        return nullable == NullableState.NULLABLE && type.isTypeParameter()
               && type.arrays() == 0 && type.typeParameter().isMethodTypeParameter()
               && (method.isStatic() || method.isConstructor());
    }

    private static OutputBuilder parameter(ParameterInfo p, List<String> wildcardNames, Qualification q) {
        CSharpProgram program = CSharpContext.program();
        String type;
        if (wildcardNames != null) {
            type = CSharpTypeName.withNullable(p.parameterizedType(),
                    CSharpWildcards.type(p.parameterizedType(), wildcardNames, q), program.facts().nullable(p));
        } else if (boxedTypeArgument(p.parameterizedType(), p.methodInfo(), o -> o.parameters().size() > p.index()
                ? o.parameters().get(p.index()).parameterizedType() : null)) {
            type = CSharpTypeName.argument(p.parameterizedType(), q);
        } else {
            type = CSharpTypeName.declared(p.parameterizedType(), program.readOnly(p), program.facts().nullable(p), q);
            if (program.facts().preconditionNonNull(p) && program.facts().nullable(p) != NullableState.NULLABLE) {
                CSharpContext.message(CSharpPrintCode.PRECONDITION_NON_NULL, p, p.methodInfo().name() + "(" + p.name()
                        + "): non-nullable, the method's precondition");
            }
            if (nullableTypeParameter(p, program.facts().nullable(p))) type += "?";
        }
        return new OutputBuilderImpl().add(CSharpAttributes.uses(p.annotations(), LibraryMapping.Target.PARAMETER, q, false)).add(new TextImpl((p.isVarArgs() ? "params " : "") + type + " "
                                                        + CSharpContext.declare(p.name())));
    }
}
