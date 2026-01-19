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


public class TestWarnForWrongIndependentHC extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.nio.ByteBuffer;
            import java.nio.CharBuffer;
            import java.nio.charset.Charset;
            import java.nio.charset.CharsetEncoder;
            import java.nio.charset.StandardCharsets;
            
            public class Function15634639_file755568 {
            
                public static String unicodeEncode(String str) {
                    try {
                        Charset charset = StandardCharsets.ISO_8859_1;
                        CharsetEncoder encoder = charset.newEncoder();
                        ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(str));
                        return new String(bbuf.array());
                    } catch (Exception e) {
                        return ("Encoding problem");
                    }
                }
            }
            """;

    @DisplayName("issue in LinkHelper INDEPENDENT_HC while both target and source are byte[]")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }
}
