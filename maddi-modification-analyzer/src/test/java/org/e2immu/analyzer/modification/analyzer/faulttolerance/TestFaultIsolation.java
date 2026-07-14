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

package org.e2immu.analyzer.modification.analyzer.faulttolerance;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.analyzer.impl.SingleIterationAnalyzerImpl;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fault isolation (hardening roadmap §2): a crash while analyzing one {@code Info} is recorded as an ERROR finding
 * and analysis continues with the rest, instead of aborting the whole run. We inject a deterministic crash by
 * feeding the analyzer a method whose type was never prepped — linking then trips {@code assert vd != null}
 * (tests run with {@code -ea}) — alongside a good, prepped method, and check that the good one is still analyzed.
 */
public class TestFaultIsolation extends CommonTest {

    @Language("java")
    private static final String GOOD = """
            package a.b;
            public class Good {
                private int count;
                public void inc() { count++; }
            }
            """;

    @Language("java")
    private static final String BAD = """
            package a.b;
            public class Bad {
                private int n;
                public int compute(int x) { int y = x + n; return y; }
            }
            """;

    /** Good is prepped; Bad is deliberately NOT prepped, so its method crashes when linked. */
    private record Fixture(List<Info> order, MethodInfo goodMethod, MethodInfo badMethod) {
    }

    private Fixture buildOrder() {
        TypeInfo good = javaInspector.parse("a.b.Good", GOOD);
        List<Info> goodOrder = prepWork(good);
        TypeInfo bad = javaInspector.parse("a.b.Bad", BAD); // NOT prepped on purpose
        MethodInfo badMethod = bad.methodStream().filter(m -> m.name().equals("compute")).findFirst().orElseThrow();
        MethodInfo goodMethod = good.methodStream().filter(m -> m.name().equals("inc")).findFirst().orElseThrow();
        List<Info> order = new ArrayList<>();
        order.add(badMethod);       // crashes first...
        order.addAll(goodOrder);    // ...the rest must still be analyzed
        return new Fixture(order, goodMethod, badMethod);
    }

    private SingleIterationAnalyzerImpl analyzer(boolean faultTolerant) {
        return new SingleIterationAnalyzerImpl(javaInspector, new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setFaultTolerant(faultTolerant).setGuardContracts(false).build());
    }

    @DisplayName("fault-tolerant: the crash is isolated, reported, and the good method is still analyzed")
    @Test
    public void testIsolated() throws IOException {
        Fixture f = buildOrder();
        SingleIterationAnalyzerImpl analyzer = analyzer(true);
        analyzer.go(f.order()); // must NOT throw

        List<Message> crashes = analyzer.messages().stream()
                .filter(m -> Set.of(SingleIterationAnalyzerImpl.ANALYZER_CRASH, SingleIterationAnalyzerImpl.LINK_CRASH)
                        .contains(m.category()))
                .toList();
        assertEquals(1, crashes.size(), "expected one crash finding, have: "
                                        + analyzer.messages().stream().map(Message::message).toList());
        Message crash = crashes.getFirst();
        assertTrue(crash.level().isError());
        assertEquals(f.badMethod().fullyQualifiedName(), crash.info().fullyQualifiedName());
        assertTrue(crash.message().contains("isolated"), crash.message());

        // isolation proof: the good method WAS analyzed (its links were computed), the bad one was not
        assertTrue(f.goodMethod().analysis().haveAnalyzedValueFor(METHOD_LINKS),
                "the good method must still be analyzed despite the crash on the bad one");
        assertFalse(f.badMethod().analysis().haveAnalyzedValueFor(METHOD_LINKS));
    }

    @DisplayName("default (fail-fast): the same crash aborts the run")
    @Test
    public void testFailFast() {
        Fixture f = buildOrder();
        SingleIterationAnalyzerImpl analyzer = analyzer(false);
        assertThrows(Throwable.class, () -> analyzer.go(f.order()));
    }
}
