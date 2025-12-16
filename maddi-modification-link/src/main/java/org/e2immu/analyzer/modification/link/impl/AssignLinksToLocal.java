package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;

public record AssignLinksToLocal(Runtime runtime) {

    public Links go(Variable target, Links links) {
        Variable primary = links.primary();
        Links.Builder rvBuilder = new LinksImpl.Builder(target);

        rvBuilder.add(LinkNature.IS_IDENTICAL_TO, primary);
        Links reassigned = links.changePrimaryTo(runtime, target);
        reassigned.linkSet().forEach(link -> rvBuilder.add(link.from(), link.linkNature(), link.to()));
        return rvBuilder.build();
    }
}
