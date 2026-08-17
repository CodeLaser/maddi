package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.modification.prepwork.variable.Links;
import io.codelaser.maddi.modification.prepwork.variable.impl.LinksImpl;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.variable.Variable;

public record AssignLinksToLocal(Runtime runtime) {

    public Links go(Variable target, Links links) {
        Variable primary = links.primary();
        Links.Builder rvBuilder = new LinksImpl.Builder(target);

        if (primary != null) {
            rvBuilder.add(LinkNatureImpl.IS_ASSIGNED_FROM, primary);
        }
        if (target != null) {
            Links reassigned = links.changePrimaryTo(runtime, target);
            reassigned.linkSet().forEach(link -> rvBuilder.add(link.from(), link.linkNature(), link.to()));
        }
        return rvBuilder.build();
    }
}
