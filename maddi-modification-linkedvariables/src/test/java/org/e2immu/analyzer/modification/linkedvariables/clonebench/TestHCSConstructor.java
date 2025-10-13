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

package org.e2immu.analyzer.modification.linkedvariables.clonebench;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;


public class TestHCSConstructor extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import org.junit.jupiter.api.Assertions;
            import org.junit.jupiter.api.Test;
            
            import java.util.ArrayList;
            import java.util.Arrays;
            import java.util.HashSet;
            import java.util.Set;
            import java.util.stream.Collectors;
            
            public class ArrayList_Union_NoDup {

                public static ArrayList<String> unionAddAllStreamDistinct(ArrayList<String> list1, ArrayList<String> list2) {
                    ArrayList<String> unionList = new ArrayList<>();
                    unionList.addAll(list1);
                    unionList.addAll(list2);
                    return unionList.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
                }
                @Test
                public void tests() {
                    ArrayList<String> list1 = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "d", "d"));
                    ArrayList<String> list2 = new ArrayList<>(Arrays.asList("b", "d", "e", "f", "f", "d"));
                    ArrayList<String> expected = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f"));
                    Assertions.assertEquals(expected, unionAddAllStreamDistinct(list1, list2));
                }
            }
            
            """;

    @DisplayName("various dependent variable issues")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }
}
