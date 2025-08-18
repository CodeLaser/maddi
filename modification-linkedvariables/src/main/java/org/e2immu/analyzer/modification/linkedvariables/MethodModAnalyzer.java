package org.e2immu.analyzer.modification.linkedvariables;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.Map;
import java.util.Set;

/*
Phase 1.
Single method analyzer.

Analyzes statements in a method, and tries to determine if the method is @Modified.
Computes linking.

While it could also write out method independence and parameter independence, this code sits in Phase 3.
 */
public interface MethodModAnalyzer extends Analyzer {

    interface Output extends Analyzer.Output {

        Set<MethodInfo> waitForMethods();

        Set<TypeInfo> waitForIndependenceOfTypes();

        Map<String, Integer> infoHistogram();
    }

    Output go(MethodInfo methodInfo, boolean activateCycleBreaking);
}
