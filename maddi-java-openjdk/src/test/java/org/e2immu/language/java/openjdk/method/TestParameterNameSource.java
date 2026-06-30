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

package org.e2immu.language.java.openjdk.method;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
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
