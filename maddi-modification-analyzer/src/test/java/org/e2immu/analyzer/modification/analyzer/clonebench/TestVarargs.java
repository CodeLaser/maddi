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


public class TestVarargs extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.io.*;
            import java.net.*;
            import java.util.*;
            
            public class Function9706280_file1648039 {
            
                public static void displayInterfaceInformation(NetworkInterface netint) throws SocketException {
                    console.printf("Display name: %s%n", netint.getDisplayName());
                    console.printf("Name: %s%n", netint.getName());
                    Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                    for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                        console.printf("InetAddress: %s%n", inetAddress);
                    }
                    console.printf("Parent: %s%n", netint.getParent());
                    console.printf("Up? %s%n", netint.isUp());
                    console.printf("Loopback? %s%n", netint.isLoopback());
                    console.printf("PointToPoint? %s%n", netint.isPointToPoint());
                    console.printf("Supports multicast? %s%n", netint.isVirtual());
                    console.printf("Virtual? %s%n", netint.isVirtual());
                    console.printf("Hardware address: %s%n", Arrays.toString(netint.getHardwareAddress()));
                    console.printf("MTU: %s%n", netint.getMTU());
                    List<InterfaceAddress> interfaceAddresses = netint.getInterfaceAddresses();
                    for (InterfaceAddress addr : interfaceAddresses) {
                        console.printf("InterfaceAddress: %s%n", addr.getAddress());
                    }
                    console.printf("%n");
                    Enumeration<NetworkInterface> subInterfaces = netint.getSubInterfaces();
                    for (NetworkInterface networkInterface : Collections.list(subInterfaces)) {
                        console.printf("%nSubInterface%n");
                        displayInterfaceInformation(networkInterface);
                    }
                    console.printf("%n");
                }
            
                public static final PrintStream console = System.out;
            }
            """;

    @DisplayName("varargs")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }

    @Language("java")
    private static final String INPUT2 = """
             import java.io.IOException;
             import java.nio.file.Files;
             import java.nio.file.Path;
             import java.util.stream.Stream;
            
             public class Slf4jEffort {
               private Stream<Path> getProjectsStream(Path root) throws IOException {
                 try (Stream<Path> s = Files.walk(root)) {
                   return s.filter(Files::isRegularFile);
                 }
               }
            }
            """;

    @DisplayName("method reference causes index out of bounds")
    @Test
    public void test2() {
        TypeInfo B = javaInspector.parse(INPUT2);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT3 = """
            import java.nio.ByteBuffer;
            import java.util.Formatter;
            
            public class Hex {
              /**
               * Format a buffer to show the buffer in a table with 16 bytes per row, hex values and ascii
               * values are shown.
               *
               * @param data the buffer
               * @return a String with the formatted data
               */
              public static String format(ByteBuffer data) {
                if (data == null) return "";
                ByteBuffer bb = data.duplicate();
                try (Formatter f = new Formatter()) {
                  StringBuilder ascii = new StringBuilder(30);
                  StringBuilder hex = new StringBuilder(30);
                  for (int rover = 0; bb.hasRemaining(); rover += 16) {
                    ascii.setLength(0);
                    hex.setLength(0);
                    for (int g = 0; g < 2 && bb.hasRemaining(); g++) {
                      hex.append(' ');
                      ascii.append("  ");
                      for (int i = 0; i < 8 && bb.hasRemaining(); i++) {
                        byte c = bb.get();
                        hex.append(' ');
                        hex.append(toHex(c));
                        if (c < ' ' || c > 0x7E) c = '.';
                        ascii.append((char) c);
                      }
                    }
                    f.format("0x%04x%-50s%s%n", rover, hex, ascii);
                  }
                  return f.toString();
                }
              }
            
              static final char[] HEX = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
              };
            
              public static String toHex(byte b) {
                char low = HEX[b & 0xF];
                char high = HEX[(b & 0xF0) >> 4];
                return new String(new char[] {high, low});
              }
            }
            """;

    @DisplayName("link to parameter")
    @Test
    public void test3() {
        TypeInfo B = javaInspector.parse(INPUT3);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }

}
