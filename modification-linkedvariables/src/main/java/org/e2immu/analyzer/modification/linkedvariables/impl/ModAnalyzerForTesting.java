package org.e2immu.analyzer.modification.linkedvariables.impl;

import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.language.cst.api.info.Info;

import java.util.List;

public interface ModAnalyzerForTesting {
    List<AnalyzerException> go(List<Info> analysisOrder);

    default void go(List<Info> analysisOrder, int iterations) {
        for (int i = 0; i < iterations; ++i) go(analysisOrder);
    }
}
