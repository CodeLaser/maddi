/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ForEachStatement;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Kotlin's read-only collection types for fields and for the results of methods (maddi-mod#25's structural verdict,
 * the step after the parameters of {@link KotlinMethodPrinter}). A read-only {@code List<T>} is covariant where
 * {@code MutableList<T>} is invariant, but it must agree with every expression that reads it, so the reads must all be
 * in sight: those of a private field or a closed method in its compilation unit, those of any member of a type in the
 * {@link KotlinWorld} (the code translated together) in the world's types that mention it.
 * <ul>
 *   <li>a collection field the analysis proves structurally unmodified ({@link FieldInfo#isStructurallyUnmodified()}),
 *   or one only ever given immutable values ({@code List.copyOf(x)}, maddi#141),
 *   is a {@code List} when each read of it is the receiver of a call that adds and removes nothing (the stored
 *   verdict, or the library's {@code @NotModified}), the collection of a
 *   for-each, an operand of {@code ==} or a string concatenation, an argument to a parameter that takes a read-only
 *   collection, the value assigned to another read-only field, or the value returned by a read-only method.
 *   Assignments to it are no reads: a {@code MutableList} is a {@code List}. A call the analysis proves
 *   {@code @Identity} ({@code ensureNotNull(builder.variables, …)}) carries the field's value: its result is judged
 *   where it is used, as a read of the field;</li>
 *   <li>a method returns a {@code List} when each of its {@code return}s returns such a field or an immutable value
 *   (an abstract one has none to check), and each call of it is read as the field is (an override may still return
 *   a {@code MutableList}: Kotlin's results are covariant). A caller that changed the result would have changed the
 *   field, which then is not structurally unmodified. An override does so only where each member it overrides
 *   returns a {@code List} too, or no collection at all ({@code Object}), maddi-mod#25: a {@code List} is no
 *   {@code MutableList}.</li>
 * </ul>
 * The two depend on each other ({@code items} is read-only when {@code items()} is, which is when {@code items} is):
 * a cycle is assumed read-only, and each member's other uses are checked where it is entered, as for parameters.
 */
final class KotlinReadOnly {

    private static final ThreadLocal<Set<Object>> IN_PROGRESS =
            ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));
    // verdicts reached inside a cycle: read-only ones hold once the cycle's root is read-only too
    private static final ThreadLocal<Set<Object>> PROVISIONAL =
            ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));
    // members found not read-only while some cycle is open: that holds whatever the cycle assumes
    private static final ThreadLocal<Set<Object>> REFUSED =
            ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    private KotlinReadOnly() {
    }

    /** {@code type} is a collection type Kotlin has a read-only interface for. */
    static boolean isCollection(ParameterizedType type) {
        return type != null && type.arrays() == 0 && type.typeInfo() != null
               && !KotlinTypeName.readOnly(type, null).equals(KotlinTypeName.of(type, null));
    }

    static boolean field(FieldInfo fieldInfo) {
        if (!KotlinContext.translatingJava() || fieldInfo.isSynthetic() || fieldInfo.access() == null
            || !fieldInfo.access().isPrivate() && !inWorld(fieldInfo.owner())
            || KotlinTypePrinter.fromKotlinSource(fieldInfo.owner())
            || !isCollection(KotlinNullability.fieldType(fieldInfo)) || inPrimaryConstructor(fieldInfo)) {
            return false;
        }
        boolean unmodified = KotlinContext.options().modification().structurallyUnmodified(fieldInfo);
        if (!unmodified && !bornImmutable(fieldInfo)) return false;
        return decided(fieldInfo, () -> onlyRead(fieldInfo, fieldInfo.owner()));
    }

    /**
     * Every value the field is given is an immutable value, or null (#141): {@code this.items = List.copyOf(x)}.
     * Nothing can change it then, whatever the analysis concluded about its uses; the reads still decide.
     */
    private static boolean bornImmutable(FieldInfo fieldInfo) {
        List<Expression> values = new java.util.ArrayList<>();
        if (fieldInfo.initializer() != null && !fieldInfo.initializer().isEmpty()) values.add(fieldInfo.initializer());
        KotlinWorld world = KotlinContext.options().world();
        List<TypeInfo> writers = world.contains(fieldInfo.owner().primaryType()) ? world.mentioning(fieldInfo)
                : List.of(fieldInfo.owner().primaryType());
        writers.forEach(writer -> forEachBody(writer, (method, body) -> body.visit((Element e) -> {
            if (e instanceof Assignment a && a.variableTarget() instanceof FieldReference fr
                && fr.fieldInfo() == fieldInfo) {
                values.add(a.value());
            }
            return true;
        })));
        return !values.isEmpty() && values.stream().allMatch(KotlinReadOnly::immutableValue);
    }

    // the values that land in a read-only position, for the primary type being printed: computed once per type
    private static final ThreadLocal<Object[]> BOUND = new ThreadLocal<>();

    /**
     * {@code e} lands where a read-only collection is declared: the initializer of, or a value assigned to, a
     * read-only field; a value a read-only method returns; an argument to a collection parameter that takes a
     * read-only one (not a type variable's, whose type comes back elsewhere). Through the branches of a
     * conditional and the right of an elvis. There {@code List.of(…)} may print as {@code listOf(…)} (#141).
     */
    static boolean boundToReadOnly(Expression e) {
        TypeInfo current = KotlinContext.currentType();
        if (current == null || !KotlinContext.translatingJava()) return false;
        TypeInfo primary = current.primaryType();
        Object[] cached = BOUND.get();
        if (cached == null || cached[0] != primary) {
            cached = new Object[]{primary, boundValues(primary)};
            BOUND.set(cached);
        }
        @SuppressWarnings("unchecked")
        Set<Expression> bound = (Set<Expression>) cached[1];
        return bound.contains(unwrapped(e));
    }

    private static Set<Expression> boundValues(TypeInfo primary) {
        Set<Expression> bound = Collections.newSetFromMap(new IdentityHashMap<>());
        forEachType(primary, type -> {
            for (FieldInfo f : type.fields()) {
                if (f.initializer() != null && !f.initializer().isEmpty() && field(f)) addValue(bound, f.initializer());
            }
        });
        forEachBody(primary, (method, body) -> {
            boolean readOnlyResult = method != null && body == method.methodBody() && returns(method);
            body.visit((Element e) -> {
                if (e instanceof Lambda) return false; // its returns are the lambda's
                switch (e) {
                    case Assignment a when a.variableTarget() instanceof FieldReference fr && field(fr.fieldInfo()) ->
                            addValue(bound, a.value());
                    case ReturnStatement rs when readOnlyResult && rs.expression() != null -> addValue(bound, rs.expression());
                    case MethodCall mc when mc.methodInfo() != null -> arguments(bound, mc.parameterExpressions(), mc.methodInfo());
                    case ConstructorCall cc when cc.constructor() != null ->
                            arguments(bound, cc.parameterExpressions(), cc.constructor());
                    default -> {
                    }
                }
                return true;
            });
        });
        return bound;
    }

    private static void arguments(Set<Expression> bound, List<Expression> args, MethodInfo callee) {
        for (int i = 0; i < args.size() && i < callee.parameters().size(); i++) {
            io.codelaser.maddi.cst.api.info.ParameterInfo p = callee.parameters().get(i);
            if (p.isVarArgs() || p.parameterizedType().typeParameter() != null
                || !isCollection(p.parameterizedType())) {
                continue;
            }
            if (KotlinMethodPrinter.acceptsReadOnly(callee, i)) addValue(bound, args.get(i));
        }
    }

    private static void addValue(Set<Expression> bound, Expression value) {
        Expression u = unwrapped(value);
        bound.add(u);
        if (u instanceof InlineConditional ic) {
            KotlinNullCheckShapes.Elvis elvis = KotlinNullCheckShapes.elvis(ic);
            if (elvis != null) {
                addValue(bound, elvis.otherwise());
            } else {
                addValue(bound, ic.ifTrue());
                addValue(bound, ic.ifFalse());
            }
        }
    }

    private static void forEachType(TypeInfo typeInfo, java.util.function.Consumer<TypeInfo> consumer) {
        consumer.accept(typeInfo);
        typeInfo.subTypes().forEach(t -> forEachType(t, consumer));
    }

    /**
     * An immutable value (at least immutable-HC) or an unmodifiable view, or null: a call whose result is one
     * ({@link io.codelaser.maddi.cst.impl.analysis.ModificationFacts#immutableResult},
     * {@link io.codelaser.maddi.cst.impl.analysis.ModificationFacts#unmodifiableResult}). Either way nothing is added
     * to or removed from it through this reference, which is what Kotlin's read-only type says.
     */
    static boolean immutableValue(Expression e) {
        Expression u = unwrapped(e);
        return switch (u) {
            case NullConstant nc -> true;
            case MethodCall mc -> mc.methodInfo() != null
                                  && (KotlinContext.options().modification().immutableResult(mc.methodInfo())
                                      || KotlinContext.options().modification().unmodifiableResult(mc.methodInfo()));
            case InlineConditional ic -> immutableValue(ic.ifTrue()) && immutableValue(ic.ifFalse());
            default -> false;
        };
    }

    static boolean returns(MethodInfo methodInfo) {
        if (!KotlinContext.translatingJava() || methodInfo.isConstructor()
            || KotlinTypePrinter.fromKotlinSource(methodInfo.typeInfo())
            || !isCollection(KotlinNullability.returnType(methodInfo))) {
            return false;
        }
        boolean bodiless = methodInfo.isAbstract() || methodInfo.methodBody() == null;
        if (bodiless && !methodInfo.isAbstract()) return false;
        if (!methodInfo.overrides().isEmpty()) {
            // maddi-mod#25: Kotlin's results are covariant, so a List may override a List or an Any, never a
            // MutableList; nor a type variable's result, which the subtype may have made a MutableList. An override
            // of a generic member prints that member's result type (KotlinMethodPrinter.genericOverridden).
            boolean overridable = methodInfo.overrides().stream().allMatch(o -> o.typeParameters().isEmpty()
                    && o.returnType() != null && o.returnType().typeParameter() == null
                    && (!isCollection(o.returnType()) || inWorld(o.typeInfo()) && returns(o)));
            if (!overridable || !inWorld(methodInfo.typeInfo())) return false;
        }
        // closed: the callers of an open member are all over the code base, and a caller in another file that hands
        // the result to a MutableList parameter or field is out of this file's sight (as a closed one's may be too,
        // but far less often: on langchain4j-core, opening it turned 6 read-only declarations into 34, and cost 2
        // compiling files there and 1 on fernflower)
        boolean closed = methodInfo.isStatic() || methodInfo.isFinal()
                         || methodInfo.access() != null && methodInfo.access().isPrivate()
                         || methodInfo.typeInfo().isFinal();
        if (!closed && !inWorld(methodInfo.typeInfo())) return false;
        return decided(methodInfo, () -> {
            if (bodiless) return onlyRead(methodInfo, methodInfo.typeInfo());
            List<ReturnStatement> returns = returnStatements(methodInfo);
            return !returns.isEmpty() && returns.stream().allMatch(rs -> rs.expression() != null
                    && (KotlinExpressionPrinter.unwrap(rs.expression()) instanceof VariableExpression ve
                        && ve.variable() instanceof FieldReference fr && field(fr.fieldInfo())
                        || immutableValue(rs.expression())))
                   && onlyRead(methodInfo, methodInfo.typeInfo());
        });
    }

    /** All code that uses the type's members is translated with it ({@link KotlinWorld}), whatever their access. */
    private static boolean inWorld(TypeInfo typeInfo) {
        return KotlinContext.options().world().contains(typeInfo.primaryType());
    }

    /**
     * The verdict on a field or method: a cycle through it is assumed read-only, and checked where it is entered.
     * The assumption only ever widens what is accepted, so a member found not read-only inside a cycle is not
     * read-only, and is remembered at once. One found read-only depends on the open assumptions: it is remembered
     * for the cycle while no member turns out not read-only (which may have been assumed), and for the world when
     * the cycle's root is read-only. Without that each member of a long chain of fields assigned to each other was
     * decided again for each path to it: guava's printing did not finish (maddi-mod#50).
     */
    private static boolean decided(Object member, java.util.function.BooleanSupplier compute) {
        KotlinWorld world = KotlinContext.options().world();
        Boolean known = world.readOnly(member);
        if (known != null) return known;
        Set<Object> visiting = IN_PROGRESS.get();
        Set<Object> provisional = PROVISIONAL.get();
        Set<Object> refused = REFUSED.get();
        if (refused.contains(member)) return false;
        if (provisional.contains(member) || !visiting.add(member)) return true;
        boolean result;
        try {
            result = compute.getAsBoolean();
        } finally {
            visiting.remove(member);
        }
        if (!result) {
            world.readOnly(member, false);
            refused.add(member);
            provisional.clear();
        } else if (visiting.isEmpty()) {
            world.readOnly(member, true);
            provisional.forEach(m -> world.readOnly(m, true));
            provisional.clear();
        } else {
            provisional.add(member);
        }
        if (visiting.isEmpty()) refused.clear();
        return result;
    }

    /** A property of the primary constructor's header, which prints Java's type. */
    private static boolean inPrimaryConstructor(FieldInfo fieldInfo) {
        return fieldInfo.owner().constructors().stream().flatMap(c -> c.parameters().stream())
                .anyMatch(p -> KotlinTypePrinter.propertyOf(p) == fieldInfo);
    }

    /** The method's own {@code return}s: not those of its lambdas. */
    private static List<ReturnStatement> returnStatements(MethodInfo methodInfo) {
        List<ReturnStatement> returns = new java.util.ArrayList<>();
        ((Element) methodInfo.methodBody()).visit((Element e) -> {
            if (e instanceof Lambda) return false;
            if (e instanceof ReturnStatement rs) returns.add(rs);
            return true;
        });
        return returns;
    }

    /**
     * Each read of the field, or call of the method, is one a read-only type allows: in the world's code that
     * mentions it when its type is translated with the world, else in its own compilation unit. The reads are the
     * field's expressions (the method's calls and references) and the {@code @Identity} calls on them, whose results
     * carry the value; the argument of such a call is consumed by it.
     */
    private static boolean onlyRead(Object member, TypeInfo owner) {
        Set<Element> accepted = Collections.newSetFromMap(new IdentityHashMap<>());
        int[] reads = {0};
        KotlinWorld world = KotlinContext.options().world();
        List<TypeInfo> readers = world.contains(owner.primaryType()) ? world.mentioning(member)
                : List.of(owner.primaryType());
        readers.forEach(reader -> forEachBody(reader, (method, body) -> {
            Set<Expression> carriers = carriers(member, body);
            if (carriers.isEmpty()) return;
            reads[0] += carriers.size();
            // each verdict below is asked only where a read of the member lands (maddi-mod#50)
            Set<ReturnStatement> readOnlyReturns = Collections.newSetFromMap(new IdentityHashMap<>());
            if (method != null && body == method.methodBody()) {
                List<ReturnStatement> own = returnStatements(method);
                if (own.stream().anyMatch(rs -> rs.expression() != null && carriers.contains(unwrapped(rs.expression())))
                    && returns(method)) {
                    readOnlyReturns.addAll(own);
                }
            }
            body.visit((Element e) -> {
                switch (e) {
                    case Assignment a -> {
                        accept(carriers, a.target(), accepted);
                        if (a.variableTarget() instanceof FieldReference fr && fr.fieldInfo() != member
                            && carriers.contains(unwrapped(a.value())) && field(fr.fieldInfo())) {
                            accept(carriers, a.value(), accepted);
                        }
                    }
                    case MethodCall mc -> {
                        // what a read-only type allows: a call the analysis, or the library's contract, says adds
                        // and removes nothing (#141; a list of mutator names before)
                        if (mc.object() != null && mc.methodInfo() != null && carriers.contains(unwrapped(mc.object()))
                            && (KotlinContext.options().modification().structurallyNonModifying(mc.methodInfo())
                                || view(mc))) {
                            // a view's own uses decide, it is a carrier
                            accept(carriers, mc.object(), accepted);
                        }
                        if (identity(mc) && carriers.contains(mc)) {
                            accept(carriers, mc.parameterExpressions().getFirst(), accepted);
                        } else {
                            passedOn(carriers, mc.parameterExpressions(), mc.methodInfo(), accepted);
                        }
                    }
                    case ConstructorCall cc when cc.constructor() != null ->
                            passedOn(carriers, cc.parameterExpressions(), cc.constructor(), accepted);
                    case ForEachStatement fe -> {
                        if (fe.expression() != null) accept(carriers, fe.expression(), accepted);
                    }
                    case StringConcat sc -> {
                        accept(carriers, sc.lhs(), accepted);
                        accept(carriers, sc.rhs(), accepted);
                    }
                    case BinaryOperator bo when bo.operator() != null
                                                && ("==".equals(bo.operator().name()) || "!=".equals(bo.operator().name())
                                                    || bo.parameterizedType() != null
                                                       && bo.parameterizedType().isJavaLangString()) -> {
                        accept(carriers, bo.lhs(), accepted);
                        accept(carriers, bo.rhs(), accepted);
                    }
                    case ReturnStatement rs when readOnlyReturns.contains(rs) && rs.expression() != null ->
                            accept(carriers, rs.expression(), accepted);
                    default -> {
                    }
                }
                return true;
            });
        }));
        return accepted.size() == reads[0];
    }

    /**
     * The field's reads (the method's calls and references, a reference never allowed) in {@code body}, and the
     * {@code @Identity} calls whose first argument is one, transitively.
     */
    private static Set<Expression> carriers(Object member, Element body) {
        Set<Expression> carriers = Collections.newSetFromMap(new IdentityHashMap<>());
        List<MethodCall> identityCalls = new java.util.ArrayList<>();
        List<MethodCall> viewCalls = new java.util.ArrayList<>();
        body.visit((Element e) -> {
            if (e instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr
                && fr.fieldInfo() == member
                || e instanceof MethodCall mc && mc.methodInfo() == member
                || e instanceof io.codelaser.maddi.cst.api.expression.MethodReference mr && mr.methodInfo() == member) {
                carriers.add((Expression) e);
            }
            if (e instanceof MethodCall mc && identity(mc)) identityCalls.add(mc);
            if (e instanceof MethodCall mc && view(mc)) viewCalls.add(mc);
            return true;
        });
        boolean changed = true;
        while (changed) {
            changed = false;
            for (MethodCall mc : identityCalls) {
                if (!carriers.contains(mc) && carriers.contains(unwrapped(mc.parameterExpressions().getFirst()))) {
                    carriers.add(mc);
                    changed = true;
                }
            }
            // unfiltered.entrySet().iterator(), then remove(): a view is the collection, its uses must be reads too
            // (maddi-mod#54: the analysis's verdict misses removal through a view)
            for (MethodCall mc : viewCalls) {
                if (!carriers.contains(mc) && carriers.contains(unwrapped(mc.object()))) {
                    carriers.add(mc);
                    changed = true;
                }
            }
        }
        return carriers;
    }

    private static boolean view(MethodCall mc) {
        return mc.object() != null && mc.methodInfo() != null && !mc.methodInfo().isStatic()
               && KotlinMethodPrinter.VIEWS.contains(mc.methodInfo().name());
    }

    private static boolean identity(MethodCall mc) {
        return mc.methodInfo() != null && !mc.parameterExpressions().isEmpty() && KotlinContext.options().modification().identity(mc.methodInfo());
    }

    private static Expression unwrapped(Expression e) {
        Expression u = KotlinExpressionPrinter.unwrap(e);
        return u == null ? e : u;
    }

    // a method reference hands the result to code out of sight (Optional.map(Memory::messages), then add): never
    // accepted, also where it is an argument to a parameter that takes any value
    private static void accept(Set<Expression> carriers, Expression e, Set<Element> accepted) {
        Expression u = unwrapped(e);
        if (carriers.contains(u) && !(u instanceof io.codelaser.maddi.cst.api.expression.MethodReference)) {
            accepted.add(u);
        }
    }

    /**
     * An argument to a parameter that takes a read-only collection. Not to one typed by a type variable that comes
     * back in the result ({@code Optional.of(T)}: an {@code Optional<List<…>>} where the code may want a
     * {@code MutableList}) or that is bounded by a mutable collection ({@code <T : MutableCollection<*>>} takes no
     * read-only one).
     */
    private static void passedOn(Set<Expression> carriers, List<Expression> args, MethodInfo callee,
                                 Set<Element> accepted) {
        if (callee == null || callee.parameters().isEmpty()) return;
        for (int i = 0; i < args.size(); i++) {
            if (!carriers.contains(unwrapped(args.get(i)))) continue;
            ParameterizedType type = callee.parameters().get(Math.min(i, callee.parameters().size() - 1)).parameterizedType();
            ParameterizedType element = i >= callee.parameters().size() - 1 && callee.parameters().getLast().isVarArgs()
                                        && type.arrays() == 1 ? type.componentType() : type;
            io.codelaser.maddi.cst.api.info.TypeParameter tp = element.typeParameter();
            if (tp != null && (CstQueries.mentions(callee.returnType(), tp)
                               || tp.typeBounds().stream().anyMatch(KotlinReadOnly::isCollection))) {
                continue;
            }
            if (KotlinMethodPrinter.acceptsReadOnly(callee, i)) accept(carriers, args.get(i), accepted);
        }
    }


    /**
     * Every body of the compilation unit, with its method (null for a field initializer): the types, nested, anonymous
     * and local ones too, which {@code visit} does not enter.
     */
    /**
     * A private {@code Iterator} field only ever advanced ({@code hasNext()}, {@code next()}): Kotlin's read-only
     * Iterator, which a read-only collection's iterator() is (guava's Sets.union, {@code itr1 = set1.iterator()} of a
     * read-only Set). Calling next() is no modification Kotlin's types speak of.
     */
    static boolean advancedOnlyIterator(FieldInfo fieldInfo) {
        ParameterizedType type = fieldInfo.type();
        if (!KotlinContext.translatingJava() || type.arrays() != 0 || type.typeInfo() == null
            || !"java.util.Iterator".equals(type.typeInfo().fullyQualifiedName())
            || fieldInfo.access() == null || !fieldInfo.access().isPrivate() && !fieldInfo.owner().isAnonymous()) {
            return false;
        }
        Set<Expression> advanced = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Expression> uses = new java.util.ArrayList<>();
        forEachBody(fieldInfo.owner(), (m, body) -> body.visit((Element e) -> {
            if (e instanceof MethodCall mc && mc.object() != null && mc.parameterExpressions().isEmpty()
                && ("hasNext".equals(mc.methodInfo().name()) || "next".equals(mc.methodInfo().name()))) {
                advanced.add(KotlinExpressionPrinter.unwrap(mc.object()));
            }
            if (e instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
                && ve.variable() instanceof io.codelaser.maddi.cst.api.variable.FieldReference fr
                && fr.fieldInfo() == fieldInfo) {
                uses.add(ve);
            }
            return true;
        }));
        return !uses.isEmpty() && uses.stream().allMatch(advanced::contains);
    }

    static void forEachBody(TypeInfo typeInfo, BiConsumer<MethodInfo, Element> consumer) {
        typeInfo.constructorAndMethodStream().forEach(m -> {
            if (m.methodBody() != null) bodyAndItsTypes(m, (Element) m.methodBody(), consumer);
        });
        typeInfo.fields().forEach(f -> {
            if (f.initializer() != null && !f.initializer().isEmpty()) {
                bodyAndItsTypes(null, (Element) f.initializer(), consumer);
            }
        });
        typeInfo.subTypes().forEach(t -> forEachBody(t, consumer));
    }

    private static void bodyAndItsTypes(MethodInfo method, Element body, BiConsumer<MethodInfo, Element> consumer) {
        consumer.accept(method, body);
        body.visit((Element e) -> {
            if (e instanceof ConstructorCall cc && cc.anonymousClass() != null) forEachBody(cc.anonymousClass(), consumer);
            if (e instanceof LocalTypeDeclaration ltd) forEachBody(ltd.typeInfo(), consumer);
            return true;
        });
    }
}
