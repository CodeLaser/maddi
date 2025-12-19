package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.analyzer.modification.analyzer.FieldAnalyzer;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.runtime.Runtime;

public class FieldAnalyzerImpl extends CommonAnalyzerImpl implements FieldAnalyzer {

    protected FieldAnalyzerImpl(Runtime runtime, IteratingAnalyzer.Configuration configuration) {
        super(configuration);
    }

    @Override
    public Output go(FieldInfo fieldInfo, boolean cycleBreakingActive) {
        return null;
    }
}
