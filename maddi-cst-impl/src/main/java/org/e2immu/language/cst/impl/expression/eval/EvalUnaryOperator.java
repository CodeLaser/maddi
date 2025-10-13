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

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;

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
