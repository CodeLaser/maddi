package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.impl.localvar.IntermediateVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.LinkGraph.followGraph;
import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.UNMODIFIED_VARIABLE;

record WriteLinksAndModification(Runtime runtime) {

    record WriteResult(Map<Variable, Links> newLinks, Set<Variable> modifiedOutsideVariableData) {
    }

    @NotNull WriteResult go(Statement statement,
                            VariableData vd,
                            Set<Variable> previouslyModified,
                            Set<Variable> modifiedDuringEvaluation,
                            Map<LinkGraph.V, Map<LinkGraph.V, LinkNature>> graph) {
        Map<Variable, Links> newLinkedVariables = new HashMap<>();
        Set<Variable> modifiedVariables = Stream.concat(previouslyModified.stream(),
                modifiedDuringEvaluation.stream()).collect(Collectors.toUnmodifiableSet());
        Set<Variable> unmarkedModifications = new HashSet<>(modifiedVariables);

        vd.variableInfoStream(Stage.EVALUATION).forEach(vi -> {
            doVariable(statement, graph, vi, unmarkedModifications, modifiedVariables, newLinkedVariables);
        });

        return new WriteResult(newLinkedVariables, unmarkedModifications);
    }

    private void doVariable(Statement statement,
                            Map<LinkGraph.V, Map<LinkGraph.V, LinkNature>> graph,
                            VariableInfo vi,
                            Set<Variable> unmarkedModifications,
                            Set<Variable> modifiedVariables,
                            Map<Variable, Links> newLinkedVariables) {
        Variable variable = vi.variable();
        unmarkedModifications.remove(variable);

        Links.Builder builder = followGraph(graph, variable);

        if (variable instanceof ReturnVariable rv) {
            handleReturnVariable(rv, builder);
        } else {
            boolean unmodified = assignedInThisStatement(statement, vi)
                                 || !modifiedVariables.contains(variable)
                                    && notLinkedToModified(builder, modifiedVariables);
            builder.removeIf(l -> Util.lvPrimaryOrNull(l.to()) instanceof IntermediateVariable);

            if (!vi.analysis().haveAnalyzedValueFor(UNMODIFIED_VARIABLE)) {
                Value.Bool newValue = ValueImpl.BoolImpl.from(unmodified);
                vi.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE, newValue);
            }
            if (!unmodified) {
                // TODO turning ⊆ into ~ when modified is best done directly in followGraph(),
                //  otherwise we cannot change ≤ into ∩ easily for derivative relations (if we must have them)
                builder.replaceSubsetSuperset(variable);
            }
        }
        Links newLinks = builder.build();
        if (newLinkedVariables.put(variable, newLinks) != null) {
            throw new UnsupportedOperationException("Each real variable must be a primary");
        }
    }

    private void handleReturnVariable(ReturnVariable rv, Links.Builder builder) {
        // replace all intermediates by a marker; don't worry about duplicate makers for now
        // don't bother with modifications; not relevant.
        boolean needMarker = false;
        List<Link> newLinks = new ArrayList<>();
        for (Link link : builder) {
            if (link.linkNature().isIdenticalTo()
                && link.to() instanceof IntermediateVariable iv && iv.isNewObject()) {
                needMarker = true;
            } else if (LinkVariable.acceptForLinkedVariables(link.from())
                       && LinkVariable.acceptForLinkedVariables(link.to())) {
                newLinks.add(link);
            }
        }
        if (needMarker) {
            Variable marker = MarkerVariable.someValue(runtime, rv.methodInfo().returnType());
            newLinks.addFirst(new LinksImpl.LinkImpl(rv, IS_ASSIGNED_FROM, marker));
        }
        builder.replaceAll(newLinks);
    }

    private static boolean assignedInThisStatement(Statement statement, VariableInfo vi) {
        String index = statement.source().index();
        return vi.assignments().contains(index) && !vi.reads().indices().contains(index);
    }

    private boolean notLinkedToModified(Links.Builder builder, Set<Variable> modifiedVariables) {
        for (Link link : builder) {
            Variable toPrimary = Util.primary(link.to());
            if (modifiedVariables.contains(toPrimary)) {
                LinkNature ln = link.linkNature();
                if (ln == IS_IDENTICAL_TO
                    && link.to() instanceof FieldReference fr
                    && VirtualFieldComputer.isVirtualModificationField(fr.fieldInfo())) {
                    return false;
                }
                if (ln == CONTAINS_AS_FIELD
                    || ln == SHARES_FIELDS // see impl/TestInstanceOf,2
                    || ln == CONTAINS_AS_MEMBER) {
                    return false;
                }
                // for now, we ONLY propagate through §m
               /* if (ln == IS_ASSIGNED_TO) {
                    Value.Immutable immutable = new AnalysisHelper().typeImmutable(link.to().parameterizedType());
                    return immutable.isAtLeastImmutableHC();
                }*/
            }
        }
        return true;
    }

}
