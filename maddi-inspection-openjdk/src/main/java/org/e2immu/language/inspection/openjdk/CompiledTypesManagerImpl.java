package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.SourceFile;

import java.util.*;

// important: this class should not retain any references to OpenJDK structures
public class CompiledTypesManagerImpl implements CompiledTypesManager {
    private final SourceSet javaBase;
    private final Map<String, TypeInfo> typesLoaded = new HashMap<>();

    public CompiledTypesManagerImpl(SourceSet javaBase) {
        this.javaBase = javaBase;
    }

    @Override
    public SourceSet javaBase() {
        return javaBase;
    }

    @Override
    public TypeData typeDataOrNull(String fqn, SourceSet sourceSetOfRequest, SourceSet nearestSourceSet, boolean complainSingle) {
        return null;
    }

    @Override
    public void addTypeInfo(SourceFile sourceFile, TypeInfo typeInfo) {
        typesLoaded.put(typeInfo.fullyQualifiedName(), typeInfo);
    }

    @Override
    public List<TypeInfo> typesLoaded(Boolean compiled) {
        return typesLoaded.values().stream().filter(ti -> compiled == null
                                                 || ti.compilationUnit().sourceSet().externalLibrary() == compiled)
                .sorted(Comparator.comparing(TypeInfo::fullyQualifiedName))
                .toList();
    }

    @Override
    public TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        return typesLoaded.get(fullyQualifiedName);
    }

    @Override
    public void preload(String thePackage) {
        // TODO
    }
}
