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

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * The comparator's SINGLETON is shared by every thread that sorts expressions; its memoization cache was one
 * IdentityHashMap, unsynchronized. Under concurrent computeIfAbsent the table corrupts and get() loops forever:
 * a duplication-detection corpus run was found after an hour with 113 worker threads spinning in
 * IdentityHashMap.get, one in put, all from ExpressionComparator.compare. The cache is per thread now.
 * <p>
 * The test hammers the singleton from 16 threads over more distinct expressions than the cache holds (so it
 * clears while others read), bounded by a preemptive timeout: the corrupted map hangs, a correct one returns
 * the single-threaded answers.
 */
public class TestExpressionComparatorConcurrent extends CommonTest {

    @DisplayName("16 threads comparing through the shared singleton terminate, and agree with one thread")
    @Test
    public void test() throws Exception {
        List<Expression> expressions = new ArrayList<>();
        for (int v = 0; v < 400; v++) {
            expressions.add(r.sum(r.product(k, r.newInt(v)), r.sum(i, r.newInt(v % 7))));
        }
        int n = expressions.size();
        int[][] expected = new int[n][n];
        for (int p = 0; p < n; p++) {
            for (int q = 0; q < n; q++) {
                expected[p][q] = Integer.signum(ExpressionComparator.SINGLETON.compare(expressions.get(p), expressions.get(q)));
            }
        }
        assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
            try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
                List<Future<Integer>> futures = new ArrayList<>();
                for (int t = 0; t < 16; t++) {
                    int seed = t;
                    futures.add(pool.submit(() -> {
                        int disagreements = 0;
                        int p = seed;
                        int q = (seed * 31) % n;
                        for (int round = 0; round < 200_000; round++) {
                            p = (p * 1103515245 + 12345) & 0x7fffffff;
                            q = (q * 214013 + 2531011) & 0x7fffffff;
                            int pi = p % n;
                            int qi = q % n;
                            int c = Integer.signum(ExpressionComparator.SINGLETON.compare(expressions.get(pi), expressions.get(qi)));
                            if (c != expected[pi][qi]) disagreements++;
                        }
                        return disagreements;
                    }));
                }
                int disagreements = 0;
                for (Future<Integer> f : futures) disagreements += f.get();
                assertEquals(0, disagreements, "a concurrent compare answered differently from the single-threaded one");
            }
        });
    }
}
