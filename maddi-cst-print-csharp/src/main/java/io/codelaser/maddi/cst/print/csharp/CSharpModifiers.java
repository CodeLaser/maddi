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

import io.codelaser.maddi.cst.api.info.Access;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.FieldModifier;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeModifier;

/**
 * Access and inheritance modifiers. C# code writes its access modifiers out, so every declaration gets one, except
 * where C# forbids it (the public members of an interface).
 * <ul>
 *   <li>Java's package access is {@code internal}: a translated code base is one assembly;</li>
 *   <li>Java's {@code protected} grants the package access too, which is C#'s {@code protected internal};</li>
 *   <li>a Java method can be overridden unless it is final, static or private, so it is {@code virtual} in a class
 *   that can be extended; an override of a class's method is {@code override} ({@code sealed override} when final),
 *   an implementation of an interface's method is not.</li>
 * </ul>
 */
final class CSharpModifiers {

    private CSharpModifiers() {
    }

    private enum Declared {PUBLIC, PROTECTED, PACKAGE, PRIVATE}

    /**
     * A field's access as declared. Not {@link FieldInfo#access()}, which is combined with its type's: a protected
     * field of a package-private class reads as package access there.
     */
    static String access(FieldInfo f) {
        Declared declared = Declared.PACKAGE;
        for (FieldModifier m : f.modifiers()) {
            if (m.isPublic()) declared = Declared.PUBLIC;
            else if (m.isProtected()) declared = Declared.PROTECTED;
            else if (m.isPrivate()) declared = Declared.PRIVATE;
        }
        return csharp(declared, f.owner(), f);
    }

    /** A nested type's access as declared, as for a field. */
    static String access(TypeInfo t, TypeInfo enclosing) {
        Declared declared = Declared.PACKAGE;
        for (TypeModifier m : t.typeModifiers()) {
            if (m.isPublic()) declared = Declared.PUBLIC;
            else if (m.isProtected()) declared = Declared.PROTECTED;
            else if (m.isPrivate()) declared = Declared.PRIVATE;
        }
        return csharp(declared, enclosing, t);
    }

    /**
     * A method's access: as declared, an interface's abstract, default and static methods public. An override has
     * the access of the class method it overrides: Java may widen it, C# may not.
     */
    static String access(MethodInfo m, TypeInfo owner) {
        MethodInfo overridden = m.overrides().stream()
                .filter(o -> o != m && !o.typeInfo().isInterface() && CSharpNames.translated(o.typeInfo()))
                .findFirst().orElse(null);
        if (overridden != null) return access(overridden, overridden.typeInfo());
        Access access = m.access();
        Declared declared = access == null || access.isPackage() ? Declared.PACKAGE : access.isPublic() ? Declared.PUBLIC
                : access.isProtected() ? Declared.PROTECTED : access.isPrivate() ? Declared.PRIVATE : Declared.PACKAGE;
        return csharp(declared, owner, m);
    }

    /**
     * The C# modifier; null for a public member of an interface, where C# writes none. A private declaration that code
     * outside its owner reaches is {@code internal} ({@link CSharpAccess}).
     */
    private static String csharp(Declared declared, TypeInfo owner, Object info) {
        if (declared == Declared.PRIVATE && CSharpContext.reachedFromOutside(info)) return "internal";
        if (owner != null && owner.isInterface() && (declared == Declared.PUBLIC || declared == Declared.PACKAGE)) {
            return null;
        }
        return switch (declared) {
            case PUBLIC -> "public";
            case PROTECTED -> "protected internal";
            case PRIVATE -> "private";
            case PACKAGE -> "internal";
        };
    }

    /** {@code virtual}, {@code override}, {@code sealed override}, {@code abstract}; null when none applies. */
    static String inheritance(MethodInfo m, TypeInfo owner) {
        if (m.isStatic() || m.isConstructor() || owner.isInterface()) return null;
        boolean overridesClassMethod = m.overrides().stream().anyMatch(o -> o != m && !o.typeInfo().isInterface());
        if (overridesClassMethod) {
            if (m.isAbstract()) return "abstract override";
            return m.isFinal() && extensible(owner) ? "sealed override" : "override";
        }
        if (m.isAbstract()) return "abstract";
        if (m.isFinal() || m.access() != null && m.access().isPrivate() || !extensible(owner)) return null;
        return "virtual";
    }

    /** A class that can be extended: not final, not an enum or record. */
    static boolean extensible(TypeInfo typeInfo) {
        return typeInfo.typeNature().isClass() && !typeInfo.isFinal() && !typeInfo.typeNature().isRecord()
               && !typeInfo.typeNature().isEnum();
    }
}
