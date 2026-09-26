package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMarkDegradedRace extends CommonTest {

    private static final int METHODS = 400;

    @DisplayName("two threads marking the same method degraded do not crash")
    @Test
    public void test() throws InterruptedException {
        StringBuilder sb = new StringBuilder("package a.md;\nclass M {\n");
        for (int i = 0; i < METHODS; i++) sb.append("    void m").append(i).append("() { }\n");
        sb.append("}\n");
        TypeInfo M = javaInspector.parse("a.md.M", sb.toString());
        List<MethodInfo> methods = M.methods();

        // guava's Fingerprint2011.weakHashLength32WithSeeds crashed run-to-run: marking a method degraded checked
        // for the property and then set it, and another thread linking the same callee (load64) set it in between;
        // 'set' refuses any overwrite, so the exception killed the caller being linked
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CyclicBarrier barrier = new CyclicBarrier(2);
        Runnable racer = () -> {
            for (MethodInfo mi : methods) {
                try {
                    barrier.await();
                    LinkComputerImpl.markDegraded(mi);
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                    barrier.reset();
                    return;
                }
            }
        };
        Thread t1 = new Thread(racer);
        Thread t2 = new Thread(racer);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
        assertTrue(methods.stream().allMatch(mi -> mi.analysis().haveAnalyzedValueFor(
                PropertyImpl.DEGRADED_ANALYSIS_METHOD)));
    }
}
