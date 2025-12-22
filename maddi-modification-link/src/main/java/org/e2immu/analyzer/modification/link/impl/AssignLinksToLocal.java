package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;

public record AssignLinksToLocal(Runtime runtime) {

    public Links go(Variable target, Links links) {
        Variable primary = links.primary();
        Links.Builder rvBuilder = new LinksImpl.Builder(target);

        if(primary != null) {
            rvBuilder.add(LinkNatureImpl.IS_IDENTICAL_TO, primary);
        }
        if(target != null) {
            Links reassigned = links.changePrimaryTo(runtime, target);
            reassigned.linkSet().forEach(link -> rvBuilder.add(link.from(), link.linkNature(), link.to()));
        }
        return rvBuilder.build();
    }
}
