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


public class TestNullInHCSFindAll extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            //onlyjava
            import java.io.File;
            import java.util.Vector;
            
            public class Function1152888_file1952792 {
            
                public File[] makeClassPathArray() {
                    String classPath;
                    classPath = System.getProperty("java.boot.class.path");
                    int instanceOfSep = -1;
                    int nextInstance = classPath.indexOf(File.pathSeparatorChar, instanceOfSep + 1);
                    Vector elms = new Vector();
                    while (nextInstance != -1) {
                        elms.add(new File(classPath.substring(instanceOfSep + 1, nextInstance)));
                        instanceOfSep = nextInstance;
                        nextInstance = classPath.indexOf(File.pathSeparatorChar, instanceOfSep + 1);
                    }
                    elms.add(new File(classPath.substring(instanceOfSep + 1)));
                    File[] result = new File[elms.size()];
                    elms.copyInto(result);
                    return result;
                }
            }
            """;

    @DisplayName("@Identity method")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }

}
