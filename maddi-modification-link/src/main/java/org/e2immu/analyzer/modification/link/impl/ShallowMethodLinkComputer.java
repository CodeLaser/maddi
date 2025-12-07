package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;

import java.util.List;

public record ShallowMethodLinkComputer(Runtime runtime, VirtualFieldComputer virtualFieldComputer) {

    public MethodLinkedVariables go(MethodInfo methodInfo) {
        TypeInfo typeInfo = methodInfo.typeInfo();
        VirtualFields vf = typeInfo.analysis().getOrCreate(VirtualFields.VIRTUAL_FIELDS, () ->
                virtualFieldComputer.computeOnDemand(typeInfo));
        List<Links> ofParameters = List.of();
        Links ofReturnValue;
        if (methodInfo.hasReturnValue() && vf.hiddenContent() != null) {
            ReturnVariableImpl rv = new ReturnVariableImpl(methodInfo);
            Links.Builder builder = new LinksImpl.Builder(rv);
            //int multiplicity = virtualFieldComputer.computeMultiplicity(methodInfo.returnType());
            ParameterizedType returnType = methodInfo.returnType();
            FieldReference hiddenContentFr = runtime.newFieldReference(vf.hiddenContent());
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
        return new MethodLinkedVariablesImpl(ofReturnValue, ofParameters);
    }
}
