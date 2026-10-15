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
import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.InlineConditional;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.NullConstant;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.api.variable.Variable;

/**
 * Java's null-check conditionals that Kotlin writes with an operator (CodeLaser/maddi#140, item 1):
 * <ul>
 *     <li>{@code x != null ? x : d} and {@code x == null ? d : x} are {@code x ?: d};</li>
 *     <li>{@code x != null ? x.m(a) : null} and {@code x == null ? null : x.m(a)} are {@code x?.m(a)}.</li>
 * </ul>
 * Only where the Kotlin means what the Java does. {@code x} is a read of a local, a parameter or a field of
 * {@code this}: Java reads it twice, Kotlin once, and nothing runs in between. The arguments of the call do not
 * mention it, as they would rely on a smart cast that {@code ?.} does not give them. And {@code x} is one that Kotlin
 * types nullable: on a non-null one the operator is dead code, and the conditional is printed as written.
 */
final class KotlinNullCheckShapes {

    private KotlinNullCheckShapes() {
    }

    /** {@code value ?: otherwise} */
    record Elvis(Expression value, Expression otherwise) {
    }

    /** {@code receiver?.call}: the call is the conditional's non-null branch, on {@code receiver} */
    record SafeCall(Expression receiver, MethodCall call) {
    }

    static Elvis elvis(InlineConditional ic) {
        Tested tested = tested(ic);
        if (tested == null) return null;
        Expression nonNull = unwrap(tested.whenNonNull());
        if (nonNull instanceof VariableExpression ve && ve.variable().equals(tested.variable().variable())) {
            return new Elvis(tested.variable(), tested.whenNull());
        }
        return null;
    }

    static SafeCall safeCall(InlineConditional ic) {
        Tested tested = tested(ic);
        if (tested == null || !(unwrap(tested.whenNull()) instanceof NullConstant)) return null;
        if (!(unwrap(tested.whenNonNull()) instanceof MethodCall mc) || mc.objectIsImplicit() || mc.object() == null
            || !(unwrap(mc.object()) instanceof VariableExpression ve)
            || !ve.variable().equals(tested.variable().variable())) {
            return null;
        }
        Variable variable = ve.variable();
        for (Expression argument : mc.parameterExpressions()) {
            boolean[] mentions = {false};
            argument.visit((Element e) -> {
                if (e instanceof VariableExpression v && v.variable().equals(variable)) mentions[0] = true;
                return !mentions[0];
            });
            if (mentions[0]) return null;
        }
        return new SafeCall(mc.object(), mc);
    }

    private record Tested(VariableExpression variable, Expression whenNull, Expression whenNonNull) {
    }

    // the conditional tests a plain read for null; which branch has it non-null
    private static Tested tested(InlineConditional ic) {
        if (!KotlinContext.translatingJava()) return null;
        if (!(unwrap(ic.condition()) instanceof BinaryOperator bo) || bo.operator() == null) return null;
        String operator = bo.operator().name();
        if (!"==".equals(operator) && !"!=".equals(operator)) return null;
        Expression other = unwrap(bo.rhs()) instanceof NullConstant ? bo.lhs()
                : unwrap(bo.lhs()) instanceof NullConstant ? bo.rhs() : null;
        if (other == null || !(unwrap(other) instanceof VariableExpression ve) || !plainRead(ve.variable())) {
            return null;
        }
        if (!KotlinNullability.nullableInKotlin(ve)) return null;
        boolean nullWhenTrue = "==".equals(operator);
        return new Tested(ve, nullWhenTrue ? ic.ifTrue() : ic.ifFalse(), nullWhenTrue ? ic.ifFalse() : ic.ifTrue());
    }

    private static boolean plainRead(Variable variable) {
        return variable instanceof LocalVariable || variable instanceof ParameterInfo
               || variable instanceof FieldReference fr && (fr.scope() == null || fr.fieldInfo().isStatic()
                                                            || unwrap(fr.scope()) instanceof VariableExpression s
                                                               && s.variable() instanceof This);
    }

    private static Expression unwrap(Expression e) {
        return KotlinExpressionPrinter.unwrap(e);
    }
}
