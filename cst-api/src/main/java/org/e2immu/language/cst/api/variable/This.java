package org.e2immu.language.cst.api.variable;

import org.e2immu.language.cst.api.info.TypeInfo;

public interface This extends Variable {
    TypeInfo typeInfo();

    TypeInfo explicitlyWriteType();

    boolean writeSuper();
}
