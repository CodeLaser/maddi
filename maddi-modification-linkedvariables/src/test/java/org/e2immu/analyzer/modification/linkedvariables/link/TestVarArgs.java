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

package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_PARAMETER;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVarArgs extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            import java.io.Closeable;
            
            public class B {
            
                public static void closeAndIgnoreErrors(Closeable... closeables) {
                    for (Closeable closeable : closeables) {
                        closeAndIgnoreErrors(closeable);
                    }
                }
            }
            """;

    @DisplayName("varargs 1")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        MethodInfo closeAndIgnoreErrors = B.findUniqueMethod("closeAndIgnoreErrors", 1);

        assertEquals("X", closeAndIgnoreErrors.analysis()
                .getOrDefault(HiddenContentSelector.HCS_METHOD, HiddenContentSelector.NONE).toString());
        ParameterInfo p0 = closeAndIgnoreErrors.parameters().get(0);
        assertEquals("0", p0.analysis()
                .getOrDefault(HCS_PARAMETER, HiddenContentSelector.NONE).toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            import java.util.Map;
            import java.util.Vector;
            
            public class Function23679178_file2027755 {
            
                public Map<String, Comparable<?>> executeAutoitFile(String fullPath, String workDir, String autoItLocation, int timeout, Object... params) throws Exception {
                    Vector<Object> parameters = new Vector<Object>();
                    if (params.length == 1 && params[0] instanceof Vector) {
                        parameters = (Vector<Object>) params[0];
                    } else {
                        for (Object param : params) {
                            parameters.add(param);
                        }
                    }
                    return executeAutoitFile(fullPath, workDir, autoItLocation, timeout, parameters);
                }
            }
            
            """;

    @DisplayName("varargs 2")
    @Test
    public void test2() {
        TypeInfo B = javaInspector.parse(INPUT2);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT3 = """
            import java.util.Collection;
            
            public class B {
            
                public static <T extends Collection<I>, I> T combine(T target, Collection<I>... collections) {
                    for (Collection<I> collection : collections) {
                        target.addAll(collection);
                    }
                    return target;
                }
            }
            """;

    @DisplayName("varargs 3, catch null in LinkHelper.continueLinkedVariables")
    @Test
    public void test3() {
        TypeInfo B = javaInspector.parse(INPUT3);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }
}
