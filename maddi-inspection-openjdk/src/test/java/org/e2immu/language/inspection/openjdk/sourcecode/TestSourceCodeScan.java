package org.e2immu.language.inspection.openjdk.sourcecode;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.openjdk.SourceCodeScan;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSourceCodeScan {

    @Language("java")
    private static final String INPUT1 = """
            /*
            at the very beginning
             */
            package org.e2immu.analyser.resolver.testexample;
            // a comment before the imports
            import java.util.List;
            // a comment in between the imports
            import java.util.Collection;
            /* a trailing comment after the imports */
            
            // a comment before class
            /* a second comment before class */
            public class MethodCall_3 {
            
                interface Get {
                    String get();
                }
            
                record GetOnly(String s) implements Get {
            
                    @Override
                    public String get() {
                        return s;
                    }
                }
            
                public void accept(List<Get> list) {
                    list.forEach(get -> System.out.println(get.get()));
                }
            
                public void test() {
                    // here, List.of(...) becomes a List<Get> because of the context of 'accept(...)'
                    accept(List.of(new GetOnly("hello")));
                }
            }
            // trailing class comment
            """;

    @Test
    public void test() {
        SourceCodeScan sourceCodeScan = new SourceCodeScan(new RuntimeImpl());
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT1);

        Iterator<Map.Entry<Source, List<Comment>>> cIterator = r.comments().entrySet().iterator();

        testComment(cIterator, "4-1:4-49", "\nat the very beginning\n ");
        testComment(cIterator, "6-1:6-22", " a comment before the imports");
        testComment(cIterator, "8-1:8-28", " a comment in between the imports");

        Iterator<Map.Entry<Source, String>> kIterator = r.keywords().entrySet().iterator();
        testKeyword(kIterator, "4-1:4-7", "package");
        testKeyword(kIterator, "6-1:6-6", "import");
        testKeyword(kIterator, "8-1:8-6", "import");
    }

    private static void testKeyword(Iterator<Map.Entry<Source, String>> kIterator, String compact2, String keyword) {
        assertTrue(kIterator.hasNext());
        Map.Entry<Source, String> k2 = kIterator.next();
        assertEquals(compact2, k2.getKey().compact2());
        assertEquals(keyword, k2.getValue());
    }

    private static void testComment(Iterator<Map.Entry<Source, List<Comment>>> cIterator, String expected, String expected1) {
        assertTrue(cIterator.hasNext());
        Map.Entry<Source, List<Comment>> c1 = cIterator.next();
        assertEquals(expected, c1.getKey().compact2());
        assertEquals(expected1, c1.getValue().getFirst().comment());
    }
}
