package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.StaticImportMap;

import java.util.*;

public class StaticImportMapImpl implements StaticImportMap {

    private final Set<TypeInfo> staticAsterisk = new LinkedHashSet<>();
    private final Map<String, TypeInfo> staticMemberToTypeInfo = new HashMap<>();

    @Override
    public void addStaticAsterisk(TypeInfo typeInfo) {
        staticAsterisk.add(typeInfo);
    }

    @Override
    public void putStaticMemberToTypeInfo(String member, TypeInfo typeInfo) {
        staticMemberToTypeInfo.put(member, typeInfo);
    }

    /*
     used in TypeContextImpl.staticFieldImports
    */
    @Override
    public Iterable<? extends Map.Entry<String, TypeInfo>> staticMemberToTypeInfoEntrySet() {
        return staticMemberToTypeInfo.entrySet();
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
    public TypeInfo getStaticMemberToTypeInfo(String methodName) {
        return staticMemberToTypeInfo.get(methodName);
    }
}
