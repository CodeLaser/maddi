package org.e2immu.analyzer.modification.linkedvariables;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.Map;
import java.util.Set;

/*
Phase 3:

Given the modification and linking of methods and fields,
compute independence of methods, fields, parameters, and primary type,
and forward the modification of fields to the parameters linked to it.
 */
public interface TypeModIndyAnalyzer extends Analyzer {

    interface Output extends Analyzer.Output {
        boolean resolvedInternalCycles();

        Map<MethodInfo, Set<MethodInfo>> waitForMethodModifications();

        Map<MethodInfo, Set<TypeInfo>> waitForTypeIndependence();

    }

    Output go(TypeInfo primaryType, Map<MethodInfo, Set<MethodInfo>> methodsWaitFor, boolean cycleBreakingActive);
}
