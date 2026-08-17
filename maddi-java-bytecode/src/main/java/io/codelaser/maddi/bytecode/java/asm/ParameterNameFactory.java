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

package org.e2immu.bytecode.java.asm;


import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.HashSet;
import java.util.Set;

public class ParameterNameFactory {
    private final Set<String> names = new HashSet<>();

    public String next(ParameterizedType type) {
        String base;
        if (type.typeInfo() != null) {
            // cheap way of finding out with high likelihood if we're dealing with a primitive
            // under no conditions can we trigger a type inspection here!
            if (type.typeInfo().fullyQualifiedName().indexOf('.') < 0) {
                base = firstLetterLowerCase(type.typeInfo().simpleName()).substring(0, 1);
            } else {
                base = firstLetterLowerCase(type.typeInfo().simpleName());
            }
        } else if (type.typeParameter() != null) {
            base = firstLetterLowerCase(type.typeParameter().simpleName());
        } else {
            base = "p";
        }
        if (!names.contains(base)) {
            names.add(base);
            return base;
        }
        int index = 1;
        while (true) {
            String name = base + index;
            if (!names.contains(name)) {
                names.add(name);
                return name;
            }
            index++;
        }
    }

    void register(String name) {
        this.names.add(name);
    }

    private static String firstLetterLowerCase(String s) {
        return Character.toLowerCase(s.charAt(0)) + (s.length() > 1 ? s.substring(1) : "");
    }
}
