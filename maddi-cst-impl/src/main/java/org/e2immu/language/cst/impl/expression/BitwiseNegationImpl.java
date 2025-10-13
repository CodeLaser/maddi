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

package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.BitwiseNegation;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.util.internal.util.IntUtil;

import java.util.List;

public class BitwiseNegationImpl extends UnaryOperatorImpl implements BitwiseNegation {

    public BitwiseNegationImpl(List<Comment> comments, Source source, MethodInfo operator, Precedence precedence, Expression expression) {
        super(comments, source, operator, expression, precedence);
    }

    @Override
    public Double numericValue() {
        Double d = expression.numericValue();
        if (d != null && IntUtil.isMathematicalInteger(d)) {
            long bitwiseNegation = ~((long) (double) d);
            return (double) bitwiseNegation;
        }
        return null;
    }

    @Override
    public int wrapperOrder() {
        return 0;
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new BitwiseNegationImpl(comments(), source(), operator, precedence, expression.rewire(infoMap));
    }
}
