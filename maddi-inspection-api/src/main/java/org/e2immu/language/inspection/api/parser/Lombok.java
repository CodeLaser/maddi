package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.FieldInfo;

public interface Lombok {
    void handleField(FieldInfo fieldInfo);
}
