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

package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;

public class EvalBinaryOperator {
    private final Runtime runtime;

    public EvalBinaryOperator(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression eval(Expression lhs, Expression rhs, BinaryOperator operator) {
        Expression evaluated = determineValue(lhs, rhs, operator.operator());
        return evaluated != null ? evaluated : operator;
    }

    private Expression determineValue(Expression l, Expression r, MethodInfo operator) {
        if (operator == runtime.equalsOperatorObject()) {
            if (l.equals(r)) {
                return runtime.constantTrue();
            }
            if (l.isNullConstant() && runtime.isNotNull0(r)) return runtime.constantFalse();
            if (r.isNullConstant() && runtime.isNotNull0(l)) return runtime.constantFalse();
            return runtime.equals(l, r);
        }
        if (operator == runtime.equalsOperatorInt()) {
            if (l.equals(r)) return runtime.constantTrue();
            return runtime.equals(l, r);
        }
        if (operator == runtime.notEqualsOperatorObject()) {
            if (l.equals(r)) return runtime.constantFalse();

            // HERE are the !=null checks
            if (l.isNullConstant() && runtime.isNotNull0(r)) return runtime.constantTrue();
            if (r.isNullConstant() && runtime.isNotNull0(l)) return runtime.constantTrue();
            return runtime.negate(runtime.equals(l, r));
        }
        if (operator == runtime.notEqualsOperatorInt()) {
            if (l.equals(r)) return runtime.constantFalse();
            return runtime.negate(runtime.equals(l, r));
        }

        // from here on, straightforward operations
        if (operator == runtime.plusOperatorInt()) {
            return runtime.sum(l, r);
        }
        if (operator == runtime.minusOperatorInt()) {
            return runtime.sum(l, runtime.negate(r));
        }
        if (operator == runtime.multiplyOperatorInt()) {
            return runtime.product(l, r);
        }
        if (operator == runtime.divideOperatorInt()) {
            return runtime.divide(l, r);
        }
        if (operator == runtime.remainderOperatorInt()) {
            return runtime.remainder(l, r);
        }
        if (operator == runtime.lessEqualsOperatorInt()) {
            return runtime.less(l, r, true);
        }
        if (operator == runtime.lessOperatorInt()) {
            return runtime.less(l, r, false);
        }
        if (operator == runtime.greaterEqualsOperatorInt()) {
            return runtime.greater(l, r, true);
        }
        if (operator == runtime.greaterOperatorInt()) {
            return runtime.greater(l, r, false);
        }
        if (operator == runtime.plusOperatorString()) {
            return runtime.newStringConcat(l, r);
        }
        if (operator == runtime.andOperatorBool()) {
            return runtime.and(l, r);
        }
        if (operator == runtime.orOperatorBool()) {
            return runtime.or(l, r);
        }

        // more obscure operators: ignore
        return null;
    }

}
