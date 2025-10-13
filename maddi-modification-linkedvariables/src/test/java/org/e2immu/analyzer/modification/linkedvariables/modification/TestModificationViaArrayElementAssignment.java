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

package org.e2immu.analyzer.modification.linkedvariables.modification;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestModificationViaArrayElementAssignment extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            public class X {
              public static void insertionSort(Comparable[] a, int first, int last) {
                for (int unsorted = first + 1; unsorted <= last; unsorted++) {
                  Comparable firstUnsorted = a[unsorted];
                  insertInOrder(firstUnsorted, a, first, unsorted - 1);
                }
              }
            
              private static void insertInOrder(Comparable element, Comparable[] a, int first, int last) {
                if (element.compareTo(a[last]) >= 0) a[last + 1] = element;
                else if (first < last) {
                  a[last + 1] = a[last];
                  insertInOrder(element, a, first, last - 1);
                } else {
                  a[last + 1] = a[last];
                  a[last] = element;
                }
              }
            }
            """;

    @DisplayName("issue in translateHcs")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        MethodInfo insertInOrder = B.findUniqueMethod("insertInOrder", 4);
        assertTrue(insertInOrder.parameters().get(1).isModified());

        MethodInfo insertionSort = B.findUniqueMethod("insertionSort", 3);
        assertTrue(insertionSort.parameters().get(0).isModified());
    }
}
