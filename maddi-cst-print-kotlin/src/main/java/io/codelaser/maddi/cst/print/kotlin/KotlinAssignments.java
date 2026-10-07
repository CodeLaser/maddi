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
package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Which variables are assigned after their declaration, for {@code val} versus {@code var}. Syntactic, and exact
 * where it is used: a Java local is assigned only in its own method body (a lambda or an inner class may not
 * assign a captured local), and a private field only in its compilation unit.
 */
final class KotlinAssignments {

    private static final ThreadLocal<Map<TypeInfo, Set<FieldInfo>>> ASSIGNED_FIELDS =
            ThreadLocal.withInitial(WeakHashMap::new);

    private KotlinAssignments() {
    }

    /** True when an assignment (including {@code ++}, {@code +=}) in {@code scope} targets {@code variable}. */
    static boolean assignedIn(Element scope, Variable variable) {
        boolean[] found = {false};
        scope.visit((Element e) -> {
            if (e instanceof Assignment a && variable.equals(a.variableTarget())) found[0] = true;
            return !found[0];
        });
        return found[0];
    }

    /** A private field with an initializer that no code of its compilation unit assigns: a {@code val}. */
    static boolean neverReassigned(FieldInfo fieldInfo) {
        if (fieldInfo.access() == null || !fieldInfo.access().isPrivate()) return false;
        TypeInfo primary = fieldInfo.owner().primaryType();
        return !ASSIGNED_FIELDS.get().computeIfAbsent(primary, KotlinAssignments::assignedFields).contains(fieldInfo);
    }

    private static Set<FieldInfo> assignedFields(TypeInfo primary) {
        Set<FieldInfo> assigned = new HashSet<>();
        if (primary.compilationUnit() != null) {
            primary.compilationUnit().types().forEach(t -> collect(t, assigned));
        } else {
            collect(primary, assigned);
        }
        return assigned;
    }

    private static void collect(TypeInfo typeInfo, Set<FieldInfo> assigned) {
        typeInfo.constructorAndMethodStream().forEach(m -> {
            if (m.methodBody() != null) collect((Element) m.methodBody(), assigned);
        });
        typeInfo.fields().forEach(f -> {
            if (f.initializer() != null) collect((Element) f.initializer(), assigned);
        });
        typeInfo.subTypes().forEach(t -> collect(t, assigned));
    }

    /** Through lambdas (which {@code visit} enters) and anonymous and local classes (which it does not). */
    private static void collect(Element element, Set<FieldInfo> assigned) {
        element.visit((Element e) -> {
            if (e instanceof Assignment a && a.variableTarget() instanceof FieldReference fr) assigned.add(fr.fieldInfo());
            if (e instanceof ConstructorCall cc && cc.anonymousClass() != null) collect(cc.anonymousClass(), assigned);
            if (e instanceof LocalTypeDeclaration ltd) collect(ltd.typeInfo(), assigned);
            return true;
        });
    }
}
