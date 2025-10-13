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
import org.e2immu.language.cst.api.expression.BooleanConstant;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.TextImpl;

import java.util.List;

public class BooleanConstantImpl extends ConstantExpressionImpl<Boolean> implements BooleanConstant {
    private final ParameterizedType booleanPt;
    private final boolean constant;

    public BooleanConstantImpl(Predefined predefined, boolean constant) {
        this(List.of(), null, predefined.booleanParameterizedType(), constant);
    }

    public BooleanConstantImpl(List<Comment> comments, Source source, ParameterizedType booleanPt, boolean constant) {
        super(comments, source, 1);
        this.booleanPt = booleanPt;
        this.constant = constant;
    }

    @Override
    public Expression withSource(Source source) {
        return new BooleanConstantImpl(comments(), source, booleanPt, constant);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof BooleanConstant bc && constant == bc.constant();
    }

    @Override
    public int hashCode() {
        return constant ? 1 : 0;
    }

    @Override
    public Boolean constant() {
        return constant;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return booleanPt;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_CONSTANT_BOOLEAN;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        BooleanConstant bc = (BooleanConstant) expression;
        if (constant == bc.constant()) return 0;
        return constant ? -1 : 1;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(new TextImpl(Boolean.toString(constant)));
    }

    public BooleanConstant negate() {
        return new BooleanConstantImpl(comments(), source(), booleanPt, !constant);
    }
}
