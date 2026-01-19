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
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;


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

}
