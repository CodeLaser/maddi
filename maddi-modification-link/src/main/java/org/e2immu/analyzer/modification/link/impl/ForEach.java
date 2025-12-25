package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.Stage;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.Iterator;
import java.util.List;

public record ForEach(Runtime runtime, ExpressionVisitor expressionVisitor) {

    public ExpressionVisitor.Result linkIntoIterable(ParameterizedType elementType,
                                                     Expression forEachExpression,
                                                     VariableData previousVd,
                                                     Stage stageOfPrevious) {
        TypeInfo iterator = runtime.getFullyQualified(Iterator.class, false);
        TypeInfo iterableType = runtime.getFullyQualified(Iterable.class, false);
        MethodInfo iterableIterator = iterableType.findUniqueMethod("iterator", 0);
        ParameterizedType concreteIteratorType = runtime.newParameterizedType(iterator,
                List.of(elementType.ensureBoxed(runtime)));
        MethodCall mcIterator = runtime.newMethodCallBuilder()
                .setSource(runtime.noSource())
                .setObject(forEachExpression)
                .setMethodInfo(iterableIterator)
                .setParameterExpressions(List.of())
                .setConcreteReturnType(concreteIteratorType)
                .build();
        MethodInfo iteratorNext = iterator.findUniqueMethod("next", 0);
        MethodCall mc = runtime.newMethodCallBuilder()
                .setSource(runtime.noSource())
                .setObject(mcIterator)
                .setMethodInfo(iteratorNext)
                .setParameterExpressions(List.of())
                .setConcreteReturnType(elementType)
                .build();
        return expressionVisitor.visit(mc, previousVd, stageOfPrevious);
    }
}
