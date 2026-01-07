package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.impl.localvar.AppliedFunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.FunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public record PropagateApplied(Runtime runtime) {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropagateApplied.class);

    public Set<Variable> go(VariableData variableData, Stage stage, List<Result> params, List<Links> linksList) {
        if (variableData != null) {
            int i = 0;
            for (Result result : params) {
                Links links = linksList.get(Math.min(i, linksList.size() - 1));
                for (Link link : links) {
                    if (link.linkNature().isDecoration() && link.to() instanceof AppliedFunctionalInterfaceVariable afi) {
                        Variable nr = result.links().primary();
                        VariableInfo nrVi = variableData.variableInfo(nr, stage);
                        FunctionalInterfaceVariable concreteFunctional = nrVi.linkedVariables().stream()
                                .filter(l -> l.linkNature().isIdenticalTo()
                                             && l.to() instanceof FunctionalInterfaceVariable)
                                .map(l -> (FunctionalInterfaceVariable) l.to())
                                .findFirst().orElseThrow();
                        LOGGER.debug("Propagate applied functional interface? {}", link);
                    }
                }
                ++i;
            }
        }
        return Set.of();
    }
}
