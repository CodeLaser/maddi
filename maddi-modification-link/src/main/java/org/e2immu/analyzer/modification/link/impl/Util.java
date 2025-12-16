package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;

public class Util {
    public static Iterable<Variable> goUp(Variable variable) {
        return new Iterable<>() {
            @Override
            public @NotNull Iterator<Variable> iterator() {
                return new Iterator<>() {
                    Variable v = variable;

                    @Override
                    public boolean hasNext() {
                        return v != null;
                    }

                    @Override
                    public Variable next() {
                        Variable rv = v;
                        if (v instanceof FieldReference fr && fr.scopeVariable() != null) {
                            v = fr.scopeVariable();
                        } else if (v instanceof DependentVariable dv) {
                            v = dv.arrayVariable();
                        } else {
                            v = null;
                        }
                        return rv;
                    }
                };
            }
        };
    }

    public static Variable primaryOfVirtual(Variable variable) {
        if (variable instanceof FieldReference fr) {
            if (fr.fieldInfo().name().startsWith("$")) {
                return primaryOfVirtual(fr.scopeVariable());
            }
            return null;
        }
        if (variable instanceof DependentVariable dv) {
            Variable primary = primaryOfVirtual(dv.arrayVariable());
            assert primary == null || dv.indexExpression() instanceof IntConstant ic && ic.constant() < 0; // -1, -2, ...
            return primary;
        }
        return null;
    }

    public static boolean isPartOf(Variable base, Variable sub) {
        if (base.equals(sub)) return true;
        return base.equals(primaryOfVirtual(sub));
    }

    public static LocalVariable lvPrimaryOrNull(Variable variable) {
        if (variable instanceof LocalVariable lv) return lv;
        if (primaryOfVirtual(variable) instanceof LocalVariable lv) return lv;
        return null;
    }

    public static @NotNull ParameterInfo parameterPrimary(Variable variable) {
        return (ParameterInfo) primary(variable);
    }

    static Variable primary(Variable variable) {
        Variable primary = primaryOfVirtual(variable);
        return Objects.requireNonNullElse(primary, variable);
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
