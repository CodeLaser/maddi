package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public record LinkMethodCall(Runtime runtime, AtomicInteger variableCounter) {

    public ExpressionVisitor.Result methodCall(MethodCall mc,
                                               ExpressionVisitor.Result object,
                                               List<ExpressionVisitor.Result> params,
                                               MethodLinkedVariables mlv) {
        Links concreteReturnValue;

        Map<Variable, Links> extra = new HashMap<>(object.extra().map());
        params.forEach(r -> r.extra().forEach(e ->
                extra.merge(e.getKey(), e.getValue(), Links::merge)));

        Variable objectPrimary = object.links().primary();
        Variable rvPrimary = mlv.ofReturnValue().primary();
        if (rvPrimary != null) {
            assert rvPrimary instanceof ReturnVariable
                    : "the links of the method return value must be in the return variable";
            assert !mc.methodInfo().isVoid() : "Cannot be a void function if we have a return variable";
            Variable newPrimary;
            TranslationMap.Builder tmBuilder = runtime.newTranslationMapBuilder();
            if (objectPrimary == null) {
                // make a temporary variable holding the result of the method call; it'll get filtered out in the end
                newPrimary = runtime.newLocalVariable("rv" + variableCounter.getAndIncrement(),
                        rvPrimary.parameterizedType());
            } else {
                assert !mc.methodInfo().isStatic() : """
                        objectPrimary!=null indicates that we have an instance function.
                        Therefore we must translate 'this' to the method's object primary""";
                newPrimary = objectPrimary;
                This thisVar = runtime.newThis(mc.methodInfo().typeInfo().asSimpleParameterizedType());
                tmBuilder.put(thisVar, newPrimary);
            }
            // the return value can also contain references to parameters... we should replace them by
            // actual arguments
            int index = 0;
            for (ExpressionVisitor.Result pr : params) {
                if (pr.links().primary() != null) {
                    tmBuilder.put(mc.methodInfo().parameters().get(index), pr.links().primary());
                }
                ++index;
            }
            concreteReturnValue = mlv.ofReturnValue().changePrimaryTo(runtime, newPrimary, tmBuilder.build());


        } else {
            concreteReturnValue = LinksImpl.EMPTY;
        }
        return new ExpressionVisitor.Result(concreteReturnValue, new LinkedVariablesImpl(extra));
    }

}
