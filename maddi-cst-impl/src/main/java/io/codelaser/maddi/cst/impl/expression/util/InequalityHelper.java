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


import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.expression.util.OneVariable;

import java.util.ArrayList;
import java.util.List;

public class InequalityHelper {

    private record Term(double a, OneVariable v) {
    }

    public static Inequality extract(GreaterThanZero gt0) {
        List<Term> terms = new ArrayList<>();
        if (!recursivelyCollectTerms(gt0.expression(), terms)) return null;
        List<Term> withoutVariable = terms.stream().filter(t -> t.v == null).toList();
        if (withoutVariable.size() > 1) return null;
        double c = withoutVariable.isEmpty() ? 0.0 : withoutVariable.get(0).a;

        List<Term> withVariable = terms.stream().filter(t -> t.v != null).toList();
        if (withVariable.size() == 1) {
            Term t1 = withVariable.get(0);
            return new LinearInequalityInOneVariable(t1.a, t1.v, c, gt0.allowEquals());
        }
        if (withVariable.size() == 2) {
            Term t1 = withVariable.get(0);
            Term t2 = withVariable.get(1);
            return new LinearInequalityInTwoVariables(t1.a, t1.v, t2.a, t2.v, c, gt0.allowEquals());
        }
        // not recognized
        return null;
    }

    private static boolean recursivelyCollectTerms(Expression expression,
                                                   List<Term> terms) {
        OneVariable oneVariableRhs;
        ConstantExpression<?> ce;
        Product product;
        if ((product = expression.asInstanceOf(Product.class)) != null
                && (ce = product.lhs().asInstanceOf(ConstantExpression.class)) != null
                && ((oneVariableRhs = extractOneVariable(product.rhs())) != null)) {
            terms.add(new Term(extractDouble((Number) ce.constant()), oneVariableRhs));
            return true;
        }
        Sum sum;
        if ((sum = expression.asInstanceOf(Sum.class)) != null) {
            if (!recursivelyCollectTerms(sum.lhs(), terms)) return false;
            return recursivelyCollectTerms(sum.rhs(), terms);
        }
        OneVariable oneVariable;
        if ((oneVariable = extractOneVariable(expression)) != null) {
            terms.add(new Term(1.0, oneVariable));
            return true;
        }
        Numeric numeric;
        if ((numeric = expression.asInstanceOf(Numeric.class)) != null) {
            terms.add(new Term(numeric.doubleValue(), null));
            return true;
        }
        CharConstant cc;
        if ((cc = expression.asInstanceOf(CharConstant.class)) != null) {
            terms.add(new Term(cc.constant(), null));
        }
        Negation negation;
        if ((negation = expression.asInstanceOf(Negation.class)) != null) {
            List<Term> sub = new ArrayList<>();
            if (!recursivelyCollectTerms(negation.expression(), sub)) return false;
            sub.forEach(term -> terms.add(new Term(-term.a, term.v)));
            return true;
        }
        return false;
    }

    private static OneVariable extractOneVariable(Expression expression) {
        VariableExpression ve;
        if ((ve = expression.asInstanceOf(VariableExpression.class)) != null) return ve.variable();
        MethodCall mc;
        if ((mc = expression.asInstanceOf(MethodCall.class)) != null
                && mc.object().isInstanceOf(VariableExpression.class)) {
            return mc;
        }
        return null;
    }

    private static double extractDouble(Number number) {
        return number.doubleValue();
    }

    public static boolean onlyNotEquals(List<Expression> expressions) {
        return expressions.stream().allMatch(e -> {
            Equals eq;
            Negation n;
            return (n = e.asInstanceOf(Negation.class)) != null
                    && (eq = n.expression().asInstanceOf(Equals.class)) != null
                    && eq.lhs().isConstant() && eq.rhs().isInstanceOf(VariableExpression.class);
        });
    }

    public static Double extractEquals(List<Expression> expressions) {
        return expressions.stream().filter(e -> {
                    Equals eq;
                    return (eq = e.asInstanceOf(Equals.class)) != null
                            && eq.lhs().isConstant()
                            && !(eq.lhs().isNullConstant());
                })
                .map(e -> extractDouble((Number) ((e.asInstanceOf(Equals.class))
                        .lhs().asInstanceOf(ConstantExpression.class).constant())))
                .findFirst().orElse(null);
    }

}
