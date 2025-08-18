package org.e2immu.language.cst.api.element;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.Variable;

public interface Visitor {

    default boolean beforeModule(ModuleInfo moduleInfo) {
        return true; // go deeper;
    }

    default void afterModule() {
        // do nothing
    }

    default boolean beforeStatement(Statement statement) {
        return true; // go deeper
    }

    default void afterStatement(Statement statement) {
        // do nothing
    }

    default void startSubBlock(int n) {
        // do nothing
    }

    default void endSubBlock(int n) {
        // do nothing
    }

    default boolean beforeExpression(Expression expression) {
        return true; // go deeper
    }

    default void afterExpression(Expression expression) {
        // do nothing
    }

    default boolean beforeVariable(Variable variable) {
        return true;
    }

    default void afterVariable(Variable variable) {
        // do nothing
    }

    default boolean beforeJavaDoc(JavaDoc javaDoc) {
        return true;
    }

    default void afterJavaDoc(JavaDoc javaDoc) {
        // do nothing
    }

    default boolean beforeElement(Element element) {
        return true;
    }

    default void afterElement(Element element) {
    }
}
