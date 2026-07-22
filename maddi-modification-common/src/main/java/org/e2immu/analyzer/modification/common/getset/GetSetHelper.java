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
import org.e2immu.language.cst.api.statement.BreakOrContinueStatement;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.statement.ThrowStatement;
import org.e2immu.language.cst.api.statement.YieldStatement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
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

    Leading-guard tolerance
    ------------------------
    Recognition does not require the return/assignment to be the *first* statement, only the first *non-inert*
    one: a getter/setter may be preceded by a prefix of "inert guard" statements. The motivating case is an
    accessor that opens with a gated diagnostic, e.g.

        PropertyValueMap analysis() {
            if (ConsumptionEdgeRecorder.ENABLED) { ConsumptionEdgeRecorder.record(this); }  // inert guard
            return propertyValueMap;                                                          // the getter
        }

    which is plainly the getter it looks like, but whose first statement is an IfStatement, not a return.

    Why this is sound even though recognition sits *below* the analysis (it runs in PrepAnalyzer, before any
    modification / link / immutability verdict exists, and its single output GET_SET_FIELD is trusted verbatim by
    the entire stack above -- notably ExpressionVisitor, which *replaces* every call `x.m()` with a read of
    `x.field`, bypassing the body): the guard must be provably immaterial to that replacement, checked purely
    syntactically here. {@link #isInertGuard} accepts a statement only if, across its whole subtree, it

      1. falls through -- no return / yield / throw / break / continue (so the trailing return is the only exit,
         and the guard cannot introduce a second behaviour);
      2. writes no field of `this` -- no assignment to a recursively-'this' field/array (the returned field, and
         every sibling, is untouched);
      3. references no field of `this` at all, in condition or body (subsumes 2 for reads, and keeps the branch
         decision independent of the object's state);
      4. makes no call *on* `this` -- a recursively-'this' receiver could mutate the object's own state.

    A call that merely *passes* `this` to a static/other-object method (`record(this)`) is allowed: its only
    possible effect is on state the object does not own -- a disclaimed static side effect in the sense of
    road-to-immutability section 050 (@StaticSideEffects / @IgnoreModifications), which by construction cannot cap
    the object's immutability, so dropping it at the call site corrupts no verdict. What is deliberately NOT
    tolerated: a guard that reads/writes an own field, calls a method on `this`, or can exit early -- those are a
    second behaviour, not a getter with a benign prelude.
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
            // peel a prefix of inert guard statements; recognition then runs on the first non-inert statement
            int guards = leadingInertGuardCount(methodBody);
            java.util.List<Statement> statements = methodBody.statements();
            if (guards >= statements.size()) return null; // only guards, no return/assignment: not an accessor
            Statement s0 = statements.get(guards);
            if (s0 instanceof ReturnStatement rs) {
                // a getter is exactly [inert guards] return this.field; -- unreachable code after a return is a
                // compile error, so the return is necessarily the last statement, but assert it to be explicit
                if (guards != statements.size() - 1) return null;
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
            } else if (checkSetMethodEnsureFluent(methodInfo, methodBody, guards) && s0 instanceof ExpressionAsStatement eas) {
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

    // {@code guards} is the number of leading inert-guard statements already peeled (see doGetSetAnalysis): the
    // setter shape is matched on the suffix statements[guards..], so a guarded setter is recognised exactly as
    // the bare one. A plain setter is that suffix of length 1 (the assignment); a fluent setter is length 2
    // (assignment, then `return this;`).
    private static boolean checkSetMethodEnsureFluent(MethodInfo methodInfo, Block methodBody, int guards) {
        int rest = methodBody.size() - guards;
        Value.Bool fluent;
        if (rest == 1 && methodBody.statements().get(guards) instanceof ExpressionAsStatement) {
            fluent = ValueImpl.BoolImpl.FALSE;
        } else if (rest == 2
                   && methodBody.statements().get(guards) instanceof ExpressionAsStatement
                   && methodBody.statements().get(guards + 1) instanceof ReturnStatement rs
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

    /**
     * The number of leading statements of {@code methodBody} that are inert guards (see the class comment):
     * scanning stops at the first non-inert statement, which is where getter/setter recognition then begins.
     */
    private static int leadingInertGuardCount(Block methodBody) {
        int n = methodBody.size();
        int g = 0;
        while (g < n && isInertGuard(methodBody.statements().get(g))) g++;
        return g;
    }

    /**
     * Is this leading statement immaterial to get/set recognition? See the class comment for the four
     * requirements and the soundness argument. Checked purely syntactically over the statement's whole subtree,
     * because at this point (PrepAnalyzer) no modification/link verdict exists yet.
     */
    private static boolean isInertGuard(Statement s) {
        boolean[] inert = {true};
        s.visit(e -> {
            if (!inert[0]) return false;
            // 1. no early exit: the trailing return must be the only way out of the method
            if (e instanceof ReturnStatement || e instanceof YieldStatement
                || e instanceof ThrowStatement || e instanceof BreakOrContinueStatement) {
                inert[0] = false;
                return false;
            }
            // 2. no write to a field of 'this'
            if (e instanceof Assignment a && isThisScoped(a.variableTarget())) {
                inert[0] = false;
                return false;
            }
            // 3. no reference to a field of 'this' (read or write, condition or body)
            if (e instanceof VariableExpression ve && isThisScoped(ve.variable())) {
                inert[0] = false;
                return false;
            }
            // 4. no call *on* 'this' (a recursively-'this' receiver could mutate the object's own state);
            //    a call that merely passes 'this' as an argument -- record(this) -- is allowed (see class comment)
            if (e instanceof MethodCall mc && mc.object() instanceof VariableExpression ove
                && ove.variable() instanceof This) {
                inert[0] = false;
                return false;
            }
            return true;
        });
        return inert[0];
    }

    /** A variable that reads or writes a field of {@code this}: a recursively-'this'-scoped field, or an index into one. */
    private static boolean isThisScoped(Variable v) {
        if (v instanceof FieldReference fr) return fr.scopeIsRecursivelyThis();
        if (v instanceof DependentVariable dv) return isThisScoped(dv.arrayVariable());
        return false;
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
