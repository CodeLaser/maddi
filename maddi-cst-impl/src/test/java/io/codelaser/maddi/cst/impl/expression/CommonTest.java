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

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.expression.BooleanConstant;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.InstanceOf;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;

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
