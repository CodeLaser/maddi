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
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.FieldReference;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The code that is translated together: the primary types whose Kotlin is compiled as one, main and test sources. A
 * member it holds may take Kotlin's read-only collection type when each use of it in these types allows one, whatever
 * its access ({@link KotlinReadOnly}); Java code compiled against the Kotlin classes sees {@code java.util.List} either
 * way. {@link #FILE}, the default, is the printer on its own, which sees one compilation unit: then only a private
 * field or a closed method's result can be read-only.
 */
public final class KotlinWorld {

    /** Only the compilation unit being printed. */
    public static final KotlinWorld FILE = new KotlinWorld(List.of());

    private final Set<TypeInfo> types;
    // per primary type, the fields and methods its code mentions, computed when first asked
    private final Map<TypeInfo, Set<Object>> mentioned = new ConcurrentHashMap<>();

    // the read-only verdicts on fields and methods (KotlinReadOnly), decided once for the world
    private final Map<Object, Boolean> readOnly = new ConcurrentHashMap<>();

    private KotlinWorld(Collection<TypeInfo> primaryTypes) {
        Set<TypeInfo> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(primaryTypes);
        this.types = Collections.unmodifiableSet(set);
    }

    /** The primary types translated and compiled together. */
    public static KotlinWorld of(Collection<TypeInfo> primaryTypes) {
        return primaryTypes.isEmpty() ? FILE : new KotlinWorld(primaryTypes);
    }

    public boolean isFile() {
        return types.isEmpty();
    }

    /** The primary type is translated with the others: every use of its members is in sight. */
    public boolean contains(TypeInfo primaryType) {
        return types.contains(primaryType);
    }

    /** The primary types whose code mentions the field or method: reads, calls, method references. */
    public List<TypeInfo> mentioning(Object member) {
        return types.stream().filter(t -> mentioned.computeIfAbsent(t, KotlinWorld::mentions).contains(member))
                .toList();
    }

    /** A read-only verdict remembered; null when not decided, or for {@link #FILE}, which remembers nothing. */
    Boolean readOnly(Object member) {
        return readOnly.get(member);
    }

    void readOnly(Object member, boolean verdict) {
        if (!isFile()) readOnly.put(member, verdict);
    }

    private static Set<Object> mentions(TypeInfo primaryType) {
        Set<Object> members = Collections.newSetFromMap(new IdentityHashMap<>());
        KotlinReadOnly.forEachBody(primaryType, (method, body) -> body.visit((Element e) -> {
            switch (e) {
                case VariableExpression ve when ve.variable() instanceof FieldReference fr -> members.add(fr.fieldInfo());
                case MethodCall mc when mc.methodInfo() != null -> members.add(mc.methodInfo());
                case MethodReference mr when mr.methodInfo() != null -> members.add(mr.methodInfo());
                default -> {
                }
            }
            return true;
        }));
        return Collections.unmodifiableSet(members);
    }
}
