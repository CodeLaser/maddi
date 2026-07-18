package org.e2immu.analyzer.modification.prepwork;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * elasticsearch first contact: a member record of an anonymous class, forward-referenced from a sibling
 * method (BinaryFieldMapperTests.$13.BytesCompareUnsigned). The scanner-side duplicate was fixed; this
 * pins the PREP side, which crashed on the record's component field ('analysisOfInitializer' UOE /
 * 'has a null initializer' assert) because the field builders were never committed.
 */
public class TestAnonymousMemberRecord extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.stream.Stream;
            class X {
                interface Support { Object example(); }
                Support make() {
                    return new Support() {
                        @Override
                        public Object example() {
                            return Stream.of(new byte[]{1}).map(Cmp::new).sorted().toList();
                        }
                        private record Cmp(byte[] bytes) implements Comparable<Cmp> {
                            @Override
                            public int compareTo(Cmp o) { return 0; }
                        }
                    };
                }
            }
            """;

    @Disabled("OPEN BUG (task #33): forward reference resolves the record via the lazy class-symbol path; "
            + "the later source visit rebuilds members on the same TypeInfo. See sv-remaining-catalogue.md.")
    @DisplayName("prep over a forward-referenced member record of an anonymous class")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        new PrepAnalyzer(runtime).doPrimaryType(X);
        assertNotNull(X);
    }
}
