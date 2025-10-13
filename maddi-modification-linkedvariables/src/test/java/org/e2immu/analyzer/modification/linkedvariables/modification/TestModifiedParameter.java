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
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestModifiedParameter extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.io.File;
            import java.util.List;
            
            class Function18024_file101780 {
                private int findSources(File dir, List<String> args) {
                    File[] files = dir.listFiles();
                    if (files == null || files.length == 0) return 0;
                    int found = 0;
                    for (int i = 0; i < files.length; i++) {
                        File file = files[i];
                        if (file.isDirectory()) {
                            found += findSources(file, args);
                        } else if (file.getName().endsWith(".java")) {
                            args.add(file.toString());
                            found++;
                        }
                    }
                    return found;
                }
            }
            """;

    @DisplayName("used to fix a null-pointer exception")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        MethodInfo findSources = B.findUniqueMethod("findSources", 2);
        ParameterInfo p1 = findSources.parameters().get(1);
        assertTrue(p1.isModified());
    }
}
