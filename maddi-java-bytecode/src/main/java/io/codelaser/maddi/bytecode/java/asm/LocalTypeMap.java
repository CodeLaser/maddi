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

package io.codelaser.maddi.bytecode.java.asm;


import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager;

/*
In the local type map, types are either
 */
public interface LocalTypeMap {

    // delegate to CTM
    boolean acceptFQN(String fqName);

    // delegate to CTM
    String pathToFqn(String name);

    CompiledTypesManager.TypeData typeData(String fqn, SourceSet sourceSet, SourceSet nearestSourceSet);

    /*
    now = directly
    trigger = leave in TRIGGER_BYTE_CODE state; if never visited, it'll not be loaded
    queue = ensure that it gets loaded before building the type map
     */
    enum LoadMode {NOW, TRIGGER, QUEUE}

    /*
    up to a TRIGGER_BYTE_CODE_INSPECTION stage, or, when start is true,
    actual loading
     */
    @Modified
    TypeInfo getOrCreate(String fqn, SourceSet sourceSetOfRequest, SourceSet nearestSourceSet, LoadMode loadMode);

    /*
     Call from My*Visitor back to ByteCodeInspector, as part of a `inspectFromPath(Source)` call.
     */

    // do actual byte code inspection
    @Modified
    TypeInfo inspectFromPath(CompiledTypesManager.TypeData typeData, SourceSet sourceSetOfRequest, LoadMode loadMode);

    boolean allowCreationOfStubTypes();

}
