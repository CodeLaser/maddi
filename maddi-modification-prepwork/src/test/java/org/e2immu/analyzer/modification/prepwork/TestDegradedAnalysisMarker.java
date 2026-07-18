package org.e2immu.analyzer.modification.prepwork;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task #36 (degradation visibility): a type isolated by fault-tolerant prep stamps
 * DEGRADED_ANALYSIS_METHOD on all its methods, so per-call consumers (VL2O / extract-interface)
 * know to treat them pessimistically. Uses the task-#33 shape (forward-referenced member record of
 * an anonymous class) as a real degradation trigger; when #33 is fixed, replace the input with
 * another trigger or drop this in favour of a synthetic one.
 */
public class TestDegradedAnalysisMarker extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.stream.Stream;
            class X {
                interface Support { Object example(); }
                Support make() {
                    return new Support() {
                        @Override
                        public Object example() {
                            return Stream.of(new byte[]{1}).map(Cmp::new).sorted().toList();
                        }
                        private record Cmp(byte[] bytes) implements Comparable<Cmp> {
                            @Override
                            public int compareTo(Cmp o) { return 0; }
                        }
                    };
                }
            }
            """;

    @DisplayName("fault-tolerant prep isolation stamps DEGRADED_ANALYSIS_METHOD")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        PrepAnalyzer prep = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder()
                .setFaultTolerant(true).build());
        prep.doPrimaryTypes(java.util.Set.of(X));
        assertFalse(prep.exceptions().isEmpty(), "the #33 shape must still trigger isolation");
        boolean anyStamped = X.recursiveSubTypeStream()
                .flatMap(TypeInfo::constructorAndMethodStream)
                .anyMatch(mi -> mi.analysis().getOrDefault(PropertyImpl.DEGRADED_ANALYSIS_METHOD,
                        ValueImpl.BoolImpl.FALSE).isTrue());
        assertTrue(anyStamped, "isolated type's methods must carry the degradation marker");
    }
}
