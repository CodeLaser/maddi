package org.e2immu.analyzer.modification.common.getset;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.GET_SET_FIELD;

public record ApplyGetSetTranslation(Runtime runtime) implements TranslationMap {
    @Override
    public Variable translateVariableRecursively(Variable variable) {
        return runtime.translateVariableRecursively(this, variable);
    }

    @Override
    public boolean isClearAnalysis() {
        return false;
    }

    /*
    a chain of @Fluent setters will be translated to a chain of comma expressions, as follows:

    new Builder().setA(a).setB(b).setC(c).build()

    becomes

    (e.a = a, e.b = b, e.c = c, e.build())  with e the 'new Builder()' object.

    This ensures that all assignments have the same scope object.
    To do this properly, we must (a) properly implement comma expressions, (b) have simple @Fluent already analyzed

     */
    @Override
    public Expression translateExpression(Expression expression) {
        if (expression instanceof Lambda || expression instanceof ConstructorCall cc && cc.anonymousClass() != null) {
            return null; // keep hands off
        }
        if (expression instanceof SwitchExpression se) {
            Expression tSelect = se.selector().translate(this);
            if (tSelect == se.selector()) return null; // no change, we'll not go deeper here but someplace else
            return se.withSelector(tSelect);
        }
        if (expression instanceof MethodCall mc) {
            ensureGetSet(mc.methodInfo());

            Value.FieldValue getSet = mc.methodInfo().analysis().getOrDefault(GET_SET_FIELD,
                    ValueImpl.GetSetValueImpl.EMPTY);
            if (getSet.field() != null) {
                ParameterizedType type = mc.concreteReturnType();
                Source objectSourceIn = mc.object().source();
                Source objectSource = objectSourceIn == null ? runtime.noSource() : objectSourceIn;
                assert objectSource != null;

                if (getSet.setter()) {
                    // setter
                    return replaceByAssignmentOrComma(mc, getSet, type, objectSource);
                }
                // getter
                return replaceByVariableExpression(mc, getSet, type, objectSource);
            }
        }
        return expression;
    }

    // getter
    private VariableExpression replaceByVariableExpression(MethodCall mc,
                                                           Value.FieldValue getSet,
                                                           ParameterizedType type,
                                                           Source objectSource) {
        Expression object = unwrap(mc.object().translate(this));
        Variable variable;
        if (mc.parameterExpressions().size() == 1) {
            ParameterizedType arrayType = type.copyWithArrays(type.arrays() + 1);
            FieldReference fr = runtime.newFieldReference(getSet.field(), object, arrayType);
            Expression index = mc.parameterExpressions().getFirst();
            Expression replacedIndex = index.translate(this);
            VariableExpression arrayExpression = runtime.newVariableExpressionBuilder()
                    .setVariable(fr)
                    .setSource(objectSource)
                    .build();
            variable = runtime.newDependentVariable(arrayExpression, replacedIndex, type);
        } else {
            variable = runtime.newFieldReference(getSet.field(), object, type);
        }
        return runtime.newVariableExpressionBuilder().setVariable(variable).setSource(mc.source()).build();
    }

    // setter
    private Expression replaceByAssignmentOrComma(MethodCall mc,
                                                  Value.FieldValue getSet,
                                                  ParameterizedType type,
                                                  Source objectSource) {

        Expression translatedObject = mc.object().translate(this);
        Expression object;
        CommaExpression previousFluent;
        if (translatedObject instanceof CommaExpression ce) {
            previousFluent = ce;
            object = ce.expressions().getLast();
        } else {
            object = unwrap(translatedObject);
            previousFluent = null;
        }
        Variable v;
        if (getSet.hasIndex()) {
            Expression replacedIndex = mc.parameterExpressions().get(getSet.parameterIndexOfIndex())
                    .translate(this);
            ParameterizedType arrayType = type.copyWithArrays(type.arrays() + 1);
            FieldReference fr = runtime.newFieldReference(getSet.field(), object, arrayType);
            VariableExpression arrayExpression = runtime.newVariableExpressionBuilder()
                    .setVariable(fr)
                    .setSource(objectSource)
                    .build();
            v = runtime.newDependentVariable(arrayExpression, replacedIndex, type);
        } else {
          //    v = runtime.newFieldReference(getSet.field(), object, getSet.field().type());
            v = runtime.newFieldReference(getSet.field(), object, object.parameterizedType());
        }
        VariableExpression target = runtime.newVariableExpressionBuilder().setVariable(v).setSource(mc.source()).build();
        Expression replacedValue = mc.parameterExpressions().get(getSet.parameterIndexOfValue()).translate(this);
        Assignment assignment = runtime.newAssignmentBuilder()
                .setSource(mc.source()).
                setTarget(target).setValue(replacedValue).build();

        if (mc.methodInfo().isFluent() || previousFluent != null) {
            CommaExpression.Builder builder = runtime().newCommaBuilder();
            if (previousFluent != null) {
                builder.addExpressions(previousFluent.expressions().subList(0, previousFluent.expressions().size() - 1));
            }
            builder.addExpression(assignment);
            builder.addExpression(object);
            return builder.build();
        }
        return assignment;
    }

    private static Expression unwrap(Expression object) {
        if (object instanceof EnclosedExpression ee) return unwrap(ee.inner());
        if (object instanceof Cast cast && cast.expression() instanceof VariableExpression ve) {
            return unwrap(ve);
        }
        return object;
    }

    /*
    Depending on the order of the primary types in "PrepAnalyzer.doPrimaryTypes", the getSet analysis may have
    happened already, or not. TestCallGraph2 contains an example where this can go wrong, but note that the execution
    order for the modification analyzer does not catch this problem.

     */
    private static void ensureGetSet(MethodInfo methodInfo) {
        if (!methodInfo.isAbstract()) {
            GetSetHelper.doGetSetAnalysis(methodInfo, methodInfo.methodBody());
        }
    }
}
