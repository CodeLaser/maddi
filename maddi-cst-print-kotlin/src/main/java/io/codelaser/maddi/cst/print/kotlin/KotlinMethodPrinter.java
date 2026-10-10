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
import io.codelaser.maddi.cst.api.expression.NullConstant;
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
        b.add(KotlinAnnotations.print(methodInfo.annotations(), qualification));
        java.util.Optional<io.codelaser.maddi.cst.api.output.element.Keyword> visibility =
                KotlinModifiers.visibility(methodInfo, typeInfo);
        if (KotlinContext.translatingJava() && methodInfo.isStatic() && visibility.isEmpty()) {
            // in the companion object: Java code calls it as C.m(), not C.Companion.m()
            b.add(new TextImpl("@JvmStatic")).add(SpaceEnum.ONE);
        }
        visibility.ifPresent(v -> b.add(v).add(SpaceEnum.ONE));
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
            MethodInfo genericSuper = genericOverridden(methodInfo);
            List<io.codelaser.maddi.cst.api.info.TypeParameter> typeParameters = genericSuper != null
                    ? genericSuper.typeParameters() : methodInfo.typeParameters();
            if (!typeParameters.isEmpty()) {
                b.add(SymbolEnum.LEFT_ANGLE_BRACKET);
                b.add(typeParameters.stream()
                        .map(tp -> new OutputBuilderImpl().add(new TextImpl(KotlinTypeName.typeParameter(tp, qualification)
                                                                             + (nonNullBound(methodInfo, tp) ? " : Any" : ""))))
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
            MethodInfo genericSuper = genericOverridden(methodInfo);
            ParameterizedType rt = KotlinNullability.returnType(genericSuper != null ? genericSuper : methodInfo);
            // the body throws instead of returning null: the override keeps Kotlin's non-null result
            if (rt != null && !methodInfo.isAbstract() && methodInfo.methodBody() != null
                && nullOverride(expressionBody(methodInfo.methodBody())) != null) {
                rt = rt.withNullable(io.codelaser.maddi.cst.api.type.NullableState.NONNULL);
            }
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
            List<OutputBuilder> reassigned = KotlinStatementPrinter.reassignedParameters(methodInfo.parameters(), body, qualification);
            Expression expressionBody = reassigned.isEmpty() ? expressionBody(body) : null;
            if (!reassigned.isEmpty()) {
                b.add(SpaceEnum.ONE).add(KotlinStatementPrinter.block(reassigned, body.statements(), qualification));
            } else if (nullOverride(expressionBody) != null) {
                // Kotlin's member returns non-null, so `return null` cannot be: a DELIBERATE behaviour change, the caller
                // gets the exception instead of the null (fernflower's InstructionSequence.clone(), "to be overwritten")
                KotlinContext.message(KotlinPrintMessage.Code.NULL_OVERRIDE_THROWS, methodInfo,
                        typeInfo.simpleName() + "." + methodInfo.name() + "()");
                b.add(SpaceEnum.ONE).add(KotlinSymbols.assignment("=")).add(SpaceEnum.ONE)
                        .add(new TextImpl("throw " + nullOverride(expressionBody) + "()"));
            } else if (expressionBody != null && genericOverridden(methodInfo) != null) {
                // Class<AiServiceCompletedEvent> overriding <T extends AiServiceEvent> Class<T>: Java's unchecked
                // override, Kotlin's unchecked cast
                MethodInfo genericSuper = genericOverridden(methodInfo);
                b.add(SpaceEnum.ONE).add(KotlinSymbols.assignment("=")).add(SpaceEnum.ONE)
                        .add(SymbolEnum.LEFT_PARENTHESIS)
                        .add(KotlinExpressionPrinter.widened(expressionBody, null, qualification))
                        .add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE).add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                        .add(new TextImpl(KotlinTypeName.of(KotlinNullability.returnType(genericSuper), qualification)));
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
     * {@code return null;} as the whole body of an override whose Kotlin member has no nullable result: the exception
     * the printed body throws instead, or null. {@code clone()} (Cloneable's returns Any), and the collection members
     * Kotlin types non-null: {@code Map.Entry.setValue} (an entry that cannot be set throws
     * UnsupportedOperationException by Java's own contract), {@code Iterator.next}, {@code List.get} and {@code set}.
     */
    private String nullOverride(Expression expressionBody) {
        if (methodInfo.isConstructor() || methodInfo.overrides().isEmpty() || expressionBody == null
            || !(KotlinExpressionPrinter.unwrap(expressionBody) instanceof NullConstant)) {
            return null;
        }
        if ("clone".equals(methodInfo.name()) && methodInfo.parameters().isEmpty()) return "CloneNotSupportedException";
        boolean nonNullMember = methodInfo.overrides().stream().anyMatch(m -> switch (m.typeInfo().fullyQualifiedName()) {
            case "java.util.Map.Entry" -> "setValue".equals(m.name());
            case "java.util.Iterator", "java.util.ListIterator" -> "next".equals(m.name()) || "previous".equals(m.name());
            case "java.util.List" -> "get".equals(m.name()) || "set".equals(m.name());
            default -> false;
        });
        return nonNullMember ? "UnsupportedOperationException" : null;
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
                : KotlinStatementPrinter.reassignedParameters(methodInfo.parameters(), body, q);
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
        // @JsonProperty("text") text: String -- Jackson's creator, JUnit's @TempDir, a tool's @P read them
        OutputBuilder ob = new OutputBuilderImpl().add(KotlinAnnotations.print(pi.annotations(), q));
        if (pi.isVarArgs()) ob.add(new TextImpl("vararg")).add(SpaceEnum.ONE);
        ParameterizedType declared = KotlinNullability.parameterType(pi);
        ParameterizedType type = pi.isVarArgs() ? declared.componentType() : declared; // vararg names: String
        String printed = typeOverride != null ? typeOverride
                : readOnlyParameter(pi) ? KotlinTypeName.readOnly(type, q) : KotlinTypeName.of(type, q);
        ob.add(new TextImpl(KotlinNames.name(pi.name()))).add(SymbolEnum.COLON_LABEL).add(new TextImpl(printed));
        return ob;
    }

    /**
     * A collection parameter the modification analysis proves unmodified ({@link ParameterInfo#isUnmodified()}):
     * Kotlin's read-only interface, covariant where MutableList is invariant. {@code changeEverywhere(…,
     * List<Statement> statements)} only iterates, and is called with a {@code List<Statement?>} and with a
     * {@code MutableList<Statement>}; only {@code List<Statement?>} takes both. Not without the analysis (unmodified
     * is then false), and only for a method no other can override and that overrides none: Kotlin wants an
     * override's parameter types exactly the overridden member's.
     */
    private static boolean readOnlyParameter(ParameterInfo pi) {
        MethodInfo m = pi.methodInfo();
        if (!KotlinContext.translatingJava() || pi.isVarArgs() || m.isConstructor() || !m.overrides().isEmpty()) return false;
        boolean closed = m.isStatic() || m.isFinal() || m.access() != null && m.access().isPrivate()
                         || m.typeInfo().isFinal();
        if (!closed || !pi.isUnmodified()) return false;
        // a cycle (fromMap passes map to rawFallback, which passes it back) is assumed read-only: every parameter on it
        // is unmodified, and each one's other uses are checked where the cycle is entered
        java.util.Set<ParameterInfo> visiting = READ_ONLY_IN_PROGRESS.get();
        if (!visiting.add(pi)) return true;
        try {
            return onlyRead(pi, m.methodBody());
        } finally {
            visiting.remove(pi);
        }
    }

    private static final ThreadLocal<java.util.Set<ParameterInfo>> READ_ONLY_IN_PROGRESS =
            ThreadLocal.withInitial(() -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    /**
     * Can the argument at {@code index} of a call to {@code callee} be a read-only collection? A parameter that is
     * not a collection takes one ({@code Object}; the callee's own {@code T}: {@code ensureNotNull(map, "map")}), and
     * so does a library method's collection parameter, a platform type {@code (Mutable)Map<K, V>!}; a translated
     * method's collection parameter only when it is itself printed read-only. Not a type variable of the receiver's:
     * {@code ThreadLocal<MutableMap<…>>.set(value: T)} takes the mutable type it was declared with.
     */
    private static boolean acceptsReadOnly(MethodInfo callee, int index) {
        if (callee == null || callee.parameters().isEmpty()) return false;
        ParameterInfo p = callee.parameters().get(Math.min(index, callee.parameters().size() - 1));
        if (index >= callee.parameters().size() - 1 && p.isVarArgs()) return false;
        ParameterizedType type = p.parameterizedType();
        if (type.typeParameter() != null) return type.arrays() == 0 && type.typeParameter().isMethodTypeParameter();
        if (KotlinTypeName.readOnly(type, null).equals(KotlinTypeName.of(type, null))) return true; // not a collection
        return !KotlinNullability.translated(callee.typeInfo()) || readOnlyParameter(p);
    }

    /**
     * Every use of {@code pi} reads it: the receiver of a call ({@code statements.size()}) or what a for-each
     * iterates. Unmodified is not enough: returned, passed on or assigned, the read-only List lands where a
     * MutableList is declared ({@code return items;} from a method that returns one).
     */
    private static boolean onlyRead(ParameterInfo pi, Block body) {
        if (body == null) return false;
        java.util.Set<io.codelaser.maddi.cst.api.element.Element> reads =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        int[] uses = {0};
        body.visit((io.codelaser.maddi.cst.api.element.Element e) -> {
            if (e instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc && mc.object() != null
                && KotlinExpressionPrinter.unwrap(mc.object()) instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
                && pi.equals(ve.variable())) {
                reads.add(ve);
            }
            if (e instanceof io.codelaser.maddi.cst.api.statement.ForEachStatement fe && fe.expression() != null
                && KotlinExpressionPrinter.unwrap(fe.expression()) instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
                && pi.equals(ve.variable())) {
                reads.add(ve);
            }
            // passed on to a parameter that takes a read-only collection: isRepresentable(map, KEYS), rawFallback(map)
            if (e instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc) {
                passedOn(pi, mc.parameterExpressions(), mc.methodInfo(), reads);
            }
            if (e instanceof io.codelaser.maddi.cst.api.expression.ConstructorCall cc && cc.constructor() != null) {
                passedOn(pi, cc.parameterExpressions(), cc.constructor(), reads);
            }
            if (e instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve && pi.equals(ve.variable())) {
                uses[0]++;
            }
            return true;
        });
        return reads.size() == uses[0];
    }

    private static void passedOn(ParameterInfo pi, List<io.codelaser.maddi.cst.api.expression.Expression> args,
                                 MethodInfo callee, java.util.Set<io.codelaser.maddi.cst.api.element.Element> reads) {
        for (int i = 0; i < args.size(); i++) {
            if (KotlinExpressionPrinter.unwrap(args.get(i)) instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
                && pi.equals(ve.variable()) && acceptsReadOnly(callee, i)) {
                reads.add(ve);
            }
        }
    }
    /**
     * The generic member a method without type parameters overrides: Java lets {@code Class<AiServiceCompletedEvent>
     * eventClass()} override {@code <T extends AiServiceEvent> Class<T> eventClass()} (unchecked); Kotlin does not, and
     * the override is printed with the member's type parameters and result, its value cast. Only an abstract method
     * or one whose body is a single expression, which takes the cast; null otherwise.
     */
    private MethodInfo genericOverridden(MethodInfo m) {
        if (m.isConstructor() || !m.typeParameters().isEmpty() || m.overrides().isEmpty()) return null;
        if (!m.isAbstract() && (m.methodBody() == null || expressionBody(m.methodBody()) == null)) return null;
        return m.overrides().stream().filter(o -> !o.typeParameters().isEmpty() && mentionsOwnTypeParameter(o))
                .findFirst().orElse(null);
    }

    private static boolean mentionsOwnTypeParameter(MethodInfo o) {
        ParameterizedType rt = o.returnType();
        if (rt == null) return false;
        java.util.Set<io.codelaser.maddi.cst.api.info.TypeParameter> own = new java.util.HashSet<>(o.typeParameters());
        java.util.ArrayDeque<ParameterizedType> todo = new java.util.ArrayDeque<>(List.of(rt));
        while (!todo.isEmpty()) {
            ParameterizedType t = todo.pop();
            if (t.typeParameter() != null && own.contains(t.typeParameter())) return true;
            todo.addAll(t.parameters());
        }
        return false;
    }

    /**
     * {@code <T> T ensureNotNull(T object, String name)}: the parameter is nullable, the result is not, and both are
     * T. Kotlin's unbounded {@code <T>} is {@code T : Any?}, so a nullable argument makes T nullable and the "non-null"
     * result with it: {@code this.metadata = ensureNotNull(metadata, "metadata")} did not type-check against a
     * {@code Metadata}. Bounded {@code T : Any}, T is the argument's non-null type and {@code T?} the parameter's.
     * Only for an unbounded type parameter of a method that overrides nothing (an override cannot change bounds).
     */
    static boolean nonNullBound(MethodInfo methodInfo, io.codelaser.maddi.cst.api.info.TypeParameter tp) {
        if (!methodInfo.overrides().isEmpty() || tp.typeBounds().stream().anyMatch(b -> !b.isJavaLangObject())) {
            return false;
        }
        ParameterizedType rt = KotlinNullability.returnType(methodInfo);
        if (rt == null || rt.typeParameter() != tp || rt.arrays() > 0 || KotlinNullability.isNullable(rt)) return false;
        return methodInfo.parameters().stream().anyMatch(p -> {
            ParameterizedType pt = KotlinNullability.parameterType(p);
            ParameterizedType element = p.isVarArgs() && pt.arrays() > 0 ? pt.componentType() : pt;
            return element.typeParameter() == tp && element.arrays() == 0 && KotlinNullability.isNullable(element);
        });
    }

}
