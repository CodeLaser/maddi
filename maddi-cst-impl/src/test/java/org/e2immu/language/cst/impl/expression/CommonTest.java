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

import org.e2immu.language.cst.api.expression.BooleanConstant;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.InstanceOf;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;

public abstract class CommonTest {
    protected final Runtime r = new RuntimeImpl();

    protected final BooleanConstant FALSE = r.constantFalse();
    protected final BooleanConstant TRUE = r.constantTrue();
    protected final VariableExpression a = r.newVariableExpression(createVariable("a", r.booleanParameterizedType()));
    protected final VariableExpression b = r.newVariableExpression(createVariable("b", r.booleanParameterizedType()));
    protected final VariableExpression c = r.newVariableExpression(createVariable("c", r.booleanParameterizedType()));
    protected final VariableExpression d = r.newVariableExpression(createVariable("d", r.booleanParameterizedType()));

    protected final VariableExpression k = r.newVariableExpression(createVariable("k", r.intParameterizedType()));
    protected final VariableExpression i = r.newVariableExpression(createVariable("i", r.intParameterizedType()));
    protected final VariableExpression j = r.newVariableExpression(createVariable("j", r.intParameterizedType()));

    protected final VariableExpression l = r.newVariableExpression(createVariable("l", r.doubleParameterizedType()));
    protected final VariableExpression m = r.newVariableExpression(createVariable("m", r.doubleParameterizedType()));
    protected final VariableExpression n = r.newVariableExpression(createVariable("n", r.doubleParameterizedType()));

    protected final VariableExpression s = r.newVariableExpression(createVariable("s", r.stringParameterizedType()));

    protected final VariableExpression x = r.newVariableExpression(createVariable("x", r.charParameterizedType()));

    protected final VariableExpression dd = r.newVariableExpression(createVariable("dd", r.doubleParameterizedType().ensureBoxed(r)));

    protected Variable createVariable(String name, ParameterizedType type) {
        return r.newLocalVariable(name, type);
    }

    protected InstanceOf newInstanceOf(Expression e, ParameterizedType testType) {
        return r.newInstanceOfBuilder().setExpression(e).setTestType(testType).build();
    }

    protected Expression multiply(Expression lhs, Expression rhs) {
        return r.newBinaryOperatorBuilder()
                .setLhs(lhs).setRhs(rhs).setOperator(r.multiplyOperatorInt())
                .setPrecedence(r.precedenceOfBinaryOperator(r.multiplyOperatorInt()))
                .setParameterizedType(r.widestType(lhs.parameterizedType(), rhs.parameterizedType()))
                .build();
    }
}
