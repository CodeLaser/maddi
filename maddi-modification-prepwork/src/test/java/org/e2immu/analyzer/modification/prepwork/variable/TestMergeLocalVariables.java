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

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMergeLocalVariables extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.io.IOException;
            import java.net.HttpURLConnection;
            import java.net.URI;
            import java.net.URISyntaxException;
            import java.net.URL;
            import java.util.HashMap;
            import java.util.Map;
            import java.util.List;
            class X {
              public static synchronized Map<String, Object> method(String wwwurl)
                    throws IOException, URISyntaxException {
                  Map<String, Object> resultMap = new HashMap<>();
                  URI uri = new URI(wwwurl);
                  URL url = new URL(uri.toASCIIString());
                  HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                  conn.setRequestMethod("GET");
                  for (String key : conn.getHeaderFields().keySet()) {
                      List<String> headerInfo = conn.getHeaderFields().get(key);
                      if (headerInfo.size() > 0) {
                          resultMap.put(key, headerInfo.get(0));
                      }
                  }
                  String contentType = conn.getContentType();
                  if (!(contentType == null)) {
                      return resultMap;
                  }
                  return resultMap;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(B);

        MethodInfo methodInfo = B.findUniqueMethod("method", 1);
        VariableData vd5 = VariableDataImpl.of(methodInfo.methodBody().statements().get(5));
        Assertions.assertEquals("X.method(String):0:wwwurl, conn, key, resultMap, uri, url", vd5.knownVariableNamesToString());

        VariableData vd6 = VariableDataImpl.of(methodInfo.methodBody().statements().get(6));
        Assertions.assertEquals("X.method(String):0:wwwurl, conn, contentType, resultMap, uri, url", vd6.knownVariableNamesToString());
    }

    @Language("java")
    private static final String INPUT2 = """
            public class X {
              private static void mergeSort(Object[] src, Object[] dest, int[] srcMap, int[] dstMap, int low, int high) {
                  for (int i = low; i < high; i++) {
                    for (int j = i; j > low && ((Comparable) dest[j - 1]).compareTo(dest[j]) > 0; j--) {
                      swap(dest, j, j - 1);
                    }
                }
              }
              private static void swap(Object[] x, int a, int b) {
                Object t = x[a];
                x[a] = x[b];
                x[b] = t;
              }
            }
            """;

    @Test
    public void test2() {
        TypeInfo B = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(B);
    }
}
