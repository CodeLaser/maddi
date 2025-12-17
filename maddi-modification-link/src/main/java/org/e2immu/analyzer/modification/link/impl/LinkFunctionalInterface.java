package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public record LinkFunctionalInterface(Runtime runtime, VirtualFieldComputer virtualFieldComputer,
                                      MethodInfo currentMethod) {

    record Triplet(Variable from, LinkNature linkNature, Variable to) {
    }

    List<Triplet> go(ParameterInfo pi,
                     Variable fromTranslated,
                     LinkNature linkNature,
                     Variable returnPrimary,
                     IntFunction<Links> paramProvider,
                     Variable objectPrimary) {

        // FUNCTIONAL INTERFACE

        MethodInfo sam = pi.parameterizedType().typeInfo().singleAbstractMethod();
        if (sam.parameters().isEmpty()) {

            // SUPPLIER: grab the "to" of the primary, if it is present (get==c.alternative in the example of a Supplier)
            Links links = paramProvider.apply(pi.index());
            return links.linkSet().stream()
                    .filter(l -> l.from().equals(links.primary())).map(Link::to)
                    .map(to -> new Triplet(fromTranslated, linkNature, to))
                    .limit(1)
                    .toList();
        }

        // FUNCTION
        Links links = paramProvider.apply(pi.index());
        Set<Variable> toPrimaries = links.linkSet().stream().map(l -> Util.primary(l.to()))
                .collect(Collectors.toUnmodifiableSet());
        List<Triplet> result = new ArrayList<>();
        for (Variable newPrimary : toPrimaries) {
            TranslationMap tm3 = runtime.newTranslationMapBuilder().put(newPrimary, objectPrimary).build(); // FIXME
            TranslationMap tm2 = runtime.newTranslationMapBuilder().put(links.primary(), returnPrimary).build();
            for (Link l : links) {
                Variable tfrom = tm2.translateVariableRecursively(l.from());
                Variable upscaledFrom = virtualFieldComputer.upscale(tfrom, objectPrimary, returnPrimary,
                        currentMethod.typeInfo(), false, 0);
                int arrayDelta = 0;
                Variable lTo = l.to();
                if (lTo instanceof DependentVariable) {
                    continue; // skip this link?
                }
                //while(lTo instanceof DependentVariable dv){
                //    lTo = dv.arrayVariable();
                //    --arrayDelta;
                //}
                Variable tto = tm3.translateVariableRecursively(lTo);
                Variable upscaledTo = virtualFieldComputer.upscale(tto, objectPrimary, returnPrimary,
                        currentMethod.typeInfo(), true, arrayDelta);
                result.add(new Triplet(upscaledFrom, l.linkNature(), upscaledTo));
            }
        }
        return result;
    }
}
