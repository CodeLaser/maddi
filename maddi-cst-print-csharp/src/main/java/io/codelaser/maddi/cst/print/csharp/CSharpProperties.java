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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java's getters and setters as C#'s properties, decided for the whole program. A class's getter whose body is
 * {@code return f;}, of a field of its own of the same type, is the property that takes the field's place:
 * {@code public string Name { get; }}, with the field's initializer; every reference to the field reads the property,
 * {@code x.getName()} is {@code x.Name}. A setter {@code void setName(String n) { this.name = n; }} is its
 * {@code set}, {@code x.setName(v)} is {@code x.Name = v}; a non-final field without one is {@code private set}.
 * <p>
 * The property is named after the getter: {@code getName()}, {@code isDone()} and {@code name()} are {@code Name},
 * {@code Done} and {@code Name}. A getter qualifies when nothing overrides it and it overrides nothing, no method
 * reference names it, and the name is not taken in its class, the classes above it or those that extend it.
 */
final class CSharpProperties {

    /**
     * {@code field} becomes the property {@code name}, read by {@code getter}, written by {@code setter} (null: none,
     * the property's {@code set} is the field's).
     */
    record Property(FieldInfo field, String name, MethodInfo getter, MethodInfo setter, boolean settable) {
    }

    private CSharpProperties() {
    }

    static Map<FieldInfo, Property> analyze(List<TypeInfo> all, Set<MethodInfo> overridden) {
        Set<MethodInfo> referenced = new HashSet<>();
        for (TypeInfo t : all) {
            for (Element element : CSharpProgram.code(t)) {
                element.visit((Element e) -> {
                    if (e instanceof MethodReference mr) referenced.add(mr.methodInfo());
                    return true;
                });
            }
        }
        Set<FieldInfo> written = writtenOutsideConstructors(all);
        Map<FieldInfo, Property> properties = new HashMap<>();
        for (TypeInfo t : all) {
            if (!eligibleType(t)) continue;
            Map<FieldInfo, List<MethodInfo>> getters = new HashMap<>();
            for (MethodInfo m : t.methods()) {
                FieldInfo f = gettersField(m, t);
                if (f != null && alone(m, overridden, referenced)) getters.computeIfAbsent(f, k -> new ArrayList<>()).add(m);
            }
            Set<String> taken = new HashSet<>();
            getters.forEach((f, gs) -> {
                if (gs.size() != 1) return;
                MethodInfo getter = gs.getFirst();
                String name = propertyName(getter);
                if (!taken.add(name) || clashes(name, t, f, getter, all)) return;
                MethodInfo setter = t.methods().stream()
                        .filter(m -> settersField(m, t) == f && ("set" + name).equals(m.name())
                                     && alone(m, overridden, referenced))
                        .findFirst().orElse(null);
                properties.put(f, new Property(f, name, getter, setter, setter != null || written.contains(f)));
            });
        }
        return Map.copyOf(properties);
    }

    /** The fields assigned anywhere but in their own class's constructors and initializers. */
    private static Set<FieldInfo> writtenOutsideConstructors(List<TypeInfo> all) {
        Set<FieldInfo> written = new HashSet<>();
        for (TypeInfo t : all) {
            List<MethodInfo> code = new ArrayList<>(t.methods());
            code.addAll(t.constructors());
            for (MethodInfo m : code) {
                if (m.methodBody() == null) continue;
                m.methodBody().visit((Element e) -> {
                    if (e instanceof Assignment a && a.variableTarget() instanceof FieldReference fr
                        && !(m.isConstructor() && fr.fieldInfo().owner().equals(t))) {
                        written.add(fr.fieldInfo());
                    }
                    return true;
                });
            }
        }
        return written;
    }

    /** A class of the program, printed as a class: not an interface, enum, record, anonymous or local type. */
    private static boolean eligibleType(TypeInfo t) {
        return CSharpNames.translated(t) && t.typeNature().isClass() && !t.isAnonymous() && t.enclosingMethod() == null
               && !t.typeNature().isRecord() && !t.typeNature().isEnum();
    }

    /** Nothing overrides {@code m}, it overrides nothing, and no method reference names it. */
    private static boolean alone(MethodInfo m, Set<MethodInfo> overridden, Set<MethodInfo> referenced) {
        return !m.isStatic() && !m.isAbstract() && !m.isSynthetic() && m.typeParameters().isEmpty()
               && m.overrides().stream().allMatch(o -> o == m) && !overridden.contains(m) && !referenced.contains(m);
    }

    /** {@code return f;}: the field {@code f} of {@code t}, non-static, of the method's return type; null otherwise. */
    private static FieldInfo gettersField(MethodInfo m, TypeInfo t) {
        if (!m.parameters().isEmpty() || m.isVoid() || m.isConstructor() || m.methodBody() == null) return null;
        List<Statement> statements = statements(m);
        if (statements.size() != 1 || !(statements.getFirst() instanceof ReturnStatement rs) || rs.hasNoValue()) return null;
        FieldInfo f = ownField(rs.expression(), t);
        return f != null && f.type().equals(m.returnType()) ? f : null;
    }

    /** {@code this.f = p;}: the field {@code f} of {@code t} that the only parameter is assigned to; null otherwise. */
    private static FieldInfo settersField(MethodInfo m, TypeInfo t) {
        if (m.parameters().size() != 1 || !m.isVoid() || m.methodBody() == null) return null;
        List<Statement> statements = statements(m);
        if (statements.size() != 1 || !(statements.getFirst() instanceof ExpressionAsStatement eas)
            || !(CSharpExpressionPrinter.unwrap(eas.expression()) instanceof Assignment a)
            || a.assignmentOperator() != null) {
            return null;
        }
        ParameterInfo p = m.parameters().getFirst();
        if (!(CSharpExpressionPrinter.unwrap(a.value()) instanceof VariableExpression ve) || !p.equals(ve.variable())) {
            return null;
        }
        FieldInfo f = ownField(a.target(), t);
        return f != null && f.type().equals(p.parameterizedType()) ? f : null;
    }

    private static List<Statement> statements(MethodInfo m) {
        return m.methodBody().statements().stream().filter(s -> !s.isSynthetic()).toList();
    }

    /** {@code f} or {@code this.f}, a non-static, non-volatile field declared in {@code t}; null otherwise. */
    private static FieldInfo ownField(Expression e, TypeInfo t) {
        if (!(CSharpExpressionPrinter.unwrap(e) instanceof VariableExpression ve)
            || !(ve.variable() instanceof FieldReference fr)) {
            return null;
        }
        FieldInfo f = fr.fieldInfo();
        if (f.isStatic() || f.isVolatile() || f.isSynthetic() || !t.equals(f.owner())) return null;
        boolean thisScope = fr.isDefaultScope() || fr.scope() == null
                            || fr.scope() instanceof VariableExpression sv && sv.variable() instanceof This th
                               && t.equals(th.typeInfo());
        return thisScope ? f : null;
    }

    /** {@code getName}, {@code isDone} (of a boolean), {@code name}: {@code Name}, {@code Done}, {@code Name}. */
    private static String propertyName(MethodInfo getter) {
        String n = getter.name();
        if (n.length() > 3 && n.startsWith("get") && Character.isUpperCase(n.charAt(3))) n = n.substring(3);
        else if (n.length() > 2 && n.startsWith("is") && Character.isUpperCase(n.charAt(2))
                 && getter.returnType().isBoolean()) {
            n = n.substring(2);
        }
        return CSharpNames.pascal(n);
    }

    /**
     * {@code name} is taken: by the class itself, a nested type, another field, or a method other than the
     * getter's own setter, in the class, the classes above it or those that extend it.
     */
    private static boolean clashes(String name, TypeInfo t, FieldInfo field, MethodInfo getter, List<TypeInfo> all) {
        if (name.equals(CSharpNames.type(t))) return true;
        List<TypeInfo> related = new ArrayList<>();
        for (TypeInfo s = t; s != null; s = s.parentClass() == null ? null : s.parentClass().typeInfo()) related.add(s);
        for (TypeInfo other : all) {
            if (other != t && other.superTypesExcludingJavaLangObject().contains(t)) related.add(other);
        }
        for (TypeInfo r : related) {
            if (r.subTypes().stream().anyMatch(st -> name.equals(CSharpNames.type(st)))) return true;
            if (r.fields().stream().anyMatch(f -> f != field && (name.equals(f.name()) || name.equals(CSharpNames.pascal(f.name()))))) {
                return true;
            }
            for (MethodInfo m : r.methods()) {
                if (m == getter) continue;
                String pascal = CSharpNames.pascal(m.name());
                if (pascal.equals(name) || pascal.equals(name + "Value")) return true;
            }
        }
        return false;
    }
}
