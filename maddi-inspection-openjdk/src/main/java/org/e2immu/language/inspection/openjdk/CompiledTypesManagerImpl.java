package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.SourceFile;

// important: this class should not retain any references to OpenJDK structures
public class CompiledTypesManagerImpl implements CompiledTypesManager {
    @Override
    public TypeData typeDataOrNull(String fqn, SourceSet sourceSetOfRequest, SourceSet nearestSourceSet, boolean complainSingle) {
        return null;
    }

    @Override
    public void addTypeInfo(SourceFile sourceFile, TypeInfo typeInfo) {

    }

    @Override
    public TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        return null;
    }

    @Override
    public void preload(String thePackage) {
        // TODO
    }
}
