/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.impl.parser;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.parser.StaticImportMap;

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
