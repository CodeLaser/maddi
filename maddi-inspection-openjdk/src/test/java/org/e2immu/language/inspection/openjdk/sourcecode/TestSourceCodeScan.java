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
            
            import java.util.List;
            
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
            """;

    @Test
    public void test() {
        SourceCodeScan sourceCodeScan = new SourceCodeScan(new RuntimeImpl());
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT1);
        assertEquals(1, r.comments().size());

        Iterator<Map.Entry<Source, List<Comment>>> cIterator = r.comments().entrySet().iterator();
        assertTrue(cIterator.hasNext());
        Map.Entry<Source, List<Comment>> c1 = cIterator.next();
        assertEquals("4-1:4-49", c1.getKey().compact2());
        assertEquals("\nat the very beginning\n ", c1.getValue().getFirst().comment());

        Iterator<Map.Entry<Source, String>> kIterator = r.keywords().entrySet().iterator();
        assertTrue(kIterator.hasNext());
        Map.Entry<Source, String> k1 = kIterator.next();
        assertEquals("4-1:4-7", k1.getKey().compact2());
        assertEquals("package", k1.getValue());

    }
}
