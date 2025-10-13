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
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.*;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestLinkFields extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            import java.util.ArrayList;
            class X {
                private final List<String> list1 = new ArrayList<>();
                private final List<String> list2 = new ArrayList<>();
            }
            """;

    @Disabled("NYI")
    @DisplayName("basics of field linking")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        FieldInfo list1 = X.getFieldByName("list1", true);
        assertTrue(list1.isPropertyFinal());
        assertEquals("", list1.analysis()
                .getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_FIELD, LinkedVariablesImpl.class).toString());
        FieldInfo list2 = X.getFieldByName("list2", true);
        assertEquals("", list2.analysis()
                .getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_FIELD, LinkedVariablesImpl.class).toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            import java.util.ArrayList;
            class X {
                private final List<String> list1 = new ArrayList<>();
                private final List<String> list2 = new ArrayList<>();
            }
            """;

    @Disabled("NYI")
    @DisplayName("obviously linked fields")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        FieldInfo list1 = X.getFieldByName("list1", true);
        assertTrue(list1.isPropertyFinal());
        assertEquals("", list1.analysis()
                .getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_FIELD, LinkedVariablesImpl.class).toString());
        FieldInfo list2 = X.getFieldByName("list2", true);
        assertEquals("", list2.analysis()
                .getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_FIELD, LinkedVariablesImpl.class).toString());
    }
}
