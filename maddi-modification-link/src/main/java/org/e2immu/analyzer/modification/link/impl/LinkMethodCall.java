package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.TypeInfo;
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

        Map<Variable, Links> extra = new HashMap<>(object.extra().map());
        params.forEach(r -> r.extra().forEach(e ->
                extra.merge(e.getKey(), e.getValue(), Links::merge)));

        Variable objectPrimary = object.links().primary();
        Variable rvPrimary = mlv.ofReturnValue().primary();
        Links.Builder rvBuilder = new LinksImpl.Builder(rvPrimary);
        if (objectPrimary != null && rvPrimary != null) {
            for (Link rvLink : mlv.ofReturnValue()) {
                for (Link objLink : object.links()) {
                    if (objectPrimary.equals(objLink.from()) && objLink.linkNature() == LinkNature.IS_IDENTICAL_TO) {
                        // this is the actual object, as a direct variable
                        rvBuilder.add(rvLink.linkNature(), replaceThis(objLink.to(), mc.methodInfo().typeInfo()));
                    }
                }
            }
        }
        return new ExpressionVisitor.Result(rvBuilder.build(), new LinkedVariablesImpl(extra));
    }

    private Variable replaceThis(Variable variable, TypeInfo thisType) {
        This thisVar = runtime.newThis(thisType.asParameterizedType());
        TranslationMap tm = runtime.newTranslationMapBuilder()
                .put(thisVar, variable)
                .build();
        return tm.translateVariableRecursively(variable);
    }
}
