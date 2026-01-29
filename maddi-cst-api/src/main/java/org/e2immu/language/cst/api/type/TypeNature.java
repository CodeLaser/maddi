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

package org.e2immu.language.cst.api.type;

import org.e2immu.language.cst.api.output.element.Keyword;

public interface TypeNature {
    default boolean isClass() {
        return false;
    }

    default boolean isEnum() {
        return false;
    }

    default boolean isInterface() {
        return false;
    }

    default boolean isRecord() {
        return false;
    }

    default boolean isStatic() {
        return false;
    } // is true for all but inner classes in Java

    default boolean isAnnotation() {
        return false;
    }

    /*
    A stub type is created during parsing, because both source and byte code are missing.
    The type's inspection data gets filled up as well as possible, so that we don't have to stop
    parsing.
     */
    default boolean isStub() {
        return false;
    }

    default boolean isPackageInfo() {
        return false;
    }

    Keyword keyword();
}
