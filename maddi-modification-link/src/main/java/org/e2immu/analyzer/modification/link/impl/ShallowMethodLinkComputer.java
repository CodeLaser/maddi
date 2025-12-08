package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ShallowMethodLinkComputer(Runtime runtime, VirtualFieldComputer virtualFieldComputer) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShallowMethodLinkComputer.class);

    public MethodLinkedVariables go(MethodInfo methodInfo) {
        TypeInfo typeInfo = methodInfo.typeInfo();
        VirtualFields vf = typeInfo.analysis().getOrCreate(VirtualFields.VIRTUAL_FIELDS, () ->
                virtualFieldComputer.computeOnDemand(typeInfo));
        FieldReference hiddenContentFr = vf.hiddenContent() == null ? null : runtime.newFieldReference(vf.hiddenContent());
        ReturnVariableImpl rv = new ReturnVariableImpl(methodInfo);
        Links.Builder ofReturnValue = new LinksImpl.Builder(rv);

        if (methodInfo.hasReturnValue() && vf.hiddenContent() != null) {
            Value.Independent independent = methodInfo.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD,
                    ValueImpl.IndependentImpl.DEPENDENT);
            ParameterizedType returnType = methodInfo.returnType();
            if (returnType.typeParameter() != null && independent.isIndependentHc()) {
                int arrays = returnType.arrays();
                int arraysVF = vf.hiddenContent().type().arrays();
                if (arrays == arraysVF) {
                    if (vf.hiddenContent().type().typeParameter() != null) {
                        // must be the same; same element
                        ofReturnValue.add(LinkNature.IS_IDENTICAL_TO, hiddenContentFr);
                    }
                }
            }
        }

        List<Links> ofParameters = new ArrayList<>(methodInfo.parameters().size());
        for (ParameterInfo pi : methodInfo.parameters()) {
            Links.Builder builder = new LinksImpl.Builder(pi);
            ParameterizedType type = pi.parameterizedType();

            Value.Independent independent = pi.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER,
                    ValueImpl.IndependentImpl.DEPENDENT);
            Map<Integer, Integer> dependencies = independent.linkToParametersReturnValue();
            if (!dependencies.isEmpty()) {
                LOGGER.debug("Parameter {} has dependencies on {}", pi, dependencies);
                int linkLevel = dependencies.getOrDefault(-1, -1);
                // a dependence from the parameter into the return variable; we'll add it to the return variable
                // linkLevel 1 == independent HC
                if (type.typeParameter() != null && linkLevel == 1) {
                    int arrays = type.arrays();
                    int arraysVF = rv.parameterizedType().arrays();
                    if (arrays == arraysVF) {
                        if (rv.parameterizedType().typeParameter() != null) {
                            // must be the same; same element
                            ofReturnValue.add(LinkNature.IS_IDENTICAL_TO, pi);
                        }
                    }
                }
            } else if (type.typeParameter() != null && vf.hiddenContent() != null && independent.isIndependentHc()) {
                int arrays = type.arrays();
                int arraysVF = vf.hiddenContent().type().arrays();
                if (arrays == arraysVF) {
                    if (vf.hiddenContent().type().typeParameter() != null) {
                        // must be the same; same element
                        builder.add(LinkNature.IS_IDENTICAL_TO, hiddenContentFr);
                    }
                }
            }
            ofParameters.add(builder.build());
        }
        return new MethodLinkedVariablesImpl(ofReturnValue.build(), ofParameters);
    }
}
