package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.ArrayList;
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

        List<Link> links = new ArrayList<>();

        for (Link rvLink : mlv.ofReturnValue()) {
            for (Link objLink : object.links()) {
                if (objLink.from() == null && objLink.linkNature() == LinkNature.IS_IDENTICAL_TO) {
                    // this is the actual object, as a direct variable
                    Link link = rvLink.replaceThis(runtime, objLink.to(), mc.methodInfo().typeInfo());
                    links.add(link);
                }
            }
        }
        return new ExpressionVisitor.Result(new LinksImpl(links), new LinkedVariablesImpl(extra));
    }
}
