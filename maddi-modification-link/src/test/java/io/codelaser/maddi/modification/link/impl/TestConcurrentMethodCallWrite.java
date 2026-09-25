/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Two worker threads may compute the links of the same method at the same time: RecursionPrevention's in-progress
 * set is per thread, and ExpressionVisitor's LOCK branch deliberately lets a caller on another thread recompute a
 * method whose links are not written yet ("the loser's write is a no-op"). The per-call-site write of
 * {@code variablesLinkedToObject} in {@code writeOutMethodCallAnalysis} was a check-then-set on the shared
 * MethodCall, so both threads could pass the check and the second {@code set} threw "Trying to overwrite a value
 * for property variablesLinkedToObject". Seen on Eclipse Collections in 2 of 4 runs
 * ({@code AbstractMutableByteKeySet.anySatisfy}, a [link-crash] that isolates the method).
 */
public class TestConcurrentMethodCallWrite extends CommonTest {

    private static final int METHODS = 400;
    private static final int CALLS = 30;

    private static String source() {
        StringBuilder sb = new StringBuilder("package a.b;\nimport java.util.List;\nclass X {\n");
        for (int m = 0; m < METHODS; m++) {
            sb.append("    int m").append(m).append("(List<String> list) {\n        int n = 0;\n");
            for (int c = 0; c < CALLS; c++) {
                sb.append("        n += list.get(").append(c).append(").length() + list.size();\n");
            }
            sb.append("        return n;\n    }\n");
        }
        return sb.append("}\n").toString();
    }

    @DisplayName("two threads linking the same method both write each call site once, without a crash")
    @Test
    public void test() throws Exception {
        TypeInfo X = javaInspector.parse("a.b.X", source());
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        List<Throwable> failures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            for (int m = 0; m < METHODS; m++) {
                MethodInfo methodInfo = X.findUniqueMethod("m" + m, 1);
                CyclicBarrier barrier = new CyclicBarrier(2);
                List<Future<?>> futures = new ArrayList<>();
                for (int t = 0; t < 2; t++) {
                    futures.add(executor.submit(() -> {
                        barrier.await();
                        return tlc.doMethod(methodInfo);
                    }));
                }
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (java.util.concurrent.ExecutionException e) {
                        failures.add(e.getCause());
                    }
                }
            }
        }
        assertEquals(List.of(), failures.stream().map(Throwable::toString).distinct().toList());
    }
}
