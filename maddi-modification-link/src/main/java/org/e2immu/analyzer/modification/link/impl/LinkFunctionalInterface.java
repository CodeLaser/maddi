package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public record LinkFunctionalInterface(Runtime runtime, VirtualFieldComputer virtualFieldComputer,
                                      MethodInfo currentMethod) {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkFunctionalInterface.class);

    record Triplet(Variable from, LinkNature linkNature, Variable to) {
    }

    List<Triplet> go(ParameterInfo pi,
                     Variable fromTranslated,
                     LinkNature linkNature,
                     Variable returnPrimary,
                     IntFunction<Links> paramProvider,
                     Variable objectPrimary) {

        // FUNCTIONAL INTERFACE

        MethodInfo sam = pi.parameterizedType().typeInfo().singleAbstractMethod();
        Links links = paramProvider.apply(pi.index());

        if (sam.parameters().isEmpty()) {

            /*
            SUPPLIER: grab the "to" of the primary, if it is present (get==c.alternative in the example of a Supplier)

            In a supplier, the return value of the SAM must consist of variables external to the lambda,
            as it has no no parameters itself. We directly link to them.
             */
      //      return links.linkSet().stream()
      //              .filter(l -> l.from().equals(links.primary()))
       //             .map(l -> new Triplet(fromTranslated, linkNature, l.to()))
      //              .toList();
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
        Set<Variable> toPrimaries = links.linkSet().stream().map(l -> Util.primary(l.to()))
                .collect(Collectors.toUnmodifiableSet());
        List<Triplet> result = new ArrayList<>();

        VirtualFields vfMapSource = virtualFieldComputer.compute(objectPrimary.parameterizedType(), false)
                .virtualFields();
        VirtualFields vfMapTarget = virtualFieldComputer.compute(returnPrimary.parameterizedType(), false)
                .virtualFields();

        for (Variable newPrimary : toPrimaries) {
            TranslationMap tmMapSource = runtime.newTranslationMapBuilder().put(newPrimary, objectPrimary).build(); // FIXME
            TranslationMap tmMapTarget = runtime.newTranslationMapBuilder().put(links.primary(), returnPrimary).build();
            for (Link l : links) {
                Variable from = translateAndRecreateVirtualFields(tmMapTarget, l.from(), vfMapSource, vfMapTarget,
                        false);
                Variable to = translateAndRecreateVirtualFields(tmMapSource, l.to(), vfMapSource, vfMapTarget,
                        true);
                if (from != null && to != null) {
                    result.add(new Triplet(from, l.linkNature(), to));
                }
            }
        }
        // TODO post-processing? stream2.§yxs[-2].§xs~stream1.§xys[-1].§xs + stream2.§yxs[-1].§ys~stream1.§xys[-2].§ys
        //  into stream2.§yxs~stream1.§xys
        return result;
    }

    private Variable translateAndRecreateVirtualFields(TranslationMap tm,
                                                       Variable variable,
                                                       VirtualFields vfMapSource,
                                                       VirtualFields vfMapTarget,
                                                       boolean dimensionsFromMapSource) {
        if (variable instanceof DependentVariable) return null; // FIXME
        Variable translated = tm.translateVariableRecursively(variable);
        Variable upscaled;
        TypeParameter sourceTp = vfMapSource.hiddenContent().type().typeParameter();
        TypeParameter targetTp = vfMapTarget.hiddenContent().type().typeParameter();
        if (targetTp != null && sourceTp != null) {
            int arrays = dimensionsFromMapSource ? vfMapSource.hiddenContent().type().arrays()
                    : vfMapTarget.hiddenContent().type().arrays();
            FieldInfo newField = virtualFieldComputer.newField(sourceTp.simpleName().toLowerCase() + "s".repeat(arrays),
                    vfMapSource.hiddenContent().type().copyWithArrays(arrays), currentMethod.typeInfo());
            // the scope must be the primary of translated, since we're completely re-creating the virtual field
            Variable primaryOfTranslated = Util.primary(translated);
            upscaled = runtime.newFieldReference(newField, runtime.newVariableExpression(primaryOfTranslated),
                    newField.type());
        } else {
            upscaled = translated;
        }

        LOGGER.debug("translated and upscale: {} -> {} -> {}", variable, translated, upscaled);
        return upscaled;
    }
}
