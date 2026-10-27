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

package io.codelaser.maddi.cst.impl.expression.util;


import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.GreaterThanZero;

import java.util.List;

public record Interval(double left, boolean leftIncluded, double right, boolean rightIncluded) {

    public Interval {
        assert Double.isFinite(left) || Double.isFinite(right);
        assert left < right || left == right && (leftIncluded || rightIncluded);
        assert Double.isFinite(left) || left == Double.NEGATIVE_INFINITY;
        assert Double.isFinite(right) || right == Double.POSITIVE_INFINITY;
    }

    public boolean isPoint() {
        return left == right;
    }

    public boolean isOpenLeft() {
        return left == Double.NEGATIVE_INFINITY && Double.isFinite(right);
    }

    public boolean isOpenRight() {
        return right == Double.POSITIVE_INFINITY && Double.isFinite(left);
    }

    public boolean isClosed() {
        return Double.isFinite(left) && Double.isFinite(right);
    }

    public Interval combine(Interval other) {
        if (isOpenLeft()) {
            assert other.isOpenRight();
            return new Interval(other.left, other.leftIncluded, right, other.rightIncluded);
        }
        if (isOpenRight()) {
            assert other.isOpenLeft();
            if (left > other.right) {
                // we cannot represent this interval: [a,inf] and [-inf,b], with a > b
                return null;
            }
            return new Interval(left, leftIncluded, other.right, other.rightIncluded);
        }
        throw new IllegalStateException();
    }

    public static Interval extractInterval(List<Expression> expressions) {
        if (expressions.size() == 1) {
            return extractInterval(expressions.get(0));
        }
        if (expressions.size() == 2) {
            Interval i1 = extractInterval(expressions.get(0));
            Interval i2 = extractInterval(expressions.get(1));
            return i1 == null || i2 == null ? null : i1.combine(i2);
        }
        return null;
    }

    public static Interval extractInterval(Expression expression) {
        GreaterThanZero gt0;
        if ((gt0 = expression.asInstanceOf(GreaterThanZero.class)) != null) {
            Inequality inequality = InequalityHelper.extract(gt0);
            if (inequality instanceof LinearInequalityInOneVariable one) return one.interval();
        }
        return null;
    }
}
