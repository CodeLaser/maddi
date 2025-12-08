package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;

import java.util.ArrayList;
import java.util.List;

public record ShallowMethodLinkComputer(Runtime runtime, VirtualFieldComputer virtualFieldComputer) {

    public MethodLinkedVariables go(MethodInfo methodInfo) {
        TypeInfo typeInfo = methodInfo.typeInfo();
        VirtualFields vf = typeInfo.analysis().getOrCreate(VirtualFields.VIRTUAL_FIELDS, () ->
                virtualFieldComputer.computeOnDemand(typeInfo));
        FieldReference hiddenContentFr = vf.hiddenContent() == null ? null: runtime.newFieldReference(vf.hiddenContent());
        List<Links> ofParameters = new ArrayList<>(methodInfo.parameters().size());
        Links ofReturnValue;
        if (methodInfo.hasReturnValue() && vf.hiddenContent() != null) {
            ReturnVariableImpl rv = new ReturnVariableImpl(methodInfo);
            Links.Builder builder = new LinksImpl.Builder(rv);
            //int multiplicity = virtualFieldComputer.computeMultiplicity(methodInfo.returnType());
            ParameterizedType returnType = methodInfo.returnType();
            if (returnType.typeParameter() != null) {
                int arrays = returnType.arrays();
                int arraysVF = vf.hiddenContent().type().arrays();
                if (arrays == arraysVF) {
                    if (vf.hiddenContent().type().typeParameter() != null) {
                        // must be the same; same element
                        builder.add(LinkNature.IS_IDENTICAL_TO, hiddenContentFr);
                    }
                }
            }
            ofReturnValue = builder.build();
        } else {
            ofReturnValue = LinksImpl.EMPTY;
        }
        for(ParameterInfo pi: methodInfo.parameters()) {
            Links.Builder builder = new LinksImpl.Builder(pi);
            ParameterizedType type = pi.parameterizedType();
            if (type.typeParameter() != null && vf.hiddenContent() != null) {
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
        return new MethodLinkedVariablesImpl(ofReturnValue, ofParameters);
    }
}
