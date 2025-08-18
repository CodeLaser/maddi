package org.e2immu.analyzer.modification.linkedvariables.clonebench;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestHCTNotYetLoaded extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.math.BigInteger;
            
            public class Function17122361_file1119884 {
              private static long vecVecMultModM(long[] v, long[] w, BigInteger modul) {
                BigInteger result = BigInteger.ZERO;
                for (int i = 0; i < v.length; i++) {
                  result = multModM(v[i], w[i], modul).add(result).mod(modul);
                }
                return result.longValue();
              }

              private static BigInteger multModM(long r, long s, BigInteger modul) {
                return BigInteger.valueOf(r).multiply(BigInteger.valueOf(s)).mod(modul);
              }
            }
            """;

    @DisplayName("test HCT not yet loaded")
    @org.junit.jupiter.api.Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);

        TypeInfo bigInt = javaInspector.compiledTypesManager().get(BigInteger.class);
        HiddenContentTypes hct = bigInt.analysis().getOrNull(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.class);
        assertEquals("", hct.detailedSortedTypes());

        analyzer.go(ao);
    }
}
