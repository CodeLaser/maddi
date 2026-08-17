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

import org.e2immu.language.cst.api.expression.Divide;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.expression.Numeric;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.expression.DivideImpl;
import org.e2immu.language.cst.impl.expression.IntConstantImpl;

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
