package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

interface LinkComputerRecursion {
    void doAnonymousType(TypeInfo typeInfo);

    /** see {@link org.e2immu.analyzer.modification.link.LinkComputer#setLockComputeDisabled(boolean)} */
    default boolean lockComputeDisabled() {
        return false;
    }

    /** see {@link org.e2immu.analyzer.modification.link.LinkComputer#recordSummaryConsumption} */
    default void recordSummaryConsumption(MethodInfo consumer, MethodInfo consumed) {
        // no-op by default
    }

    MethodLinkedVariables doMethod(MethodInfo methodInfo);

    MethodLinkedVariables recurseMethod(MethodInfo methodInfo);

    MethodLinkedVariables doMethodShallowDoNotWrite(MethodInfo methodInfo);
}
