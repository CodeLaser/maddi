package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.MethodModAnalyzer;
import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;

import java.util.List;

public class MethodModAnalyzerImpl extends CommonAnalyzerImpl implements MethodModAnalyzer, ModAnalyzerForTesting {
    public MethodModAnalyzerImpl(Runtime runtime, IteratingAnalyzer.Configuration configuration) {
        super(configuration);
    }

    @Override
    public Output go(MethodInfo methodInfo, boolean activateCycleBreaking) {
        return null;
    }

    @Override
    public List<AnalyzerException> go(List<Info> analysisOrder) {
        return List.of();
    }
}
