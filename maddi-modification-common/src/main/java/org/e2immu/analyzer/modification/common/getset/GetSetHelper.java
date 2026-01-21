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

package org.e2immu.analyzer.modification.common.getset;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import static org.e2immu.language.inspection.api.util.CreateSyntheticFieldsForGetSet.*;

public class GetSetHelper {

    /*
    a getter or accessor is a method that does nothing but return a field.
    a setter is a method that does nothing but set the value of a field. It may return "this".

    there are some complications:
    - the field only has to have a scope which is recursively 'this', it does not have to be 'this' directly.
    - overriding a method which has a @GetSet marker (e.g. interface method)
    - array access or direct list indexing. we will always determine from the context whether we're dealing with
      indexing or not, and store the whole field in one go

    TODO overrides! compatibility, direct override, etc.

    Obvious limitations
    - we're using a field to store the information, we should use field reference (see TestGetSet)
    - alternative packing systems: a map with string constants, getting values at fixed positions (see TestGetSet)
     */
    public static boolean doGetSetAnalysis(MethodInfo methodInfo, Block methodBody) {
        assert methodBody != null;
        assert !methodInfo.isConstructor();
        if (methodBody.isEmpty()) {
            if (!methodInfo.isAbstract()) {
                methodInfo.analysis().getOrCreate(PropertyImpl.FLUENT_METHOD, () -> ValueImpl.BoolImpl.FALSE);
            }
            return false;
        }
        return methodInfo.analysis().getOrCreate(PropertyImpl.GET_SET_FIELD, () -> {
            Statement s0 = methodBody.statements().getFirst();
            if (s0 instanceof ReturnStatement rs) {
                if (!methodInfo.analysis().haveAnalyzedValueFor(PropertyImpl.FLUENT_METHOD)) {
                    methodInfo.analysis().set(PropertyImpl.FLUENT_METHOD, ValueImpl.BoolImpl.FALSE);
                }
                if (rs.expression() instanceof VariableExpression ve
                    && ve.variable() instanceof FieldReference fr
                    && fr.scopeIsRecursivelyThis()) {
                    // return this.field;
                    return new ValueImpl.GetSetValueImpl(fr.fieldInfo(), false, -1, false);
                } else if (rs.expression() instanceof VariableExpression ve
                           && ve.variable() instanceof DependentVariable dv
                           && dv.arrayVariable() instanceof FieldReference fr
                           && dv.indexVariable() instanceof ParameterInfo
                           && fr.scopeIsRecursivelyThis()) {
                    // return this.objects[param]
                    return new ValueImpl.GetSetValueImpl(fr.fieldInfo(), false, 0, false);
                } else if (rs.expression() instanceof MethodCall mc
                           && overrideOf(mc.methodInfo(), LIST_GET)
                           && mc.parameterExpressions().getFirst() instanceof VariableExpression ve
                           && ve.variable() instanceof ParameterInfo
                           && mc.object() instanceof VariableExpression ve2
                           && ve2.variable() instanceof FieldReference fr
                           && fr.scopeIsRecursivelyThis()) {
                    // return this.list.get(param);
                    return new ValueImpl.GetSetValueImpl(fr.fieldInfo(), false, 0, true);
                } else {
                    return null;
                }
            } else if (checkSetMethodEnsureFluent(methodInfo, methodBody) && s0 instanceof ExpressionAsStatement eas) {
                if (eas.expression() instanceof Assignment a
                    && a.variableTarget() instanceof FieldReference fr && fr.scopeIsRecursivelyThis()
                    && a.value() instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo) {
                    // this.field = param
                    return new ValueImpl.GetSetValueImpl(fr.fieldInfo(), true, -1, false);
                } else if (eas.expression() instanceof Assignment a
                           && a.variableTarget() instanceof DependentVariable dv
                           && dv.arrayVariable() instanceof FieldReference fr && fr.scopeIsRecursivelyThis()
                           && dv.indexVariable() instanceof ParameterInfo
                           && a.value() instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo) {
                    // this.objects[i] = param
                    return new ValueImpl.GetSetValueImpl(fr.fieldInfo(), true, 0, false);
                } else if (eas.expression() instanceof MethodCall mc
                           && overrideOf(mc.methodInfo(), LIST_SET)
                           && mc.parameterExpressions().get(0) instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo
                           && mc.parameterExpressions().get(1) instanceof VariableExpression ve2 && ve2.variable() instanceof ParameterInfo
                           && mc.object() instanceof VariableExpression ve3 && ve3.variable() instanceof FieldReference fr
                           && fr.scopeIsRecursivelyThis()) {
                    // this.list.set(i, object)
                    return new ValueImpl.GetSetValueImpl(fr.fieldInfo(), true, 0, true);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }) != null;
    }

    private static boolean checkSetMethodEnsureFluent(MethodInfo methodInfo, Block methodBody) {
        Value.Bool fluent;
        if (methodBody.size() == 1 && methodBody.lastStatement() instanceof ExpressionAsStatement) {
            fluent = ValueImpl.BoolImpl.FALSE;
        } else if (methodBody.size() == 2
                   && methodBody.statements().get(0) instanceof ExpressionAsStatement
                   && methodBody.statements().get(1) instanceof ReturnStatement rs
                   && rs.expression() instanceof VariableExpression veThis
                   && veThis.variable() instanceof This) {
            fluent = ValueImpl.BoolImpl.TRUE;
        } else {
            return false;
        }
        if (!methodInfo.analysis().haveAnalyzedValueFor(PropertyImpl.FLUENT_METHOD)) {
            methodInfo.analysis().set(PropertyImpl.FLUENT_METHOD, fluent);
        }
        return true;
    }

    public static boolean isSetter(MethodInfo mi) {
        // there could be an accessor called "set()", so for that to be a setter, it must have at least one parameter
        return mi.isVoid() || isComputeFluent(mi) || mi.name().startsWith("set") && !mi.parameters().isEmpty();
    }

    public static boolean isComputeFluent(MethodInfo mi) {
        String fluentFqn = Fluent.class.getCanonicalName();
        if (mi.annotations().stream().anyMatch(ae -> fluentFqn.equals(ae.typeInfo().fullyQualifiedName()))) {
            return true;
        }
        return !mi.methodBody().isEmpty()
               && mi.methodBody().lastStatement() instanceof ReturnStatement rs
               && rs.expression() instanceof VariableExpression ve && ve.variable() instanceof This;
    }

    public static int parameterIndexOfIndex(MethodInfo mi, boolean setter) {
        if (setter) {
            if (2 == mi.parameters().size()) {
                if (mi.parameters().getFirst().parameterizedType().isInt()) return 0;
                if (mi.parameters().get(1).parameterizedType().isInt()) return 1;
            }
            return -1;
        }
        // getter
        return mi.parameters().size() == 1 && mi.parameters().getFirst().parameterizedType().isInt() ? 0 : -1;
    }
}
