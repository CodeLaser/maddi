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

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Product;
import io.codelaser.maddi.cst.api.expression.Sum;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.expression.ProductImpl;

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
