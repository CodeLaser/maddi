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

public record LinearInequalityInTwoVariables(double a,
                                             OneVariable x,
                                             double b,
                                             OneVariable y,
                                             double c, boolean allowEquals) implements Inequality {
    public LinearInequalityInTwoVariables {
        assert a != 0.0;
        assert b != 0.0;
        assert x != null;
        assert y != null;
        assert !x.equals(y);
    }

    public boolean accept(double px, double py) {
        double sum = a * px + b * py + c;
        return allowEquals ? sum >= 0 : sum > 0;
    }

    public boolean isOpenLeftX() {
        return a < 0;
    }

    public boolean isOpenRightX() {
        return a > 0;
    }

    public boolean isOpenLeftY() {
        return b < 0;
    }

    public boolean isOpenRightY() {
        return b > 0;
    }

    public Boolean accept(List<Expression> expressionsInX, List<Expression> expressionsInY) {
        if (InequalityHelper.onlyNotEquals(expressionsInX) || InequalityHelper.onlyNotEquals(expressionsInY)) return true;
        Double xEquals = InequalityHelper.extractEquals(expressionsInX);
        Double yEquals = InequalityHelper.extractEquals(expressionsInY);
        if (xEquals != null && yEquals != null) {
            return accept(xEquals, yEquals);
        }
        if (xEquals != null) {
            // we have x to a constant, and inequalities for y => linear inequality in one variable
            LinearInequalityInOneVariable inequality = new LinearInequalityInOneVariable(
                    b, y, a * xEquals + c, allowEquals);
            return inequality.accept(expressionsInY);
        }
        if (yEquals != null) {
            LinearInequalityInOneVariable inequality = new LinearInequalityInOneVariable(
                    a, x, b * yEquals + c, allowEquals);
            return inequality.accept(expressionsInX);
        }
        // at least one inequality on x, at least one on y; they can be expressed as intervals
        Interval intervalX = Interval.extractInterval( expressionsInX);
        Interval intervalY = Interval.extractInterval( expressionsInY);
        if (intervalX == null || intervalY == null) return null;

        if (intervalX.isClosed() && intervalY.isClosed()) {
            // is a box
            return accept(intervalX.left(), intervalY.left()) || accept(intervalX.right(), intervalY.left()) ||
                    accept(intervalX.left(), intervalY.right()) || accept(intervalX.right(), intervalY.right());
        }
        if (intervalX.isClosed()) {
            if (intervalY.isOpenLeft()) { // looks like the capital letter PI
                return accept(intervalX.left(), intervalY.right()) || accept(intervalX.right(), intervalY.right()) ||
                        isOpenLeftY();
            }
            // looks like the letter U
            return accept(intervalX.left(), intervalY.left()) || accept(intervalX.right(), intervalY.left()) ||
                    isOpenRightY();
        }
        if (intervalY.isClosed()) {
            if (intervalX.isOpenLeft()) { // looks like ]
                return accept(intervalX.right(), intervalY.left()) || accept(intervalX.right(), intervalY.right()) ||
                        isOpenLeftX();
            }
            // looks like [
            return accept(intervalX.left(), intervalY.left()) || accept(intervalX.right(), intervalY.left()) ||
                    isOpenRightX();
        }
        // neither are closed; look like 90 degrees rotations of L; first, try the corner point
        if (accept(intervalX.isOpenLeft() ? intervalX.right() : intervalX.left(), intervalY.isOpenLeft() ? intervalY.right() : intervalY.left()))
            return true;

        return intervalX.isOpenRight() && isOpenRightX() || intervalX.isOpenLeft() && isOpenLeftX()
                || intervalY.isOpenRight() && isOpenRightY() || intervalY.isOpenLeft() && isOpenLeftY();
    }

}
