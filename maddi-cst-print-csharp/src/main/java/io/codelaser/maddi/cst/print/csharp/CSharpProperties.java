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
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

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
 * <p>
 * Methods that override one another are one property, when all are parameterless with one return type, in classes
 * and interfaces that no lambda targets: an interface's method is {@code T Name { get; }} (with its default body,
 * {@code T Name => expr;}), an abstract one {@code abstract T Name { get; }}, a getter of a field takes the field's
 * place, and any other body is a computed property, {@code T Name => expr;} or {@code T Name { get { … } }}.
 */
final class CSharpProperties {

    /**
     * {@code field} becomes the property {@code name}, read by {@code getter}, written by {@code setter} (null: none,
     * the property's {@code set} is the field's).
     */
    record Property(FieldInfo field, String name, MethodInfo getter, MethodInfo setter, boolean settable) {
    }

    /**
     * The properties of the program: the fields properties take the place of, and the methods that are properties
     * without a field, computed ({@code T Name => expr;}) or abstract ({@code T Name { get; }}), with their names.
     */
    record Analysis(Map<FieldInfo, Property> fields, Map<MethodInfo, String> computed) {
        static final Analysis NONE = new Analysis(Map.of(), Map.of());
    }

    private CSharpProperties() {
    }

    static Analysis analyze(List<TypeInfo> all, Set<MethodInfo> overridden, Set<TypeInfo> delegates) {
        Set<MethodInfo> referenced = new HashSet<>();
        // a delegate has no property; an interface's lambda adapter implements its property as one
        Set<TypeInfo> functionalTargets = new HashSet<>(delegates);
        for (TypeInfo t : all) {
            for (Element element : CstQueries.code(t)) {
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
                if (!taken.add(name) || clashes(name, t, f, Set.of(getter), all)) return;
                MethodInfo setter = t.methods().stream()
                        .filter(m -> settersField(m, t) == f && ("set" + name).equals(m.name())
                                     && alone(m, overridden, referenced))
                        .findFirst().orElse(null);
                properties.put(f, new Property(f, name, getter, setter, setter != null || written.contains(f)));
            });
        }
        Map<MethodInfo, String> computed = new HashMap<>();
        for (List<MethodInfo> family : families(all)) {
            family(family, all, referenced, functionalTargets, written, properties, computed);
        }
        return new Analysis(Map.copyOf(properties), Map.copyOf(computed));
    }

    /** The methods of the program that override one another, connected: the families of two or more. */
    static List<List<MethodInfo>> families(List<TypeInfo> all) {
        Map<MethodInfo, MethodInfo> parent = new HashMap<>();
        UnaryOperator<MethodInfo>[] find = new UnaryOperator[1];
        find[0] = m -> {
            MethodInfo p = parent.getOrDefault(m, m);
            if (p == m) return m;
            MethodInfo root = find[0].apply(p);
            parent.put(m, root);
            return root;
        };
        Set<MethodInfo> members = new HashSet<>();
        for (TypeInfo t : all) {
            for (MethodInfo m : t.methods()) {
                for (MethodInfo o : m.overrides()) {
                    if (o == m) continue;
                    members.add(m);
                    members.add(o);
                    parent.put(find[0].apply(m), find[0].apply(o));
                }
            }
        }
        Map<MethodInfo, List<MethodInfo>> byRoot = new HashMap<>();
        for (MethodInfo m : members) byRoot.computeIfAbsent(find[0].apply(m), k -> new ArrayList<>()).add(m);
        return List.copyOf(byRoot.values());
    }

    /**
     * A family of parameterless methods with one return type, in classes and interfaces of the program that no lambda
     * targets: one property. A member that returns a field of its class takes its place; another is computed, an
     * abstract one declared.
     */
    private static void family(List<MethodInfo> family, List<TypeInfo> all, Set<MethodInfo> referenced,
                               Set<TypeInfo> functionalTargets, Set<FieldInfo> written,
                               Map<FieldInfo, Property> properties, Map<MethodInfo, String> computed) {
        MethodInfo first = family.getFirst();
        for (MethodInfo m : family) {
            TypeInfo owner = m.typeInfo();
            if (!CSharpNames.translated(owner) || m.isStatic() || m.isSynthetic() || !m.parameters().isEmpty()
                || m.isVoid() || !m.typeParameters().isEmpty() || referenced.contains(m)
                || !m.returnType().equals(first.returnType()) || !m.name().equals(first.name())
                || functionalTargets.contains(owner) || owner.isAnonymous() || owner.enclosingMethod() != null
                || !(owner.isInterface() && !owner.typeNature().isAnnotation() || eligibleType(owner))) {
                return;
            }
            // a default method that implements an inherited interface's method is an explicit implementation
            if (owner.isInterface() && m.methodBody() != null && !m.isAbstract()
                && m.overrides().stream().anyMatch(o -> o != m)) {
                return;
            }
        }
        String name = propertyName(first);
        Set<MethodInfo> members = new HashSet<>(family);
        for (MethodInfo m : family) {
            if (clashes(name, m.typeInfo(), null, members, all)) return;
        }
        for (MethodInfo m : family) {
            FieldInfo f = m.typeInfo().isInterface() ? null : gettersField(m, m.typeInfo());
            if (f != null && !properties.containsKey(f)) {
                properties.put(f, new Property(f, name, m, null, written.contains(f)));
            } else {
                computed.put(m, name);
            }
        }
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
        return CstQueries.own(m.methodBody().statements());
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
    private static boolean clashes(String name, TypeInfo t, FieldInfo field, Set<MethodInfo> getters,
                                   List<TypeInfo> all) {
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
                if (getters.contains(m)) continue;
                String pascal = CSharpNames.pascal(m.name());
                if (pascal.equals(name) || pascal.equals(name + "Value")) return true;
            }
        }
        return false;
    }
}
