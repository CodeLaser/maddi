package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.HashMap;
import java.util.Map;

public class TypeData {
    private final Map<String, SourceSet> sourceSetMap = new HashMap<>();
    private final Map<String, TypeInfo> singleTypeForFQN = new HashMap<>();

    public SourceSet getSourceSet(String name) {
        return sourceSetMap.get(name);
    }

    public void put(SourceSet sourceSet) {
        sourceSetMap.put(sourceSet.name(), sourceSet);
    }

    public TypeInfo getType(String fullyQualifiedName) {
        return singleTypeForFQN.get(fullyQualifiedName);
    }

    public void put(TypeInfo typeInfo) {
        singleTypeForFQN.put(typeInfo.fullyQualifiedName(), typeInfo);
    }
}
