package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The type-nature keyword's position, on the hand-written (CongoCC) front end.
 * <p>
 * The openjdk front end has recorded this for a while ({@code TestDetailedSources.testTypeNatureKeyword});
 * this side did not, so the two disagreed about a detail a caller cannot see is missing until it asks. It is
 * the token a class-to-record conversion replaces, and it is deliberately recorded rather than derived from
 * the simple name, because any amount of whitespace or a line break may sit between the two.
 */
public class TestTypeNatureKeyword extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public final class X {
                interface I { }
                enum E { A }
                record R(int i) { }
                public
                static
                class Spaced { }
            }
            """;

    @DisplayName("class/interface/enum/record keyword, keyed by the TypeNature")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1, new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true).setFailFast(true).build());

        assertNotNull(X.source().detailedSources(), "detailed sources must be on");
        assertEquals("2-14:2-18", X.source().detailedSources().detail(X.typeNature()).compact2());
        assertEquals("2-20:2-20", X.source().detailedSources().detail(X.simpleName()).compact2());

        TypeInfo i = X.findSubType("I");
        assertEquals("3-5:3-13", i.source().detailedSources().detail(i.typeNature()).compact2());
        TypeInfo e = X.findSubType("E");
        assertEquals("4-5:4-8", e.source().detailedSources().detail(e.typeNature()).compact2());
        TypeInfo r = X.findSubType("R");
        assertEquals("5-5:5-10", r.source().detailedSources().detail(r.typeNature()).compact2());

        // why it is recorded and not derived: a line break between the keyword and the name
        TypeInfo spaced = X.findSubType("Spaced");
        assertEquals("8-5:8-9", spaced.source().detailedSources().detail(spaced.typeNature()).compact2());
        assertEquals("8-11:8-16", spaced.source().detailedSources().detail(spaced.simpleName()).compact2());
    }
}
