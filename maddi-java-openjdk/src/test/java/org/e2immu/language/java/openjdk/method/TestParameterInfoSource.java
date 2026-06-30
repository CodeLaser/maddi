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

public class TestParameterInfoSource extends CommonTest {

    @Language("java")
    private static final String CLASS_A = """
            package a;
            import java.util.List;
            import java.util.function.Function;
            import java.util.stream.Collectors;
            
            class A<T> {
                public List<String> transform(List<T> items) {
                    return items.stream()
                               .map(this::processItem)
                               .collect(Collectors.toList());
                }

                private String processItem(T item) {
                    return item.toString();
                }
            }
            """;

    @DisplayName("Ensure that there is always a source for pi, also when transform (a method reference to "
                + "processItem, scanned before its declaration) is present")
    @Test
    public void test1() {
        TypeInfo A = scan("a.A", CLASS_A);
        MethodInfo m = A.findUniqueMethod("processItem", 1);
        ParameterInfo pi = m.parameters().getFirst();
        assertNotNull(pi.source());
        // 'item' on line 13: 'private String processItem(T item) {'
        assertEquals("-@13:32-13:37", pi.source().toString());
    }

}
