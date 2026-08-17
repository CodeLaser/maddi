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

package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Instance;
import org.e2immu.language.cst.api.expression.InstanceOf;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;

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
