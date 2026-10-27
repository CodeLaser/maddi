/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
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
