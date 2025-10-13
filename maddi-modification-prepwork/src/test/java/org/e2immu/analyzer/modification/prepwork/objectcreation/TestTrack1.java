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

package org.e2immu.analyzer.modification.prepwork.objectcreation;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestTrack1 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            import java.util.ArrayList;
            import java.util.LinkedList;
            import java.util.List;
            public class X<T> {
                private static final Logger LOGGER = LoggerFactory.getLogger(X.class);
                public static List<T> makeList() {
                    return new LinkedList<>();
                }
                public static List<T> makeList2() {
                    List<T> list = new LinkedList<>();
                    LOGGER.info("List {}", list);
                    return list;
                }
                public static List<T> makeList3(boolean b) {
                    if(b) {
                        List<T> list = new LinkedList<>();
                        LOGGER.info("List {}", list);
                        return list;
                    }
                    return null;
                }
                public static List<T> makeList4(boolean b) {
                    if(b) {
                        List<T> list = new LinkedList<>();
                        LOGGER.info("List {}", list);
                        return list;
                    }
                    return new ArrayList<>();
                }
            }
            """;

    @DisplayName("basics")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime,
                new PrepAnalyzer.Options.Builder().setTrackObjectCreations(true).build());
        {
            MethodInfo makeList = X.findUniqueMethod("makeList", 0);
            analyzer.doMethod(makeList);
            VariableData vdMethod = VariableDataImpl.of(makeList);
            assertNotNull(vdMethod);
            assertEquals("a.b.X.makeList(), oc:10-16:java.util.LinkedList<T>", vdMethod.knownVariableNamesToString());
        }
        {
            MethodInfo makeList = X.findUniqueMethod("makeList4", 1);
            analyzer.doMethod(makeList);
            VariableData vdMethod = VariableDataImpl.of(makeList);
            assertNotNull(vdMethod);
            assertEquals("""
                    a.b.X.LOGGER, a.b.X.makeList4(boolean), a.b.X.makeList4(boolean):0:b, \
                    oc:27-28:java.util.LinkedList<T>, oc:31-16:java.util.ArrayList<T>\
                    """, vdMethod.knownVariableNamesToString());
        }
    }
}
