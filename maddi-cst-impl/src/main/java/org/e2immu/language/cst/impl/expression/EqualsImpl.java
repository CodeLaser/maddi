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
import org.e2immu.language.cst.api.expression.Equals;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

public class EqualsImpl extends BinaryOperatorImpl implements Equals {

    public EqualsImpl(List<Comment> comments, Source source, MethodInfo operator, Precedence precedence,
                      Expression lhs, Expression rhs, ParameterizedType parameterizedType) {
        super(comments, source, operator, precedence, lhs, rhs, parameterizedType);
    }

    public EqualsImpl(Runtime runtime, Expression lhs, Expression rhs) {
        super(List.of(), null, lhs.isNumeric() ? runtime.equalsOperatorInt() : runtime.equalsOperatorObject(),
                runtime.precedenceEquality(), lhs, rhs,
                runtime.booleanParameterizedType());
    }

    @Override
    public Expression withSource(Source source) {
        return new EqualsImpl(comments(), source, operator, precedence, lhs, rhs, parameterizedType);
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression tl = lhs.translate(translationMap);
        Expression tr = rhs.translate(translationMap);
        if (tl == lhs && tr == rhs) return this;
        return new EqualsImpl(comments(), source(), operator, precedence, tl, tr, parameterizedType);
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new EqualsImpl(comments(), source(), operator, precedence, lhs.rewire(infoMap), rhs.rewire(infoMap),
                parameterizedType);
    }
}
