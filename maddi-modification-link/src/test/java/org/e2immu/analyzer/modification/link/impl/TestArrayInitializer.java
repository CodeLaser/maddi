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

package org.e2immu.analyzer.modification.link.impl;


import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


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
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(B);

        MethodInfo get = B.findUniqueMethod("NthLowestSkill", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodLinkedVariables mlv = tlc.doMethod(get);
        assertEquals("[-] --> -", mlv.toString());
    }


    @Language("java")
    private static final String INPUT2 = """
            import java.io.File;
            
            public class B {
                File base;
              public File[] add(File[] list, File item) {
                if (null == item) return list;
                else if (null == list) return new File[] {item};
                else {
                  File[] copier = new File[] { base, item};
                  return copier;
                }
              }
            }
            """;

    @DisplayName("constructor calls")
    @Test
    public void test2() {
        TypeInfo B = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(B);

        MethodInfo get = B.findUniqueMethod("add", 2);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodLinkedVariables mlv = tlc.doMethod(get);
        assertEquals("[-, -] --> add←0:list,add←$_v,add[0]←1:item,add[0]←this.base,add[1]←1:item",
                mlv.toString());
    }


}
