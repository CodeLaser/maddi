package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
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
        if (sam == null || linksList.isEmpty()) return List.of();

        boolean isSupplier = sam.parameters().isEmpty();
        boolean isConsumer = sam.noReturnValue();

        if (isSupplier || isConsumer) {
            if (isConsumer && linksList.stream().allMatch(Links::isEmpty)) {
                // we must keep the connection to the primary (see TestForEachLambda,6)
                return linksList.stream()
                        .filter(links -> !links.isEmpty())
                        .map(links -> new Triplet(fromTranslated, CONTAINS_AS_MEMBER, links.primary()))
                        .toList();
            }
            /*
            SUPPLIER: grab the "to" of the primary, if it is present (get==c.alternative in the example of a Supplier)

            In a supplier, the return value of the SAM must consist of variables external to the lambda,
            as it has no no parameters itself. We directly link to them.

            In a BiConsumer, we must make synthetic fields...
             */
            List<Triplet> result = new ArrayList<>();
            int i = 0;
            for (Links links : linksList) {
                for (Link link : links) {
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
                                // 0:vi∩4:map.§$$s[-1], fromTranslated = $__rv11247.§$s, sam = Consumer<T>
                                // FIXME working on example in TestConsumers; source: Flags line 240
                                throw new UnsupportedOperationException();
                            }
                        } else {
                            // Consumer (e.g. Test1,10, forEach(this::doType), 0:typeInfo.§m≡this.§m)
                            // Supplier (e.g. TestSupplier,5,7 ()->this.main.subList(0,2), get.§m≡1:main.§m,get.§xs⊆1:main.§xs)
                            //    both links are translated ->
                            TranslationMap tm = new VariableTranslationMap(runtime)
                                    .put(Util.primary(link.from()), fromTranslated);
                            from = tm.translateVariableRecursively(link.from());
                        }
                        if (LinksImpl.LinkImpl.noRelationBetweenMAndOtherVirtualFields(from, link.to())) {
                            Triplet t = new Triplet(from, linkNature, link.to());
                            result.add(t);
                        }
                    }
                }
                ++i;
            }
            return result;
        }


        // FUNCTION
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
                    if (links.primary() != null) {
                        result.add(new Triplet(fromTranslated, CONTAINS_AS_MEMBER, links.primary()));
                    }
                }
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
                        if (from != null && to != null
                            && LinksImpl.LinkImpl.noRelationBetweenMAndOtherVirtualFields(from, to)) {
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

    private static FI correspondingField(FieldReference frKv, FieldInfo sub) {
        int i = 0;
        for (FieldInfo fi : frKv.fieldInfo().type().typeInfo().fields()) {
            if (fi.type().equals(sub.type())) {
                return new FI(fi, i);
            }
            ++i;
        }
        throw new UnsupportedOperationException();
    }
}
