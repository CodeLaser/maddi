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
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.integration.JavaInspector;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/*
Part of the LinkMethodCall code, objectToReturnValue.

Call a method with a method reference argument, knowing that the method reference will be used/called.
The "$_afi0" applied function interface variable is the marker for the result of calling the method reference.

TestBiFunction
    link = make ← $_afi0
    translated = extract ← this.ix, extract ← this.jx
    toAdd = $__rv1 ← this.ix, $__rv1 ← this.jx
TestStaticBiFunction

TestModificationFunctional,1: the MR is a direct argument
TestModificationFunctional,2,2b: the MR is a field inside the argument. What we can do is open up the record variable,
and find out that it links to a functional interface variable, nr.function ← Λ$_fi1.
In TestBiFunction, this fiv has already been expanded at the beginning of LinkMethodCall.

In TestModificationFunctional,5, the MR is a "field" defined by a getter on an interface
 */
public record LinkAppliedFunctionalInterface(JavaInspector javaInspector,
                                             Runtime runtime,
                                             LinkComputer.Options linkComputerOptions,
                                             VirtualFieldComputer virtualFieldComputer,
                                             MethodInfo currentMethod,
                                             VariableData variableData,
                                             Stage stage) {
    public void go(
            Links.Builder builder,
            Function<Variable, List<Links>> paramProvider,
            AppliedFunctionalInterfaceVariable applied,
            Set<Variable> extraModified,
            Variable fromTranslated,
            LinkNature linkNature,
            Variable objectPrimary) {
        List<Links> list = paramProvider.apply(applied.sourceOfFunctionalInterface());
        ParameterizedType functionalType;
        if (!applied.sourceOfFunctionalInterface().parameterizedType().isStandardFunctionalInterface()) {
            // we must search for links to FIVs, and expand them
            SearchResult sr = searchAndExpand(list);
            if (sr == null) return;
            // do the default translation from formal parameter to argument
            extraModified.addAll(sr.extraModified);
            functionalType = sr.functionalType;
            list = List.of(sr.links);
        } else if (isLinkedToParameter(list)) {
            // TestModificationFunctional,4; indirection method
            builder.add(LinkNatureImpl.IS_ASSIGNED_FROM, applied);
            return;
        } else {
            functionalType = applied.sourceOfFunctionalInterface().parameterizedType();
        }
        List<Links> translated = replaceParametersByEvalInApplied(list, applied.params());
        List<LinkFunctionalInterface.Triplet> toAdd = new LinkFunctionalInterface(runtime, virtualFieldComputer,
                currentMethod).go(functionalType, fromTranslated, linkNature, builder.primary(), translated,
                objectPrimary);
        toAdd.forEach(t -> builder.add(t.from(), t.linkNature(), t.to()));
    }

    private boolean isLinkedToParameter(List<Links> list) {
        return list.stream().anyMatch(links ->
                links.stream().anyMatch(l -> l.to() instanceof ParameterInfo pi
                                             && pi.methodInfo().equals(currentMethod)
                                             && l.linkNature().isAssignedFrom()));
    }

    private record SearchResult(ParameterizedType functionalType, Links links, Set<Variable> extraModified) {
    }

    private SearchResult searchAndExpand(List<Links> list) {
        Set<Variable> extraModified = new HashSet<>();
        for (Links links : list) {
            if (links.primary() == null) continue;
            Variable list0Primary = links.primary();
            VariableInfoContainer vic = variableData.variableInfoContainerOrNull(list0Primary.fullyQualifiedName());
            if (vic != null) {
                VariableInfo vi = vic.best(stage);
                List<FunctionalInterfaceVariable> fis = vi.linkedVariables().stream()
                        .filter(l -> l.to() instanceof FunctionalInterfaceVariable)
                        .map(l -> (FunctionalInterfaceVariable) l.to())
                        .toList();
                for (FunctionalInterfaceVariable fi : fis) {
                    TranslationMap.Builder builder = runtime.newTranslationMapBuilder();
                    if (fi.result().links().primary() instanceof ReturnVariable rv) {
                        // FIXME this is hard-coded, needs general implementation
                        // See TestModificationFunctional,5,6,7
                        builder.put(rv.methodInfo().parameters().getFirst(), list0Primary);
                    }
                    TranslationMap tm = builder.build();
                    fi.modified().stream()
                            .map(tm::translateVariableRecursively)
                            .filter(this::acceptForExtra)
                            .forEach(extraModified::add);
                    Result expanded = fi.result().expandFunctionalInterfaceVariables();
                    return new SearchResult(fi.parameterizedType(), expanded.links(), extraModified);
                }

            }
        }
        return null;
    }

    private boolean acceptForExtra(Variable v) {
        return !(v instanceof ParameterInfo pi) || pi.methodInfo().equals(currentMethod);
    }

    private List<Links> replaceParametersByEvalInApplied(List<Links> list, List<Result> params) {
        return list.stream().map(links -> replaceParametersByEvalInApplied(links, params)).toList();
    }

    /*
    the default is to add the link directly, as for TestBiFunction, link to the field this.jx
    but when the link points to a parameter, we must replace this parameter by the argument to the SAM
    In the case of TestBiFunction, this is the field ix.


     */
    private Links replaceParametersByEvalInApplied(Links links, List<Result> params) {
        if (links.primary() == null) return LinksImpl.EMPTY;
        Links.Builder builder = new LinksImpl.Builder(links.primary());
        for (Link link : links) {
            if (link.to() instanceof ParameterInfo pi) {
                // replace (TestBiFunction, link to extract:0:x)
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
                // copy (TestBiFunction, link to this.jx)
                builder.add(link.from(), link.linkNature(), link.to());
            }
        }
        return builder.build();
    }

}
