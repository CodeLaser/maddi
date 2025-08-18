/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.language.cst.impl.expression.util;


import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class ExtractComponentsOfTooComplex implements Predicate<Element> {
    final Set<Expression> expressions = new HashSet<>();
    final Set<Variable> variablesToIgnore = new HashSet<>();

    private final Runtime runtime;

    public ExtractComponentsOfTooComplex(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    public boolean test(Element e) {
        if (e instanceof Lambda lambda) {
            variablesToIgnore.addAll(lambda.parameters());
            variablesToIgnore.add(runtime.newThis(lambda.methodInfo().typeInfo().asParameterizedType()));
            return true;
        }
        if (e instanceof VariableExpression ve) {
            add(ve.variable(), ve);
            return false;
        }
        if (e instanceof MethodCall mc) {
            expressions.add(mc);
            return false;
        }
        return true;
    }

    protected void add(Variable v, VariableExpression variableExpression) {
        // essentially, parameters and this from lambda's inside the method; they stay as they are
        if (variablesToIgnore.contains(v)) return;

        // all the rest is added, work will be done during expansion
        expressions.add(variableExpression);
    }

    public Set<Expression> getExpressions() {
        return expressions;
    }
}
