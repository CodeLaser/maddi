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

package org.e2immu.analyzer.modification.analyzer.clonebench;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.NullConstant;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class TestVarious extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.io.*;
            import java.util.*;
            import java.util.regex.*;
            
            public class Function9837402_file1210466 {
              public static void copy(Reader r, Writer w) throws IOException {
                char[] buffer = new char[1024 * 16];
                for (; ; ) {
                  int n = r.read(buffer, 0, buffer.length);
                  if (n < 0) break;
                  w.write(buffer, 0, n);
                }
              }
            }
            """;

    @DisplayName("empty block causes issues")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }

    @Language("java")
    private static final String INPUT2 = """
            import java.io.File;
            import java.io.FileInputStream;
            import java.io.IOException;
            import java.nio.ByteBuffer;
            import java.nio.channels.FileChannel;
            
            public class X {
              public void read(File f) throws IOException {
                FileInputStream fis = new FileInputStream(f);
                FileChannel fc = fis.getChannel();
                buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
                int o = 0;
                do {
                  int l = buffer.getInt(o);
                  if (l <= STRING_OFFSET) {
                    throw new IOException("Corrupted file : invalid packet length");
                  }
                  addOffset(o);
                  o += l;
                } while (o < buffer.limit());
              }
            
              private static final int LEVEL_OFFSET = 0 + 4;
              private static final int MILLIS_OFFSET = LEVEL_OFFSET + 2;
              private static final int NUMBER_OFFSET = MILLIS_OFFSET + 8;
              private static final int THREAD_OFFSET = NUMBER_OFFSET + 8;
              private static final int LOGGER_LENGTH_OFFSET = THREAD_OFFSET + 4;
              private static final int MESSAGE_LENGTH_OFFSET = LOGGER_LENGTH_OFFSET + 1;
              private static final int SOURCE_CLASS_LENGTH_OFFSET = MESSAGE_LENGTH_OFFSET + 2;
              private static final int SOURCE_METHOD_LENGTH_OFFSET = SOURCE_CLASS_LENGTH_OFFSET + 1;
              private static final int STRING_OFFSET = SOURCE_METHOD_LENGTH_OFFSET + 1;
              private int maxEntry;
              private ByteBuffer buffer;
              private int[] offsets;
              private int nbrEntry;
              private int start;
            
              protected int addOffset(int offset) {
                if ((maxEntry < 0) && ((nbrEntry + start + 1) >= offsets.length)) {
                  int[] n = new int[offsets.length * 2];
                  System.arraycopy(offsets, 0, n, 0, offsets.length);
                  offsets = n;
                }
                int index = (nbrEntry + start) % offsets.length;
                int previous = offsets[index];
                offsets[index] = offset;
                if (nbrEntry < offsets.length) {
                  nbrEntry++;
                  return -1;
                } else {
                  start = (start + 1) % offsets.length;
                  return previous;
                }
              }
            }
            """;

    @Test
    public void test2() {
        TypeInfo B = javaInspector.parse(INPUT2);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT3 = """
            import java.util.*;
            
            public class X {
              private boolean addToSortedList(Object element) {
                int min = 0;
                int max = size() - 1;
                boolean found = false;
                int currentIndex = 0;
                int compareResult;
                if (max >= 0) {
                  do {
                    currentIndex = (min + max) / 2;
                    compareResult = ((Comparable) myList.get(currentIndex)).compareTo(element);
                    if (compareResult < 0) {
                      min = currentIndex + 1;
                    } else if (compareResult > 0) {
                      max = currentIndex - 1;
                    } else {
                      found = true;
                    }
                  } while ((min <= max) && (found == false));
                }
                if (found == false && size() > 0) {
                  if (((Comparable) element).compareTo(get(currentIndex)) > 0) {
                    myList.add(currentIndex + 1, element);
                  } else {
                    myList.add(currentIndex, element);
                  }
                  return true;
                } else if (found == false) {
                  myList.add(currentIndex, element);
                  return true;
                } else {
                  System.out.println("Element found in vector already.");
                  return false;
                }
              }
            
              private List myList;
            
              /**
               * Get the element at specified index
               *
               * @param index element index
               * @return element from the index
               */
              public Object get(int index) {
                return myList.get(index);
              }
            
              /**
               * Return the number of elements in the list.
               *
               * @return number of elements
               */
              public int size() {
                return myList.size();
              }
            }
            """;

    @Test
    public void test3() {
        TypeInfo B = javaInspector.parse(INPUT3);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.nio.charset.StandardCharsets;
            public class X {
                final static char [] HEX_CHAR_TABLE = { '0', 'a'};
                public String toHexString(byte[] bytes) {
                  byte[] hex = new byte[2 * bytes.length];
                  int index = 0;
                  for (byte b : bytes) {
                    int v = b & 0xFF;
                    hex[index++] = HEX_CHAR_TABLE[v >>> 4];
                    hex[index++] = HEX_CHAR_TABLE[v & 0xF];
                  }
                  return new String(hex, StandardCharsets.US_ASCII);
                }
            }
            """;

    @DisplayName("parameterized type issue in compute linked variables, for-loop")
    @Test
    public void test4() {
        TypeInfo B = javaInspector.parse(INPUT4);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT5 = """
            import java.io.IOException;
            import java.io.InputStream;
            import java.io.InputStreamReader;
            import java.net.URL;
            
            public class Function1277812_file269662 {
              private static String getTextFromURLStream(
                  URL url, int startOffset, int endOffset, String charset) throws IOException {
                if (url == null) return null;
                if (startOffset > endOffset) throw new IOException();
                InputStream fis = url.openStream();
                InputStreamReader fisreader =
                    charset == null ? new InputStreamReader(fis) : new InputStreamReader(fis, charset);
                int len = endOffset - startOffset;
                int bytesAlreadyRead = 0;
                char[] buffer = new char[len];
                int bytesToSkip = startOffset;
                long bytesSkipped = 0;
                do {
                  bytesSkipped = fisreader.skip(bytesToSkip);
                  bytesToSkip -= bytesSkipped;
                } while ((bytesToSkip > 0) && (bytesSkipped > 0));
                do {
                  int count = fisreader.read(buffer, bytesAlreadyRead, len - bytesAlreadyRead);
                  if (count < 0) {
                    break;
                  }
                  bytesAlreadyRead += count;
                } while (bytesAlreadyRead < len);
                fisreader.close();
                return new String(buffer);
              }
            }
            """;

    @DisplayName("not part of primary")
    @Test
    public void test5() {
        TypeInfo B = javaInspector.parse(INPUT5);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT6 = """
            import java.util.Map;
            import java.util.function.BiFunction;
            public class X {
            public String alternativeComputeIfPresent2(Map<String, String> map,
                        String key,
                        BiFunction<? super String, ? super String, ? extends String> mappingFunction) {
                    return map.entrySet().stream()
                            .filter(entry -> entry.getKey().equals(key))
                            .map(entry -> {
                                String newValue = mappingFunction.apply(entry.getKey(), entry.getValue());
                                if (newValue != null) {
                                    entry.setValue(newValue);
                                    return newValue;
                                } else {
                                    map.remove(entry.getKey());
                                    return null;
                                }
                            })
                            .findFirst()
                            .orElse(null);
                }
            }
            """;

    @DisplayName("makeSub")
    @Test
    public void test6() {
        TypeInfo B = javaInspector.parse(INPUT6);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT7 = """
            import java.util.Random;
            public class X {
            public static boolean[][] randomGraph(int n, double p, Random random) {
                if (n <= 0) return null;
                boolean[][] g = new boolean[n][n];
                if (p > 1.0) p = 1.0;
                if (p < 0.0) p = 0.0;
                for (int i = 0; i < n; i++) {
                  g[i][i] = false;
                  for (int j = i + 1; j < n; j++) {
                    g[i][j] = (random.nextDouble() <= p);
                    g[j][i] = g[i][j];
                  }
                }
                return g;
              }
            }
            """;

    @DisplayName("don't make subs of the null constant")
    @Test
    public void test7() {
        TypeInfo B = javaInspector.parse(INPUT7);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }

    @Language("java")
    private static final String INPUT8 = """
            import java.lang.reflect.Field;
            import java.lang.reflect.Method;
            import java.util.HashMap;
            import java.util.Map;
            
            public class X {
            
                public static Map<String, Class> toNameClassMap(Map<String, Object> items) {
                    Map<String, Class> res = new HashMap<String, Class>();
                    if (items == null || items.size() == 0) {
                        return res;
                    }
                    for (String key : items.keySet()) {
                        Object o = items.get(key);
                        if (o instanceof Method) {
                            Method m = (Method) o;
                            if (m.getName().startsWith("get") || m.getName().startsWith("is")) {
                                res.put(key, m.getReturnType());
                            } else if (m.getName().startsWith("set")) {
                                res.put(key, m.getParameterTypes()[0]);
                            } else {
                                res.put(key, o.getClass());
                            }
                        } else if (o instanceof Field) {
                            res.put(key, ((Field) o).getType());
                        } else {
                            res.put(key, o.getClass());
                        }
                    }
                    return res;
                }
            }
            """;

    @DisplayName("array access with method array expression")
    @Test
    public void test8() {
        TypeInfo B = javaInspector.parse(INPUT8);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT9 = """
            import java.util.Map;
            import java.util.TreeMap;
            
            public class X {
            
                public Map<String, String> arrayToMap(Object[] envArray) {
                    Map<String, String> env = new TreeMap<>();
                    for (Object x : envArray) {
                        Object[] o = (Object[]) x;
                        if (o.length != 2) throw new RuntimeException();
                        env.put((String) o[0], (String) o[1]);
                    }
                    return env;
                }
            }
            """;

    @DisplayName("subs of index type cannot be made unless the arrays are available")
    @Test
    public void test9() {
        TypeInfo B = javaInspector.parse(INPUT9);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }

    @Language("java")
    private static final String INPUT10 = """
            import java.util.HashMap;
            import java.util.Map;
            
            public class X {
            
                public Map getInput() {
                    Map m = new HashMap();
                    for (String k : input.keySet()) {
                        Object value = input.get(k);
                        if (value != null) {
                            if (((Object[]) value).length == 1) {
                                value = ((Object[]) value)[0];
                            } else if (((Object[]) value).length == 0) {
                                value = null;
                            }
                        }
                        m.put(k, value);
                    }
                    return m;
                }
            
                public Map<String, Object> input = new HashMap<String, Object>();
            }
            """;

    @DisplayName("more restrictions on subs of index type")
    @Test
    public void test10() {
        TypeInfo B = javaInspector.parse(INPUT10);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT11 = """
            public class X {
            
                public static void logNormalize(double[] x) {
                    double alpha = 0.0;
                    for (double v : x) alpha += Math.exp(v);
                    addAll(x, -Math.log(alpha));
                }
            
                public static void addAll(double[] x, double val) {
                    for (int i = 0; i < x.length; i++) x[i] += val;
                }
            }
            """;

    @DisplayName("more restrictions on subs of index type")
    @Test
    public void test11() {
        TypeInfo B = javaInspector.parse(INPUT11);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT12 = """
            //onlyjava
            import java.io.IOException;
            import java.lang.reflect.InvocationTargetException;
            import java.lang.reflect.Method;
            import java.net.URL;
            
            class X {
                public static void openURL(URL url) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException, InterruptedException {
                    String osName = System.getProperty("os.name");
                    if (osName.startsWith("Mac OS")) {
                        Class fileMgr = Class.forName("com.apple.eio.FileManager");
                        Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
                        openURL.invoke(null, new Object[] { url.toString() });
                    } else if (osName.startsWith("Windows")) {
                        String cmdLine = "rundll32 url.dll,FileProtocolHandler " + url.toString();
                        Process exec = Runtime.getRuntime().exec(cmdLine);
                        exec.waitFor();
                    } else {
                        String[] browsers = { "firefox", "opera" };
                        String browser = null;
                        for (int count = 0; count < browsers.length && browser == null; count++) {
                            if (Runtime.getRuntime().exec(new String[] { "which", browsers[count] }).waitFor() == 0) {
                                browser = browsers[count];
                            }
                        }
                        if (browser == null) {
                            throw new IllegalStateException("Could not find web browser");
                        } else {
                            Runtime.getRuntime().exec(new String[] { browser, url.toString() });
                        }
                    }
                }
            }
            """;

    @DisplayName("bugfix for array initializer directly in local variable creation")
    @Test
    public void test12() {
        TypeInfo B = javaInspector.parse(INPUT12);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT13 = """
            class X {
                private static int[] addIndex(final int[] indexes, final int newIndex, final boolean excludeFirstIndex) {
                    int[] newIndices = null;
                    if (excludeFirstIndex) {
                        newIndices = new int[indexes.length];
                        for (int i = 0, z = indexes.length - 1; i < z; i++) {
                            newIndices[i] = indexes[i + 1];
                        }
                    } else {
                        newIndices = new int[indexes.length + 1];
                        for (int i = 0, z = indexes.length; i < z; i++) {
                            newIndices[i] = indexes[i];
                        }
                    }
                    newIndices[newIndices.length - 1] = newIndex;
                    return newIndices;
                }
            }
            """;

    @DisplayName("omit null constant from constants to be replaced")
    @Test
    public void test13() {
        TypeInfo B = javaInspector.parse(INPUT13);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }

    @Language("java")
    private static final String INPUT14 = """
            public class X {
              public void invDctNxN(double[][] dcts, int[][] pixels) {
                int u = 0;
                int v = 0;
                double two_over_sqrtncolsnrows = 2.0 / Math.sqrt((double) N * M);
                double[][] tmp = null;
                tmp = new double[N][N];
                for (u = 0; u < N; u++) {
                  for (v = 0; v < M; v++) {
                    tmp[u][v] = dcts[u][v];
                  }
                }
                for (u = 0; u <= M - 1; u++) {
                  invFctNoScale(tmp[u]);
                }
                for (v = 0; v <= N - 1; v++) {
                  for (u = 0; u <= M - 1; u++) {
                    nxnTmp[u] = tmp[u][v];
                  }
                  invFctNoScale(nxnTmp);
                  for (u = 0; u <= M - 1; u++) {
                    tmp[u][v] = nxnTmp[u] * two_over_sqrtncolsnrows;
                  }
                }
                for (u = 0; u < N; u++) {
                  for (v = 0; v < M; v++) {
                    pixels[u][v] = pixelRange((int) (tmp[u][v] + 128.5));
                  }
                }
                tmp = null;
              }
            
              /** Constant for Inverse of Square Root of 2 */
              private static final double INVROOT2 = 0.7071067814;
            
              private double[] nxnTmp = null;
              private double[] nxnCosTable = null;
              private int nxnLog2N = 0;
              private int N = 0;
              private int M = 0;
            
              private int pixelRange(int p) {
                return ((p > 255) ? 255 : (p < 0) ? 0 : p);
              }
            
              private void bitrev(double[] f, int len, int startIdx) {
                int i;
                int j;
                int m;
                double tmp;
                if (len <= 2) return;
                j = 1;
                for (i = 1; i <= len; i++) {
                  if (i < j) {
                    tmp = f[startIdx + j - 1];
                    f[startIdx + j - 1] = f[startIdx + i - 1];
                    f[startIdx + i - 1] = tmp;
                  }
                  m = len >> 1;
                  while (j > m) {
                    j = j - m;
                    m = (m + 1) >> 1;
                  }
                  j = j + m;
                }
              }
            
              private void invSums(double[] f) {
                int stepsize;
                int stage;
                int curptr;
                int nthreads;
                int thread;
                int step;
                int nsteps;
                for (stage = 1; stage <= nxnLog2N - 1; stage++) {
                  nthreads = 1 << (stage - 1);
                  stepsize = nthreads << 1;
                  nsteps = (1 << (nxnLog2N - stage)) - 1;
                  for (thread = 1; thread <= nthreads; thread++) {
                    curptr = N - thread;
                    for (step = 1; step <= nsteps; step++) {
                      f[curptr] += f[curptr - stepsize];
                      curptr -= stepsize;
                    }
                  }
                }
              }
            
              private void unscramble(double[] f, int len) {
                int i;
                int ii1;
                int ii2;
                double tmp;
                ii1 = len - 1;
                ii2 = len >> 1;
                for (i = 0; i < (len >> 2); i++) {
                  tmp = f[ii1];
                  f[ii1] = f[ii2];
                  f[ii2] = tmp;
                  ii1--;
                  ii2++;
                }
                bitrev(f, len >> 1, 0);
                bitrev(f, len >> 1, len >> 1);
                bitrev(f, len, 0);
              }
            
              private void invButterflies(double[] f) {
                int stage;
                int ii1;
                int ii2;
                int butterfly;
                int ngroups;
                int group;
                int wingspan;
                int increment;
                int baseptr;
                double Cfac;
                double T;
                for (stage = 1; stage <= nxnLog2N; stage++) {
                  ngroups = 1 << (nxnLog2N - stage);
                  wingspan = 1 << (stage - 1);
                  increment = wingspan << 1;
                  for (butterfly = 1; butterfly <= wingspan; butterfly++) {
                    Cfac = nxnCosTable[wingspan + butterfly - 1];
                    baseptr = 0;
                    for (group = 1; group <= ngroups; group++) {
                      ii1 = baseptr + butterfly - 1;
                      ii2 = ii1 + wingspan;
                      T = Cfac * f[ii2];
                      f[ii2] = f[ii1] - T;
                      f[ii1] = f[ii1] + T;
                      baseptr += increment;
                    }
                  }
                }
              }
            
              private void invFctNoScale(double[] f) {
                f[0] *= INVROOT2;
                invSums(f);
                bitrev(f, N, 0);
                invButterflies(f);
                unscramble(f, N);
              }
            }
            """;

    @DisplayName("illegal links to constants")
    @Test
    public void test14() {
        TypeInfo B = javaInspector.parse(INPUT14);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        MethodInfo methodInfo = B.findUniqueMethod("invDctNxN", 2);

        VariableData vd5 = VariableDataImpl.of(methodInfo.methodBody().statements().get(5));
        VariableInfo tmp5 = vd5.variableInfo("tmp");
        assertEquals("""
                tmp←$_ce57,tmp[u][v]←0:dcts[u][v],tmp[u][v]∈0:dcts[u],tmp[u][v]∈tmp[u],tmp[u][v]∈$_ce57,\
                tmp[u][v]∈∈0:dcts,tmp[u]∋0:dcts[u][v],tmp[u]∈$_ce57,tmp[u]~0:dcts[u],tmp∋∋0:dcts[u][v],tmp~0:dcts[u]\
                """, tmp5.linkedVariables().toString());
        Link l0 = tmp5.linkedVariables().link(0);
        assertEquals("tmp←$_ce57", l0.toString());
        if (l0.to() instanceof MarkerVariable mv) {
            assertInstanceOf(NullConstant.class, mv.assignmentExpression());
        } else fail();

        VariableData vd7 = VariableDataImpl.of(methodInfo.methodBody().statements().get(7));
        VariableInfo tmp7 = vd7.variableInfo("tmp");
        assertEquals("""
                tmp[u][0]←0:dcts[u][0],tmp[u][0]∈0:dcts[u],tmp[u][0]∈tmp[u],tmp[u][0]∈$_ce57,tmp[u][0]∈∈0:dcts,\
                tmp[u]∋0:dcts[u][0],tmp[u]∈$_ce57,tmp[u]~0:dcts[u],tmp[u]~0:dcts,tmp←$_ce57,tmp∋∋0:dcts[u][0],\
                tmp~0:dcts[u],tmp~0:dcts,tmp[u][0]∩this.nxnTmp[u],tmp[u][0]∩tmp[u][v],tmp[u][v]→this.nxnTmp[u],\
                tmp[u][v]∈tmp[u],tmp[u][v]∈$_ce57,tmp[u][v]∈this.nxnTmp,tmp[u][v]∩0:dcts[u],tmp[u][v]∩0:dcts[u][0],\
                tmp[u]∋this.nxnTmp[u],tmp[u]~this.nxnTmp,tmp∋∋this.nxnTmp[u],tmp~this.nxnTmp\
                """, tmp7.linkedVariables().toString());

        Statement s8 = methodInfo.methodBody().statements().get(8);
        VariableData vd8 = VariableDataImpl.of(s8);
        VariableInfo tmp8 = vd8.variableInfo("tmp");
        assertEquals("Type double[][]", tmp8.variable().parameterizedType().toString());
        assertEquals("""
                tmp[u][0]←0:dcts[u][0],tmp[u][0]∈0:dcts[u],tmp[u][0]∈tmp[u],tmp[u][0]∈$_ce57,tmp[u][0]∈∈0:dcts,\
                tmp[u][0]∩this.nxnTmp[u],tmp[u][0]∩tmp[u][v],tmp[u][v]→this.nxnTmp[u],tmp[u][v]∈tmp[u],\
                tmp[u][v]∈$_ce57,tmp[u][v]∈this.nxnTmp,tmp[u][v]∩0:dcts[u],tmp[u][v]∩0:dcts[u][0],\
                tmp[u]∋0:dcts[u][0],tmp[u]∋this.nxnTmp[u],tmp[u]∈$_ce57,tmp[u]~0:dcts[u],tmp[u]~0:dcts,\
                tmp[u]~this.nxnTmp,tmp←$_ce57,tmp∋∋0:dcts[u][0],tmp∋∋this.nxnTmp[u],tmp~0:dcts[u],tmp~0:dcts,\
                tmp~this.nxnTmp\
                """, tmp8.linkedVariables().toString());
    }

}
