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
