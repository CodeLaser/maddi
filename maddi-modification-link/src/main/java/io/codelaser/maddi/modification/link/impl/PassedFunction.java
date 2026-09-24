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

package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.TypeExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

import java.util.List;

/**
 * Call-site specialisation of a parameter that a method modifies only by handing it to one of its own functional
 * parameters ({@link PropertyImpl#MODIFIED_THROUGH_PASSED_FUNCTION}). vavr's {@code Value.toQueue()} calls
 * {@code ValueModule.toTraversable(this, Queue.empty(), Queue::of, Queue::ofAll)}, whose body is
 * {@code ofAll.apply(value)}: {@code value} is modified for SOME function ({@code Function.apply(@Modified T)}),
 * but not for {@code Queue::ofAll}, so {@code toQueue()} does not modify its receiver.
 * <p>
 * Shared by the engine ({@link MethodModification}) and the MODREACH shadow pass, which must agree.
 */
public final class PassedFunction {

    private PassedFunction() {
    }

    public static String entry(int functionalParameterIndex, int samArgumentIndex) {
        return functionalParameterIndex + ":" + samArgumentIndex;
    }

    /**
     * @return true when the argument for {@code calleeParameter} is certainly NOT modified at this call site: the
     * callee modifies it only through its functional parameters, and every function passed for them here provably
     * leaves the corresponding argument alone. False whenever that cannot be established.
     */
    public static boolean unmodifiedAtCallSite(ParameterInfo calleeParameter, List<Expression> arguments) {
        Value.SetOfStrings through = calleeParameter.analysis().getOrDefault(
                PropertyImpl.MODIFIED_THROUGH_PASSED_FUNCTION, ValueImpl.SetOfStringsImpl.EMPTY_SET);
        if (through.set().isEmpty()) return false;
        for (String entry : through.set()) {
            int colon = entry.indexOf(':');
            int functionalParameterIndex = Integer.parseInt(entry.substring(0, colon));
            int samArgumentIndex = Integer.parseInt(entry.substring(colon + 1));
            if (functionalParameterIndex >= arguments.size()) return false;
            Boolean modifies = modifiesArgument(arguments.get(functionalParameterIndex), samArgumentIndex);
            if (modifies == null || modifies) return false;
        }
        return true;
    }

    /**
     * Does the function expression, applied, modify its SAM argument number {@code samArgumentIndex}?
     *
     * @return null when unknown: not a method reference or lambda, or the verdict is not decided yet
     */
    static Boolean modifiesArgument(Expression function, int samArgumentIndex) {
        if (function instanceof MethodReference mr) {
            MethodInfo target = mr.methodInfo();
            int index = samArgumentIndex;
            if (!target.isStatic() && !target.isConstructor() && mr.scope() instanceof TypeExpression) {
                // unbound receiver (String::length): SAM argument 0 is the receiver
                if (index == 0) {
                    Value.Bool nonModifying = target.analysis().getOrNull(PropertyImpl.NON_MODIFYING_METHOD,
                            ValueImpl.BoolImpl.class);
                    return nonModifying == null ? null : nonModifying.isFalse();
                }
                index--;
            }
            return parameterModified(target, index);
        }
        if (function instanceof Lambda lambda) {
            return parameterModified(lambda.methodInfo(), samArgumentIndex);
        }
        return null;
    }

    private static Boolean parameterModified(MethodInfo methodInfo, int index) {
        if (index >= methodInfo.parameters().size()) return null;
        ParameterInfo pi = methodInfo.parameters().get(index);
        if (pi.isVarArgs()) return null;
        Value.Bool unmodified = pi.analysis().getOrNull(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
        return unmodified == null ? null : unmodified.isFalse();
    }
}
