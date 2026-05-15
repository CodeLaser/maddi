package org.e2immu.language.inspection.openjdk.sourcecode;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.openjdk.SourceCodeScan;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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
    public void test1() {
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

    @Language("java")
    String INPUT2 = """
            package a.b;
            class C {
                int method(String s, int k) {
                   if(s.length()==1)
                   // comment
                   {
                      return k;
                      // this was i
                   }
                   int i = s.substring(0, 5).length();
                   return 2*i;
                }
            }
            """;

    @Test
    public void test2() {
        SourceCodeScan sourceCodeScan = new SourceCodeScan(new RuntimeImpl());
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT2);

        Iterator<Map.Entry<Source, List<Comment>>> cIterator = r.comments().entrySet().iterator();
        testComment(cIterator, "6-8:9-8", " comment");

        Iterator<Map.Entry<Source, List<Comment>>> tIterator = r.trailingComments().entrySet().iterator();
        testComment(tIterator, "6-8:9-8", " this was i");

        Iterator<Map.Entry<Source, Map<Object, Object>>> aIterator = r.argumentLists().entrySet().iterator();
        testArgumentList(aIterator, "3-5:12-5", "END_OF_PARAMETER_LIST=3-31:3-31");
        testArgumentList(aIterator, "3-16:3-23", "SUCCEEDING_COMMA=3-24:3-24");
        testArgumentList(aIterator, "3-26:3-30", "PRECEDING_COMMA=3-24:3-24");
        testArgumentList(aIterator, "4-11:4-20", "END_OF_ARGUMENT_LIST=4-20:4-20");
        testArgumentList(aIterator, "10-16:10-32", "ARGUMENT_COMMAS=10-29:10-29, END_OF_ARGUMENT_LIST=10-32:10-32");
        testArgumentList(aIterator, "10-16:10-41", "END_OF_ARGUMENT_LIST=10-41:10-41");
        assertFalse(aIterator.hasNext());
    }

    @SuppressWarnings("unchecked")
    private static void testArgumentList(Iterator<Map.Entry<Source, Map<Object, Object>>> aIterator,
                                         String source, String mapToString) {
        assertTrue(aIterator.hasNext(), "Have no more argument lists");
        Map.Entry<Source, Map<Object, Object>> entry = aIterator.next();
        assertEquals(source, entry.getKey().compact2());
        List<String> list = new ArrayList<>();
        for (Map.Entry<Object, Object> e : entry.getValue().entrySet()) {
            String key = "?";
            String value;
            if (e.getKey() == DetailedSources.ARGUMENT_COMMAS) {
                key = "ARGUMENT_COMMAS";
                List<Object> commas = (List<Object>) e.getValue();
                value = commas.stream().map(c -> ((Source) c).compact2()).collect(Collectors.joining(";"));
            } else {
                value = ((Source) e.getValue()).compact2();
                if (e.getKey() == DetailedSources.END_OF_ARGUMENT_LIST) {
                    key = "END_OF_ARGUMENT_LIST";
                } else if (e.getKey() == DetailedSources.END_OF_PARAMETER_LIST) {
                    key = "END_OF_PARAMETER_LIST";
                } else if (e.getKey() == DetailedSources.PRECEDING_COMMA) {
                    key = "PRECEDING_COMMA";
                } else if (e.getKey() == DetailedSources.SUCCEEDING_COMMA) {
                    key = "SUCCEEDING_COMMA";
                }
            }
            list.add(key + "=" + value);
        }
        assertEquals(mapToString, list.stream().sorted().collect(Collectors.joining(", ")));
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
