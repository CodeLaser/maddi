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
