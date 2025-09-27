package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.info.TypeInfo;


public interface ByteCodeInspector {

    TypeInfo load(SourceFile sourceFile);

    TypeInfo load(SourceFile sourceFile, TypeInfo typeInfo);
}
