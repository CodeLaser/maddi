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
        var patterns = KotlinContext.enterMethod(methodInfo);
        try {
            methodInfo.parameters().forEach(p -> KotlinContext.declared(p.name()));
            return printMethod(qualification);
        } finally {
            KotlinContext.exitMethod(patterns);
        }
    }

    private OutputBuilder printMethod(Qualification qualification) {
        OutputBuilder b = new OutputBuilderImpl();
        if (methodInfo.isStaticInitializer() || methodInfo.isInstanceInitializer()) {
            // a static initializer belongs in the companion object (step 3); until then it reads as an `init`
            return b.add(new TextImpl("init")).add(SpaceEnum.ONE)
                    .add(KotlinStatementPrinter.block(methodInfo.methodBody(), qualification));
        }
        KotlinModifiers.visibility(methodInfo, typeInfo).ifPresent(v -> b.add(v).add(SpaceEnum.ONE));
        if (!methodInfo.overrides().isEmpty()) {
            // an abstract class re-declaring an interface method, to narrow its return type
            if (methodInfo.isAbstract() && !typeInfo.isInterface()) b.add(KeywordImpl.ABSTRACT).add(SpaceEnum.ONE);
            b.add(KotlinKeyword.OVERRIDE).add(SpaceEnum.ONE);
        } else if (methodInfo.isAbstract() && !typeInfo.isInterface()) {
            b.add(KeywordImpl.ABSTRACT).add(SpaceEnum.ONE);
        } else if (isOpen()) {
            b.add(KotlinKeyword.OPEN).add(SpaceEnum.ONE);
        }

        String property = KotlinMappedMembers.overriddenProperty(methodInfo);
        if (methodInfo.isConstructor()) {
            b.add(KotlinKeyword.CONSTRUCTOR);
        } else if (property != null) {
            // Kotlin's member is a property: override val size: Int get() = …
            b.add(new TextImpl("val")).add(SpaceEnum.ONE).add(new TextImpl(property));
        } else {
            b.add(KotlinKeyword.FUN).add(SpaceEnum.ONE);
            if (!methodInfo.typeParameters().isEmpty()) {
                b.add(SymbolEnum.LEFT_ANGLE_BRACKET);
                b.add(methodInfo.typeParameters().stream()
                        .map(tp -> new OutputBuilderImpl().add(new TextImpl(tp.simpleName())))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
                b.add(SymbolEnum.RIGHT_ANGLE_BRACKET).add(SpaceEnum.ONE);
            }
            // List.remove(int) is Kotlin's removeAt: there is no remove(index) to override
            boolean removeAt = !methodInfo.overrides().isEmpty() && KotlinMappedMembers.isRemoveAt(methodInfo)
                               && !KotlinTypePrinter.fromKotlinSource(typeInfo);
            b.add(new TextImpl(removeAt ? "removeAt" : KotlinNames.name(methodInfo.name())));
        }

        if (property == null) b.add(parameters(methodInfo, qualification));

        if (!methodInfo.isConstructor()) {
            ParameterizedType rt = KotlinNullability.returnType(methodInfo);
            if (rt != null && !rt.isVoidOrJavaLangVoid()) {
                b.add(SymbolEnum.COLON_LABEL).add(new TextImpl(KotlinTypeName.of(rt, qualification)));
            }
        }

        if (property != null && !methodInfo.isAbstract()) b.add(SpaceEnum.ONE).add(new TextImpl("get()"));
        if (!methodInfo.isAbstract()) {
            Block body = methodInfo.methodBody();
            if (methodInfo.isConstructor()) {
                constructorBody(b, body, qualification);
                return b;
            }
            List<OutputBuilder> reassigned = KotlinStatementPrinter.reassignedParameters(methodInfo.parameters(), body);
            Expression expressionBody = reassigned.isEmpty() ? expressionBody(body) : null;
            if (!reassigned.isEmpty()) {
                b.add(SpaceEnum.ONE).add(KotlinStatementPrinter.block(reassigned, body.statements(), qualification));
            } else if (expressionBody != null) {
                OutputBuilder cast = KotlinNullability.typeVariableReturn(methodInfo, expressionBody, qualification);
                b.add(SpaceEnum.ONE).add(KotlinSymbols.assignment("=")).add(SpaceEnum.ONE)
                        .add(cast != null ? cast : KotlinNullability.toTarget(expressionBody,
                                KotlinNullability.returnType(methodInfo), KotlinNullability.translated(typeInfo),
                                KotlinExpressionPrinter.widened(expressionBody, KotlinNullability.returnType(methodInfo),
                                        qualification), qualification));
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
                            : KotlinExpressionPrinter.arguments(eci.parameterExpressions(), eci.methodInfo(), q));
            statements = statements.subList(1, statements.size());
        } else if (KotlinTypePrinter.hasWrittenSuperclass(typeInfo)) {
            b.add(SpaceEnum.ONE).add(SymbolEnum.COLON).add(SpaceEnum.ONE).add(KeywordImpl.SUPER)
                    .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        }
        List<OutputBuilder> reassigned = body == null ? List.of()
                : KotlinStatementPrinter.reassignedParameters(methodInfo.parameters(), body);
        if (!statements.isEmpty() || !reassigned.isEmpty()) {
            b.add(SpaceEnum.ONE).add(KotlinStatementPrinter.block(reassigned, statements, q));
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

    /** {@code (a: Int, vararg b: String)}. */
    static OutputBuilder parameters(MethodInfo methodInfo, Qualification q) {
        if (methodInfo.parameters().isEmpty()) return new OutputBuilderImpl().add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        return methodInfo.parameters().stream()
                .map(pi -> parameter(pi, isEqualsOverride(methodInfo) ? "Any?" : mappedParameterType(methodInfo, pi, q), q))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                        SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration()));
    }

    /** Kotlin's parameter type for an override of a mapped collection member: {@code E}, {@code Collection<E>}. */
    private static String mappedParameterType(MethodInfo methodInfo, ParameterInfo pi, Qualification q) {
        ParameterizedType t = KotlinMappedMembers.overriddenParameterType(methodInfo, pi.index());
        if (t == null) return null;
        if (t.typeInfo() != null && "java.util.Collection".equals(t.typeInfo().fullyQualifiedName())) {
            // read-only, invariant: Kotlin's MutableCollection<E>.addAll(elements: Collection<E>)
            return "Collection<" + KotlinTypeName.of(t.parameters().getFirst(), q) + ">";
        }
        return KotlinTypeName.of(t, q);
    }

    /** The {@code super(…)}/{@code this(…)} a constructor body starts with; null when it has none. */
    static ExplicitConstructorInvocation explicitConstructorInvocation(MethodInfo constructor) {
        Block body = constructor.methodBody();
        if (body == null) return null;
        return body.statements().stream().filter(s -> !s.isSynthetic()).findFirst()
                .filter(s -> s instanceof ExplicitConstructorInvocation)
                .map(s -> (ExplicitConstructorInvocation) s).orElse(null);
    }

    /**
     * {@code equals(Object)}: Kotlin declares {@code Any.equals(other: Any?)}, and an override must take the same
     * parameter type. (A signature Kotlin fixes; what other parameters may be null is not decided here.)
     */
    private static boolean isEqualsOverride(MethodInfo methodInfo) {
        return "equals".equals(methodInfo.name()) && methodInfo.parameters().size() == 1 && !methodInfo.isStatic()
               && methodInfo.parameters().getFirst().parameterizedType().isJavaLangObject()
               && !KotlinTypePrinter.fromKotlinSource(methodInfo.typeInfo());
    }

    private static OutputBuilder parameter(ParameterInfo pi, String typeOverride, Qualification q) {
        OutputBuilder ob = new OutputBuilderImpl();
        if (pi.isVarArgs()) ob.add(new TextImpl("vararg")).add(SpaceEnum.ONE);
        ParameterizedType declared = KotlinNullability.parameterType(pi);
        ParameterizedType type = pi.isVarArgs() ? declared.componentType() : declared; // vararg names: String
        ob.add(new TextImpl(KotlinNames.name(pi.name()))).add(SymbolEnum.COLON_LABEL)
                .add(new TextImpl(typeOverride != null ? typeOverride : KotlinTypeName.of(type, q)));
        return ob;
    }
}
