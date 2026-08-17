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
