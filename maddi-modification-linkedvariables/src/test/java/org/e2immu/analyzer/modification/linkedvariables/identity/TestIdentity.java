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

package org.e2immu.analyzer.modification.linkedvariables.identity;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


public class TestIdentity extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import static java.util.Objects.requireNonNull;
            
            import java.io.PrintWriter;
            import java.io.StringWriter;
            
            public class ExceptionUtils {
              public static String printStackTrace(Throwable throwable) {
                requireNonNull(throwable, "throwable may not be null");
                StringWriter stringWriter = new StringWriter();
                try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
                  throwable.printStackTrace(printWriter);
                }
                return stringWriter.toString();
              }
            }
            """;

    @DisplayName("using @Identity method")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT2 = """
            import java.net.MalformedURLException;
            import java.net.URL;
            
            public class X {
            
                public static URL method(URL jarURL) {
                    String jarfile = jarURL.toString();
                    try {
                        return new URL(jarfile);
                    } catch (MalformedURLException e) {
                        return jarURL;
                    }
                }
            }
            """;

    @DisplayName("should not be @Identity method")
    @Test
    public void test2() {
        TypeInfo B = javaInspector.parse(INPUT2);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        MethodInfo method = B.findUniqueMethod("method", 1);
        VariableData vd = VariableDataImpl.of(method.methodBody().lastStatement());
        VariableInfo viRv = vd.variableInfo(method.fullyQualifiedName());
        assertEquals("D:-, A:[1.0.0, 1.1.0, 1=M]", viRv.assignments().toString());
        assertEquals("Type java.net.URL", viRv.staticValues().toString());

        assertFalse(method.isIdentity());
    }


    @Language("java")
    private static final String INPUT3 = """
            public class X {
            
                public int method(int amb, int num) {
                    int x1 = 0, x2 = 1, y1 = 1, y2 = 0, q, r, x, y;
                    if (amb == 0)
                        return num;
                    while (amb > 0) {
                        q = num / amb;
                        r = num - q * amb;
                        x = x2 - q * x1;
                        y = y2 - q * y1;
                        num = amb;
                        amb = r;
                        x2 = x1;
                        x1 = x;
                        y2 = y1;
                        y1 = y;
                    }
                    return num;
                }
            }
            """;

    @DisplayName("should not be @Identity method, num has been re-assigned")
    @Test
    public void test3() {
        TypeInfo B = javaInspector.parse(INPUT3);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        MethodInfo method = B.findUniqueMethod("method", 2);
        VariableData vd = VariableDataImpl.of(method.methodBody().lastStatement());
        VariableInfo viRv = vd.variableInfo(method.fullyQualifiedName());
        assertEquals("D:-, A:[1.0.0, 3]", viRv.assignments().toString());
        assertEquals("", viRv.staticValues().toString());

        assertFalse(method.isIdentity());
    }


    @Language("java")
    private static final String INPUT4 = """
            import java.io.File;
            
            public class B {
              public static File[] add(File[] list, File item) {
                if (null == item) return list;
                else if (null == list) return new File[] {item};
                else {
                  int len = list.length;
                  File[] copier = new File[len + 1];
                  System.arraycopy(list, 0, copier, 0, len);
                  copier[len] = item;
                  return copier;
                }
              }
            }""";

    @DisplayName("should not be @Identity method")
    @Test
    public void test4() {
        TypeInfo B = javaInspector.parse(INPUT4);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        MethodInfo method = B.findUniqueMethod("add", 2);
        VariableData vd = VariableDataImpl.of(method.methodBody().lastStatement());
        VariableInfo viRv = vd.variableInfo(method.fullyQualifiedName());
        assertEquals("D:-, A:[0.0.0, 0.1.0.0.0, 0.1.0.1.4, 0.1.0=M, 0=M]", viRv.assignments().toString());
        assertEquals("Type java.io.File[] this[len]=item", viRv.staticValues().toString());

        assertFalse(method.isIdentity());
    }


    @Language("java")
    private static final String INPUT5 = """
            public class B {
                private static int method(long lVal) { return (int)lVal; }
            }
            """;

    @DisplayName("should not be @Identity method: long to int cast")
    @Test
    public void test5() {
        TypeInfo B = javaInspector.parse(INPUT5);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        MethodInfo method = B.findUniqueMethod("method", 1);

        assertFalse(method.isIdentity());
    }

}
