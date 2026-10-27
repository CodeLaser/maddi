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


import io.codelaser.maddi.cst.api.expression.And;
import io.codelaser.maddi.cst.api.expression.Equals;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;

import java.util.List;

public record LhsRhs(Expression lhs, Expression rhs) {

    public static List<LhsRhs> extractEqualities(Expression e) {
      //  DelayedExpression de;
      //  if ((de = e.asInstanceOf(DelayedExpression.class)) != null) {
      //      return extractEqualities(de.getOriginal());
      //  }
        Equals equals;
        if ((equals = e.asInstanceOf(Equals.class)) != null) {
            return List.of(new LhsRhs(equals.lhs(), equals.rhs()));
        }
        LhsRhs equalsMethod = equalsMethodCall(e);
        if (equalsMethod != null) return List.of(equalsMethod);
        And and;
        if ((and = e.asInstanceOf(And.class)) != null) {
            return and.expressions().stream().flatMap(clause -> extractEqualities(clause).stream()).toList();
        }
        return List.of();
    }

    public static LhsRhs equalsMethodCall(Expression e) {
        MethodCall mc;
        if (e != null
            && (mc = e.asInstanceOf(MethodCall.class)) != null
            && mc.methodInfo().name().equals("equals")
            && mc.parameterExpressions().size() == 1) {
            Expression rhs = mc.parameterExpressions().get(0);
            if (rhs.isConstant()) {
                return new LhsRhs(rhs, mc.object());
            }
            return new LhsRhs(mc.object(), rhs);
        }
        return null;
    }
}
