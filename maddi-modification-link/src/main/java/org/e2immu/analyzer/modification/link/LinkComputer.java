package org.e2immu.analyzer.modification.link;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

public interface LinkComputer {
    // tests only!
    void doPrimaryType(TypeInfo primaryType);

    Link doField(FieldInfo fieldInfo);

    MethodLinkedVariables doMethod(MethodInfo methodInfo);

}
