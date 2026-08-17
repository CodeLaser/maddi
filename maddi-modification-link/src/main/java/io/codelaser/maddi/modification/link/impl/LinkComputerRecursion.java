package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;

interface LinkComputerRecursion {
    void doAnonymousType(TypeInfo typeInfo);

    /** see {@link io.codelaser.maddi.modification.link.LinkComputer#setLockComputeDisabled(boolean)} */
    default boolean lockComputeDisabled() {
        return false;
    }

    /** see {@link io.codelaser.maddi.modification.link.LinkComputer#recordSummaryConsumption} */
    default void recordSummaryConsumption(MethodInfo consumer, MethodInfo consumed) {
        // no-op by default
    }

    MethodLinkedVariables doMethod(MethodInfo methodInfo);

    MethodLinkedVariables recurseMethod(MethodInfo methodInfo);

    MethodLinkedVariables doMethodShallowDoNotWrite(MethodInfo methodInfo);
}
