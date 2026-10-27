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

import io.codelaser.maddi.cst.api.expression.Divide;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.IntConstant;
import io.codelaser.maddi.cst.api.expression.Numeric;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.expression.DivideImpl;
import io.codelaser.maddi.cst.impl.expression.IntConstantImpl;

public class EvalDivide {
    private final Runtime runtime;

    public EvalDivide(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression divide(Expression lhs, Expression rhs) {

        if (lhs instanceof Numeric ln && ln.doubleValue() == 0) return lhs;
        if (rhs instanceof Numeric rn && rn.doubleValue() == 1) return lhs;
        if (lhs instanceof IntConstant li && rhs instanceof IntConstant ri && ri.constant() != 0)
            return runtime.newInt(li.constant() / ri.constant());

        // a/n1/n2 = a/n1*n2
        if (lhs instanceof Divide d2) {
            return new DivideImpl(runtime, d2.lhs(), runtime.product(d2.rhs(), rhs));
        }
        // any unknown lingering
        if (lhs.isEmpty() || rhs.isEmpty()) throw new UnsupportedOperationException();

        return new DivideImpl(runtime, lhs, rhs);
    }

}
