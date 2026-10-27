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


import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.variable.Variable;

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
