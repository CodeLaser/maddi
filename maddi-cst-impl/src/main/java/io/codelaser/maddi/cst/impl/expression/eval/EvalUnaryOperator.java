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

import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;

public class EvalUnaryOperator {
    private final Runtime runtime;

    public EvalUnaryOperator(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression eval(Expression value, UnaryOperator unaryOperator) {
        MethodInfo operator = unaryOperator.operator();

        // !!, ~~
        if (value instanceof UnaryOperator second && second.operator() == operator) {
            return second.expression();
        }
        if (runtime.bitWiseNotOperatorInt() == operator) {
            return runtime.newBitwiseNegation(unaryOperator.comments(), unaryOperator.source(), value);
        }
        if (runtime.unaryPlusOperatorInt() == operator) {
            return value;
        }
        assert runtime.unaryMinusOperatorInt() == operator || runtime.logicalNotOperatorBool() == operator;
        return runtime.negate(value);
    }
}
