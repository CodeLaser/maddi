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

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.NullConstant;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.IfElseStatement;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.statement.TryStatement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A getter tested against null and then used again, {@code if (stat.getExprents() != null && !stat.getExprents().isEmpty())}:
 * Kotlin cannot smart-cast a call's result, so the second call would need {@code !!}. When the getter returns a field
 * that never changes (final, or effectively final by the analysis) of a receiver that does not either ({@code this}, a
 * parameter, a local never reassigned, known non-null), every call returns the same value, and Kotlin's idiom is a
 * local copy: {@code val exprents = stat.getExprents()} before the statement, {@code exprents} for each call of it, which
 * the test then smart-casts. The copy serves the rest of the block too. Only when no class translated with this one
 * overrides the getter, whose result could then be anything.
 */
final class KotlinValCopies {

    private KotlinValCopies() {
    }

    /** The {@code val} declarations to print before {@code statement}; the copies are declared for it. */
    static List<OutputBuilder> before(Statement statement, Qualification q) {
        if (!KotlinContext.translatingJava()) return List.of();
        KotlinSmartCasts.forget(statement);
        if (!(statement instanceof IfElseStatement || statement instanceof ReturnStatement
              || statement instanceof ExpressionAsStatement || statement instanceof LocalVariableCreation)
            || !KotlinSmartCasts.inBlock(statement)) {
            return List.of();
        }
        List<Expression> header = new ArrayList<>();
        if (statement instanceof LocalVariableCreation lvc) {
            lvc.localVariableStream().forEach(lv -> {
                if (lv.assignmentExpression() != null) header.add(lv.assignmentExpression());
            });
        } else if (statement.expression() != null) {
            header.add(statement.expression());
        }
        Map<Object, MethodCall> tested = new LinkedHashMap<>();
        header.forEach(h -> h.visit((Element e) -> {
            if (e instanceof Lambda) return false;
            if (e instanceof BinaryOperator bo && bo.operator() != null
                && ("==".equals(bo.operator().name()) || "!=".equals(bo.operator().name()))) {
                Expression other = KotlinExpressionPrinter.unwrap(bo.rhs()) instanceof NullConstant ? bo.lhs()
                        : KotlinExpressionPrinter.unwrap(bo.lhs()) instanceof NullConstant ? bo.rhs() : null;
                if (other != null && KotlinExpressionPrinter.unwrap(other) instanceof MethodCall mc && eligible(mc, statement)) {
                    tested.putIfAbsent(java.util.List.of(mc.methodInfo(), KotlinSmartCasts.receiverKey(mc)), mc);
                }
            }
            return true;
        }));
        List<OutputBuilder> declarations = new ArrayList<>();
        for (MethodCall mc : tested.values()) {
            if (KotlinSmartCasts.copy(mc) != null || occurrences(mc, statement) < 2) continue;
            ParameterizedType type = KotlinNullability.kotlinType(mc);
            if (type == null || !KotlinNullability.isNullable(type)) continue;
            FieldInfo field = field(mc.methodInfo());
            String name = freshName(field, mc);
            OutputBuilder value = KotlinExpressionPrinter.print(mc, q);
            declarations.add(new OutputBuilderImpl().add(KotlinKeyword.VAL).add(SpaceEnum.ONE)
                    .add(new TextImpl(KotlinNames.name(name))).add(KotlinSymbols.assignment("=")).add(value));
            KotlinSmartCasts.declare(new KotlinSmartCasts.Copy(name, mc.methodInfo(), KotlinSmartCasts.receiverKey(mc), type),
                    statement);
        }
        return declarations;
    }

    /**
     * A call without arguments of a getter of a field that never changes, on a receiver that does not either and that is
     * known non-null (evaluating it before the statement must not throw where the statement would not have).
     */
    private static boolean eligible(MethodCall mc, Statement statement) {
        MethodInfo getter = mc.methodInfo();
        if (getter == null || getter.isStatic() || !mc.parameterExpressions().isEmpty()
            || !KotlinNullability.translated(getter.typeInfo())) {
            return false;
        }
        FieldInfo field = field(getter);
        if (field == null || !field.isFinal() && !field.isPropertyFinal()) return false;
        Object receiver = KotlinSmartCasts.receiverKey(mc);
        if (receiver == null) return false;
        if (receiver instanceof Variable v) {
            if (!(v instanceof ParameterInfo || v instanceof LocalVariable) || KotlinContext.reassigned(v)
                || declaredIn(statement, v) || KotlinNullability.nullableInKotlin(mc.object())) {
                return false;
            }
            if (v instanceof ParameterInfo pi && pi.methodInfo() != KotlinContext.currentMethod()
                && !KotlinContext.state().methods().contains(pi.methodInfo())) {
                return false;
            }
        }
        return !KotlinContext.options().world().overridden(getter);
    }

    /**
     * The field a getter returns: the analysis's getter link (records, Kotlin sources), or a body that is one
     * {@code return f;} of a field of {@code this}. Null for anything else.
     */
    static FieldInfo field(MethodInfo getter) {
        Value.FieldValue fieldValue = getter.getSetField();
        if (fieldValue != null && fieldValue.field() != null) {
            return fieldValue.setter() || fieldValue.list() ? null : fieldValue.field();
        }
        if (getter.methodBody() == null || getter.isStatic()) return null;
        List<Statement> statements = io.codelaser.maddi.cst.print.translate.CstQueries.own(getter.methodBody().statements());
        if (statements.size() != 1 || !(statements.getFirst() instanceof ReturnStatement rs) || rs.expression() == null) {
            return null;
        }
        if (KotlinExpressionPrinter.unwrap(rs.expression()) instanceof VariableExpression ve
            && ve.variable() instanceof io.codelaser.maddi.cst.api.variable.FieldReference fr && !fr.isStatic()
            && (fr.scope() == null || KotlinExpressionPrinter.unwrap(fr.scope()) instanceof VariableExpression sv
                                      && sv.variable() instanceof io.codelaser.maddi.cst.api.variable.This)) {
            return fr.fieldInfo();
        }
        return null;
    }

    /** The variable is declared inside the statement: a copy before it cannot read it. */
    private static boolean declaredIn(Statement statement, Variable variable) {
        boolean[] found = {false};
        statement.visit((Element e) -> {
            if (e instanceof LocalVariableCreation lvc && lvc.localVariableStream().anyMatch(variable::equals)) found[0] = true;
            if (e instanceof Lambda l && l.parameters().stream().anyMatch(variable::equals)) found[0] = true;
            if (e instanceof TryStatement ts && ts.catchClauses().stream().anyMatch(c -> variable.equals(c.catchVariable()))) {
                found[0] = true;
            }
            return !found[0];
        });
        return found[0];
    }

    private static int occurrences(MethodCall call, Statement statement) {
        Object receiver = KotlinSmartCasts.receiverKey(call);
        int[] n = {0};
        statement.visit((Element e) -> {
            if (e instanceof MethodCall mc && mc.methodInfo() == call.methodInfo() && mc.parameterExpressions().isEmpty()
                && receiver.equals(KotlinSmartCasts.receiverKey(mc))) {
                n[0]++;
            }
            return true;
        });
        return n[0];
    }

    /**
     * The field's name, unless a local, a parameter or a lambda parameter of the method, a field it reads by its bare
     * name, or another copy in scope has it: then with a suffix.
     */
    private static String freshName(FieldInfo field, MethodCall mc) {
        Set<String> taken = new HashSet<>(KotlinSmartCasts.copyNames());
        MethodInfo method = KotlinContext.currentMethod();
        for (MethodInfo m : KotlinContext.state().methods()) {
            m.parameters().forEach(p -> taken.add(p.name()));
            if (m.methodBody() != null) {
                ((Element) m.methodBody()).visit((Element e) -> {
                    if (e instanceof LocalVariableCreation lvc) lvc.localVariableStream().forEach(lv -> taken.add(lv.simpleName()));
                    if (e instanceof Lambda l) l.parameters().forEach(p -> taken.add(p.name()));
                    if (e instanceof TryStatement ts) ts.catchClauses().forEach(c -> {
                        if (c.catchVariable() != null) taken.add(c.catchVariable().simpleName());
                    });
                    return true;
                });
            }
        }
        if (method == null) return field.name() + "Value";
        // a field the method reads by its bare name: a local of that name would hide it (not the getter's own field
        // of this, whose copy holds the same value)
        for (MethodInfo m : KotlinContext.state().methods()) {
            if (m.methodBody() == null) continue;
            ((Element) m.methodBody()).visit((Element e) -> {
                if (e instanceof VariableExpression ve && ve.variable() instanceof io.codelaser.maddi.cst.api.variable.FieldReference fr
                    && (fr.scope() == null || fr.isStatic() || KotlinExpressionPrinter.unwrap(fr.scope()) instanceof VariableExpression sv
                                                               && sv.variable() instanceof io.codelaser.maddi.cst.api.variable.This)
                    && !(fr.fieldInfo() == field && KotlinSmartCasts.THIS.equals(KotlinSmartCasts.receiverKey(mc)))) {
                    taken.add(fr.fieldInfo().name());
                }
                return true;
            });
        }
        String base = field.name();
        if (!taken.contains(base)) return base;
        for (int i = 0; ; i++) {
            String candidate = base + "Value" + (i == 0 ? "" : String.valueOf(i + 1));
            if (!taken.contains(candidate)) return candidate;
        }
    }
}
