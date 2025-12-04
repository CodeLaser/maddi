package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkedVariables;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.ArrayList;
import java.util.List;

public class Expand {

    public static Links connect(LocalVariable lv, Links links) {
        return links; // FIXME more!
    }

    public static Links expandReturnValue(ReturnVariable returnVariable, Links links, LinkedVariables extra, VariableData vd) {
        List<Link> list = new ArrayList<>(links.links());
        Link link = links.first();
        if (link != null) {
            list.set(0, new LinkImpl(returnVariable, link.linkNature(), link.to()));
        }
        list.removeIf(l -> l.from() != null && containsLocal(l.from()) || containsLocal(l.to()));
        return new LinksImpl(list);
    }

    private static boolean containsLocal(Variable variable) {
        return variable.variableStreamDescend().anyMatch(v -> v instanceof LocalVariable);
    }
}
