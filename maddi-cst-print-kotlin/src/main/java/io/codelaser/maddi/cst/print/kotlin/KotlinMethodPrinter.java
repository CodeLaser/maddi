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

import io.codelaser.maddi.cst.print.translate.CstQueries;
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
        if (!methodInfo.overrides().isEmpty() && !interfaceOnlyToArray(methodInfo)) {
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
                                                                             + (nonNullBoundOfTypeVariable(methodInfo, tp) ? " & Any" : "")
                                                                             + (nonNullBound(methodInfo, tp)
                                                                                || methodInfo.overrides().isEmpty()
                                                                                   && KotlinTypeName.nonNullClassTypeParameter(tp) ? " : Any" : ""))))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
                b.add(SymbolEnum.RIGHT_ANGLE_BRACKET).add(SpaceEnum.ONE);
            }
            // List.remove(int) is Kotlin's removeAt: there is no remove(index) to override
            boolean removeAt = !methodInfo.overrides().isEmpty() && KotlinMappedMembers.isRemoveAt(methodInfo)
                               && !KotlinTypePrinter.fromKotlinSource(typeInfo);
            // UnsignedInteger.intValue() overriding Number's: Kotlin's Number declares toInt()
            String numberFunction = !methodInfo.overrides().isEmpty() && !KotlinTypePrinter.fromKotlinSource(typeInfo)
                    ? KotlinMappedMembers.function(methodInfo) : null;
            // guava's ChunksCharSequence.charAt(index) overriding CharSequence's: Kotlin's is the operator get
            boolean indexGet = !methodInfo.overrides().isEmpty() && KotlinMappedMembers.isIndexGet(methodInfo)
                               && !KotlinTypePrinter.fromKotlinSource(typeInfo);
            b.add(new TextImpl(removeAt ? "removeAt" : indexGet ? "get" : numberFunction != null ? numberFunction
                    : KotlinNames.name(methodInfo.name())));
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
                String mapDefault = KotlinMappedMembers.mapDefaultResult(methodInfo, qualification);
                b.add(SymbolEnum.COLON_LABEL).add(new TextImpl(mapDefault != null ? mapDefault
                        : KotlinReadOnly.returns(methodInfo) ? KotlinTypeName.readOnly(rt, qualification)
                        : KotlinTypeName.of(rt, qualification)));
            }
            MethodInfo boundsFrom = genericOverridden(methodInfo);
            String where = KotlinTypeName.whereClause(boundsFrom != null ? boundsFrom.typeParameters()
                    : methodInfo.typeParameters(), qualification);
            if (!where.isEmpty()) b.add(SpaceEnum.ONE).add(new TextImpl(where));
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
                KotlinContext.message(KotlinPrintCode.NULL_OVERRIDE_THROWS, methodInfo,
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
                                KotlinExpressionPrinter.returned(expressionBody, KotlinNullability.returnType(methodInfo),
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
        return nonNullMember ? "UnsupportedOperationException"
                : nonNullOverrideResult(methodInfo) ? "NullPointerException" : null;
    }

    /**
     * An override whose Kotlin result is non-null, as what it overrides declares it, and not a type variable (which
     * takes an unchecked cast): jenkins's LegacySecurityRealm.authenticate, overriding Spring's non-null
     * AuthenticationManager.authenticate, cannot return null.
     */
    static boolean nonNullOverrideResult(MethodInfo m) {
        if (!KotlinContext.translatingJava() || m.isConstructor() || m.overrides().isEmpty()
            || genericOverridden(m) != null) {
            return false;
        }
        ParameterizedType rt = KotlinNullability.returnType(m);
        return rt != null && !rt.isVoidOrJavaLangVoid() && !rt.isTypeParameter() && !rt.isPrimitiveExcludingVoid()
               && !KotlinNullability.isNullable(rt);
    }

    /**
     * A Java {@code super(…)}/{@code this(…)} first statement is Kotlin's delegation in the header:
     * {@code constructor(x: Int) : super(x) { … }}. A class without a primary constructor must have every secondary
     * one delegate to the superclass, so an implicit {@code super()} is written out when the class has a parent.
     */
    private void constructorBody(OutputBuilder b, Block body, Qualification q) {
        List<Statement> statements = body == null ? List.of()
                : CstQueries.own(body.statements());
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
               && (KotlinTypePrinter.isOpen(typeInfo) || overriddenByEnumConstant(typeInfo, methodInfo));
    }

    /**
     * guava's CaseFormat: an enum's constants override its {@code convert} in their bodies. Kotlin's enum class is
     * final, its members too unless they say {@code open}.
     */
    private static boolean overriddenByEnumConstant(io.codelaser.maddi.cst.api.info.TypeInfo typeInfo, MethodInfo methodInfo) {
        if (!typeInfo.typeNature().isEnum()) return false;
        return typeInfo.fields().stream()
                .filter(f -> f.initializer() instanceof io.codelaser.maddi.cst.api.expression.ConstructorCall cc
                             && cc.anonymousClass() != null)
                .map(f -> ((io.codelaser.maddi.cst.api.expression.ConstructorCall) f.initializer()).anonymousClass())
                .anyMatch(body -> body.methods().stream().anyMatch(m -> m.overrides().contains(methodInfo)));
    }

    /** The expression of a single-`return` body (Kotlin `fun … = expr`), or {@code null} for a block body. */
    private static Expression expressionBody(Block body) {
        return CstQueries.returnedValue(CstQueries.own(body.statements()));
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
        String mapDefault = KotlinMappedMembers.mapDefaultParameter(methodInfo, pi.index(), q);
        if (mapDefault != null) return mapDefault;
        ParameterizedType t = KotlinMappedMembers.overriddenParameterType(methodInfo, pi.index());
        if (t == null) return null;
        if (!KotlinMappedMembers.overriddenCollectionParameter(methodInfo, pi.index())) return KotlinTypeName.of(t, q);
        if (t.typeInfo() != null && "java.util.Collection".equals(t.typeInfo().fullyQualifiedName())) {
            // read-only, invariant: Kotlin's MutableCollection<E>.addAll(elements: Collection<E>)
            return "Collection<" + KotlinTypeName.of(t.parameters().getFirst(), q) + ">";
        }
        if (t.typeInfo() != null && "java.util.Map".equals(t.typeInfo().fullyQualifiedName())) {
            // read-only, keys out-projected: Kotlin's MutableMap<K, V>.putAll(from: Map<out K, V>)
            return "Map<out " + KotlinTypeName.of(t.parameters().get(0), q) + ", "
                   + KotlinTypeName.of(t.parameters().get(1), q) + ">";
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
        if (KotlinNullability.verdicts().preconditionNonNull(pi) && !KotlinNullability.isNullable(declared)) {
            KotlinContext.message(KotlinPrintCode.PRECONDITION_NON_NULL, pi, "the NPE moves from "
                    + pi.methodInfo().name() + "'s body to the call");
        }
        if (KotlinNullability.verdicts().rejectsNull(pi) && !KotlinNullability.isNullable(declared)) {
            KotlinContext.message(KotlinPrintCode.NULL_REJECTION_REPLACED, pi, "a Java caller's null meets the entry"
                    + " check's NPE, not " + pi.methodInfo().name() + "'s own exception");
        }
        ParameterizedType type = pi.isVarArgs() ? declared.componentType() : declared; // vararg names: String
        if (typeOverride == null) {
            ParameterizedType generic = genericParameter(pi);
            if (generic != null) {
                typeOverride = KotlinTypeName.inSignature(() -> KotlinTypeName.of(generic.withNullable(declared.nullable()), q));
            }
        }
        if (typeOverride == null) {
            ParameterizedType overridden = erasureOverride(pi);
            if (overridden != null) {
                typeOverride = KotlinTypeName.inSignature(() -> KotlinTypeName.of(overridden.withNullable(declared.nullable()), q));
            }
        }
        java.util.function.Supplier<String> print = () ->
                // before the read-only collection rule, which leaves an array invariant
                covariantArray(pi, type) ? outArray(type, q)
                : readOnlyParameter(pi) ? KotlinTypeName.readOnly(type, q)
                : KotlinTypeName.of(type, q);
        // a constructor's E & Any stays: no override repeats it (guava's Equivalence.Wrapper takes an
        // Equivalence<? super @NonNull T>, which wrap's Equivalence<T> is for an S extends @Nullable T)
        // a member nothing overrides keeps ? super E & Any: guava's static Maps.difference takes an
        // Equivalence<? super @NonNull V>, into a null-marked Equivalence<T : Any>. Not a plain E & Any:
        // ObjectArrays.concat's Class<@NonNull T> would type its newArray result Array<T & Any>
        String printed = typeOverride != null ? typeOverride
                : pi.methodInfo().isConstructor() ? print.get()
                : KotlinTypeName.inSignature(notOverridable(pi.methodInfo()), print);
        ob.add(new TextImpl(KotlinNames.name(pi.name()))).add(SymbolEnum.COLON_LABEL).add(new TextImpl(printed));
        return ob;
    }

    /**
     * The overridden member's parameter type, seen through the class, where Java's override declares a looser one
     * that erases the same: guava's DirectedGraphConnections.removeSuccessor(Object node) overriding
     * removeSuccessor(N node), TreeRangeMap's putAll(RangeMap<Comparable<?>, ?>) overriding putAll(RangeMap<K, ?
     * extends V>). Kotlin's override takes the overridden signature. Null otherwise.
     */
    static ParameterizedType erasureOverride(ParameterInfo pi) {
        MethodInfo m = pi.methodInfo();
        if (!KotlinContext.translatingJava() || m.overrides().isEmpty() || pi.isVarArgs()) return null;
        ParameterizedType own = pi.parameterizedType();
        for (MethodInfo o : m.overrides()) {
            if (!KotlinNullability.translated(o.typeInfo()) || o.parameters().size() <= pi.index()) continue;
            ParameterizedType theirs = o.parameters().get(pi.index()).parameterizedType();
            boolean objectForVariable = own.isJavaLangObject() && own.arrays() == 0 && theirs.typeParameter() != null
                                        && theirs.arrays() == 0 && !theirs.typeParameter().isMethodTypeParameter();
            boolean looserWildcard = own.typeInfo() != null && own.typeInfo() == theirs.typeInfo()
                                     && looserWildcards(own, theirs);
            if (!objectForVariable && !looserWildcard) continue;
            ParameterizedType seen;
            try {
                // with the overridden declaration's type-use @Nullable arguments (RangeMap.merge's ? super @Nullable V)
                seen = KotlinInheritedDefaults.seenThrough(KotlinTypePrinter.declaredArguments(theirs), o.typeInfo(),
                        m.typeInfo().asParameterizedType());
            } catch (RuntimeException | AssertionError e) {
                continue;
            }
            if (seen != null && seen != theirs && (seen.typeParameter() == null
                                                   || KotlinContext.typeParameterInScope(seen.typeParameter()))) {
                return seen;
            }
        }
        return null;
    }

    /**
     * The generic member's parameter type, where the override's takes its type parameters: {@code Class<T>} for
     * jenkins's raw {@code Class type} of {@code convert(Class<T> type, Object value)}. Null otherwise.
     */
    private static ParameterizedType genericParameter(ParameterInfo pi) {
        MethodInfo genericSuper = genericOverridden(pi.methodInfo());
        if (genericSuper == null || genericSuper.parameters().size() <= pi.index()) return null;
        ParameterizedType theirs = genericSuper.parameters().get(pi.index()).parameterizedType();
        ParameterizedType own = pi.parameterizedType();
        return theirs.typeInfo() != null && theirs.typeInfo() == own.typeInfo() && theirs.arrays() == own.arrays()
               && own.parameters().isEmpty() && !theirs.parameters().isEmpty() ? theirs : null;
    }

    /** Same type arguments, but {@code ?} in {@code own} where {@code theirs} has a bounded wildcard. */
    private static boolean looserWildcards(ParameterizedType own, ParameterizedType theirs) {
        if (own.parameters().size() != theirs.parameters().size() || own.parameters().isEmpty()) return false;
        boolean looser = false;
        for (int i = 0; i < own.parameters().size(); i++) {
            ParameterizedType a = own.parameters().get(i);
            ParameterizedType b = theirs.parameters().get(i);
            if (a == null || b == null) return false;
            if (a.isUnboundWildcard() && b.wildcard() != null && !b.isUnboundWildcard()) looser = true;
            else if (a.isUnboundWildcard() != b.isUnboundWildcard()) {
                if (!(b.isTypeParameter() || a.typeInfo() == b.typeInfo())) return false;
            }
        }
        return looser;
    }

    /** {@code Array<out Any?>}: the parameter's own type, element states included, covariant. */
    private static String outArray(ParameterizedType type, Qualification q) {
        String printed = KotlinTypeName.of(type, q);
        return printed.startsWith("Array<") ? "Array<out " + printed.substring("Array<".length()) : printed;
    }

    /**
     * Java's arrays are covariant: an {@code Object[]} parameter takes any reference array, a {@code Type[]} one an
     * array of any subtype's; Kotlin's {@code Array<T>} is invariant and takes no {@code Array<Document?>}
     * ({@code isNullOrEmpty(documents)}), and ties the elements' states of caller and callee both ways (guava's
     * {@code Joiner.join(@Nullable Object[] parts)} made TypeResolver's {@code upperBounds} nullable).
     * {@code Array<out T>} does, where the method only reads the array: it assigns no element, and the array itself
     * is not returned, stored, cloned, nor handed to a translated method's invariant array parameter. Not in an
     * override, whose parameter types are the overridden member's.
     */
    static boolean covariantArray(ParameterInfo pi, ParameterizedType type) {
        MethodInfo m = pi.methodInfo();
        if (!KotlinContext.translatingJava() || pi.isVarArgs() || type.arrays() != 1
            || !m.overrides().isEmpty() || m.methodBody() == null) {
            return false;
        }
        // nor where an override may take it invariant: guava's Hashing.makeHash, overridden in ConcatenatedHashFunction
        boolean closed = m.isConstructor() || m.isStatic() || m.isFinal() || m.access() != null && m.access().isPrivate()
                         || m.typeInfo().isFinal();
        if (!closed) {
            java.util.List<MethodInfo> family = KotlinContext.options().world().family(m);
            if (family == null || family.size() != 1) return false;
        }
        // a primary constructor's parameter that names an array field is that property: guava's
        // CombinedHasher(hashers) keeps Hasher[] hashers, handed on as an invariant Array<Hasher>. Not
        // WildcardTypeImpl(lowerBounds, upperBounds), which keeps ImmutableLists of that name
        if (m.isConstructor() && m.typeInfo().fields().stream()
                .anyMatch(f -> f.name().equals(pi.name()) && f.type().arrays() > 0)) {
            return false;
        }
        ParameterizedType element = type.componentType();
        if (element.isPrimitiveExcludingVoid() || element.typeInfo() == null && element.typeParameter() == null) {
            return false;
        }
        java.util.Set<ParameterInfo> visiting = COVARIANT_IN_PROGRESS.get();
        if (!visiting.add(pi)) return true; // a cycle: each one's other uses are checked where it is entered
        try {
            return onlyReadArray(pi);
        } finally {
            visiting.remove(pi);
        }
    }

    private static final ThreadLocal<java.util.Set<ParameterInfo>> COVARIANT_IN_PROGRESS =
            ThreadLocal.withInitial(() -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    private static boolean onlyReadArray(ParameterInfo pi) {
        boolean[] escapes = {false};
        escapes(pi, (io.codelaser.maddi.cst.api.element.Element) pi.methodInfo().methodBody(), escapes);
        return !escapes[0];
    }

    /** The uses of {@code pi} in {@code body}, and in the bodies of the anonymous classes there, which capture it. */
    private static void escapes(io.codelaser.maddi.cst.api.variable.Variable pi, io.codelaser.maddi.cst.api.element.Element body, boolean[] escapes) {
        // elements.clone() spread into a vararg (guava's ImmutableList.copyOf: construct(elements.clone())): the copy
        // goes where an Array<out T> goes; the call is visited before its arguments
        java.util.Set<io.codelaser.maddi.cst.api.expression.Expression> spreadClones =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        body.visit((io.codelaser.maddi.cst.api.element.Element e) -> {
            if (e instanceof io.codelaser.maddi.cst.api.expression.MethodCall call) {
                spreadClones.addAll(clonesIntoVarargs(pi, call.parameterExpressions(), call.methodInfo()));
            } else if (e instanceof io.codelaser.maddi.cst.api.expression.ConstructorCall call) {
                spreadClones.addAll(clonesIntoVarargs(pi, call.parameterExpressions(), call.constructor()));
            }
            switch (e) {
                case io.codelaser.maddi.cst.api.expression.Assignment a -> {
                    if (a.variableTarget() instanceof io.codelaser.maddi.cst.api.variable.DependentVariable dv
                        && pi.equals(dv.arrayVariable()) || isPi(pi, a.value()) || pi.equals(a.variableTarget())) {
                        escapes[0] = true;
                    }
                }
                case io.codelaser.maddi.cst.api.statement.ReturnStatement rs when isPi(pi, rs.expression()) ->
                        escapes[0] = true;
                case io.codelaser.maddi.cst.api.statement.LocalVariableCreation lvc -> {
                    // guava's ObjectArrays.newArray: T[] empty = reference.length == 0 ? reference : copyOf(…), only
                    // handed on to Arrays.copyOf: the local's own uses decide (RegularImmutableMap's entries[i] = …
                    // makes it escape)
                    lvc.localVariableStream().filter(lv -> isPi(pi, lv.assignmentExpression())).forEach(lv -> {
                        if (!ALIASES_IN_PROGRESS.get().add(lv)) return;
                        try {
                            escapes(lv, body, escapes);
                        } finally {
                            ALIASES_IN_PROGRESS.get().remove(lv);
                        }
                    });
                }
                case io.codelaser.maddi.cst.api.expression.MethodCall mc -> {
                    if (cloneOf(pi, mc) && !spreadClones.contains(mc)) escapes[0] = true;
                    if (!acceptsOut(pi, mc.parameterExpressions(), mc.methodInfo())) escapes[0] = true;
                }
                case ExplicitConstructorInvocation eci -> {
                    // jenkins's UrlValidator: this(schemes, authorityValidator, options, …), whose schemes is
                    // reassigned there, an invariant Array<String>?
                    if (eci.methodInfo() == null && eci.parameterExpressions().stream().anyMatch(x -> isPi(pi, x))
                        || eci.methodInfo() != null && !acceptsOut(pi, eci.parameterExpressions(), eci.methodInfo())) {
                        escapes[0] = true;
                    }
                }
                case io.codelaser.maddi.cst.api.expression.ConstructorCall cc -> {
                    if (cc.constructor() == null && cc.parameterExpressions().stream().anyMatch(x -> isPi(pi, x))
                        || cc.constructor() != null && !acceptsOut(pi, cc.parameterExpressions(), cc.constructor())) {
                        escapes[0] = true;
                    }
                    // guava's fromHashers(hashers): new Hasher() { … makeHash(hashers) … }
                    if (cc.anonymousClass() != null) {
                        cc.anonymousClass().methods().stream().filter(am -> am.methodBody() != null).forEach(am -> escapes(
                                pi, (io.codelaser.maddi.cst.api.element.Element) am.methodBody(), escapes));
                        cc.anonymousClass().fields().stream().filter(f -> f.initializer() != null && !f.initializer().isEmpty())
                                .forEach(f -> escapes(pi, f.initializer(), escapes));
                    }
                }
                default -> {
                }
            }
            return !escapes[0];
        });
    }

    private static final ThreadLocal<java.util.Set<io.codelaser.maddi.cst.api.variable.Variable>> ALIASES_IN_PROGRESS =
            ThreadLocal.withInitial(java.util.HashSet::new);

    private static boolean cloneOf(io.codelaser.maddi.cst.api.variable.Variable pi, io.codelaser.maddi.cst.api.expression.MethodCall mc) {
        return mc.object() != null && isPi(pi, mc.object()) && mc.methodInfo() != null
               && "clone".equals(mc.methodInfo().name()) && mc.parameterExpressions().isEmpty();
    }

    /** The {@code pi.clone()} arguments that are a call's whole vararg array: printed spread, a copy again. */
    private static List<io.codelaser.maddi.cst.api.expression.Expression> clonesIntoVarargs(
            io.codelaser.maddi.cst.api.variable.Variable pi, List<io.codelaser.maddi.cst.api.expression.Expression> args, MethodInfo callee) {
        if (callee == null || callee.parameters().isEmpty() || args.size() != callee.parameters().size()
            || !callee.parameters().getLast().isVarArgs()) {
            return List.of();
        }
        io.codelaser.maddi.cst.api.expression.Expression last = KotlinExpressionPrinter.unwrap(args.getLast());
        return last instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc && cloneOf(pi, mc)
               ? List.of(mc) : List.of();
    }

    // the value is pi, also as a branch of a conditional: guava's RegularImmutableMap.fromEntryArrayCheckingBucketOverflow,
    // entries = n == entryArray.length ? entryArray : createEntryArray(n), an invariant local
    private static boolean isPi(io.codelaser.maddi.cst.api.variable.Variable pi, io.codelaser.maddi.cst.api.expression.Expression e) {
        if (e == null) return false;
        io.codelaser.maddi.cst.api.expression.Expression x = KotlinExpressionPrinter.unwrap(e);
        if (x instanceof io.codelaser.maddi.cst.api.expression.InlineConditional ic) {
            return isPi(pi, ic.ifTrue()) || isPi(pi, ic.ifFalse());
        }
        return x instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve && pi.equals(ve.variable());
    }

    /**
     * Each argument that is {@code pi} lands where an {@code Array<out T>} is accepted: a library method's array
     * parameter (Kotlin's platform {@code Array<(out) T>!}), a vararg (spread), a type variable, or a translated
     * method's array parameter that is itself printed covariant.
     */
    private static boolean acceptsOut(io.codelaser.maddi.cst.api.variable.Variable pi, List<io.codelaser.maddi.cst.api.expression.Expression> args,
                                      MethodInfo callee) {
        for (int i = 0; i < args.size(); i++) {
            if (!isPi(pi, args.get(i))) continue;
            if (callee == null || callee.parameters().isEmpty()) return false;
            ParameterInfo p = callee.parameters().get(Math.min(i, callee.parameters().size() - 1));
            // this.rest = checkNotNull(rest): the array comes back, as what the call returns
            if (passThrough(callee, Math.min(i, callee.parameters().size() - 1))) return false;
            if (p.isVarArgs()) continue;
            if (!KotlinNullability.translated(callee.typeInfo())) continue;
            ParameterizedType t = p.parameterizedType();
            if (t.arrays() == 0 && (t.typeParameter() != null || t.typeInfo() != null && t.typeInfo().isJavaLangObject())) {
                continue;
            }
            if (!covariantArray(p, KotlinNullability.parameterType(p))) return false;
        }
        return true;
    }

    /**
     * A collection parameter the modification analysis proves structurally unmodified
     * ({@link ParameterInfo#isStructurallyUnmodified()}: the collection itself is not modified; its elements may be,
     * which Kotlin's read-only {@code List<T>} allows too): Kotlin's read-only interface, covariant where MutableList
     * is invariant. {@code changeEverywhere(…, List<Statement> statements)} only iterates, and is called with a
     * {@code List<Statement?>} and with a {@code MutableList<Statement>}; only {@code List<Statement?>} takes both.
     * Not without the analysis (unmodified is then false). For a method no other can override and that overrides
     * none, its own verdict; for any other, its override family's in the {@link KotlinWorld} (maddi-mod#25): Kotlin
     * wants an override's parameter types exactly the overridden member's.
     */
    static boolean readOnlyParameter(ParameterInfo pi) {
        return readOnlyOwn(pi) && widerOverloadsReadOnly(pi);
    }

    /**
     * guava's ImmutableIntArray.Builder: {@code addAll(Iterable<Integer>)} passes its argument on in a cast, and stays
     * a MutableIterable; {@code addAll(Collection<Integer>)} would be read-only. Kotlin's Collection is no
     * MutableIterable, so a MutableCollection argument fits both and neither is more specific: an overload's
     * collection parameter is read-only only when the overload taking a supertype there is too.
     */
    private static boolean widerOverloadsReadOnly(ParameterInfo pi) {
        MethodInfo m = pi.methodInfo();
        TypeInfo own = pi.parameterizedType().typeInfo();
        if (own == null || pi.parameterizedType().arrays() != 0) return true;
        return m.typeInfo().methods().stream().filter(o -> o != m && o.name().equals(m.name())
                        && o.parameters().size() == m.parameters().size() && o.isStatic() == m.isStatic())
                .map(o -> o.parameters().get(pi.index()))
                .filter(op -> op.parameterizedType().arrays() == 0 && op.parameterizedType().typeInfo() != null
                        && op.parameterizedType().typeInfo() != own
                        && KotlinTypeName.isMapped(op.parameterizedType().typeInfo().fullyQualifiedName())
                        && isSubtype(own, op.parameterizedType().typeInfo(), new java.util.HashSet<>()))
                // an Iterable prints MutableIterable whatever its verdict (acceptsReadOnly): guava's addAll(Iterable)
                // only reads its values
                .allMatch(op -> !KotlinTypeName.mutableOnly(op.parameterizedType().typeInfo().fullyQualifiedName())
                                && readOnlyParameter(op));
    }

    private static boolean isSubtype(TypeInfo sub, TypeInfo sup, java.util.Set<TypeInfo> visited) {
        if (sub == null || !visited.add(sub)) return false;
        if (sub == sup) return true;
        if (sub.parentClass() != null && isSubtype(sub.parentClass().typeInfo(), sup, visited)) return true;
        return sub.interfacesImplemented().stream().anyMatch(i -> isSubtype(i.typeInfo(), sup, visited));
    }

    private static boolean readOnlyOwn(ParameterInfo pi) {
        MethodInfo m = pi.methodInfo();
        if (!KotlinContext.translatingJava() || pi.isVarArgs() || m.isConstructor()) return false;
        boolean closed = m.isStatic() || m.isFinal() || m.access() != null && m.access().isPrivate()
                         || m.typeInfo().isFinal();
        if (closed && m.overrides().isEmpty()) return readOnlyIn(pi);
        // maddi-mod#25, the family rule: in the code translated together, the override family is complete, and the
        // position is read-only for all of it when each member's parameter there is (Kotlin wants an override's
        // parameter types exactly the overridden member's; callers pass a MutableList to a List either way)
        java.util.List<MethodInfo> family = KotlinContext.options().world().family(m);
        if (family == null) return false;
        KotlinWorld world = KotlinContext.options().world();
        Boolean known = world.readOnly(pi);
        if (known != null) return known;
        // each member's own collection type there: an implementation of Handler<T>.handle(T t) as
        // handle(List<X> xs) overrides it in a class that implements Handler<MutableList<X>>
        boolean verdict = family.stream().allMatch(member -> member.parameters().size() > pi.index()
                && !member.parameters().get(pi.index()).isVarArgs()
                && member.parameters().get(pi.index()).parameterizedType().typeParameter() == null
                && KotlinReadOnly.isCollection(member.parameters().get(pi.index()).parameterizedType())
                && readOnlyIn(member.parameters().get(pi.index())));
        if (READ_ONLY_IN_PROGRESS.get().isEmpty()) {
            family.forEach(member -> world.readOnly(member.parameters().get(pi.index()), verdict));
        }
        return verdict;
    }

    /**
     * One method's parameter: structurally unmodified, and only read in its body (a method without one, abstract, has
     * the analysis' verdict folded over its implementations).
     */
    private static boolean readOnlyIn(ParameterInfo pi) {
        if (!KotlinContext.options().modification().structurallyUnmodified(pi)) return false;
        MethodInfo m = pi.methodInfo();
        if (m.isAbstract() || m.methodBody() == null || m.methodBody().isEmpty() && m.typeInfo().isInterface()) {
            return true;
        }
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
    static boolean acceptsReadOnly(MethodInfo callee, int index) {
        if (callee == null || callee.parameters().isEmpty()) return false;
        ParameterInfo p = callee.parameters().get(Math.min(index, callee.parameters().size() - 1));
        if (index >= callee.parameters().size() - 1 && p.isVarArgs()) {
            // Objects.hash(name, items): an element of Object... takes any collection
            ParameterizedType element = p.parameterizedType().arrays() == 1 ? p.parameterizedType().componentType() : null;
            // jenkins's Iterators.sequence(Iterator<? extends T>... iterators), translated: MutableIterator elements
            if (element != null && element.arrays() == 0 && element.typeInfo() != null
                && KotlinTypeName.mutableOnly(element.typeInfo().fullyQualifiedName())
                && KotlinNullability.translated(callee.typeInfo())) {
                return false;
            }
            return element != null && (element.typeParameter() != null ? element.typeParameter().isMethodTypeParameter()
                    : KotlinTypeName.readOnly(element, null).equals(KotlinTypeName.of(element, null)));
        }
        ParameterizedType type = p.parameterizedType();
        if (type.typeParameter() != null) return type.arrays() == 0 && type.typeParameter().isMethodTypeParameter();
        // guava's ObjectArrays.fillArray(Iterable<?> elements, …): a translated Iterable or Iterator parameter is always
        // Kotlin's MutableIterable, MutableIterator, which no read-only Collection is
        if (type.arrays() == 0 && type.typeInfo() != null && KotlinTypeName.mutableOnly(type.typeInfo().fullyQualifiedName())
            && KotlinNullability.translated(callee.typeInfo())) {
            return false;
        }
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
        java.util.function.Predicate<io.codelaser.maddi.cst.api.element.Element> visitor = e -> {
            if (e instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc && mc.object() != null
                && KotlinExpressionPrinter.unwrap(mc.object()) instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
                && pi.equals(ve.variable()) && !VIEWS.contains(mc.methodInfo().name())) {
                reads.add(ve);
            }
            // a view (map.entrySet(), list.iterator()) is a read where it is iterated or only read in turn: guava's
            // Maps.removeAllKeys removes through map.entrySet().iterator(), which the analysis does not see
            if (e instanceof io.codelaser.maddi.cst.api.expression.MethodCall outer && outer.object() != null
                && outer.methodInfo() != null && !VIEWS.contains(outer.methodInfo().name())
                && KotlinContext.options().modification().structurallyNonModifying(outer.methodInfo())) {
                io.codelaser.maddi.cst.api.expression.VariableExpression viewed = viewOf(pi, outer.object());
                if (viewed != null) reads.add(viewed);
            }
            if (e instanceof io.codelaser.maddi.cst.api.statement.ForEachStatement fe && fe.expression() != null) {
                io.codelaser.maddi.cst.api.expression.VariableExpression viewed = viewOf(pi, fe.expression());
                if (viewed != null) reads.add(viewed);
            }
            if (e instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc && mc.object() != null) {
                io.codelaser.maddi.cst.api.expression.VariableExpression through =
                        passedThrough(pi, KotlinExpressionPrinter.unwrap(mc.object()));
                if (through != null) reads.add(through);
            }
            // ensureNotNull(map, "map"); as a statement: its result goes nowhere
            if (e instanceof io.codelaser.maddi.cst.api.statement.ExpressionAsStatement eas && eas.expression() != null) {
                io.codelaser.maddi.cst.api.expression.VariableExpression through =
                        passedThrough(pi, KotlinExpressionPrinter.unwrap(eas.expression()));
                if (through != null) reads.add(through);
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
            // guava's SetView.minSize(Set<?> set): set instanceof SetView ? ((SetView<?>) set).minSize() : set.size().
            // A test or a cast compiles from the read-only type too; the analysis says nothing modifies the set
            if (e instanceof io.codelaser.maddi.cst.api.expression.InstanceOf io
                && KotlinExpressionPrinter.unwrap(io.expression()) instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
                && pi.equals(ve.variable())) {
                reads.add(ve);
            }
            if (e instanceof io.codelaser.maddi.cst.api.expression.Cast c
                && KotlinExpressionPrinter.unwrap(c.expression()) instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
                && pi.equals(ve.variable())) {
                reads.add(ve);
            }
            if (e instanceof io.codelaser.maddi.cst.api.expression.ConstructorCall cc && cc.constructor() != null) {
                passedOn(pi, cc.parameterExpressions(), cc.constructor(), reads);
            }
            if (e instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve && pi.equals(ve.variable())) {
                uses[0]++;
            }
            return true;
        };
        body.visit(visitor);
        // the methods of an anonymous class capture it too: guava's Maps.removeOnlySet, a ForwardingSet whose
        // delegate() returns the set as its MutableSet
        body.visit((io.codelaser.maddi.cst.api.element.Element e) -> {
            if (e instanceof io.codelaser.maddi.cst.api.expression.ConstructorCall cc && cc.anonymousClass() != null) {
                cc.anonymousClass().methods().stream().filter(am -> am.methodBody() != null)
                        .forEach(am -> am.methodBody().visit(visitor));
            }
            return true;
        });
        return reads.size() == uses[0];
    }

    /** Members whose result is a live view of the collection: modifying it modifies the collection. */
    static final java.util.Set<String> VIEWS = java.util.Set.of("entrySet", "keySet", "values", "iterator",
            "listIterator", "spliterator", "subList", "headSet", "tailSet", "subSet", "headMap", "tailMap", "subMap",
            "descendingSet", "descendingMap", "navigableKeySet", "descendingKeySet", "descendingIterator");

    /** {@code e} is a view call on {@code pi} ({@code map.entrySet()}): the parameter's use inside it. */
    private static io.codelaser.maddi.cst.api.expression.VariableExpression viewOf(
            ParameterInfo pi, io.codelaser.maddi.cst.api.expression.Expression e) {
        if (!(KotlinExpressionPrinter.unwrap(e) instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc)
            || mc.object() == null || mc.methodInfo() == null || !VIEWS.contains(mc.methodInfo().name())) {
            return null;
        }
        return KotlinExpressionPrinter.unwrap(mc.object()) instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
               && pi.equals(ve.variable()) ? ve : null;
    }

    private static void passedOn(ParameterInfo pi, List<io.codelaser.maddi.cst.api.expression.Expression> args,
                                 MethodInfo callee, java.util.Set<io.codelaser.maddi.cst.api.element.Element> reads) {
        for (int i = 0; i < args.size(); i++) {
            io.codelaser.maddi.cst.api.expression.Expression arg = KotlinExpressionPrinter.unwrap(args.get(i));
            if (arg instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
                && pi.equals(ve.variable()) && !passThrough(callee, i) && acceptsReadOnly(callee, i)) {
                reads.add(ve);
            }
            // checkNotNull(unfiltered) passed on: a read where the pass-through's result lands in one
            io.codelaser.maddi.cst.api.expression.VariableExpression through = passedThrough(pi, arg);
            if (through != null && acceptsReadOnly(callee, i)) reads.add(through);
            // new ArrayList<>(map.keySet()): a view handed to a parameter that only reads it
            io.codelaser.maddi.cst.api.expression.VariableExpression viewed = viewOf(pi, arg);
            if (viewed != null && acceptsReadOnly(callee, i)) reads.add(viewed);
        }
    }

    /**
     * {@code <T> T checkNotNull(T reference)}: the argument at {@code index} comes back as the result, so it is a read
     * only where the result is one (guava's Collections2.filter hands checkNotNull(unfiltered) to a constructor that
     * takes a MutableCollection).
     */
    private static boolean passThrough(MethodInfo callee, int index) {
        if (callee == null || index >= callee.parameters().size()) return false;
        ParameterizedType p = callee.parameters().get(index).parameterizedType();
        ParameterizedType rt = callee.returnType();
        return p.typeParameter() != null && p.arrays() == 0 && p.typeParameter().isMethodTypeParameter()
               && rt != null && rt.typeParameter() == p.typeParameter() && rt.arrays() == 0;
    }

    /** {@code e} is a pass-through call of {@code pi}: the parameter's use inside it. */
    private static io.codelaser.maddi.cst.api.expression.VariableExpression passedThrough(
            ParameterInfo pi, io.codelaser.maddi.cst.api.expression.Expression e) {
        if (!(e instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc)) return null;
        List<io.codelaser.maddi.cst.api.expression.Expression> args = mc.parameterExpressions();
        for (int k = 0; k < args.size(); k++) {
            if (KotlinExpressionPrinter.unwrap(args.get(k)) instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
                && pi.equals(ve.variable()) && passThrough(mc.methodInfo(), k)) {
                return ve;
            }
        }
        return null;
    }
    /**
     * The generic member a method without type parameters overrides: Java lets {@code Class<AiServiceCompletedEvent>
     * eventClass()} override {@code <T extends AiServiceEvent> Class<T> eventClass()} (unchecked); Kotlin does not, and
     * the override is printed with the member's type parameters, parameters and result, its values cast (jenkins's
     * raw {@code Object convert(Class type, Object value)} of beanutils's {@code <T> T convert(Class<T>, Object)});
     * null otherwise.
     */
    static MethodInfo genericOverridden(MethodInfo m) {
        if (m.isConstructor() || !m.typeParameters().isEmpty() || m.overrides().isEmpty()) return null;
        if (!m.isAbstract() && m.methodBody() == null) return null;
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
     * Only for an unbounded type parameter; an override takes the bound of what it overrides (it cannot change it), and
     * not when T is a type argument of a parameter: {@code <T> T getOrDefault(T value, Supplier<T> supplier)} returns
     * what the supplier gives, which a {@code Supplier<DocumentSplitter?>} makes nullable.
     */
    /**
     * guava's ForwardingCollection implements Collection without a Java superclass: Kotlin's MutableCollection
     * declares no toArray to override, and a member {@code fun toArray(): Array<Any?>} is Java's toArray on the JVM.
     * Through a Java class (AbstractCollection) it is visible, and overridden as usual.
     */
    static boolean interfaceOnlyToArray(MethodInfo methodInfo) {
        return KotlinContext.translatingJava() && "toArray".equals(methodInfo.name())
               && methodInfo.parameters().size() <= 1 && methodInfo.overrides().stream()
                       .allMatch(o -> o.typeInfo().isInterface() && o.typeInfo().fullyQualifiedName().startsWith("java.util."));
    }

    /**
     * {@code <T extends @NonNull B>} of a nullable-bounded B: {@code T : B & Any}, or no {@code TypeToken<T : Any>}
     * takes a T (guava's TypeToInstanceMap). As the root declaration says, which every override repeats: the
     * implementations of ClassToInstanceMap.getInstance do not all say @NonNull.
     */
    private static boolean nonNullBoundOfTypeVariable(MethodInfo methodInfo, io.codelaser.maddi.cst.api.info.TypeParameter tp) {
        List<ParameterizedType> bounds = tp.typeBounds().stream().filter(b -> !b.isJavaLangObject()).toList();
        if (!KotlinContext.translatingJava() || bounds.size() != 1 || !bounds.getFirst().isTypeParameter()
            || bounds.getFirst().arrays() > 0 || !KotlinTypeName.nullableTypeVariable(bounds.getFirst())) return false;
        int index = methodInfo.typeParameters().indexOf(tp);
        if (index < 0) return false;
        MethodInfo root = methodInfo;
        io.codelaser.maddi.cst.api.info.TypeParameter rootTp = tp;
        for (MethodInfo o : methodInfo.overrides()) {
            if (KotlinNullability.translated(o.typeInfo()) && index < o.typeParameters().size()
                && o.overrides().isEmpty()) {
                root = o;
                rootTp = o.typeParameters().get(index);
            }
        }
        return root.typeParameters().contains(rootTp) && rootTp.typeBounds().stream().anyMatch(b -> b.annotations().stream()
                .anyMatch(ae -> io.codelaser.maddi.cst.impl.analysis.NullAnnotations.NON_NULL.contains(ae.typeInfo().simpleName())));
    }

    /** A constructor, or a static, private or final method that overrides nothing: its signature is its own. */
    private static boolean notOverridable(MethodInfo m) {
        return m.isConstructor() || m.overrides().isEmpty() && (m.isStatic() || m.access() != null && m.access().isPrivate() || m.isFinal()
                                                               || m.typeInfo().isFinal());
    }

    static boolean nonNullBound(MethodInfo methodInfo, io.codelaser.maddi.cst.api.info.TypeParameter tp) {
        if (tp.typeBounds().stream().anyMatch(b -> !b.isJavaLangObject())) return false;
        if (!methodInfo.overrides().isEmpty()) {
            // AllEqualOrdering's sortedCopy overrides Ordering's <E : Any> sortedCopy: Kotlin wants the same bounds,
            // the printer's or the verdicts' (null-marked bounds) of the overridden one, not the override's own
            int index = methodInfo.typeParameters().indexOf(tp);
            return index >= 0 && methodInfo.overrides().stream().anyMatch(o -> KotlinNullability.translated(o.typeInfo())
                    && index < o.typeParameters().size()
                    && (nonNullBound(o, o.typeParameters().get(index))
                        || o.overrides().isEmpty() && KotlinTypeName.nonNullClassTypeParameter(o.typeParameters().get(index))
                        // Ordering's <E extends @NonNull T>, a T & Any; AllEqualOrdering's T is an Any?
                        || nonNullBoundOfTypeVariable(o, o.typeParameters().get(index))));
        }
        ParameterizedType rt = KotlinNullability.returnType(methodInfo);
        if (rt == null || rt.typeParameter() != tp || rt.arrays() > 0 || KotlinNullability.isNullable(rt)) return false;
        if (typeArgumentOfParameter(methodInfo, tp)) return false;
        return methodInfo.parameters().stream().anyMatch(p -> {
            ParameterizedType pt = KotlinNullability.parameterType(p);
            ParameterizedType element = p.isVarArgs() && pt.arrays() > 0 ? pt.componentType() : pt;
            return element.typeParameter() == tp && element.arrays() == 0 && KotlinNullability.isNullable(element);
        });
    }

    /**
     * T is a type argument of one of the method's parameters ({@code Supplier<T>}): a value of T may come from there,
     * as null where T is instantiated nullable.
     */
    static boolean typeArgumentOfParameter(MethodInfo methodInfo, io.codelaser.maddi.cst.api.info.TypeParameter tp) {
        return methodInfo.parameters().stream().anyMatch(p -> p.parameterizedType().parameters().stream()
                .anyMatch(a -> CstQueries.mentions(a, tp)));
    }

}
