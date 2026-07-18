package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.impl.translate.VariableTranslationMap;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.CONTAINS_AS_MEMBER;
import static org.e2immu.analyzer.modification.prepwork.Util.virtual;

/**
 * The FI <em>lifting engine</em>: given the links of the lambda/method-reference bound to an FI parameter (the wrapped
 * {@code FunctionalInterfaceVariable}'s result, as {@code linksList}), produce the {@link Triplet} links that the FI
 * application contributes at the call site. See {@code linking-manual.md} §7.3.
 * <p>
 * Parameters: {@code functionalInterfaceType} = the SAM's type (Function/Consumer/Supplier/…); {@code fromTranslated} =
 * the call-site variable standing for the SAM's "from" side (e.g. the map result's hidden content); {@code linkNature} =
 * the nature to use; {@code returnPrimary} = the map target primary; {@code objectPrimary} = the map source primary.
 * <p>
 * Two shapes dispatched on the SAM: SUPPLIER/CONSUMER (no parameters, or no return value) and FUNCTION (has both).
 */
public record LinkFunctionalInterface(Runtime runtime, VirtualFieldComputer virtualFieldComputer,
                                      MethodInfo currentMethod) {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkFunctionalInterface.class);

    record Triplet(Variable from, LinkNature linkNature, Variable to) {
    }

    List<Triplet> go(ParameterizedType functionalInterfaceType,
                     Variable fromTranslated,
                     LinkNature linkNature,
                     Variable returnPrimary,
                     List<Links> linksList,
                     Variable objectPrimary) {

        // FUNCTIONAL INTERFACE

        MethodInfo sam = functionalInterfaceType.typeInfo().singleAbstractMethod();
        if (System.getenv("LFITRACE") != null) {
            System.out.println("LFITRACE fi=" + functionalInterfaceType + " fromTranslated=" + fromTranslated
                               + " nature=" + linkNature + " returnPrimary=" + returnPrimary
                               + " objectPrimary=" + objectPrimary + " linksList=" + linksList);
        }
        if (sam == null || linksList.isEmpty()) return List.of();
        // SUPPLIER (no parameters) or CONSUMER (no return value): the SAM has only one "interesting" side.
        // A consumer that captures nothing linkable (e.g. 'list.forEach(x -> sideEffect())') falls through the loop
        // and only its bare CONTAINS_AS_MEMBER connection to the primary is kept (see the links.isEmpty() case below,
        // and TestForEachLambda,6).
        boolean isSupplier = sam.parameters().isEmpty();
        boolean isConsumer = sam.noReturnValue();

        if (isSupplier || isConsumer) {
            List<Triplet> result = new ArrayList<>();
            int i = 0;
            for (Links links : linksList) {
                for (Link link : links) {
                    // LEAF — skip a link to the SAM's own return variable (an independent, freshly produced value such
                    // as 'String::valueOf'): there is nothing external to link to.
                    if (!(Util.primary(link.to()) instanceof ReturnVariable)) {
                        Variable from;
                        if (link.from().equals(links.primary())) {
                            // also accommodate for suppliers
                            if (LinkNatureImpl.IS_ELEMENT_OF.equals(link.linkNature())) {
                                if (functionalInterfaceType.parameters().size() >= 2) {
                                    // BiConsumer (e.g. TestForEachLambda,9b)
                                    // 1:ii∈this.map.§$$s[-1], fromTranslated = map.§$$s, from = map.§$$s[-1]
                                    from = createSlice(i, fromTranslated, link.from());
                                } else {
                                    // Consumer (e.g. TestForEachLambda,3)
                                    // j->add(j), link 0:j∈this.set.§$s, fromTranslated = list.§$s
                                    from = fromTranslated;
                                }
                            } else if (LinkNatureImpl.IS_ASSIGNED_FROM.equals(link.linkNature())
                                       || LinkNatureImpl.IS_DECORATED_WITH.equals(link.linkNature())) {
                                // ←
                                // implicit Supplier (e.g. TestSupplier,1,1b,2,3)
                                // get←1:alternative, fromTranslated = $__rv1 (result of Supplier.get())
                                // ↗
                                // TestBiConsumer
                                from = fromTranslated;
                            } else {
                                from = null; // TODO
                            }
                        } else if (fromTranslated != null) {
                            // Consumer (e.g. Test1,10, forEach(this::doType), 0:typeInfo.§m≡this.§m)
                            // Supplier (e.g. TestSupplier,5,7 ()->this.main.subList(0,2), get.§m≡1:main.§m,get.§xs⊆1:main.§xs)
                            //    both links are translated ->
                            TranslationMap tm = new VariableTranslationMap(runtime)
                                    .put(Util.primary(link.from()), fromTranslated);
                            from = tm.translateVariableRecursively(link.from());
                        } else {
                            from = null;
                        }
                        if (from != null && Util.acceptModificationLink(from, link.to())) {
                            Triplet t = new Triplet(from, linkNature, link.to());
                            result.add(t);
                        }
                    }
                }
                // NB: branch 'optimize' additionally emitted a bare 'fromTranslated ∋ links.primary()' here when
                // links.isEmpty(); openjdk never emits a bare CONTAINS_AS_MEMBER for a consumer (a captured parameter
                // receiver is already represented by its own '~' element link, e.g. 'list.forEach(target::add)' ->
                // 'list.§xs ~ target.§es', and a fully-absorbing/no-op consumer leaves the source unlinked). Dropping
                // that emission keeps the canonical openjdk behaviour. See TestStreamForEachSpec, TestForEach*.
                ++i;
            }
            return result;
        }


        // FUNCTION (SAM has parameters AND a return value): lift the function's return↔parameter relationship from the
        // map source's hidden content to the map target's, e.g. 'stream.map(f)' -> 'result.§ys ⊆ source.§xs'. An
        // unrelated function (result independent of input) produces no link. The trace below follows one concrete case.
        /*
        OUTER
        MLV [-] --> map.§rs~Λ0:function
        pi = function, the parameter in map(Function)
        [rvPrimary = map   (map target, result "from")]
        fromTranslated = $__rv3.§rs    (currently "upscaled/created" for function, directly used for supplier)
        newPrimary/returnPrimary/fromPrimary = $__rv3

        objectPrimary = stream1 (map source, result "to")

        INNER (links from the SAM called 'first')
        links = first<0:ys,first==0:ys[0]
        toPrimaries = ys
        tm3: ys -> stream1 (connection to the map source)
        tm2: first -> $__rv3 (connection to the map target)

        first<0:ys      --->  (first->$__rv3->$__rv3.§xs) < (0:ys->stream1->stream1.§xss)
        first==0:ys[0]  --->  (first->$__rv3->$__rv3.§xs) == X (translation fails)

        translate + upscale = find the right virtual field that matches the dimensions
         */
        List<Triplet> result = new ArrayList<>();
        if (objectPrimary != null && returnPrimary != null) {
            for (Links links : linksList) {
                Set<Variable> toPrimaries = links.stream().map(l -> Util.primary(l.to()))
                        .collect(Collectors.toUnmodifiableSet());

                VirtualFields vfMapSource = virtualFieldComputer.compute(objectPrimary.parameterizedType(),
                        false).virtualFields();
                VirtualFields vfMapTarget = virtualFieldComputer.compute(returnPrimary.parameterizedType(),
                        false).virtualFields();
                if (toPrimaries.isEmpty()) {
                    // The function's result carries no hidden content from its source/parameters. If the remaining
                    // primary is the SAM's own return variable (e.g. 'String::valueOf', which returns a fresh String
                    // unrelated to its argument), there is nothing external to link to — adding a link here invents a
                    // phantom relationship to that internal return variable. Only keep a genuine external primary.
                    if (links.primary() != null && !(links.primary() instanceof ReturnVariable)) {
                        result.add(new Triplet(fromTranslated, CONTAINS_AS_MEMBER, links.primary()));
                    }
                }
                // LEAF — the function's result IS derived from its parameter: for each SAM link, translate the "to"
                // side to the map source and the "from" side to the map target, then upscale to the virtual field that
                // matches the concrete dimensions (translateAndRecreateVirtualFields).
                for (Variable newPrimary : toPrimaries) {
                    VariableTranslationMap tmMapSource = new VariableTranslationMap(runtime);
                    if (!Util.isPartOf(objectPrimary, newPrimary)) {
                        tmMapSource.put(newPrimary, objectPrimary);
                    } // else: TestStaticBiFunction,1,2 we don't want this.ix --> this
                    TranslationMap tmMapTarget = new VariableTranslationMap(runtime).put(links.primary(), returnPrimary);
                    for (Link l : links) {
                        Variable from = translateAndRecreateVirtualFields(tmMapTarget, l.from(), vfMapSource, vfMapTarget,
                                false);
                        Variable to = translateAndRecreateVirtualFields(tmMapSource, l.to(), vfMapSource, vfMapTarget,
                                true);
                        if (from != null && to != null && Util.acceptModificationLink(from, to)) {
                            result.add(new Triplet(from, l.linkNature(), to));
                        }
                    }
                }
            }
        }
        return result;
    }

    private Variable createSlice(int i, Variable base, Variable sub) {
        // essentially for a BiConsumer: (x,y)-> ...
        ParameterizedType sourcePt = sub.parameterizedType();
        String name = sourcePt.typeParameter() != null ? sourcePt.typeParameter().simpleName().toLowerCase() : "$";
        String names = name + "s".repeat(sourcePt.arrays());
        FieldInfo newField = virtualFieldComputer.newField(names, sourcePt,
                VariableTranslationMap.owner(runtime, sourcePt));
        // make a slice
        return SliceFactory.create(runtime, base, -1 - i, newField);
    }

    private Variable translateAndRecreateVirtualFields(TranslationMap tm,
                                                       Variable variable,
                                                       VirtualFields vfMapSource,
                                                       VirtualFields vfMapTarget,
                                                       boolean dimensionsFromMapSource) {
        if (variable instanceof DependentVariable) {
            return null; // TODO see impl/TestStream.testTakeFirst()
        }
        Variable translated = tm.translateVariableRecursively(variable);
        Variable upscaled;
        if (vfMapSource.hiddenContent() == null) return null;
        TypeParameter sourceTp = vfMapSource.hiddenContent().type().typeParameter();
        TypeParameter targetTp = vfMapTarget.hiddenContent() == null
                ? null : vfMapTarget.hiddenContent().type().typeParameter();
        int targetArrays = vfMapTarget.hiddenContent() == null ? 0 : vfMapTarget.hiddenContent().type().arrays();
        int arrays = dimensionsFromMapSource ? vfMapSource.hiddenContent().type().arrays() : targetArrays;
        if (targetTp != null) {
            if (sourceTp != null) {
                ParameterizedType type = vfMapSource.hiddenContent().type().copyWithArrays(arrays);
                String name = sourceTp.simpleName().toLowerCase() + "s".repeat(arrays);
                Variable primaryOfTranslated = Util.primary(translated);
                TypeInfo owner = VariableTranslationMap.owner(runtime, primaryOfTranslated.parameterizedType());
                FieldInfo newField = virtualFieldComputer.newField(name, type, owner);
                // the scope must be the primary of translated, since we're completely re-creating the virtual field
                upscaled = runtime.newFieldReference(newField, runtime.newVariableExpression(primaryOfTranslated),
                        newField.type());
            } else {
                // TestBiFunction,1
                // vMapSource = §ky,
                // vMapTarget = §x, targetTp = X (TP#0 in C)
                // translated = §__rv0
                // TestStaticBiFunction,2, with arrays == 1
                upscaled = translated;
            }
        } else if (translated instanceof FieldReference frK && virtual(frK)) {
            if (frK.scopeVariable() instanceof FieldReference frKv && virtual(frKv) && arrays > 0) {
                // TestStream,1
                // vfMapSource = XY[] §xys, vfMapTarget = YX[] §yxs
                // variable = return swap.§yx.§x, translated $__rv2.§yx.§x, dim = false
                // variable = entry.§xy.§x, translated stream1.§xy.§x, dim = true
                // what we want: $__rv2.§yxs[-2]  -> replace §yx by §yxs[-2]
                //               stream1.§xys[-1] -> replace §xy by §xys[-1]
                FI correspondingField = correspondingField(frKv, frK.fieldInfo());
                if (correspondingField == null) {
                    // no field of matching type in the container (SAM shapes reachable since the analysis
                    // hints attach to java.util.function): no upscaling mapping exists, keep the translation
                    return translated;
                }
                int sliceIndex = -1 - correspondingField.index;
                TypeInfo enclosing = frKv.fieldInfo().type().typeInfo().compilationUnitOrEnclosingType().getRight();
                String newTypeName = frKv.fieldInfo().simpleName().toUpperCase().replace("§", "")
                                     + "S".repeat(arrays);
                TypeInfo newContainerType = virtualFieldComputer.makeContainerType(enclosing,
                        newTypeName,
                        frKv.fieldInfo().type().typeInfo().fields());
                String newFieldName = frKv.fieldInfo().simpleName().replace("§", "")
                                      + "s".repeat(arrays);
                TypeInfo owner = VariableTranslationMap.owner(runtime, frKv.scope().parameterizedType());
                FieldInfo newFieldInfo = virtualFieldComputer.newField(newFieldName,
                        newContainerType.asParameterizedType().copyWithArrays(arrays), owner);
                FieldReference scope = runtime.newFieldReference(newFieldInfo, frKv.scope(), newFieldInfo.type());
                // make a slice
                SliceFactory.FF ff = SliceFactory.findField(frK.parameterizedType(), newContainerType);
                assert ff != null && ff.fieldInfo() != null;
                upscaled = SliceFactory.create(runtime, scope, sliceIndex, ff.fieldInfo());
            } else if (arrays > 0) {
                // TestFunction,2
                // variable = this.§es, translated = optional.§es, arrays = 1
                // what we want  optional.§es -> optional.§xys
                // TestStream,1
                TypeInfo owner = VariableTranslationMap.owner(runtime, frK.scope().parameterizedType());
                VirtualFields vf = dimensionsFromMapSource ? vfMapSource : vfMapTarget;
                FieldInfo newField = vf.hiddenContent().withOwner(owner);
                upscaled = runtime.newFieldReference(newField, frK.scope(), newField.type());
            } else {
                //TestStaticBiFunction,3
                upscaled = translated;
            }
        } else {
            // TestFunction,2 variable = this, translated = $__rv0, what we want  $__rv0.§xy
            // (note that this one equals "fromTranslated" from the 'go' method)
            // vfMapSource §xys, vfMapTarget §xy (still arrays == 0)

            // TestStaticBiFunction,3: both vfMapSource and vfMapTarget have §xy
            int arrayDiff = vfMapSource.hiddenContent().type().arrays() - targetArrays;
            if (arrayDiff == 0) {
                upscaled = translated;
            } else {
                FieldInfo targetField = vfMapTarget.hiddenContent();
                if (targetField != null) {
                    upscaled = runtime.newFieldReference(targetField, runtime.newVariableExpression(translated),
                            targetField.type());
                } else {
                    upscaled = null; // see e.g. Test1,6
                }
            }
        }

        LOGGER.debug("translated and upscale: {} -> {} -> {}", variable, translated, upscaled);
        return upscaled;
    }

    private record FI(FieldInfo fieldInfo, int index) {
    }

    // null when the container type carries no field of the requested type: the caller degrades to the
    // untouched translation (no upscaling mapping exists)
    private static FI correspondingField(FieldReference frKv, FieldInfo sub) {
        int i = 0;
        for (FieldInfo fi : frKv.fieldInfo().type().typeInfo().fields()) {
            if (fi.type().equals(sub.type())) {
                return new FI(fi, i);
            }
            ++i;
        }
        return null;
    }
}
