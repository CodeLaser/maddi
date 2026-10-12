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
 * Kotlin's read-only collection types for private fields and for the results of closed methods (maddi-mod#25's
 * structural verdict, the step after the parameters of {@link KotlinMethodPrinter}). A read-only {@code List<T>} is
 * covariant where {@code MutableList<T>} is invariant, but it must agree with every expression that reads it:
 * <ul>
 *   <li>a private collection field the analysis proves structurally unmodified
 *   ({@link FieldInfo#isStructurallyUnmodified()}) is a {@code List} when each read of it in its compilation unit is
 *   the receiver of a call, the collection of a for-each, an operand of {@code ==} or a string concatenation, an
 *   argument to a parameter that takes a read-only collection, the value assigned to another read-only field, or
 *   the value returned by a read-only method. Assignments to it are no reads: a {@code MutableList} is a
 *   {@code List}. A call the analysis proves {@code @Identity} ({@code ensureNotNull(builder.variables, …)}) carries
 *   the field's value: its result is judged where it is used, as a read of the field;</li>
 *   <li>a method that no other overrides and that overrides none returns a {@code List} when each of its
 *   {@code return}s returns such a field. A caller that changed the result would have changed the field, which then
 *   is not structurally unmodified.</li>
 * </ul>
 * The two depend on each other ({@code items} is read-only when {@code items()} is, which is when {@code items} is):
 * a cycle is assumed read-only, and each member's other uses are checked where it is entered, as for parameters.
 */
final class KotlinReadOnly {

    private static final ThreadLocal<Set<Object>> IN_PROGRESS =
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
            || !fieldInfo.access().isPrivate() || KotlinTypePrinter.fromKotlinSource(fieldInfo.owner())
            || !isCollection(KotlinNullability.fieldType(fieldInfo)) || !KotlinContext.options().modification().structurallyUnmodified(fieldInfo)
            || inPrimaryConstructor(fieldInfo)) {
            return false;
        }
        Set<Object> visiting = IN_PROGRESS.get();
        if (!visiting.add(fieldInfo)) return true;
        try {
            return onlyRead(fieldInfo);
        } finally {
            visiting.remove(fieldInfo);
        }
    }

    static boolean returns(MethodInfo methodInfo) {
        if (!KotlinContext.translatingJava() || methodInfo.isConstructor() || methodInfo.isAbstract()
            || methodInfo.methodBody() == null || !methodInfo.overrides().isEmpty()
            || KotlinTypePrinter.fromKotlinSource(methodInfo.typeInfo())
            || !isCollection(KotlinNullability.returnType(methodInfo))) {
            return false;
        }
        // closed: the callers of an open member are all over the code base, and a caller in another file that hands
        // the result to a MutableList parameter or field is out of this file's sight (as a closed one's may be too,
        // but far less often: on langchain4j-core, opening it turned 6 read-only declarations into 34, and cost 2
        // compiling files there and 1 on fernflower)
        boolean closed = methodInfo.isStatic() || methodInfo.isFinal()
                         || methodInfo.access() != null && methodInfo.access().isPrivate()
                         || methodInfo.typeInfo().isFinal();
        if (!closed) return false;
        Set<Object> visiting = IN_PROGRESS.get();
        if (!visiting.add(methodInfo)) return true;
        try {
            List<ReturnStatement> returns = returnStatements(methodInfo);
            return !returns.isEmpty() && returns.stream().allMatch(rs -> rs.expression() != null
                    && KotlinExpressionPrinter.unwrap(rs.expression()) instanceof VariableExpression ve
                    && ve.variable() instanceof FieldReference fr && field(fr.fieldInfo()));
        } finally {
            visiting.remove(methodInfo);
        }
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
     * Each read of the field in its compilation unit is one a read-only type allows. The reads are the field's
     * expressions and the {@code @Identity} calls on them, whose results carry the field's value; the argument of
     * such a call is consumed by it.
     */
    private static boolean onlyRead(FieldInfo fieldInfo) {
        Set<Element> accepted = Collections.newSetFromMap(new IdentityHashMap<>());
        int[] reads = {0};
        forEachBody(fieldInfo.owner().primaryType(), (method, body) -> {
            Set<Expression> carriers = carriers(fieldInfo, body);
            reads[0] += carriers.size();
            Set<ReturnStatement> readOnlyReturns = Collections.newSetFromMap(new IdentityHashMap<>());
            if (method != null && body == method.methodBody() && returns(method)) {
                readOnlyReturns.addAll(returnStatements(method));
            }
            body.visit((Element e) -> {
                switch (e) {
                    case Assignment a -> {
                        accept(carriers, a.target(), accepted);
                        if (a.variableTarget() instanceof FieldReference fr && fr.fieldInfo() != fieldInfo
                            && field(fr.fieldInfo())) {
                            accept(carriers, a.value(), accepted);
                        }
                    }
                    case MethodCall mc -> {
                        if (mc.object() != null) accept(carriers, mc.object(), accepted);
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
        });
        return accepted.size() == reads[0];
    }

    /** The field's reads in {@code body}, and the {@code @Identity} calls whose first argument is one, transitively. */
    private static Set<Expression> carriers(FieldInfo fieldInfo, Element body) {
        Set<Expression> carriers = Collections.newSetFromMap(new IdentityHashMap<>());
        List<MethodCall> identityCalls = new java.util.ArrayList<>();
        body.visit((Element e) -> {
            if (e instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr
                && fr.fieldInfo() == fieldInfo) {
                carriers.add(ve);
            }
            if (e instanceof MethodCall mc && identity(mc)) identityCalls.add(mc);
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
        }
        return carriers;
    }

    private static boolean identity(MethodCall mc) {
        return mc.methodInfo() != null && !mc.parameterExpressions().isEmpty() && KotlinContext.options().modification().identity(mc.methodInfo());
    }

    private static Expression unwrapped(Expression e) {
        Expression u = KotlinExpressionPrinter.unwrap(e);
        return u == null ? e : u;
    }

    private static void accept(Set<Expression> carriers, Expression e, Set<Element> accepted) {
        Expression u = unwrapped(e);
        if (carriers.contains(u)) accepted.add(u);
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
    private static void forEachBody(TypeInfo typeInfo, BiConsumer<MethodInfo, Element> consumer) {
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
