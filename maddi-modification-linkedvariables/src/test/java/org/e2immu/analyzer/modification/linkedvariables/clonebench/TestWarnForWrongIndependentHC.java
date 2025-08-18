package org.e2immu.analyzer.modification.linkedvariables.clonebench;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
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
