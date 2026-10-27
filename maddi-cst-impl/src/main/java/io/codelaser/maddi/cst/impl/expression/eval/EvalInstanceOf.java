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

package io.codelaser.maddi.cst.impl.expression.eval;

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Instance;
import io.codelaser.maddi.cst.api.expression.InstanceOf;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

public class EvalInstanceOf {
    private final Runtime runtime;

    public EvalInstanceOf(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression eval(Expression value, InstanceOf instanceOf) {
        if (value.isNullConstant()) {
            return runtime.constantFalse();
        }
        if (instanceOf.patternVariable() != null && !instanceOf.patternVariable().unnamedPattern()) {
            // local variable, or record pattern
            return instanceOf;
        }
        ParameterizedType testType = instanceOf.testType();
        if (testType.isJavaLangObject()) {
            return runtime.constantTrue();
        }
        if (value instanceof VariableExpression ve) {
            ParameterizedType type = ve.variable().parameterizedType();
            /*
             An UNBOUNDED WILDCARD carries no type information at all: '?' stands for some unknown type, so a
             test against it may very well succeed, and nothing may be concluded either way. Falling through to
             the isAssignableFrom checks below folded it to FALSE, because nothing is assignable from '?'.

             Concretely, for a List<?> parameter, 'list.get(i) instanceof String' became statically false, and
             everything guarded by it was then eliminated as dead code: a six-statement method reduced to a
             single 'return "no"'. The identical method taking List<Object> or a raw List was unaffected. Found
             downstream, where it silently emptied the deduplication fingerprint of any method destructuring a
             wildcard-typed collection.
             */
            if (type.typeInfo() == null && type.typeParameter() == null) {
                return instanceOf;
            }
            // trivial cast to same or higher type
            if (runtime.isAssignableFrom(testType, type)) {
                return runtime.constantTrue();
            }
            // see TestOperators for examples
            if (!testType.typeInfo().isInterface() && !runtime.isAssignableFrom(type, testType)) {
                return runtime.constantFalse();
            }
            // keep as is
            return instanceOf;
        }
        Instance instance;
        if ((instance = value.asInstanceOf(Instance.class)) != null) {
            boolean isAssignable = runtime.isAssignableFrom(testType, instance.parameterizedType());
            return runtime.newBoolean(isAssignable);
        }
        return instanceOf;
    }
}
