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

package io.codelaser.maddi.cst.impl.expression.util;


import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.expression.VariableExpressionImpl;
import io.codelaser.maddi.cst.impl.expression.eval.EvalSwitches;
import io.codelaser.maddi.cst.impl.variable.DescendModeEnum;

import java.util.*;
import java.util.stream.Collectors;


/*
 Full sorting of clauses in an "And" or "Or" is not allowed; we want, e.g., in != null before in.isEmpty()
 On the other hand, the sorting is our mechanism to relatively efficiently detect ways to simplify the expression.

 Different method calls, and especially modifying ones,
 should remain in the current order, even if that is not semantically optimal
 */

public class AndOrSorter {
    private AndOrSorter() {
    }

    /*
     The sort runs on every pass of the And/Or fixed point, over the whole operand list, for every evaluation -- the
     parser builds a path condition one atom at a time, so a method's conditions sort O(n^2 log n) terms in all. The
     TreeMap version keyed on a fresh VariableExpressionImpl per operand per pass, compared through the comparator's
     identity memo, which cannot hit on fresh keys. Here each operand's key is computed once, and one stable sort
     orders by (subject, kind, term): the same order as grouping by subject, then null checks sorted, other terms
     sorted, method calls in their original order.
     */
    private static final EvalSwitches.Gate SORT_KEYS_OFF = EvalSwitches.gate("NOSORTKEYS");

    private record Key(Expression subject, int kind, Expression term) {
    }

    private static final int KIND_NULL_CHECK = 0;
    private static final int KIND_OTHER = 1;
    private static final int KIND_METHOD_CALL = 2;

    private static final Comparator<Key> KEY_COMPARATOR = (k1, k2) -> {
        // the subject is a fresh instance per operand: the comparator's identity memo cannot hit, skip it
        int c = ExpressionComparator.SINGLETON.compareDirect(k1.subject, k2.subject);
        if (c != 0) return c;
        c = k1.kind - k2.kind;
        if (c != 0) return c;
        // method calls, and especially modifying ones, keep their order: the sort is stable.
        // The terms recur, across passes and across evaluations, so their comparison goes through the memo
        // (BinaryOperator.compareVariables is the expensive one)
        return k1.kind == KIND_METHOD_CALL ? 0 : k1.term.compareTo(k2.term);
    };

    public static ArrayList<Expression> sort(List<Expression> expressions) {
        if (SORT_KEYS_OFF.get()) return sortByTreeMap(expressions);
        int n = expressions.size();
        Key[] keys = new Key[n];
        for (int i = 0; i < n; i++) {
            Expression e = expressions.get(i);
            int kind = isNullCheck(e) ? KIND_NULL_CHECK : isMethodCall(e) ? KIND_METHOD_CALL : KIND_OTHER;
            keys[i] = new Key(objectOfExpression(e), kind, e);
        }
        Arrays.sort(keys, KEY_COMPARATOR); // stable
        ArrayList<Expression> result = new ArrayList<>(n);
        for (Key key : keys) result.add(key.term);
        return result;
    }

    private static ArrayList<Expression> sortByTreeMap(List<Expression> expressions) {
        TreeMap<Expression, List<Expression>> expressionsByObject = new TreeMap<>();
        for (Expression expression : expressions) {
            Expression object = objectOfExpression(expression);
            ArrayList<Expression> list = new ArrayList<>();
            list.add(expression);
            expressionsByObject.merge(object, list, (l1, l2) -> {
                l1.addAll(l2);
                return l1;
            });
        }
        List<List<Expression>> sorted =
                expressionsByObject.values().stream().map(l -> {
                    if (l.size() == 1) return l;
                    return sortList(l);
                }).toList();
        return sorted.stream().flatMap(Collection::stream)
                .collect(Collectors.toCollection(() -> new ArrayList<>(expressions.size())));
    }

    private static List<Expression> sortList(List<Expression> list) {
        ArrayList<Expression> l1 = new ArrayList<>();
        ArrayList<Expression> l2 = new ArrayList<>();
        ArrayList<Expression> l3 = new ArrayList<>();
        for (Expression e : list) {
            if (isNullCheck(e)) l1.add(e);
            else if (isMethodCall(e)) {
                l3.add(e);
            } else l2.add(e);
        }
        ArrayList<Expression> res = new ArrayList<>(list.size());
        Collections.sort(l1);
        Collections.sort(l2);
        // do NOT sort l3
        res.addAll(l1);
        res.addAll(l2);
        res.addAll(l3);
        return res;
    }

    private static boolean isMethodCall(Expression e) {
        Negation n;
        if ((n = e.asInstanceOf(Negation.class)) != null) return isMethodCall(n.expression());
        return e.isInstanceOf(MethodCall.class);
    }

    public static Expression objectOfExpression(Expression expression) {
        Expression base;
        MethodCall methodCall;
        if ((methodCall = expression.asInstanceOf(MethodCall.class)) != null) {
            if (methodCall.object().isInstanceOf(TypeExpression.class) && !methodCall.parameterExpressions().isEmpty()) {
                // e.g. Character.toUppercase(a) --> a
                base = methodCall.parameterExpressions().get(0);
            } else {
                // e.g. string.toUppercase() --> s
                base = methodCall.object();
            }
        } else {
            base = expression;
        }
        List<Variable> vars = base.variables(DescendModeEnum.NO).toList();
        if (vars.size() == 1) {
            return new VariableExpressionImpl(vars.get(0));
        }
        return expression;
    }

    public static boolean isNullCheck(Expression e) {
        Expression ee;
        Negation n;
        if ((n = e.asInstanceOf(Negation.class)) != null) {
            ee = n.expression();
        } else {
            ee = e;
        }
        BinaryOperator eq;
        return (eq = ee.asInstanceOf(BinaryOperator.class)) != null
               && (eq.lhs().isNullConstant() != eq.rhs().isNullConstant());
    }

}
