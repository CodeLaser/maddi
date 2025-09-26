package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.List;

public interface TypeMap {

    TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest);

    void put(TypeInfo typeInfo);

    void invalidate(TypeInfo typeInfo);

    List<TypeInfo> primaryTypesInPackage(String packageName);
}
