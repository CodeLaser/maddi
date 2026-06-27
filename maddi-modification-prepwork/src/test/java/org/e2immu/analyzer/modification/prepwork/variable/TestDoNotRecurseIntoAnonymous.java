package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// the 'doNotRecurseIntoAnonymous' option skips analysis of lambda/anonymous-class bodies. The enclosing method
// must still analyze without crashing: copyReadsFromAnonymousMethod used to dereference the (now absent)
// VariableData of those un-analyzed inner bodies.
public class TestDoNotRecurseIntoAnonymous extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Supplier;
            public class X {
                interface Sink { void accept(int v); }
                int method(int outer) {
                    Sink s = new Sink() {
                        int captured = outer;
                        public void accept(int v) {
                            int r = outer + v + captured;
                        }
                    };
                    Supplier<Integer> sup = () -> outer;
                    s.accept(1);
                    return sup.get();
                }
            }
            """;

    @DisplayName("doNotRecurseIntoAnonymous: enclosing method analyzes without crashing")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse(ABX, INPUT);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder()
                .setDoNotRecurseIntoAnonymous(true).build());
        assertDoesNotThrow(() -> analyzer.doPrimaryType(X));

        MethodInfo method = X.findUniqueMethod("method", 1);
        VariableData vd = VariableDataImpl.of(method.methodBody().lastStatement());
        assertNotNull(vd);
        // the enclosing method's own variables are still tracked (the closure reads from the skipped lambda/
        // anonymous bodies are simply not copied in, which is exactly what the option asks for)
        assertTrue(vd.knownVariableNamesToString().contains("sup"));
    }
}
