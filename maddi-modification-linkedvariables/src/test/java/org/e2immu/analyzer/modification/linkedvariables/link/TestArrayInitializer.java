package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;

import java.util.List;


public class TestArrayInitializer extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """   
            public class B {
              public int NthLowestSkill(int n) {
                int[] skillIds = new int[] {0, 1, 2, 3};
                for (int j = 0; j < 3; j++) {
                  for (int i = 0; i < 3 - j; i++) {
                    if (Skills()[skillIds[i]] > Skills()[skillIds[i + 1]]) {
                      int temp = skillIds[i];
                      skillIds[i] = skillIds[i + 1];
                      skillIds[i + 1] = temp;
                    }
                  }
                }
                return skillIds[n - 1];
              }
            
              private int[] _skills = new int[4];
            
              public int[] Skills() {
                return _skills;
              }
            }
            """;

    @DisplayName("various dependent variable issues")
    @org.junit.jupiter.api.Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }
}
