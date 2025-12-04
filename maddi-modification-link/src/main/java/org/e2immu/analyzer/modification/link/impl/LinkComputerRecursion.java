package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

interface LinkComputerRecursion {
    void doAnonymousType(TypeInfo typeInfo);

    MethodLinkedVariables doMethod(MethodInfo methodInfo);

    MethodLinkedVariables recurseMethod(MethodInfo methodInfo);

    MethodLinkedVariables doMethodShallowDoNotWrite(MethodInfo methodInfo);
}
