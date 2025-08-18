package org.e2immu.analyzer.modification.linkedvariables;

import org.e2immu.language.cst.api.info.Info;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.op.Cycles;

import java.util.List;
import java.util.Map;

public interface IteratingAnalyzer extends Analyzer {

    interface Configuration {
        int maxIterations();

        // the alternative is: set all to non-modifying
        boolean stopWhenCycleDetectedAndNoImprovements();

        boolean storeErrors();

        boolean trackObjectCreations();

        CycleBreakingStrategy cycleBreakingStrategy();
    }

    interface Output extends Analyzer.Output {
        G<Info> waitingFor();

        default int unresolved() {
            return waitingFor().vertices().size();
        }

        Cycles<Info> cyclesInWaitingFor();

        int iterations();

        Map<String, Integer> infoHistogram();
    }

    Output analyze(List<Info> analysisOrder);
}
