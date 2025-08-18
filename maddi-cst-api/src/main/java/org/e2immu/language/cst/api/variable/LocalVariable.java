package org.e2immu.language.cst.api.variable;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;

/*
value held by the local variable at the time of creation.
can be null (not known, not relevant)
 */
public interface LocalVariable extends Variable {

    boolean isUnnamed();

    Expression assignmentExpression();

    @Override
    default String fullyQualifiedName() {
        return simpleName();
    }

    LocalVariable translate(TranslationMap translationMap);

    LocalVariable withType(ParameterizedType type);

    /**
     * @param name the new name
     * @return a new local variable object, with the new name
     */
    LocalVariable withName(String name);

    /**
     * @param expression the new assignment expression
     * @return a new local variable object, with the new assignment expression
     */
    LocalVariable withAssignmentExpression(Expression expression);
}
