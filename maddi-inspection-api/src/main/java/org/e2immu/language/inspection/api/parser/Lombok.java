package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

public interface Lombok {
    void addConstructors(TypeInfo typeInfo, Data lombokData);

    Data handleType(TypeInfo typeInfo);

    interface Data {
        boolean addGetters();
        boolean addSetters();

        boolean allArgsConstructor();
        boolean requiredArgsConstructor();
        boolean noArgsConstructor();

        boolean builder();
    }

    void handleField(Data lombokData, FieldInfo fieldInfo);
}
