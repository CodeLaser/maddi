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

package org.e2immu.language.cst.print.kotlin;

import org.e2immu.language.cst.api.type.NullableState;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.Map;

/**
 * Renders a {@link ParameterizedType} as a Kotlin type reference: JVM primitives and common JDK types are
 * mapped to their Kotlin names (`int`→`Int`, `java.lang.String`→`String`, `java.lang.Object`→`Any`, …), arrays
 * become `Array<…>` (best-effort; a dedicated `IntArray` etc. is a refinement), and generics recurse. Nullability
 * is not tracked in the CST, so no `?` is emitted.
 */
public class KotlinTypeName {

    private static final Map<String, String> KOTLIN = Map.ofEntries(
            Map.entry("int", "Int"), Map.entry("long", "Long"), Map.entry("short", "Short"),
            Map.entry("byte", "Byte"), Map.entry("char", "Char"), Map.entry("boolean", "Boolean"),
            Map.entry("float", "Float"), Map.entry("double", "Double"), Map.entry("void", "Unit"),
            Map.entry("java.lang.Integer", "Int"), Map.entry("java.lang.Long", "Long"),
            Map.entry("java.lang.Short", "Short"), Map.entry("java.lang.Byte", "Byte"),
            Map.entry("java.lang.Character", "Char"), Map.entry("java.lang.Boolean", "Boolean"),
            Map.entry("java.lang.Float", "Float"), Map.entry("java.lang.Double", "Double"),
            Map.entry("java.lang.Void", "Unit"), Map.entry("java.lang.String", "String"),
            Map.entry("java.lang.Object", "Any"), Map.entry("java.lang.CharSequence", "CharSequence"),
            Map.entry("java.util.List", "List"), Map.entry("java.util.Map", "Map"),
            Map.entry("java.util.Set", "Set"), Map.entry("java.util.Collection", "Collection"));

    /** The Kotlin type reference as a string (no leading/trailing space); a nullable type gets a trailing `?`. */
    public static String of(ParameterizedType pt) {
        return nullable(pt, base(pt));
    }

    private static String nullable(ParameterizedType pt, String s) {
        // the Kotlin front-end records NULLABLE on the parameterized type; the (Java-oriented) default is UNSPECIFIED
        return pt.nullable() == NullableState.NULLABLE ? s + "?" : s;
    }

    private static String base(ParameterizedType pt) {
        if (pt.arrays() > 0) {
            String s = of(pt.copyWithArrays(0));
            for (int i = 0; i < pt.arrays(); i++) s = "Array<" + s + ">";
            return s;
        }
        if (pt.isTypeParameter()) {
            return pt.typeParameter().simpleName();
        }
        if (pt.typeInfo() == null) {
            return "Any"; // unbound wildcard / no type
        }
        String fqn = pt.typeInfo().fullyQualifiedName();
        String base = KOTLIN.getOrDefault(fqn, pt.typeInfo().simpleName());
        if (pt.parameters().isEmpty()) return base;
        StringBuilder sb = new StringBuilder(base).append('<');
        for (int i = 0; i < pt.parameters().size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(of(pt.parameters().get(i)));
        }
        return sb.append('>').toString();
    }
}
