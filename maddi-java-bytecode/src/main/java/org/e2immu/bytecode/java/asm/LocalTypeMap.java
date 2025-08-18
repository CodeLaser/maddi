package org.e2immu.bytecode.java.asm;


import org.e2immu.annotation.Modified;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.SourceFile;

import java.util.List;

/*
In the local type map, types are either
 */
public interface LocalTypeMap {

    // delegate to CTM
    boolean acceptFQN(String fqName);

    // get from local map, null otherwise
    TypeInfo getLocal(String fqName);

    // delegate to CTM
    String pathToFqn(String name);

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
    TypeInfo getOrCreate(String fqn, LoadMode loadMode);

    /*
     Call from My*Visitor back to ByteCodeInspector, as part of a `inspectFromPath(Source)` call.
     */

    // do actual byte code inspection
    @Modified
    TypeInfo inspectFromPath(TypeInfo typeInfo, SourceFile name,
                             TypeParameterContext typeParameterContext, LoadMode loadMode);

    boolean allowCreationOfStubTypes();

}
