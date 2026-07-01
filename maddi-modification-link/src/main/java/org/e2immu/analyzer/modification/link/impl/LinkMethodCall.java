package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.localvar.AppliedFunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.IntermediateVariable;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ObjectCreationVariableImpl;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

public record LinkMethodCall(JavaInspector javaInspector,
                             Runtime runtime,
                             LinkComputer.Options linkComputerOptions,
                             VirtualFieldComputer virtualFieldComputer,
                             AtomicInteger variableCounter,
                             MethodInfo currentMethod,
                             VariableData variableData,
                             Stage stage) {

    public Result constructorCall(ConstructorCall cc,
                                  Result object,
                                  List<Result> params,
                                  MethodLinkedVariables mlv) {
        Map<Variable, Links> extra = new HashMap<>(object.extra().map());
        MethodInfo methodInfo = cc.constructor();
        copyParamsIntoExtra(methodInfo.parameters(), params, extra);

        // A constructor is 'parametersToObject' with the new object as receiver: each argument flows into a field.
        // E.g. 'new Box<>(x)' -> 'make.t←0:x'; 'new Box<>(box.get())' -> 'makeFromGet.t←0:box.t';
        // 'new Pair<>(a, b)' -> 'makePair.a←0:a, makePair.b←1:b'.
        Links newObjectLinks = parametersToObject(methodInfo, object, params, mlv);
        if (linkComputerOptions.trackObjectCreations()) {
            // LEAF (option on) — additionally record that the temporary object primary was assigned a fresh object,
            // so later modification analysis can tell freshly-created (unaliased) objects apart.
            ObjectCreationVariable oc = new ObjectCreationVariableImpl(methodInfo, cc.source().compact(),
                    cc.parameterizedType());
            Variable objectPrimary = object.links().primary();
            assert objectPrimary != null;
            Links toObjectCreation = new LinksImpl.Builder(objectPrimary)
                    .add(LinkNatureImpl.IS_ASSIGNED_FROM, oc).build();
            extra.merge(objectPrimary, toObjectCreation, Links::merge);
        }
        return new Result(newObjectLinks, new LinkedVariablesImpl(extra));
    }

    private static void copyParamsIntoExtra(List<ParameterInfo> formalParameters,
                                            List<Result> params,
                                            Map<Variable, Links> extra) {
        int i = 0;
        for (Result r : params) {
            ParameterInfo pi = formalParameters.get(Math.min(formalParameters.size() - 1, i));
            if (!pi.parameterizedType().isFunctionalInterface()) {
                if (r.links() != null && r.links().primary() != null) {
                    extra.merge(r.links().primary(), r.links(), Links::merge);
                }
                r.extra().forEach(e ->
                        extra.merge(e.getKey(), e.getValue(), Links::merge));
            }// else: the extra information will be used, but it will not be passed on
            // it contains links related to the lambda parameters, lambda return variable, etc.
            // See e.g. TestForEachLambda,4
            ++i;
        }
    }

    private record LM(Links links, Set<Variable> extraModified) {
    }

    private List<Result> expandParams(MethodLinkedVariables mlv, boolean externalLibrary, List<Result> paramsIn) {
        List<Result> results = new ArrayList<>(paramsIn.size());
        int i = 0;
        int n = mlv.ofParameters().size();
        boolean rv = mlv.ofReturnValue().stream().anyMatch(l -> l.to() instanceof AppliedFunctionalInterfaceVariable);
        for (Result result : paramsIn) {
            Links pLinks = mlv.ofParameters().get(Math.min(n - 1, i));
            Result r;
            // why external library? we'll assume that any functional interface passed to an external method,
            // is actually called.
            if (externalLibrary
                || rv // see TestModifiedFunctional,4
                || pLinks.stream().anyMatch(l -> l.to() instanceof AppliedFunctionalInterfaceVariable)) {
                r = result.expandFunctionalInterfaceVariables();
            } else {
                r = result;
            }
            results.add(r);
            ++i;
        }
        return results;
    }

    // we're trying for both method calls and normal constructor calls
    // the latter have a newly created temporary local variable as their object primary
    public Result methodCall(MethodInfo methodInfo,
                             ParameterizedType concreteReturnType,
                             Result object,
                             List<Result> paramsIn,
                             MethodLinkedVariables mlv) {
        Map<Variable, Links> extra = new HashMap<>(object.extra().map());
        List<Result> params = expandParams(mlv,
                // FIXME hard-coded
                methodInfo.typeInfo().compilationUnit().externalLibrary()
                && !methodInfo.typeInfo().packageName().startsWith("io.codelaser"),
                paramsIn);
        copyParamsIntoExtra(methodInfo.parameters(), params, extra);
        Variable objectPrimary = object.links().primary();
        if (!object.links().isEmpty()) {
            extra.put(objectPrimary, object.links());
        }
        Links concreteReturnValue;
        Set<Variable> extraModified;
        // What does the RETURN VALUE of this call link to? Three leaves:
        if (methodInfo.isSAMOfStandardFunctionalInterface()) {
            assert methodInfo == methodInfo.typeInfo().singleAbstractMethod();
            // LEAF 1 — the call IS the SAM of a lambda held by a parameter: 'apply'/'accept'/'get' invoked on a
            // Function/Consumer/Supplier argument. The result links to a marker (AppliedFunctionalInterfaceVariable)
            // that is resolved when we know the concrete lambda. E.g. 'Y run(Function<X,Y> f, X x){return f.apply(x);}'
            // -> run's return decorated with '$_afi' on f. See TestModificationFunctional.
            concreteReturnValue = samOfFunctionalInterface(concreteReturnType, params, objectPrimary, extra);
            extraModified = new HashSet<>();
        } else if (!mlv.ofReturnValue().isEmpty()) {
            assert mlv.ofReturnValue().primary() instanceof ReturnVariable;
            // LEAF 2 — the callee summary relates its return value to the object/arguments: any accessor, fluent, or
            // factory-return. E.g. 'X read(Box<X> box){return box.get();}' -> 'read←0:box.t';
            // 'Box<X> fluent(Box<X> box){return box.self();}' -> 'fluent←0:box';
            // 'X id(X x){return identity(x);}' -> 'id←0:x' (argument passed straight through).
            LM lm = objectToReturnValue(methodInfo, concreteReturnType, params, mlv, objectPrimary);
            concreteReturnValue = lm.links;
            extraModified = lm.extraModified;
        } else {
            // LEAF 3 — the call returns nothing linkable (void, or a return unrelated to inputs): no return links.
            // E.g. 'void write(Box<X> box, X x){box.set(x);}' and 'void clear(Box<X> box){box.clear();}' -> '--> -'.
            concreteReturnValue = LinksImpl.EMPTY;
            extraModified = new HashSet<>();
        }
        // Independently: how do the ARGUMENTS relate to the OBJECT (or, for a static call, to each other)?
        if (objectPrimary != null) {
            Links newObjectLinks = parametersToObject(methodInfo, object, params, mlv);
            if (newObjectLinks.primary() instanceof This && !newObjectLinks.isEmpty()) {
                // LEAF 4a — the receiver is the implicit 'this' (an unqualified own-method call): the object-side
                // links come back rooted at 'this'; regroup them so each real field/variable becomes its own primary.
                newObjectLinks.removeThisAsPrimary().forEach(links ->
                        extra.merge(links.primary(), links, Links::merge));
            } else {
                // LEAF 4b — an ordinary receiver: merge the argument→object links under the object's primary.
                // E.g. 'box.set(x)' -> 'box*.t←x*'; 'this.myBox.set(x)' (writeField) roots at 'this.myBox';
                // 'boxes[0].set(x)' (arraySet) roots at the indexed element 'boxes[0]'.
                extra.merge(objectPrimary, newObjectLinks, Links::merge);
            }
        } else {
            // LEAF 5 — a static call has no object: relate the arguments to one another, and apply any functional
            // interface passed as an argument. E.g. 'static <T> void transfer(Box<T> from, Box<T> to){to.set(from.get());}'
            // -> 'from.t*→to*.t' (linksBetweenParameters); a passed Consumer that is invoked -> appliedFunctionalInterfaces.
            linksBetweenParameters(methodInfo, params, mlv, extra);
            appliedFunctionalInterfaces(methodInfo, params, extraModified, mlv, extra);
        }
        return new Result(concreteReturnValue, new LinkedVariablesImpl(extra))
                .addModified(extraModified, null);
    }

    private void appliedFunctionalInterfaces(MethodInfo methodInfo,
                                             List<Result> params,
                                             Set<Variable> extraModified,
                                             MethodLinkedVariables mlv,
                                             Map<Variable, Links> extra) {
        assert mlv.ofParameters().size() == methodInfo.parameters().size();
        for (Links links : mlv.ofParameters()) {
            if (links != null) {
                for (Link link : links) {
                    if (link.to() instanceof AppliedFunctionalInterfaceVariable applied) {
                        // TestModificationFunctional,7,8
                        LinkAppliedFunctionalInterface handler = new LinkAppliedFunctionalInterface(javaInspector, runtime,
                                linkComputerOptions, virtualFieldComputer, currentMethod, variableData, stage);
                        LinksImpl.Builder builder = new LinksImpl.Builder(links.primary());
                        Function<Variable, List<Links>> paramProvider = v ->
                                v instanceof ParameterInfo pi ? List.of(params.get(pi.index()).links()) : List.of();
                        handler.go(builder, paramProvider, applied, extraModified, null, link.linkNature(),
                                null);
                        extra.merge(links.primary(), builder.build(), Links::merge);
                    }
                }
            }
        }
    }

    private void linksBetweenParameters(MethodInfo methodInfo,
                                        List<Result> params,
                                        MethodLinkedVariables mlv,
                                        Map<Variable, Links> extra) {
        int i = 0;
        assert mlv.ofParameters().size() == methodInfo.parameters().size();
        for (Links links : mlv.ofParameters()) {
            if (links != null) {
                for (Link link : links) {
                    ParameterInfo to = Util.parameterPrimaryOrNull(link.to());
                    if (to == null || to.index() == i) {
                        // LEAF (skip) — the callee link's target is not another parameter, or is the same parameter
                        // (a self-link, handled elsewhere): nothing to cross-link here. See Test1,2.
                        // if to's index == i, we're talking the same index, rather than between indices
                        continue;
                    }
                    Variable toPrimary = params.get(to.index()).links().primary();
                    if (toPrimary != null) {
                        TranslationMap toTm = new VariableTranslationMap(runtime).put(to, toPrimary);
                        Variable translatedTo = toTm.translateVariableRecursively(link.to());

                        ParameterInfo from = methodInfo.parameters().get(i);
                        if (from.isVarArgs()) {
                            // LEAF — the 'from' parameter is varargs: fan the link out over every actual argument,
                            // weakening the nature per element (varargsLinkNature) and downgrading each element name.
                            // E.g. 'fillAll(Box<T> target, T... vs)' at 'fillAll(box,a,b)' -> 'box.t∈a, box.t∈b'.
                            LinkNature linkNature = varargsLinkNature(link.linkNature());
                            for (int j = i; j < params.size(); ++j) {
                                Variable fromPrimary = params.get(j).links().primary();
                                Variable linkFrom = downgrade(link.from(), fromPrimary);
                                // loop over all the elements in the varargs
                                addCrosslink(fromPrimary, extra, from, linkFrom, linkNature, translatedTo);
                            }
                        } else {
                            // LEAF — a plain (non-varargs) parameter: one direct cross-link between the two
                            // arguments. E.g. static 'transfer(Box from, Box to){to.set(from.get());}' -> 'from.t→to.t'.
                            Variable fromPrimary = params.get(i).links().primary();
                            addCrosslink(fromPrimary, extra, from, link.from(), link.linkNature(), translatedTo);
                        }
                    }
                }
            }
            ++i;
        }
    }

    // Given the callee's varargs-element link 'from' and the actual argument's primary, produce the per-element
    // 'from' variable at the call site.
    private Variable downgrade(Variable from, Variable fromPrimary) {
        if (from.parameterizedType().arrays() > 1) {
            // a multi-dimensional varargs ('T[]... rows'): drop one array dimension, and the trailing plural 's'
            // from the name ('rows' -> 'row')
            String simpleName = from.simpleName();
            assert simpleName.length() > 1 && simpleName.endsWith("s");
            ParameterizedType newType = from.parameterizedType().copyWithOneFewerArrays();
            String nameOneFewerS = simpleName.substring(0, simpleName.length() - 1);
            if (from instanceof FieldReference fr) {
                FieldInfo fi = runtime.newFieldInfo(nameOneFewerS, false, newType, fr.fieldInfo().owner());
                return runtime.newFieldReference(fi, fr.scope(), newType);
            }
            if (from instanceof LocalVariable) {
                return runtime.newLocalVariable(nameOneFewerS, newType);
            }
            throw new UnsupportedOperationException();
        }
        // a single array dimension (the common 'T... vs'), or an already-indexed element (e.g. 'vs[0]', produced by
        // a for-each over the varargs array): the element is the actual argument itself. Covers the varargs param
        // (a LocalVariable), a field ('elements.ts -> t') and a DependentVariable uniformly.
        return fromPrimary;
    }

    private void addCrosslink(Variable fromPrimary,
                              Map<Variable, Links> extra,
                              ParameterInfo from,
                              Variable linkFrom,
                              LinkNature linkNature,
                              Variable translatedTo) {
        Links.Builder builder = new LinksImpl.Builder(fromPrimary);
        TranslationMap fromTm = new VariableTranslationMap(runtime).put(from, fromPrimary);
        Variable translatedFrom = fromTm.translateVariableRecursively(linkFrom);
        if (translatedFrom != null) {
            builder.add(translatedFrom, linkNature, translatedTo);
            Links links = builder.build();
            extra.merge(fromPrimary, links, Links::merge);
        } // else: TestVarious,11
    }

    // A varargs argument is a single element of the callee's array parameter, so a whole-collection relationship at
    // the array level weakens to an element-level one for each actual argument. E.g. 'fillAll(Box<T> target, T... vs)'
    // linking 'target.t ⊆ vs' becomes, per argument, 'box.t ∈ a' and 'box.t ∈ b' (see useFill).
    private LinkNature varargsLinkNature(LinkNature linkNature) {
        if (linkNature == LinkNatureImpl.IS_SUBSET_OF) {
            return LinkNatureImpl.IS_ELEMENT_OF; // ⊆ (subset of the array) -> ∈ (element of, per argument)
        }
        if (linkNature == LinkNatureImpl.IS_SUPERSET_OF) {
            return LinkNatureImpl.CONTAINS_AS_MEMBER; // ⊇ -> ∋ (contains, per argument)
        }
        return linkNature; // identity/shares-elements/... unchanged
    }

    private LM objectToReturnValue(MethodInfo methodInfo,
                                   ParameterizedType concreteReturnType,
                                   List<Result> params,
                                   MethodLinkedVariables mlv,
                                   Variable objectPrimary) {
        Links ofReturnValue = mlv.ofReturnValue();
        assert !methodInfo.isVoid() || methodInfo.isConstructor()
                : "Cannot be a void function if we have a return variable";
        Variable newPrimary = IntermediateVariable.returnValue(runtime, variableCounter.getAndIncrement(),
                concreteReturnType);
        VariableTranslationMap tm = new VariableTranslationMap(runtime);
        if (objectPrimary != null) {
            addThisHierarchyToObjectPrimaryToTmBuilder(methodInfo, tm, objectPrimary);
        }
        // the return value can also contain references to parameters... we should replace them by
        // actual arguments
        int index = 0;
        for (Result pr : params) {
            ParameterInfo from = methodInfo.parameters().get(Math.min(index, methodInfo.parameters().size() - 1));
            Variable to = Objects.requireNonNullElseGet(pr.links().primary(), () ->
                    IntermediateVariable.parameterValue(variableCounter.getAndIncrement(),
                            pr.getEvaluated().parameterizedType(), pr.getEvaluated()));
            tm.put(from, to);
            ++index;
        }
        tm.put(ofReturnValue.primary(), newPrimary);
        Function<Variable, List<Links>> samLinks = v ->
                v instanceof ParameterInfo pi ? List.of(params.get(pi.index()).links()) : List.of();
        Links.Builder builder = new LinksImpl.Builder(newPrimary);
        Set<Variable> extraModified = new HashSet<>();
        for (Link link : ofReturnValue.linkSet()) {
            // decoration links (functional-interface '↗/↖' markers) are handled by the FI machinery, not here
            if (!link.linkNature().isDecoration())
                if (link.from().equals(ofReturnValue.primary())) {
                    // LEAF — the from-side IS the return value itself: use the fresh return primary directly.
                    // E.g. 'read←0:box.t' ('box.get()') — the whole return value links to the object's field.
                    translateHandleFunctional(tm, link, newPrimary, link.linkNature(), builder, samLinks,
                            objectPrimary, extraModified);
                } else {
                    // LEAF — the from-side is a PART of the return value (a field/element of it): translate it.
                    // E.g. a factory 'Box<X> makeFromGet(...)' whose return field links: 'makeFromGet.t←0:box.t'.
                    Variable fromTranslated = tm.translateVariableRecursively(link.from());
                    translateHandleFunctional(tm, link, fromTranslated, link.linkNature(), builder, samLinks,
                            objectPrimary, extraModified);
                }
        }
        return new LM(builder.build(), extraModified);
    }

    private void translateHandleFunctional(TranslationMap defaultTm,
                                           Link link,
                                           Variable fromTranslated,
                                           LinkNature linkNature,
                                           Links.Builder builder,
                                           Function<Variable, List<Links>> paramProvider,
                                           Variable objectPrimary,
                                           Set<Variable> extraModified) {
        ParameterizedType parameterizedType = link.to().parameterizedType();
        boolean extraTest;
        // Where does this return-value link point? Three leaves:
        if (parameterizedType.isFunctionalInterface()) {
            // LEAF A — the return relates to a functional-interface argument (the method lifts a lambda through the
            // data). E.g. 'list.stream().map(mapper)': the result's elements relate to the source's elements via the
            // mapper's own return↔param relationship. Delegated to LinkFunctionalInterface. See TestStreamMapSpec.
            List<LinkFunctionalInterface.Triplet> toAdd =
                    new LinkFunctionalInterface(runtime, virtualFieldComputer, currentMethod)
                            .go(parameterizedType, fromTranslated, linkNature, builder.primary(),
                                    paramProvider.apply(link.to()), objectPrimary);
            toAdd.forEach(t -> builder.add(t.from(), t.linkNature(), t.to()));
            extraTest = true;
        } else if (link.to() instanceof AppliedFunctionalInterfaceVariable applied) {
            // LEAF B — the return relates to an already-applied functional interface: an indirection method that
            // internally calls a passed lambda and returns (part of) its result. Delegated to
            // LinkAppliedFunctionalInterface. See TestModificationFunctional,4.
            LinkAppliedFunctionalInterface handler = new LinkAppliedFunctionalInterface(javaInspector, runtime,
                    linkComputerOptions, virtualFieldComputer, currentMethod, variableData, stage);
            handler.go(builder, paramProvider, applied, extraModified, fromTranslated, linkNature, objectPrimary);
            extraTest = true;
        } else {
            // LEAF C — a plain (non-functional) target: no FI lifting needed. E.g. 'box.get()' -> 'rv←box.t'.
            extraTest = false;
        }
        // Add the direct link unless the functional handling above already produced the outgoing links for a
        // return-variable source (in which case a second, un-lifted link would be spurious).
        if (!extraTest || !isReturnVariable(Util.firstRealVariable(fromTranslated))) {
            builder.add(fromTranslated, linkNature, defaultTm.translateVariableRecursively(link.to()));
        }
    }

    private static boolean isReturnVariable(Variable v) {
        return v instanceof ReturnVariable || v instanceof IntermediateVariable iv && iv.isReturnVariable();
    }

    private Links samOfFunctionalInterface(ParameterizedType concreteReturnType,
                                           List<Result> params,
                                           Variable objectPrimary,
                                           Map<Variable, Links> extra) {
        ParameterInfo piPrimary = Util.parameterPrimaryOrNull(objectPrimary);
        if (piPrimary == null) {
            piPrimary = findLinkToParameter(objectPrimary);
        }
        if (piPrimary == null) {
            // LEAF — the lambda being invoked does not resolve to one of the current method's parameters (e.g. a
            // locally-created lambda, or a field): nothing to say about the caller. 'f.apply(x)' where f is unknown.
            return LinksImpl.EMPTY;
        }
        // LEAF — the SAM is invoked on a parameter 'piPrimary' (the Function/Consumer/... argument): decorate that
        // parameter with an AppliedFunctionalInterfaceVariable marker ('$_afi'), and return a link from it, to be
        // resolved once the concrete lambda bound to that parameter is known at the outer call site.
        Variable applied = new AppliedFunctionalInterfaceVariable(runtime,
                variableCounter.getAndIncrement(),
                concreteReturnType,
                piPrimary,
                params);
        Links decoration = new LinksImpl.Builder(objectPrimary).add(LinkNatureImpl.IS_DECORATED_WITH, applied).build();
        extra.put(objectPrimary, decoration);
        return new LinksImpl.Builder(applied).build();
    }

    private ParameterInfo findLinkToParameter(Variable objectPrimary) {
        if (variableData == null) return null;
        return objectPrimary.variableStreamDescend()
                .map(v -> {
                    VariableInfoContainer vic = variableData.variableInfoContainerOrNull(v.fullyQualifiedName());
                    if (vic != null) {
                        VariableInfo vi = vic.best(stage);
                        if (vi.linkedVariables() != null) {
                            return vi.linkedVariables().stream()
                                    .map(l -> Util.parameterPrimaryOrNull(l.to()))
                                    .filter(Objects::nonNull)
                                    .filter(pi -> pi.methodInfo() == currentMethod)
                                    .findFirst().orElse(null);
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private @NotNull Links parametersToObject(MethodInfo methodInfo,
                                              Result object,
                                              List<Result> params,
                                              MethodLinkedVariables mlv) {
        Variable objectPrimary = object.links().primary();
        int i = 0;
        Links.Builder builder = new LinksImpl.Builder(objectPrimary);
        VariableTranslationMap tm = new VariableTranslationMap(runtime);
        addThisHierarchyToObjectPrimaryToTmBuilder(methodInfo, tm, objectPrimary);
        for (ParameterInfo pi : methodInfo.parameters()) {
            if (pi.index() < params.size()) {
                Result result = params.get(pi.index());
                if (result != null && result.links() != null) {
                    Variable primary = result.links().primary();
                    tm.put(pi, Objects.requireNonNullElseGet(primary, () ->
                            IntermediateVariable.parameterValue(variableCounter.getAndIncrement(),
                                    result.getEvaluated().parameterizedType(), result.getEvaluated())));
                }
            }
        }
        for (Links links : mlv.ofParameters()) {
            assert links != null;
            ParameterInfo pi = methodInfo.parameters().get(i);
            if (i >= params.size()) continue; // varargs
            Result r = params.get(i);
            for (Link link : links) {
                Variable translatedFrom = tm.translateVariableRecursively(link.from());
                Variable translatedTo = tm.translateVariableRecursively(link.to());
                // Only the case "translatedTo is part of the object" contributes: we reverse the callee link so
                // that the object part is the primary (e.g. 'param ∈ this' becomes 'this ∋ arg'). A former second
                // branch here repeated the same condition (dead code) with a non-reversed nature; activating it
                // for "translatedFrom is part of the object" breaks functional-interface/stream linking
                // (TestForEachMethodReference, TestStreamBasics), so the from-side case is deliberately not handled.
                if (Util.isPartOf(objectPrimary, translatedTo)) {
                    // LEAF — the argument flows into (part of) the object: reverse the callee link so the object side
                    // is primary. 'set(T v){this.t=v;}' summary 'v→this.t' becomes, at 'box.set(x)', 'box.t←x'
                    // (write); for a two-field 'set(A,B)' each argument lands in its own field (putBoth -> pair.a←a,
                    // pair.b←b); 'copyFrom(dst,src){dst.set(src.get());}' -> 'dst.t←src.t'.
                    builder.add(translatedTo, link.linkNature().reverse(), translatedFrom);
                }
            }
            ParameterizedType parameterizedType = pi.parameterizedType();
            if (parameterizedType.isFunctionalInterface() && !r.extra().isEmpty()) {
                // LEAF — the argument is a consumer/functional interface whose captured links live in its extras:
                // lift the source's elements INTO the object's hidden content. E.g. 'list.forEach(target::add)':
                // 'list.§xs ~ target.§es'. Delegated to LinkFunctionalInterface. See TestStreamForEachSpec,
                // TestForEachMethodReference.
                // pi is a consumer, the link information is in the extras

                // link from the method call object (list) into this (not into the parameter, not the return value)
                // returnPrimary ~ this
                // fromTranslated ~ upscale this
                //
                List<Links> linksList = r.extra().map().entrySet().stream()
                        .filter(e -> e.getKey() instanceof ParameterInfo)
                        .map(Map.Entry::getValue)
                        .toList();
                VariableTranslationMap vtm = new VariableTranslationMap(runtime)
                        .put(links.primary(), objectPrimary);
                for (Link link : links) {
                    List<LinkFunctionalInterface.Triplet> toAdd =
                            new LinkFunctionalInterface(runtime, virtualFieldComputer, currentMethod).
                                    go(parameterizedType, link.from(),
                                            LinkNatureImpl.SHARES_ELEMENTS,
                                            builder.primary(),
                                            linksList,
                                            objectPrimary);
                    toAdd.forEach(t ->
                            builder.add(vtm.translateVariableRecursively(t.from()), t.linkNature(), t.to()));
                }
            }
            ++i;
        }
        return builder.build();
    }

    private void addThisHierarchyToObjectPrimaryToTmBuilder(MethodInfo methodInfo,
                                                            VariableTranslationMap tmBuilder,
                                                            Variable objectPrimary) {
        TypeInfo thisType = methodInfo.typeInfo();
        Stream<TypeInfo> stream = Stream.concat(Stream.of(thisType),
                thisType.superTypesExcludingJavaLangObject().stream());
        stream.forEach(ti -> {
            This thisVar = runtime.newThis(ti.asSimpleParameterizedType());
            tmBuilder.put(thisVar, objectPrimary);
        });
    }

}
