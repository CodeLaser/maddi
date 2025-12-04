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

public record LinkMethodCall(Runtime runtime) {

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
            assert rvPrimary instanceof ReturnVariable : "the links of the method return value must be in the return variable";
            assert !mc.methodInfo().isVoid() : "Cannot be a void function if we have a return variable";
            if (objectPrimary != null) {
                assert !mc.methodInfo().isStatic() : """
                        objectPrimary!=null indicates that we have an instance function.
                        Therefore we must translate 'this' to the method's object primary""";
                This thisVar = runtime.newThis(mc.methodInfo().typeInfo().asSimpleParameterizedType());
                TranslationMap tm = runtime.newTranslationMapBuilder().put(thisVar, objectPrimary).build();
                concreteReturnValue = mlv.ofReturnValue().changePrimaryTo(objectPrimary, tm);

            } else {
                // could be static, could be that there is no primary -- we should have made a dummy one?
                concreteReturnValue = LinksImpl.EMPTY;
            }
        } else {
            concreteReturnValue = LinksImpl.EMPTY;
        }
        return new ExpressionVisitor.Result(concreteReturnValue, new LinkedVariablesImpl(extra));
    }

}
