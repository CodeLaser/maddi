package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.Map;

public interface StaticImportMap {
    void addStaticAsterisk(TypeInfo typeInfo);

    void putStaticMemberToTypeInfo(String member, TypeInfo typeInfo);

    Iterable<? extends Map.Entry<String, TypeInfo>> staticMemberToTypeInfoEntrySet();

    Iterable<? extends TypeInfo> staticAsterisk();

    TypeInfo getStaticMemberToTypeInfo(String methodName);
}
