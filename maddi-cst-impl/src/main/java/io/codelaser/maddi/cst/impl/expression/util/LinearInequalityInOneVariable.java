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
import io.codelaser.maddi.cst.api.expression.util.OneVariable;

import java.util.List;

/*
av + b  >= 0 or av + b > 0
(we're not using x because IntelliJ complains passing y onto x)
 */
public record LinearInequalityInOneVariable(double a,
                                            OneVariable v,
                                            double b,
                                            boolean allowEquals) implements Inequality {

    public LinearInequalityInOneVariable {
        assert a != 0.0;
        assert v != null;
    }

    public boolean accept(double x) {
        double sum = a * x + b;
        return allowEquals ? sum >= 0 : sum > 0;
    }

    /*
    av + b >= 0 <=> v >= -b/a (with a>0.0) or v <= b/a (a<0.0)
     */
    public Interval interval() {
        double bOverA = b / a;
        if (a > 0.0) {
            return new Interval(-bOverA, allowEquals, Double.POSITIVE_INFINITY, true);
        }
        return new Interval(Double.NEGATIVE_INFINITY, true, bOverA, allowEquals);
    }

    /*
    null = not applicable; true = compatible/there are solutions; false = incompatible/no solutions
     */
    public Boolean accept(List<Expression> expressionsInV) {
        if (InequalityHelper.onlyNotEquals(expressionsInV)) return true; // v != some constant
        Double vEquals = InequalityHelper.extractEquals(expressionsInV); // v == some constant
        if (vEquals != null) return accept(vEquals);
        Interval i = Interval.extractInterval(expressionsInV);
        if (i != null) return accept(i);
        return null;
    }

    public Boolean accept(Interval i) {
        if (i.isPoint()) return accept(i.left());
        if (a > 0.0) {
            if (i.isOpenRight()) return true;
            assert i.isOpenLeft() || i.isClosed();
            // v >= -b/a; x<=right (or left <= x <= right); solution when -b/a <= right
            double diff = -b / a - i.right();
            return allowEquals && i.rightIncluded() ? diff <= 0 : diff < 0;
        }
        if (i.isOpenLeft()) return true;
        assert i.isOpenRight() || i.isClosed();
        // v <= -b/a; left <= x (or left <= x <= right); solution when left <= -b/a
        double diff = -b / a - i.left();
        return allowEquals && i.leftIncluded() ? diff >= 0 : diff > 0;
    }
}
