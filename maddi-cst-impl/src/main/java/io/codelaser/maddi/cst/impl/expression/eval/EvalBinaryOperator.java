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

package io.codelaser.maddi.cst.impl.expression.eval;

import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;

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
