/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.cst.print.rust;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.cst.print.rust.RustTypes.Form;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A dependent field: the object stores what its constructor is given, and the caller keeps it too. Java shares the
 * object; Rust's shared form of that is a borrow with a lifetime, {@code struct View<'a> { list: &'a Vec<String> }},
 * {@code &'a mut} when the object modifies it. The constructor's parameter is then that borrow, and rustc checks that
 * the view does not outlive what it was given, nor sees it modified while it lives (E0502, E0505, E0597).
 * <p>
 * A field is borrowed when it is final, has no initializer, is of a type that is moved rather than copied or cloned,
 * and every constructor assigns it, once, a parameter the analysis calls dependent. Records keep their owned
 * components. Every other dependent parameter is moved in, and the caller's later use is rustc's E0382.
 */
final class RustBorrows {
    static final String LIFETIME = "'a";

    private final RustFacts facts;
    private final Map<TypeInfo, Map<FieldInfo, Form>> fields = new HashMap<>();

    RustBorrows(RustFacts facts) {
        this.facts = facts;
    }

    /** The borrowed fields of {@code typeInfo}, with their form; empty for a type without a lifetime. */
    Map<FieldInfo, Form> fields(TypeInfo typeInfo) {
        return fields.computeIfAbsent(typeInfo, this::compute);
    }

    boolean hasLifetime(TypeInfo typeInfo) {
        return typeInfo != null && !fields(typeInfo).isEmpty();
    }

    /** The form of a constructor parameter stored in a borrowed field; {@code null} for any other parameter. */
    Form parameter(ParameterInfo pi) {
        MethodInfo m = pi.methodInfo();
        if (!m.isConstructor()) return null;
        Map<FieldInfo, Form> borrowed = fields(m.typeInfo());
        if (borrowed.isEmpty()) return null;
        for (Map.Entry<FieldInfo, Form> e : borrowed.entrySet()) {
            if (assigned(m, e.getKey()) == pi) return e.getValue();
        }
        return null;
    }

    private Map<FieldInfo, Form> compute(TypeInfo typeInfo) {
        Map<FieldInfo, Form> out = new LinkedHashMap<>();
        if (typeInfo.typeNature().isRecord() || typeInfo.isInterface() || typeInfo.typeNature().isEnum()
            || RustTypePrinter.sourceParent(typeInfo) != null) {
            return out;
        }
        List<MethodInfo> constructors = typeInfo.constructors().stream().filter(c -> !c.isSynthetic()).toList();
        if (constructors.isEmpty()) return out;
        for (FieldInfo f : typeInfo.fields()) {
            if (f.isStatic() || f.isSynthetic() || !f.isFinal() || f.initializer() != null && !f.initializer().isEmpty()
                || RustTypes.isCopy(f.type()) || RustTypes.isString(f.type()) || f.type().isTypeParameter()
                || RustTypes.isImmutableValue(f.type(), facts) || f.type().isFunctionalInterface()) {
                continue;
            }
            boolean all = true;
            for (MethodInfo c : constructors) {
                ParameterInfo pi = assigned(c, f);
                if (pi == null || !dependent(pi)) {
                    all = false;
                    break;
                }
            }
            if (all) out.put(f, facts.unmodified(f) ? Form.SHARED : Form.EXCLUSIVE);
        }
        return out;
    }

    private static boolean dependent(ParameterInfo pi) {
        Value.Independent independent = pi.analysis().getOrNull(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.class);
        return independent != null && independent.isDependent();
    }

    /** The parameter the constructor assigns to {@code f}, as a top-level {@code this.f = p}; else {@code null}. */
    private static ParameterInfo assigned(MethodInfo constructor, FieldInfo f) {
        if (constructor.methodBody() == null) return null;
        ParameterInfo found = null;
        for (Statement s : constructor.methodBody().statements()) {
            if (s instanceof ExpressionAsStatement es && es.expression() instanceof Assignment a
                && a.target().variable() instanceof FieldReference fr && fr.fieldInfo() == f) {
                if (found != null || !fr.scopeIsThis()
                    || !(a.value() instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo pi)) {
                    return null;
                }
                found = pi;
            }
        }
        return found;
    }
}
