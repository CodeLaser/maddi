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
 *   argument to a parameter that takes a read-only collection, or the value returned by a read-only method.
 *   Assignments are no reads: a {@code MutableList} is a {@code List};</li>
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
            || !isCollection(KotlinNullability.fieldType(fieldInfo)) || !fieldInfo.isStructurallyUnmodified()) {
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

    /** Each read of the field in its compilation unit is one a read-only type allows. */
    private static boolean onlyRead(FieldInfo fieldInfo) {
        Set<Element> accepted = Collections.newSetFromMap(new IdentityHashMap<>());
        int[] uses = {0};
        forEachBody(fieldInfo.owner().primaryType(), (method, body) -> {
            Set<ReturnStatement> readOnlyReturns = Collections.newSetFromMap(new IdentityHashMap<>());
            if (method != null && body == method.methodBody() && returns(method)) {
                readOnlyReturns.addAll(returnStatements(method));
            }
            body.visit((Element e) -> {
                switch (e) {
                    case Assignment a -> accept(fieldInfo, a.target(), accepted);
                    case MethodCall mc -> {
                        if (mc.object() != null) accept(fieldInfo, mc.object(), accepted);
                        passedOn(fieldInfo, mc.parameterExpressions(), mc.methodInfo(), accepted);
                    }
                    case ConstructorCall cc when cc.constructor() != null ->
                            passedOn(fieldInfo, cc.parameterExpressions(), cc.constructor(), accepted);
                    case ForEachStatement fe -> {
                        if (fe.expression() != null) accept(fieldInfo, fe.expression(), accepted);
                    }
                    case StringConcat sc -> {
                        accept(fieldInfo, sc.lhs(), accepted);
                        accept(fieldInfo, sc.rhs(), accepted);
                    }
                    case BinaryOperator bo when bo.operator() != null
                                                && ("==".equals(bo.operator().name()) || "!=".equals(bo.operator().name())
                                                    || bo.parameterizedType() != null
                                                       && bo.parameterizedType().isJavaLangString()) -> {
                        accept(fieldInfo, bo.lhs(), accepted);
                        accept(fieldInfo, bo.rhs(), accepted);
                    }
                    case ReturnStatement rs when readOnlyReturns.contains(rs) && rs.expression() != null ->
                            accept(fieldInfo, rs.expression(), accepted);
                    case VariableExpression ve when ve.variable() instanceof FieldReference fr
                                                    && fr.fieldInfo() == fieldInfo -> uses[0]++;
                    default -> {
                    }
                }
                return true;
            });
        });
        return accepted.size() == uses[0];
    }

    private static void accept(FieldInfo fieldInfo, Expression e, Set<Element> accepted) {
        if (KotlinExpressionPrinter.unwrap(e) instanceof VariableExpression ve
            && ve.variable() instanceof FieldReference fr && fr.fieldInfo() == fieldInfo) {
            accepted.add(ve);
        }
    }

    /**
     * An argument to a parameter that takes a read-only collection. Not to a type variable's: {@code ensureNotNull(T)}
     * hands it back, typed read-only, to what may be a {@code MutableMap} (this.attributes = ensureNotNull(…)), and
     * {@code <T : MutableCollection<*>>} takes none.
     */
    private static void passedOn(FieldInfo fieldInfo, List<Expression> args, MethodInfo callee, Set<Element> accepted) {
        for (int i = 0; i < args.size(); i++) {
            if (callee == null || callee.parameters().isEmpty()) return;
            ParameterizedType type = callee.parameters().get(Math.min(i, callee.parameters().size() - 1)).parameterizedType();
            ParameterizedType element = i >= callee.parameters().size() - 1 && callee.parameters().getLast().isVarArgs()
                                        && type.arrays() == 1 ? type.componentType() : type;
            if (element.typeParameter() == null && KotlinMethodPrinter.acceptsReadOnly(callee, i)) {
                accept(fieldInfo, args.get(i), accepted);
            }
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
