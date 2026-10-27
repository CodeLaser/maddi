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

package io.codelaser.maddi.java.openjdk.method;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A formal parameter's detailed sources must contain an entry keyed by the parameter's name
 * ({@code pi.source().detailedSources().detail(pi.name())}), holding the source span of that name.
 */
public class TestParameterNameSource extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                int m(int alpha, String beta) {
                    return alpha;
                }
            }
            """;

    private static String nameDetail(ParameterInfo pi) {
        var detail = pi.source().detailedSources().detail(pi.name());
        assertNotNull(detail, "no detailed source for parameter name '" + pi.name() + "'");
        return detail.toString();
    }

    @DisplayName("formal parameter names carry their source span, keyed by pi.name()")
    @Test
    public void parameterNames() {
        TypeInfo X = scan("a.b.X", INPUT);
        MethodInfo m = X.findUniqueMethod("m", 2);
        ParameterInfo alpha = m.parameters().get(0);
        ParameterInfo beta = m.parameters().get(1);
        assertEquals("alpha", alpha.name());
        assertEquals("beta", beta.name());
        assertEquals("-@3:15-3:19", nameDetail(alpha)); // 'alpha' on line 3
        assertEquals("-@3:29-3:32", nameDetail(beta));  // 'beta' on line 3
    }
}
