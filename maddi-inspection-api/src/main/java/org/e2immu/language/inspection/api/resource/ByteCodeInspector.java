package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;


public interface ByteCodeInspector {

    TypeInfo load(SourceFile sourceFile);

    TypeInfo load(SourceFile sourceFile, TypeInfo typeInfo);

    interface TypeParameterContext {
        void add(TypeParameter typeParameter);

        TypeParameter get(String typeParamName);

        TypeParameterContext newContext();
    }

    enum Status {
        ON_DEMAND, // initial
        BEING_LOADED, // intermediate
        DONE, // final
        IN_QUEUE // to be removed
    }

    interface Data {
        Status status();

        void setStatus(Status status);

        TypeParameterContext typeParameterContext();
    }
}
