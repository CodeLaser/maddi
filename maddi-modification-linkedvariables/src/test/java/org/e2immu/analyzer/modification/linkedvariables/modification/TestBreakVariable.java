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
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestBreakVariable extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.util.HashMap;
            
            public class B {
              public void removeMode(char mode) {
                int modeIndex = chanModes.indexOf(mode);
                if (modeIndex != -1) {
                  char prefix = getModePrefix(mode);
                  if (prefix == 0) return;
                  chanModes = chanModes.replaceAll(String.valueOf(mode), "");
                  if (type == prefix) {
                    char newType = TYPE_NORMAL;
                    int maxWeight = 0;
                    for (int i = 0; i < chanModes.length(); i++) {
                      char modePrefix = getModePrefix(chanModes.charAt(i));
                      int weight = ((Integer) typesWeight.get(new Character(modePrefix))).intValue();
                      if (weight > maxWeight) {
                        newType = modePrefix;
                        maxWeight = weight;
                      }
                    }
                    type = newType;
                  }
                }
              }
            
              public static final char TYPE_NORMAL = ' ';
              private static final HashMap typesWeight = new HashMap();
              private char type = TYPE_NORMAL;
              private String chanModes = "";
            
              private char getModePrefix(char mode) {
                switch (mode) {
                  case 'q':
                    return '~';
                  case 'a':
                    return '&';
                  case 'o':
                    return '@';
                  case 'h':
                    return '%';
                  case 'v':
                    return '+';
                }
                return 0;
              }
            }
            """;

    @DisplayName("used to fix issue involving non-existent variables in ComputeLinkCompletion")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }
}
