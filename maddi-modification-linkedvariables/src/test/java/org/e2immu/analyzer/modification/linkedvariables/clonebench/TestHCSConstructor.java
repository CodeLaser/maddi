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
