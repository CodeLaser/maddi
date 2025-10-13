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

package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;

public interface HierarchyHelper {


    IsMyself isMyself(TypeInfo me, ParameterizedType type);

    IsMyself isMyself(TypeInfo me, TypeInfo bestType);

    boolean nonStaticallyEnclosingTypesContains(TypeInfo me, TypeInfo target);

    boolean parentalHierarchyContains(TypeInfo me, TypeInfo target);

    TypeInfo recursivelyImplements(TypeInfo enclosingType, String s);

    interface IsMyself {

        boolean isNo();

        boolean isYes();

        boolean inSamePrimaryType();

        boolean toFalse(Property property);
    }

}
