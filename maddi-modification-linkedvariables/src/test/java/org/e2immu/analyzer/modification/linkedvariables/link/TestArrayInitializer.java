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
