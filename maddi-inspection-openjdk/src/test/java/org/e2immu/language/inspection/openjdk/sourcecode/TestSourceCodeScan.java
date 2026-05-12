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
            
                // recursive comment
                interface Get {
                    // method comment
                    String get();
                }
            
                record GetOnly(/* param comment */ String s) implements Get {
                    @Override
                    public String get() {
                        return s;
                    }
                }
            
                public void accept(/* nice list */ List<Get> list // trailing param
                ) {
                    // statement comment
                    list.forEach(get -> System.out.println(get.get()));
                }
            
                public void test() {
                    // here, List.of(...) becomes a List<Get> because of the context of 'accept(...)'
                    accept(List.of(new GetOnly("hello")));
                    System.out.println(Collection.class);
                }
                // trailing method comment
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
        List<Comment> cs = testComment(cIterator, "13-1:40-1", " a trailing comment after the imports ",
                " a comment before class", " a second comment before class ");
        assertEquals("9-1:9-42", cs.getFirst().source().compact2());
        assertEquals("12-1:12-35", cs.getLast().source().compact2());
        testComment(cIterator, "16-5:19-5", " recursive comment");
        testComment(cIterator, "18-9:18-21", " method comment");
        testComment(cIterator, "28-40:28-53", " nice list ");

        Iterator<Map.Entry<Source, List<Comment>>> tIterator = r.trailingComments().entrySet().iterator();
        testComment(tIterator, "13-1:40-1", " trailing class comment");

        Iterator<Map.Entry<Source, String>> kIterator = r.keywords().entrySet().iterator();
        testKeyword(kIterator, "4-1:4-7", "package");
        testKeyword(kIterator, "6-1:6-6", "import");
        testKeyword(kIterator, "8-1:8-6", "import");
        testKeyword(kIterator, "13-1:13-6", "public");
        testKeyword(kIterator, "13-8:13-12", "class");
        testKeyword(kIterator, "16-5:16-13", "interface");
        testKeyword(kIterator, "21-5:21-10", "record");
        testKeyword(kIterator, "21-50:21-59", "implements");
        testKeyword(kIterator, "28-5:28-10", "public");
        testKeyword(kIterator, "34-5:34-10", "public");
    }

    private static void testKeyword(Iterator<Map.Entry<Source, String>> kIterator, String compact2, String keyword) {
        assertTrue(kIterator.hasNext(), "Have no more keywords");
        Map.Entry<Source, String> k2 = kIterator.next();
        assertEquals(compact2, k2.getKey().compact2(), "... for keyword " + k2.getValue());
        assertEquals(keyword, k2.getValue());
    }

    private static List<Comment> testComment(Iterator<Map.Entry<Source, List<Comment>>> cIterator, String compact2, String... comments) {
        assertTrue(cIterator.hasNext(), "Have no more comments/trailing comments");
        Map.Entry<Source, List<Comment>> c1 = cIterator.next();
        assertEquals(compact2, c1.getKey().compact2(), "for comment: " + c1.getValue().getFirst().comment());
        int i = 0;
        for (Comment c : c1.getValue()) {
            assertEquals(comments[i++], c.comment());
        }
        return c1.getValue();
    }

}
