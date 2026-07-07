package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

// used in LinkComputerImpl, to facilitate on-the-fly link computation
class RecursionPrevention {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecursionPrevention.class);

    final Map<MethodInfo, Long> owner = new LinkedHashMap<>();
    final Map<MethodInfo, Long> done = new LinkedHashMap<>();
    final boolean recurse;

    public RecursionPrevention(boolean recurse) {
        this.recurse = recurse;
    }

    public enum How {GET, SHALLOW, LOCK}

    public synchronized boolean sourceAllowed(MethodInfo methodInfo) {
        Long prev = owner.get(methodInfo);
        if (prev == null) {
            owner.put(methodInfo, Thread.currentThread().threadId());
            return true;
        }
        return false;
    }

    public synchronized How contains(MethodInfo methodInfo) {
        if (!recurse) return How.GET;
        Long threadId = owner.get(methodInfo);
        LOGGER.debug("Test {} = {}", methodInfo, threadId);
        return threadId != null ? How.SHALLOW : How.LOCK;
    }

    public synchronized void doneSource(MethodInfo methodInfo) {
        owner.remove(methodInfo);
        done.put(methodInfo, Thread.currentThread().threadId());
    }

    public synchronized void report(MethodInfo methodInfo) {
        LOGGER.error("-- BEGIN OF RECURSION PREVENTION REPORT -- done by? {} I am {}", done.get(methodInfo),
                Thread.currentThread().threadId());
        owner.forEach((mi, id) -> LOGGER.error("{}: {}", id, mi));
        LOGGER.error("-- END OF RECURSION PREVENTION REPORT");
    }
}
