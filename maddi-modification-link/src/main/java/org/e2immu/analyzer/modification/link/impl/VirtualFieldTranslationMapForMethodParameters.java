package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldTranslationMapImpl;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.variable.VirtualFieldTranslationMap;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;

public record VirtualFieldTranslationMapForMethodParameters(VirtualFieldComputer virtualFieldComputer,
                                                            Runtime runtime) {

    // instance call, but with method type parameters that are linked to other type parameters
    // e.g. TestStreamBasics,5 .toArray(String[]::new)
    VirtualFieldTranslationMap go(VirtualFieldTranslationMap vfTm, MethodCall mc) {
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
    VirtualFieldTranslationMap staticCall(MethodCall mc) {
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
        throw new UnsupportedOperationException("NYI");
    }

    private ParameterizedType extractValueForTp(ParameterizedType formal, ParameterizedType concrete, TypeParameter tp) {
        // X, concrete T[];  X[], formal T[]; ...
        if (tp.equals(formal.typeParameter())) {
            /* TODO E, concrete Set<X> -> we want to drill down; see TestStaticBiFunction,2b
              if ("Type java.util.Set<X>".equals(concrete.toString())) {
                  return concrete.parameters().getFirst().copyWithArrays(1);
              }
            */
            return concrete.copyWithArrays(concrete.arrays() - formal.arrays());
        }
        // List<T>, concrete  List<K>
        if (formal.typeInfo() != null && formal.typeInfo().equals(concrete.typeInfo())
            && formal.parameters().size() == concrete.parameters().size()) {
            int i = 0;
            for (ParameterizedType fp : formal.parameters()) {
                ParameterizedType res = extractValueForTp(fp, concrete.parameters().get(i), tp);
                if (res != null) return res;
                ++i;
            }
        }
        // FIXME isAssignable... use generics helper
        return null;
    }

}
