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

import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A getter handing out part of the object prints as {@code fn items(&self) -> &T}; a caller that modifies what it
 * returns ({@code b.items().clear()}, {@code t.s().r().i = 3}) needs it exclusively. Such a getter gets a twin,
 * {@code fn items_mut(&mut self) -> &mut T}, and those call sites call it, as Rust's own collections pair
 * {@code get} and {@code get_mut}. Only for a getter that returns one of its object's fields as it is
 * ({@code return items;}): the twin borrows that field. An interface's getter ({@code List<String> items();}, a default
 * method's {@code items().add(s)}) gets one declared in its trait when every implementation translated with it is
 * such a getter, and each implementation its own; not a functional interface's method, which a closure implements.
 */
final class RustGetters {
    static final String SUFFIX = "_mut";

    private RustGetters() {
    }

    /** The field a getter returns as it is: an instance method without parameters whose body is {@code return f;}. */
    static FieldInfo field(MethodInfo m) {
        if (m.isStatic() || m.isConstructor() || !m.parameters().isEmpty() || m.methodBody() == null
            || m.typeInfo().compilationUnit().externalLibrary() || m.methodBody().statements().size() != 1
            || !(m.methodBody().statements().getFirst() instanceof ReturnStatement rs) || rs.expression() == null) {
            return null;
        }
        if (CstQueries.unwrap(rs.expression()) instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr
            && !fr.fieldInfo().isStatic() && (fr.scopeIsThis() || fr.isDefaultScope())
            && fr.fieldInfo().owner() == m.typeInfo()) {
            return fr.fieldInfo();
        }
        return null;
    }

    /** An interface's getter: abstract, without parameters, declared in source. */
    static boolean abstractGetter(MethodInfo m) {
        return m.isAbstract() && !m.isStatic() && m.parameters().isEmpty() && m.typeInfo().isInterface()
               && !m.typeInfo().compilationUnit().externalLibrary()
               && m.typeInfo().methods().stream().filter(MethodInfo::isAbstract).count() > 1;
    }

    private static boolean getter(MethodInfo m) {
        return field(m) != null || abstractGetter(m);
    }

    /** The getters of {@code types} that a call site in them modifies through. */
    static Set<MethodInfo> modifiedThrough(List<TypeInfo> types, RustFacts facts) {
        Set<MethodInfo> out = new HashSet<>();
        for (TypeInfo type : types) visit(type, facts, out);
        // a field getter overriding an interface's getter: the trait declares the twin, an impl block cannot add one
        for (MethodInfo m : Set.copyOf(out)) {
            m.overrides().stream().filter(RustGetters::abstractGetter).forEach(out::add);
        }
        List<MethodInfo> all = new java.util.ArrayList<>();
        for (TypeInfo type : types) collect(type, all);
        for (MethodInfo a : Set.copyOf(out)) {
            if (!abstractGetter(a)) continue;
            List<MethodInfo> implementations = all.stream().filter(m -> m.overrides().contains(a)).toList();
            if (implementations.stream().allMatch(m -> field(m) != null)) {
                out.addAll(implementations);
            } else {
                out.remove(a);
                out.removeAll(implementations);
            }
        }
        return out;
    }

    private static void collect(TypeInfo type, List<MethodInfo> all) {
        all.addAll(type.methods());
        type.subTypes().forEach(sub -> collect(sub, all));
    }

    private static void visit(TypeInfo type, RustFacts facts, Set<MethodInfo> out) {
        type.constructorAndMethodStream().filter(m -> m.methodBody() != null).forEach(m -> m.methodBody().visit(e -> {
            switch (e) {
                case MethodCall mc when mc.object() != null && modifying(mc, facts) -> through(mc.object(), facts, out);
                case Assignment a when a.variableTarget() instanceof FieldReference fr && fr.scope() != null ->
                        through(fr.scope(), facts, out);
                default -> { }
            }
            return true;
        }));
        type.subTypes().forEach(sub -> visit(sub, facts, out));
    }

    private static boolean modifying(MethodCall mc, RustFacts facts) {
        MethodInfo callee = mc.methodInfo();
        if (callee.typeInfo().compilationUnit().externalLibrary()) return RustJdk.modifies(mc);
        return !callee.isStatic() && facts.receiver(callee) == RustFacts.Receiver.EXCLUSIVE;
    }

    // the getter call the place is, and the getter calls its object is reached through
    private static void through(Expression place, RustFacts facts, Set<MethodInfo> out) {
        Expression e = CstQueries.unwrap(place);
        while (e instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr && fr.scope() != null) {
            e = CstQueries.unwrap(fr.scope());
        }
        if (e instanceof MethodCall g && getter(g.methodInfo()) && !RustTypes.isCopy(g.methodInfo().returnType())
            && out.add(g.methodInfo()) && g.object() != null) {
            through(g.object(), facts, out);
        }
    }
}
