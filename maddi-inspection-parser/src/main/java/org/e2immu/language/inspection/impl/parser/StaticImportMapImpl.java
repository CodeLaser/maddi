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

package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.StaticImportMap;

import java.util.*;

public class StaticImportMapImpl implements StaticImportMap {

    private final Set<TypeInfo> staticAsterisk = new LinkedHashSet<>();
    private final Map<String, List<TypeInfo>> staticMemberToTypeInfo = new HashMap<>();

    @Override
    public void addStaticAsterisk(TypeInfo typeInfo) {
        staticAsterisk.add(typeInfo);
    }

    @Override
    public void addStaticMemberToTypeInfo(String member, TypeInfo typeInfo) {
        staticMemberToTypeInfo.computeIfAbsent(member, s -> new ArrayList<>()).add(typeInfo);
    }

    /*
    used in ListMethodAndConstructorCandidates, and TypeContextImpl.staticFieldImports
     */
    @Override
    public Iterable<? extends TypeInfo> staticAsterisk() {
        return staticAsterisk;
    }

    /*
    used in ListMethodAndConstructorCandidates, and TypeContextImpl.staticFieldImports
    */
    @Override
    public Iterable<TypeInfo> getStaticMemberToTypeInfo(String methodName) {
        return Objects.requireNonNullElse(staticMemberToTypeInfo.get(methodName), List.of());
    }
}
