package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.localvar.AppliedFunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.FunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;

public record LinkAppliedFunctionalInterface(JavaInspector javaInspector,
                                             Runtime runtime,
                                             LinkComputer.Options linkComputerOptions,
                                             VirtualFieldComputer virtualFieldComputer,
                                             MethodInfo currentMethod,
                                             VariableData variableData,
                                             Stage stage) {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkAppliedFunctionalInterface.class);

    public void go(
            Links.Builder builder,
            Function<Variable, List<Links>> paramProvider,
            Link link,
            AppliedFunctionalInterfaceVariable applied,
            Set<Variable> extraModified,
            Variable fromTranslated,
            LinkNature linkNature,
            Variable objectPrimary) {
        List<Links> list = paramProvider.apply(applied.sourceOfFunctionalInterface());
       // boolean match = matchAppliedFunctionalInterfaceToFunctionalInterfaceVariable(link, builder,
       //         extraModified, list);
      //  if (match) {
       //     return;
       // }
        List<Links> translated = replaceParametersByEvalInApplied(list, applied.params());
        List<LinkFunctionalInterface.Triplet> toAdd =
                applied.sourceOfFunctionalInterface() == null ? List.of()
                        : new LinkFunctionalInterface(runtime, virtualFieldComputer, currentMethod)
                        .go(applied.sourceOfFunctionalInterface().parameterizedType(),
                                fromTranslated, linkNature, builder.primary(), translated,
                                objectPrimary);
        toAdd.forEach(t -> builder.add(t.from(), t.linkNature(), t.to()));
    }


    private boolean matchAppliedFunctionalInterfaceToFunctionalInterfaceVariable(Link link,
                                                                                 Links.Builder builder,
                                                                                 Set<Variable> extraModified,
                                                                                 List<Links> list) {
        boolean match = false;
        for (Links links : list) {
            if (links.primary() instanceof ReturnVariable rv) {
                // TestModificationFunctional,1b; direct lambda
                MethodLinkedVariables mlv = rv.methodInfo().analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
                if (mlv != null) {
                    extraModified.addAll(mlv.modified());
                }
                links.stream().filter(l -> l.from().equals(links.primary()))
                        .forEach(l -> builder.add(LinkNatureImpl.IS_ASSIGNED_FROM, l.to()));
                match = true;
            } else if (links.isEmpty() && variableData != null) {
                Variable list0Primary = links.primary();
                VariableInfoContainer vic = variableData.variableInfoContainerOrNull(list0Primary.fullyQualifiedName());
                if (vic != null) {
                    VariableInfo vi = vic.best(stage);
                    List<FunctionalInterfaceVariable> fis = vi.linkedVariables().stream()
                            .filter(l -> l.to() instanceof FunctionalInterfaceVariable)
                            .map(l -> (FunctionalInterfaceVariable) l.to())
                            .toList();
                    for (FunctionalInterfaceVariable fi : fis) {
                        LOGGER.debug("Applying FI {} in AFI {}", fi, link);
                        extraModified.addAll(fi.modified());
                        fi.result().links().stream().filter(l -> l.from().equals(fi.result().links().primary()))
                                .forEach(l -> builder.add(LinkNatureImpl.IS_ASSIGNED_FROM, l.to()));
                    }
                    match = true;
                }
            }
        }
        return match;
    }


    private List<Links> replaceParametersByEvalInApplied(List<Links> list, List<Result> params) {
        return list.stream().map(links -> replaceParametersByEvalInApplied(links, params)).toList();
    }

    private Links replaceParametersByEvalInApplied(Links links, List<Result> params) {
        Links.Builder builder = new LinksImpl.Builder(links.primary());
        for (Link link : links) {
            if (link.to() instanceof ParameterInfo pi) {
                // replace
                assert pi.index() < params.size();
                Variable primary = Objects.requireNonNullElse(params.get(pi.index()).links().primary(), link.to());
                builder.add(link.from(), link.linkNature(), primary);
            } else if (link.to() instanceof FieldReference fr && fr.scopeVariable() instanceof ParameterInfo pi) {
                Result result = params.get(pi.index());
                Variable primary = Objects.requireNonNullElse(result.links().primary(), link.to());
                if (primary instanceof LocalVariable) {
                    // see TestStaticBiFunction,6: no direct mapping

                    // this is the old "join" of previous implementations; we should call expand now
                    Links ls = new LinkGraph(javaInspector, runtime, linkComputerOptions.checkDuplicateNames())
                            .indirect(links.primary(), link, result.links());
                    if (ls != null) builder.addAllDistinct(ls);

                } else {
                    builder.add(link.from(), link.linkNature(), runtime.newFieldReference(fr.fieldInfo(),
                            runtime.newVariableExpression(primary), fr.fieldInfo().type()));
                }
            } else {
                // copy
                builder.add(link.from(), link.linkNature(), link.to());
            }
        }
        return builder.build();
    }

}
