package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.TypeInfo;

public interface StaticImportMap {
    void addStaticAsterisk(TypeInfo typeInfo);

    void addStaticMemberToTypeInfo(String member, TypeInfo typeInfo);

    Iterable<? extends TypeInfo> staticAsterisk();

    Iterable<TypeInfo> getStaticMemberToTypeInfo(String methodName);
}
