package org.e2immu.analyzer.modification.linkedvariables;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.Set;

public interface AbstractMethodAnalyzer extends Analyzer {

    interface Output extends Analyzer.Output {

        Set<MethodInfo> waitForMethods();
    }

    Output go(boolean firstIteration);
}
