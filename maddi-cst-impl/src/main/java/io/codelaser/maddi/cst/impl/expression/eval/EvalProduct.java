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

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Product;
import org.e2immu.language.cst.api.expression.Sum;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.expression.ProductImpl;

import java.util.stream.Stream;

public class EvalProduct {

    private final Runtime runtime;

    public EvalProduct(Runtime runtime) {
        this.runtime = runtime;
    }

    // we try to maintain a sum of products
    public Expression eval(Expression l, Expression r) {
        Double ln = l.numericValue();
        Double rn = r.numericValue();

        if (ln != null && ln == 0 || rn != null && rn == 0) {
            return runtime.intZero();
        }

        if (ln != null && ln == 1) return r;
        if (rn != null && rn == 1) return l;
        if (ln != null && rn != null) return runtime.intOrDouble(ln * rn);

        // any unknown lingering
        if (l.isEmpty() || r.isEmpty()) throw new UnsupportedOperationException();

        if (r instanceof Sum sum) {
            Expression p1 = runtime.product(l, sum.lhs());
            Expression p2 = runtime.product(l, sum.rhs());
            return runtime.sum(p1, p2);
        }
        if (l instanceof Sum sum) {
            Expression p1 = runtime.product(sum.lhs(), r);
            Expression p2 = runtime.product(sum.rhs(), r);
            return runtime.sum(p1, p2);
        }
        return l.compareTo(r) < 0 ? new ProductImpl(runtime, l, r) : new ProductImpl(runtime, r, l);
    }

    // methods used externally
    // we have more than 2 factors, that's a product of products...
    public Expression wrapInProduct(Expression[] expressions, int i) {
        assert i >= 2;
        if (i == 2) return eval(expressions[0], expressions[1]);
        return eval(wrapInProduct(expressions, i - 1), expressions[i - 1]);
    }

    public Stream<Expression> expandFactors(Expression expression) {
        if (expression instanceof Product product) {
            return Stream.concat(expandFactors(product.lhs()), expandFactors(product.rhs()));
        }
        return Stream.of(expression);
    }
}
