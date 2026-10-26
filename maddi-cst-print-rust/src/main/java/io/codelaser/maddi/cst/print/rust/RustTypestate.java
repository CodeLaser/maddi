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

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.print.rust.RustFacts.Side;

import java.util.HashSet;
import java.util.Set;

/**
 * Eventual immutability ({@code @Mark}, {@code @Only}, road to immutability §060) as Rust's typestate pattern. The
 * struct gets a state parameter, {@code struct Config<S = Before>}; a method on either side of the transition is in
 * {@code impl<S> Config<S>}, an {@code @Only(before=…)} one (and the constructors) in {@code impl Config<Before>}, an
 * {@code @Only(after=…)} one in {@code impl Config<After>}; the {@code @Mark} takes {@code self} by value and returns
 * the object in its after state. A call on the wrong side, which Java's eventual type rejects at run time, is rustc's
 * E0599 (no such method for that state); so is a second mark.
 * <p>
 * One transition per type: every eventual method carries the same label. A type whose mark returns a value, that is
 * generic, has a lifetime, a source parent or implements a source interface keeps its plain shape.
 */
final class RustTypestate {
    static final String STATE = "S";
    static final String BEFORE = "Before";
    static final String AFTER = "After";

    private RustTypestate() {
    }

    static boolean typestate(TypeInfo typeInfo, RustContext context) {
        if (typeInfo == null || typeInfo.isInterface() || typeInfo.typeNature().isRecord()
            || typeInfo.typeNature().isEnum() || !typeInfo.typeParameters().isEmpty()
            || typeInfo.compilationUnit().externalLibrary() || RustTypePrinter.sourceParent(typeInfo) != null
            || context.borrows.hasLifetime(typeInfo)
            || typeInfo.interfacesImplemented().stream().anyMatch(i -> !i.typeInfo().compilationUnit().externalLibrary())) {
            return false;
        }
        boolean mark = false;
        Set<String> labels = new HashSet<>();
        for (MethodInfo m : typeInfo.methods()) {
            Side side = context.facts.side(m);
            if (side == Side.ANY) continue;
            if (m.isStatic() || side == Side.MARK && !m.isVoid()) return false;
            mark |= side == Side.MARK;
            labels.add(String.join(",", new java.util.TreeSet<>(context.facts.marks(m))));
        }
        return mark && labels.size() == 1;
    }

    /** The side a method of a {@link #typestate} type is printed on. */
    static Side side(MethodInfo m, RustContext context) {
        if (m.isStatic() || m.isConstructor()) return Side.BEFORE;
        return context.facts.side(m);
    }
}
