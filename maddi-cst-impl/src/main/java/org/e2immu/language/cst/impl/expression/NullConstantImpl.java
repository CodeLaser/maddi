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
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.NullConstant;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;

import java.util.List;

public class NullConstantImpl extends ConstantExpressionImpl<Object> implements NullConstant {

    private final ParameterizedType parameterizedType;

    public NullConstantImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType) {
        super(comments, source, 1);
        assert parameterizedType.isTypeOfNullConstant();
        this.parameterizedType = parameterizedType;
    }

    @Override
    public Expression withSource(Source source) {
        return new NullConstantImpl(comments(), source, parameterizedType);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullConstant;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public Object constant() {
        return null;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_CONSTANT_NULL;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        return 0; // there's only one
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(KeywordImpl.NULL);
    }
}
