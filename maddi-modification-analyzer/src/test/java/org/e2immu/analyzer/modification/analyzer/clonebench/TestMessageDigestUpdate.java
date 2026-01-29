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


public class TestMessageDigestUpdate extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.security.MessageDigest;
            import java.security.NoSuchAlgorithmException;
            
            public class Function18985881_file1268519 {
              public static final String crypt(String password, String salt) throws NoSuchAlgorithmException {
                String magic = "$1$";
                byte[] finalState;
                MessageDigest ctx, ctx1;
                long l;
                if (salt.startsWith(magic)) {
                  salt = salt.substring(magic.length());
                }
                if (salt.indexOf('$') != -1) {
                  salt = salt.substring(0, salt.indexOf('$'));
                }
                if (salt.length() > 8) {
                  salt = salt.substring(0, 8);
                }
                ctx = MessageDigest.getInstance("MD5");
                ctx.update(password.getBytes());
                ctx.update(magic.getBytes());
                ctx.update(salt.getBytes());
                ctx1 = MessageDigest.getInstance("MD5");
                ctx1.update(password.getBytes());
                ctx1.update(salt.getBytes());
                ctx1.update(password.getBytes());
                finalState = ctx1.digest();
                for (int pl = password.length(); pl > 0; pl -= 16) {
                  for (int i = 0; i < (pl > 16 ? 16 : pl); i++) ctx.update(finalState[i]);
                }
                clearbits(finalState);
                for (int i = password.length(); i != 0; i >>>= 1) {
                  if ((i & 1) != 0) {
                    ctx.update(finalState[0]);
                  } else {
                    ctx.update(password.getBytes()[0]);
                  }
                }
                finalState = ctx.digest();
                for (int i = 0; i < 1000; i++) {
                  ctx1 = MessageDigest.getInstance("MD5");
                  if ((i & 1) != 0) {
                    ctx1.update(password.getBytes());
                  } else {
                    for (int c = 0; c < 16; c++) ctx1.update(finalState[c]);
                  }
                  if ((i % 3) != 0) {
                    ctx1.update(salt.getBytes());
                  }
                  if ((i % 7) != 0) {
                    ctx1.update(password.getBytes());
                  }
                  if ((i & 1) != 0) {
                    for (int c = 0; c < 16; c++) ctx1.update(finalState[c]);
                  } else {
                    ctx1.update(password.getBytes());
                  }
                  finalState = ctx1.digest();
                }
                StringBuffer result = new StringBuffer();
                result.append(magic);
                result.append(salt);
                result.append("$");
                l = (bytes2u(finalState[0]) << 16) | (bytes2u(finalState[6]) << 8) | bytes2u(finalState[12]);
                result.append(to64(l, 4));
                l = (bytes2u(finalState[1]) << 16) | (bytes2u(finalState[7]) << 8) | bytes2u(finalState[13]);
                result.append(to64(l, 4));
                l = (bytes2u(finalState[2]) << 16) | (bytes2u(finalState[8]) << 8) | bytes2u(finalState[14]);
                result.append(to64(l, 4));
                l = (bytes2u(finalState[3]) << 16) | (bytes2u(finalState[9]) << 8) | bytes2u(finalState[15]);
                result.append(to64(l, 4));
                l = (bytes2u(finalState[4]) << 16) | (bytes2u(finalState[10]) << 8) | bytes2u(finalState[5]);
                result.append(to64(l, 4));
                l = bytes2u(finalState[11]);
                result.append(to64(l, 2));
                clearbits(finalState);
                return result.toString();
              }
            
              private static final String itoa64 =
                  "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            
              private static final String to64(long v, int size) {
                StringBuffer result = new StringBuffer();
                while (--size >= 0) {
                  result.append(itoa64.charAt((int) (v & 0x3f)));
                  v >>>= 6;
                }
                return result.toString();
              }
            
              private static final void clearbits(byte[] bits) {
                for (int i = 0; i < bits.length; i++) {
                  bits[i] = 0;
                }
              }
            
              /** convert an encoded unsigned byte value into a int with the unsigned value. */
              private static final int bytes2u(byte inp) {
                return (int) inp & 0xff;
              }
            }
            """;

    @DisplayName("test loading of AAPI")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }
}
