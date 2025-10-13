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

package org.e2immu.analyzer.modification.prepwork.escape;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAlwaysEscapes extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                static int method1(String in) {
                    int i = in.length();
                    return i;
                }
                static int method2(String in) {
                    if(in.isEmpty()) {
                        return 1;
                    } else {
                        throw new UnsupportedOperationException();
                    }
                }
                static int method3(String in) {
                    if(in.isEmpty()) {
                        return 1;
                    } else {
                        System.out.println(in);
                    }
                }
            }
            """;

    @DisplayName("basics")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        {
            MethodInfo method = X.findUniqueMethod("method1", 1);
            ComputeAlwaysEscapes.go(method);
            Statement s0 = method.methodBody().statements().get(0);
            assertFalse(s0.alwaysEscapes());
            Statement s1 = method.methodBody().statements().get(1);
            assertTrue(s1.alwaysEscapes());
        }
        {
            MethodInfo method = X.findUniqueMethod("method2", 1);
            ComputeAlwaysEscapes.go(method);
            Statement s0 = method.methodBody().statements().get(0);
            Statement s000 = s0.block().statements().get(0);
            assertTrue(s000.alwaysEscapes());
            Statement s010 = ((IfElseStatement) s0).elseBlock().statements().get(0);
            assertTrue(s010.alwaysEscapes());
            assertTrue(s0.alwaysEscapes());
        }
        {
            MethodInfo method = X.findUniqueMethod("method3", 1);
            ComputeAlwaysEscapes.go(method);
            Statement s0 = method.methodBody().statements().get(0);
            Statement s000 = s0.block().statements().get(0);
            assertTrue(s000.alwaysEscapes());
            Statement s010 = ((IfElseStatement) s0).elseBlock().statements().get(0);
            assertFalse(s010.alwaysEscapes());
            assertFalse(s0.alwaysEscapes());
        }
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            abstract class X {
                static int method1(String in) {
                    for(int i=0; ; i++) { }
                }
                static int method2(String in) {
                    for(int i=0; ; i++) { break; }
                }
                static int method3(String in) {
                   for(int i=0; ; i++) { System.out.println("?"); return; }
                }
            
                abstract int read();
                abstract void write(int i);
                void method4() {
                    while (true) {
                        int read = read();
                        if (read == -1) {
                            break;
                        }
                        write(read);
                    }
                }
                void method5() {
                    while (true) {
                        int read = read();
                        if (read == -1) {
                            return;
                        }
                        write(read);
                    }
                }
            }
            """;

    @DisplayName("loops with condition 'true'")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        {
            MethodInfo method = X.findUniqueMethod("method1", 1);
            ComputeAlwaysEscapes.go(method);
            Statement s0 = method.methodBody().statements().get(0);
            assertTrue(s0.alwaysEscapes());
        }
        {
            MethodInfo method = X.findUniqueMethod("method2", 1);
            ComputeAlwaysEscapes.go(method);
            Statement s0 = method.methodBody().statements().get(0);
            Statement s000 = s0.block().statements().get(0);
            assertFalse(s000.alwaysEscapes());
            assertFalse(s0.alwaysEscapes());
        }
        {
            MethodInfo method = X.findUniqueMethod("method3", 1);
            ComputeAlwaysEscapes.go(method);
            Statement s0 = method.methodBody().statements().get(0);
            assertTrue(s0.alwaysEscapes());
        }
        {
            MethodInfo method = X.findUniqueMethod("method4", 0);
            ComputeAlwaysEscapes.go(method);
            Statement s0 = method.methodBody().statements().get(0);
            Statement s001 = s0.block().statements().get(1);
            assertFalse(s001.alwaysEscapes());
            assertFalse(s0.alwaysEscapes());
        }
        {
            MethodInfo method = X.findUniqueMethod("method5", 0);
            ComputeAlwaysEscapes.go(method);
            Statement s0 = method.methodBody().statements().get(0);
            assertTrue(s0.alwaysEscapes());
        }
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            abstract class X {
                final Object object = new Object();
                static String method1(String in) {
                   synchronized (object) {
                        System.out.println(in);
                        return in;
                    }
                }
            }
            """;

    @DisplayName("synchronized with return")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        {
            MethodInfo method = X.findUniqueMethod("method1", 1);
            ComputeAlwaysEscapes.go(method);
            Statement s0 = method.methodBody().statements().get(0);
            assertTrue(s0.alwaysEscapes());
        }
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            abstract class X {
                final Object object = new Object();
                static String method1(String in) {
                   try {
                        if(in.isEmpty()) System.out.println("empty");
                        return in.toLowerCase();
                   } finally {
                        System.out.println(object);
                   }
                }
            }
            """;

    @DisplayName("try-finally")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        {
            MethodInfo method = X.findUniqueMethod("method1", 1);
            ComputeAlwaysEscapes.go(method);
            Statement s0 = method.methodBody().statements().get(0);
            assertTrue(s0.alwaysEscapes());
        }
    }


    @Language("java")
    private static final String INPUT5 = """
            import java.util.Date;
            import java.util.Random;
            import java.util.WeakHashMap;
            
            public class X {
              public static int method1() {
                do {
                  int random_num = randomNumberGenerator.nextInt();
                  if (random_num <= 0) continue;
                  synchronized (aceThreadList) {
                    if (aceThreadList.get(random_num) == null) {
                      return random_num;
                    }
                  }
                } while (true);
              }
            
              private static WeakHashMap aceThreadList = new WeakHashMap();
              private static Random randomNumberGenerator = new Random((new Date()).getTime());
            }
            """;

    @DisplayName("synchronized in infinite loop")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        {
            MethodInfo method = X.findUniqueMethod("method1", 0);
            ComputeAlwaysEscapes.go(method);
            Statement s0 = method.methodBody().statements().get(0);
            assertTrue(s0.alwaysEscapes());
            Statement s001 = s0.block().statements().get(1);
            assertFalse(s001.alwaysEscapes());
            Statement s002 = s0.block().statements().get(2);
            assertFalse(s002.alwaysEscapes());
        }
    }


    @Language("java")
    private static final String INPUT6 = """
            import java.io.File;
            import java.io.FileFilter;
            import java.util.ArrayList;
            import java.util.Arrays;
            import java.util.List;
            
            public class X {
              public static List<File> listFiles(final File file, final FileFilter fileFilter) {
                final List<File> list = new ArrayList<File>();
                final File[] files = file.listFiles(fileFilter);
                list.addAll(Arrays.asList(files));
                if (files.length != 0) {
                  for (final File f : files) {
                    list.addAll(listFiles(f, fileFilter));
                    return list;
                  }
                } else return list;
                return list;
              }
            }
            """;

    @DisplayName("return in for-each")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        {
            MethodInfo method = X.findUniqueMethod("listFiles", 2);
            ComputeAlwaysEscapes.go(method);
            Statement s3 = method.methodBody().statements().get(3);
            Statement s300 = s3.block().statements().get(0);
            assertFalse(s300.alwaysEscapes());
            assertFalse(s3.alwaysEscapes());
        }
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.io.IOException;
            import java.net.HttpURLConnection;
            import java.net.MalformedURLException;
            import java.net.URI;
            import java.net.URL;
            public class X {
                public int getResponseCode(URI uri) {
                    int response = -1;
                    try {
                        URL url = uri.toURL();
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        response = connection.getResponseCode();
                    } catch (MalformedURLException m) {
                        throw new MalformedURLException("URL not correct");
                    } catch (IOException e) {
                        throw new IOException("can open connection");
                    } finally {
                        return response;
                    }
                }
            }
            """;

    @DisplayName("return in finally block")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        MethodInfo method = X.findUniqueMethod("getResponseCode", 1);
        ComputeAlwaysEscapes.go(method);
        TryStatement s1 = (TryStatement) method.methodBody().statements().get(1);
        assertTrue(s1.finallyBlock().alwaysEscapes());
    }

}
