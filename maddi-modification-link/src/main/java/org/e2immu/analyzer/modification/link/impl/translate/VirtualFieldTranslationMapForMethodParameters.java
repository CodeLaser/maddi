package org.e2immu.analyzer.modification.link.impl.translate;

import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldTranslationMapImpl;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.variable.VirtualFieldTranslationMap;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.impl.parser.GenericsHelperImpl;

import java.util.Map;

public class VirtualFieldTranslationMapForMethodParameters {
    private final VirtualFieldComputer virtualFieldComputer;
    private final Runtime runtime;
    private final GenericsHelper genericsHelper;

    public VirtualFieldTranslationMapForMethodParameters(VirtualFieldComputer virtualFieldComputer,
                                                         Runtime runtime) {
        this.virtualFieldComputer = virtualFieldComputer;
        this.runtime = runtime;
        this.genericsHelper = new GenericsHelperImpl(runtime);

    }

    // instance call, but with method type parameters that are linked to other type parameters
    // e.g. TestStreamBasics,5 .toArray(String[]::new)
    public VirtualFieldTranslationMap go(VirtualFieldTranslationMap vfTm, MethodCall mc) {
        for (TypeParameter tp : mc.methodInfo().typeParameters()) {
            ParameterizedType bestValue = findValue(mc, tp);
            VirtualFields vf = virtualFieldComputer.compute(bestValue, false).virtualFields();
            if (vf.hiddenContent() != null) {
                vfTm.put(tp, vf.hiddenContent().type());
            }
        }
        return vfTm;
    }

    // static call, but with method type parameters
    public VirtualFieldTranslationMap staticCall(MethodCall mc) {
        VirtualFieldTranslationMap vfTm = new VirtualFieldTranslationMapImpl(virtualFieldComputer, runtime);
        for (TypeParameter tp : mc.methodInfo().typeParameters()) {
            ParameterizedType bestValue = findValue(mc, tp);
            vfTm.put(tp, bestValue);
        }
        return vfTm;
    }

    private ParameterizedType findValue(MethodCall mc, TypeParameter tp) {
        for (ParameterInfo pi : mc.methodInfo().parameters()) {
            if (pi.isVarArgs()) {
                for (int i = pi.index(); i < mc.parameterExpressions().size(); ++i) {
                    ParameterizedType concrete = mc.parameterExpressions().get(i).parameterizedType();
                    ParameterizedType res = extractValueForTp(pi.parameterizedType().copyWithOneFewerArrays(),
                            concrete, tp);
                    if (res != null) return res;
                }
            } else {
                ParameterizedType res = extractValueForTp(pi.parameterizedType(),
                        mc.parameterExpressions().get(pi.index()).parameterizedType(), tp);
                if (res != null) return res;
            }
        }
        // try the return type
        ParameterizedType rt = extractValueForTp(mc.methodInfo().returnType(), mc.concreteReturnType(), tp);
        if (rt != null) {
            return rt;
        }
        throw new UnsupportedOperationException("Unable to find concrete value");
    }

    private ParameterizedType extractValueForTp(ParameterizedType formal,
                                                ParameterizedType concrete,
                                                TypeParameter tp) {
        Map<NamedType, ParameterizedType> tm = genericsHelper.translateMap(formal, concrete,
                true);
        return tm.get(tp);
    }

}
