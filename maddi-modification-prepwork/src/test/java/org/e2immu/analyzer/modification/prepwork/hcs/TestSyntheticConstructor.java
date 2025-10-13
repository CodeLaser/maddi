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

package org.e2immu.analyzer.modification.prepwork.hcs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.MethodAnalyzer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestSyntheticConstructor extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            public class X {
            
                public static void systemCall(final String str) {
                    new Thread(new Runnable() {
            
                        public void run() {
                            try {
                                Runtime.getRuntime().exec(str);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
            """;

    @DisplayName("hcs of synthetic anonymous type constructor")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = new PrepAnalyzer(runtime).doPrimaryType(B);
        assertEquals("X.$0.<init>(),X.$0.run(),X.<init>(),X.$0,X.systemCall(String),X",
                ao.stream().map(Info::fullyQualifiedName).collect(Collectors.joining(",")));
        MethodInfo methodInfo = (MethodInfo) ao.getFirst();
        assertTrue(methodInfo.analysis().haveAnalyzedValueFor(HiddenContentSelector.HCS_METHOD));
    }


    @Language("java")
    private static final String INPUT2 = """
            import java.io.IOException;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Map;
            import java.util.Map.Entry;
            import java.util.TreeMap;
            import java.util.jar.JarFile;
            
            public class B {
              public static void main(String[] args) throws IOException {
                Map<String, List<String>> packages = new TreeMap<>();
                try (JarFile jarFile = new JarFile(args[0])) {
                  jarFile.stream()
                      .forEach(
                          entry -> {
                            String name = entry.getName();
                            if (name.endsWith(".class")) {
                              int lastIndexOf = name.lastIndexOf('/');
                              if (lastIndexOf > 0) {
                                String pkg = name.substring(0, lastIndexOf).replace('/', '.');
                                packages.computeIfAbsent(pkg, x -> new ArrayList<>()).add(name);
                              }
                            }
                          });
                }
                for (Entry<String, List<String>> pkg : packages.entrySet()) {
                  if (pkg.getValue().isEmpty()) {
                    continue;
                  }
                  System.out.println("<exportedPackage>" + pkg.getKey() + "</exportedPackage>");
                }
              }
            }
            """;

    @DisplayName("overwriting $0 HC")
    @Test
    public void test2() {
        ((Logger) LoggerFactory.getLogger(PrepAnalyzer.class)).setLevel(Level.DEBUG);

        TypeInfo B = javaInspector.parse(INPUT2);
        List<Info> ao = new PrepAnalyzer(runtime).doPrimaryType(B);
        assertEquals(
                // new path since a lambda only causes a variable context rather than a type context
                // see ParseLambdaExpression, line 67
                //"B.$0.$0.apply(String),B.<init>(),B.$0.accept(java.util.jar.JarEntry),B.$0.$0,B.main(String[]),B,B.$0",
                "B.$1.apply(String),B.<init>(),B.$1,B.$0.accept(java.util.jar.JarEntry),B.$0,B.main(String[]),B",
                ao.stream().map(Info::fullyQualifiedName).collect(Collectors.joining(",")));
        MethodInfo methodInfo = (MethodInfo) ao.getFirst();
        assertTrue(methodInfo.analysis().haveAnalyzedValueFor(HiddenContentSelector.HCS_METHOD));
    }

}
