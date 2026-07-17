package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// used in LinkComputerImpl, to facilitate on-the-fly link computation
class RecursionPrevention {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecursionPrevention.class);

    /*
    Recursion detection concerns a single computation stack, so the in-progress set is per thread. Sharing one
    map across parallel worker threads (PARALLEL=n) made a method "in progress" on thread A degrade thread B's
    independent computation to SHALLOW, producing scheduling-dependent summaries: a 1-method methodLinks
    flip-flop that blocked fixpoint certification plus a handful of order-dependent verdicts (fernflower
    PARALLEL=8 A/B), and a contended global monitor on the per-call-expression hot path.
     */
    private final ThreadLocal<Set<MethodInfo>> inProgress = ThreadLocal.withInitial(HashSet::new);
    private final Map<MethodInfo, Long> done = new ConcurrentHashMap<>();
    final boolean recurse;

    public RecursionPrevention(boolean recurse) {
        this.recurse = recurse;
    }

    public enum How {GET, SHALLOW, LOCK}

    public boolean sourceAllowed(MethodInfo methodInfo) {
        return inProgress.get().add(methodInfo);
    }

    public How contains(MethodInfo methodInfo) {
        if (!recurse) return How.GET;
        boolean mine = inProgress.get().contains(methodInfo);
        LOGGER.debug("Test {} = {}", methodInfo, mine);
        return mine ? How.SHALLOW : How.LOCK;
    }

    public void doneSource(MethodInfo methodInfo) {
        inProgress.get().remove(methodInfo);
        done.put(methodInfo, Thread.currentThread().threadId());
    }

    public void report(MethodInfo methodInfo) {
        LOGGER.error("-- BEGIN OF RECURSION PREVENTION REPORT -- done by? {} I am {}", done.get(methodInfo),
                Thread.currentThread().threadId());
        inProgress.get().forEach(mi -> LOGGER.error("in progress on this thread: {}", mi));
        LOGGER.error("-- END OF RECURSION PREVENTION REPORT");
    }
}
