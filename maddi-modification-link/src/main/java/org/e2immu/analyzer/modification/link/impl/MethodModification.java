package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.Stage;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.MethodReference;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record MethodModification(Runtime runtime, VariableData variableData, Stage stage) {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodModification.class);

    public Set<Variable> go(MethodCall mc, Variable objectPrimary,
                            List<Result> params, MethodLinkedVariables methodLinkedVariables) {
        Set<Variable> modified = new HashSet<>();
        if (objectPrimary != null && !mc.methodInfo().isFinalizer()) {
            if (mc.methodInfo().isModifying()) {
                modified.add(objectPrimary);
            }
        }
        for(ParameterInfo pi: mc.methodInfo().parameters()) {
            if (pi.isModified()) {
                if (pi.isVarArgs()) {
                    for (int i = mc.methodInfo().parameters().size() - 1; i < mc.parameterExpressions().size(); i++) {
                        Result rp = params.get(i);
                        handleModifiedParameter(mc.parameterExpressions().get(i), rp, modified);
                    }
                } else {
                    Result rp = params.get(pi.index());
                    handleModifiedParameter(mc.parameterExpressions().get(pi.index()), rp, modified);
                }
            }
        }
        return modified;
    }

    private void handleModifiedParameter(Expression argument, Result rp, Set<Variable> modified) {
        if (rp.links() != null && rp.links().primary() != null) {
            modified.add(rp.links().primary());
        }
        if (argument instanceof MethodReference mr) {
            propagateModificationOfObject(modified, mr);
        }
    }

    private void propagateModificationOfObject(Set<Variable> modified, MethodReference mr) {
        if (mr.methodInfo().isModifying() && mr.scope() instanceof VariableExpression ve) {
            modified.add(ve.variable());
        }
    }


    /*
    code for propagation of applied functional
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
        return Set.of();*/
}
