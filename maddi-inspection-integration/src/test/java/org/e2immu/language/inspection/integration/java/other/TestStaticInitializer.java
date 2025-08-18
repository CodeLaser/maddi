package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestStaticInitializer extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.*;

            public class InspectionGaps_2 {
                private static final Map<String, Integer> PRIORITY = new HashMap<>();

                static {
                    PRIORITY.put("e2container", 1);
                    PRIORITY.put("e2immutable", 2);
                }

                static {
                    PRIORITY.put("e1container", 3);
                    PRIORITY.put("e1immutable", 4);
                }

                private static int priority(String in) {
                    return PRIORITY.getOrDefault(in.substring(0, in.indexOf('-')), 10);
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = javaInspector.parse(INPUT);
        List<MethodInfo> methods = typeInfo.methods();
        assertEquals(3, methods.size());
        MethodInfo static0 = typeInfo.findUniqueMethod("<static_0>", 0);
        assertTrue(static0.isStatic());
        MethodInfo static1 = typeInfo.findUniqueMethod("<static_1>", 0);
        assertTrue(static1.isStatic());
    }

}
