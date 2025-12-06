package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;

public class Util {
    public static boolean isPartOf(Variable base, Variable sub) {
        if (base.equals(sub)) return true;
        if (sub instanceof FieldReference fr && fr.scopeVariable() != null) {
            return isPartOf(base, fr.scopeVariable());
        }
        if (sub instanceof DependentVariable dv) {
            return isPartOf(base, dv.arrayVariable());
        }
        return false;
    }

    static String simpleName(Variable variable) {
        if (variable instanceof ParameterInfo pi) {
            return pi.index() + ":" + pi.name();
        }
        if (variable instanceof ReturnVariable rv) {
            return rv.methodInfo().name();
        }
        if (variable instanceof FieldReference fr) {
            String scope = fr.scopeVariable() != null ? simpleName(fr.scopeVariable()) : fr.scope().toString();
            return scope + "." + fr.fieldInfo().name();
        }
        if (variable instanceof DependentVariable dv) {
            String index = dv.indexVariable() != null ? simpleName(dv.indexVariable()) : dv.indexExpression().toString();
            return simpleName(dv.arrayVariable()) + "[" + index + "]";
        }
        return variable.toString();
    }
}
