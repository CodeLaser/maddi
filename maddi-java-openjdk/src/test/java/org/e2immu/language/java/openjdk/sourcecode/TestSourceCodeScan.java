package org.e2immu.language.java.openjdk.sourcecode;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.java.openjdk.SourceCodeScan;
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
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT1, false);

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
        testComment(cIterator, "28-40:28-53", " nice list ", " trailing param");

        Iterator<Map.Entry<Source, List<Comment>>> tIterator = r.trailingComments().entrySet().iterator();
        testComment(tIterator, "4-1:41-26", " trailing class comment");

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
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT2, false);

        Iterator<Map.Entry<Source, List<Comment>>> cIterator = r.comments().entrySet().iterator();
        testComment(cIterator, "6-8:9-8", " comment");

        Iterator<Map.Entry<Source, List<Comment>>> tIterator = r.trailingComments().entrySet().iterator();
        testComment(tIterator, "6-8:9-8", " this was i");

        Iterator<Map.Entry<Source, Map<Object, Object>>> aIterator = r.argumentLists().entrySet().iterator();
        testArgumentList(aIterator, "3-5:12-5", "END_OF_PARAMETER_LIST=3-31:3-31");
        testArgumentList(aIterator, "3-16:3-23", "SUCCEEDING_COMMA=3-24:3-24");
        testArgumentList(aIterator, "3-26:3-30", "PRECEDING_COMMA=3-24:3-24");
        testArgumentList(aIterator, "4-11:4-20", "END_OF_ARGUMENT_LIST=4-20:4-20");
        // local 'int i = s.substring(0, 5).length();' : the name 'i' -> its '=', then the declarator (no commas)
        testArgumentList(aIterator, "10-12:10-12", "SUCCEEDING_EQUALS=10-14:10-14");
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
                } else if (e.getKey() == DetailedSources.SUCCEEDING_EQUALS) {
                    key = "SUCCEEDING_EQUALS";
                } else if (e.getKey() == DetailedSources.FIELD_DECLARATION) {
                    key = "FIELD_DECLARATION";
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

    @Language("java")
    public static final String INPUT3 = """
            package a.b;
            public enum OutputFormat {
            	JSON(new JSONWriter()), YAML(new YAMLWriter());
            
            	private final SwaggerWriter writer;
            
            	OutputFormat(SwaggerWriter writer) {
            		this.writer = writer;
            	}
            
            	public void write(OpenAPI swagger, File file, boolean prettyPrint) throws IOException {
            		writer.write(swagger, file, prettyPrint);
            	}
            
            	@FunctionalInterface
            	interface SwaggerWriter {
            		void write(OpenAPI swagger, File file, boolean prettyPrint) throws IOException;
            	}
            
            	static class JSONWriter implements SwaggerWriter {
            
            		@Override
            		public void write(OpenAPI swagger, File file, boolean prettyPrint) throws IOException {
            			ObjectMapper mapper = Json.mapper();
            			mapper.addMixIn(ServerVariable.class, SwaggerServerVariable.ServerVariableMixin.class);
            			if (prettyPrint) {
            				mapper.enable(SerializationFeature.INDENT_OUTPUT);
            			}
            			mapper.writeValue(file, swagger);
            		}
            	}
            
            	static class YAMLWriter implements SwaggerWriter {
            
            		@Override
            		public void write(OpenAPI swagger, File file, boolean prettyPrint) throws IOException {
            			ObjectMapper mapper = Yaml.mapper();
            			mapper.addMixIn(ServerVariable.class, SwaggerServerVariable.ServerVariableMixin.class);
            			mapper.addMixIn(Schema.class, SchemaMixin.class);
            			mapper.writeValue(file, swagger);
            		}
            	}
            }
            """;


    @Test
    public void test3() {
        SourceCodeScan sourceCodeScan = new SourceCodeScan(new RuntimeImpl());
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT3, false);

        Iterator<Map.Entry<Source, String>> kIterator = r.keywords().entrySet().iterator();
        testKeyword(kIterator, "1-1:1-7", "package");
        testKeyword(kIterator, "2-1:2-6", "public");
        testKeyword(kIterator, "2-8:2-11", "enum");
        testKeyword(kIterator, "11-2:11-7", "public");
        testKeyword(kIterator, "16-2:16-10", "interface");
        testKeyword(kIterator, "20-2:20-7", "static");
        testKeyword(kIterator, "20-9:20-13", "class");
        testKeyword(kIterator, "20-26:20-35", "implements");
    }

    @Language("java")
    public static final String INPUT4 = """
            package a.b;
            public class X {
                int min; //comment for min
                int max; //comment for max
            }
            """;

    @Test
    public void test4() {
        SourceCodeScan sourceCodeScan = new SourceCodeScan(new RuntimeImpl());
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT4, false);
        Iterator<Map.Entry<Source, List<Comment>>> cIterator = r.comments().entrySet().iterator();
        testComment(cIterator, "3-5:3-12", "comment for min");
        testComment(cIterator, "4-5:4-12", "comment for max");

        Iterator<Map.Entry<Source, List<Comment>>> tcIterator = r.trailingComments().entrySet().iterator();
        assertFalse(tcIterator.hasNext());
    }


    @Language("java")
    public static final String INPUT5 = """
            package dev.langchain4j.agentic;
            
            import dev.langchain4j.agentic.declarative.TypedKey;
            
            import static java.lang.annotation.ElementType.METHOD;
            import static java.lang.annotation.RetentionPolicy.RUNTIME;
            
            import java.lang.annotation.Retention;
            import java.lang.annotation.Target;
            
            /**
             * Java methods annotated with {@code @Agent} are considered agents that other agents can invoke.
             */
            @Retention(RUNTIME)
            @Target({METHOD})
            public @interface Agent {
            
                /**
                 * Name of the agent. If not provided, method name will be used.
                 *
                 * @return name of the agent.
                 */
                String name() default "";
            
                /**
                 * Description of the agent. This is an alias of the {@code description} attribute, and it is possible to use either.
                 * It should be clear and descriptive to allow language model to understand the agent's purpose and its intended use.
                 *
                 * @return description of the agent.
                 */
                String value() default "";
            
                /**
                 * Description of the agent. This is an alias of the {@code value} attribute, and it is possible to use either.
                 * It should be clear and descriptive to allow language model to understand the agent's purpose and its intended use.
                 *
                 * @return description of the agent.
                 */
                String description() default "";
            
                /**
                 * Key of the output variable that will be used to store the result of the agent's invocation.
                 *
                 * @return name of the output variable.
                 */
                String outputKey() default "";
            
                Class<? extends TypedKey<?>> typedOutputKey() default NoTypedKey.class;
            
                /**
                 * If true, the agent will be invoked in an asynchronous manner, allowing the workflow to continue without waiting for the agent's result.
                 *
                 * @return true if the agent should be invoked in an asynchronous manner, false otherwise.
                 */
                boolean async() default false;
            
                /**
                 * If true, the agent's execution will be silently skipped when any of its arguments is missing in the agentic scope,
                 * instead of making the agentic system's execution fail.
                 *
                 * @return true if the agent is optional, false otherwise.
                 */
                boolean optional() default false;
            
                /**
                 * Names of other agents participating in the definition of the context of this agent.
                 *
                 * @return array of names of other agents participating in the definition of the context of this agent.
                 */
                String[] summarizedContext() default {};
            
                class NoTypedKey implements TypedKey<Void> { }
            }
            """;

    @Test
    public void test5() {
        SourceCodeScan sourceCodeScan = new SourceCodeScan(new RuntimeImpl());
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT5, false);
        Iterator<Map.Entry<Source, String>> kIterator = r.keywords().entrySet().iterator();
        testKeyword(kIterator, "1-1:1-7", "package");
        testKeyword(kIterator, "3-1:3-6", "import");
        testKeyword(kIterator, "5-1:5-6", "import");
        testKeyword(kIterator, "5-8:5-13", "static");
        testKeyword(kIterator, "6-1:6-6", "import");
        testKeyword(kIterator, "6-8:6-13", "static");
        testKeyword(kIterator, "8-1:8-6", "import");
        testKeyword(kIterator, "9-1:9-6", "import");

        testKeyword(kIterator, "16-1:16-6", "public");
        testKeyword(kIterator, "16-9:16-17", "interface");
        testKeyword(kIterator, "23-19:23-25", "default");
        testKeyword(kIterator, "31-20:31-26", "default");
        testKeyword(kIterator, "39-26:39-32", "default");
        testKeyword(kIterator, "46-24:46-30", "default");
        //testKeyword(kIterator, "1-1:1-7", "extends");
        testKeyword(kIterator, "48-51:48-57", "default");
        testKeyword(kIterator, "55-21:55-27", "default");
        testKeyword(kIterator, "63-24:63-30", "default");
        testKeyword(kIterator, "70-34:70-40", "default");
        testKeyword(kIterator, "72-5:72-9", "class");
        testKeyword(kIterator, "72-22:72-31", "implements");
    }


    @Language("java")
    public static final String INPUT6 = """
            package a.b;
            class X<A, B, C> {
                int i, j, k;
                <T, U, X extends Object> void m(T t, U u) {
                }
            }
            """;

    /*
    Demonstrates the gaps: SourceCodeScan currently records comma/end-of-list positions ONLY for formal
    METHOD parameter lists. Formal TYPE parameter lists and multi-declarator FIELD lists are not scanned,
    even though DetailedSources.PRECEDING_COMMA / SUCCEEDING_COMMA are documented to cover them too.

    For INPUT6 the only entries are the ones for m(T t, U u); the assertFalse(hasNext()) below proves that
    nothing is recorded for:

      - line 2 'class X<A, B, C>' : type-parameter commas at 2-10, 2-13 and the closing '>' at 2-16
                                    (type parameters A@2-9, B@2-12, C@2-15)
      - line 3 'int i, j, k;'     : field-declarator commas at 3-10, 3-13
                                    (declarators i@3-9, j@3-12, k@3-15)
      - line 4 '<T, U>'           : method type-parameter comma at 4-7
                                    (type parameters T@4-6, U@4-9, closing '>' at 4-10)
    */
    @Test
    public void test6_gaps() {
        SourceCodeScan sourceCodeScan = new SourceCodeScan(new RuntimeImpl());
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT6, false);
        Iterator<Map.Entry<Source, Map<Object, Object>>> aIterator = r.argumentLists().entrySet().iterator();

        testArgumentList(aIterator, "2-9:2-9", "SUCCEEDING_COMMA=2-10:2-10");   // A
        testArgumentList(aIterator, "2-12:2-12", "PRECEDING_COMMA=2-10:2-10, SUCCEEDING_COMMA=2-13:2-13");  // B
        testArgumentList(aIterator, "2-15:2-15", "PRECEDING_COMMA=2-13:2-13");  // C

        testArgumentList(aIterator, "3-9:3-9", "SUCCEEDING_COMMA=3-10:3-10");   // i
        testArgumentList(aIterator, "3-12:3-12", "PRECEDING_COMMA=3-10:3-10, SUCCEEDING_COMMA=3-13:3-13");  // j
        testArgumentList(aIterator, "3-15:3-15", "PRECEDING_COMMA=3-13:3-13");  // k

        testArgumentList(aIterator, "4-5:5-5", "END_OF_PARAMETER_LIST=4-45:4-45");  // the ')' of m(...)

        testArgumentList(aIterator, "4-6:4-6", "SUCCEEDING_COMMA=4-7:4-7");
        testArgumentList(aIterator, "4-9:4-9", "PRECEDING_COMMA=4-7:4-7, SUCCEEDING_COMMA=4-10:4-10");
        testArgumentList(aIterator, "4-12:4-27", "PRECEDING_COMMA=4-10:4-10");

        testArgumentList(aIterator, "4-37:4-39", "SUCCEEDING_COMMA=4-40:4-40");     // "T t"
        testArgumentList(aIterator, "4-42:4-44", "PRECEDING_COMMA=4-40:4-40");      // "U u"

        // GAP: nothing else is recorded — no type-parameter lists, no field-declarator list.
        // When that support is added, new entries will appear here and this assertion will need updating.
        assertFalse(aIterator.hasNext(), "expected no further entries; type parameters and field "
                                         + "declarators are not yet scanned");
    }

    @Language("java")
    public static final String INPUT7 = """
            package a.b;
            class C {
                C() {
                    this(1, 2);
                }
                void calls() {
                    noArg();
                    oneArg(x);
                    threeArgs(x, y, z);
                }
                void allocations() {
                    D d0 = new D();
                    D d1 = new D(x);
                    D d2 = new D(x, y);
                }
                void nested() {
                    outer(inner(x, y), z);
                }
            }
            """;

    /*
    Exhaustive coverage of ARGUMENT_COMMAS / END_OF_ARGUMENT_LIST, which scanCall records for the three
    kinds of call sites (method call, 'new' allocation, explicit constructor invocation), at 0 / 1 / many
    arguments, plus nesting. Every method/constructor's parameter list also yields an END_OF_PARAMETER_LIST
    (even the empty '()'), so those appear interleaved.
    */
    @Test
    public void test7() {
        SourceCodeScan sourceCodeScan = new SourceCodeScan(new RuntimeImpl());
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT7, false);
        Iterator<Map.Entry<Source, Map<Object, Object>>> a = r.argumentLists().entrySet().iterator();

        testArgumentList(a, "3-5:5-5", "END_OF_PARAMETER_LIST=3-7:3-7");                              // C()
        // explicit constructor invocation, 2 args: this(1, 2)
        testArgumentList(a, "4-9:4-18", "ARGUMENT_COMMAS=4-15:4-15, END_OF_ARGUMENT_LIST=4-18:4-18");

        testArgumentList(a, "6-5:10-5", "END_OF_PARAMETER_LIST=6-16:6-16");                           // calls()
        testArgumentList(a, "7-9:7-15", "END_OF_ARGUMENT_LIST=7-15:7-15");                            // noArg()       0 args
        testArgumentList(a, "8-9:8-17", "END_OF_ARGUMENT_LIST=8-17:8-17");                            // oneArg(x)     1 arg
        // method call, 3 args -> two commas
        testArgumentList(a, "9-9:9-26", "ARGUMENT_COMMAS=9-20:9-20;9-23:9-23, END_OF_ARGUMENT_LIST=9-26:9-26");

        testArgumentList(a, "11-5:15-5", "END_OF_PARAMETER_LIST=11-22:11-22");                        // allocations()
        // each 'D dN = new D(...)' local adds: name 'dN' -> '=', then the declarator (no commas), then the call
        testArgumentList(a, "12-11:12-12", "SUCCEEDING_EQUALS=12-14:12-14");                          // d0 -> '='
        testArgumentList(a, "12-16:12-22", "END_OF_ARGUMENT_LIST=12-22:12-22");                       // new D()       0 args
        testArgumentList(a, "13-11:13-12", "SUCCEEDING_EQUALS=13-14:13-14");                          // d1 -> '='
        testArgumentList(a, "13-16:13-23", "END_OF_ARGUMENT_LIST=13-23:13-23");                       // new D(x)      1 arg
        testArgumentList(a, "14-11:14-12", "SUCCEEDING_EQUALS=14-14:14-14");                          // d2 -> '='
        // allocation, 2 args -> one comma
        testArgumentList(a, "14-16:14-26", "ARGUMENT_COMMAS=14-23:14-23, END_OF_ARGUMENT_LIST=14-26:14-26");

        testArgumentList(a, "16-5:18-5", "END_OF_PARAMETER_LIST=16-17:16-17");                        // nested()
        // nested calls: outer(inner(x, y), z) -> the outer list first (it begins earlier), then the inner
        testArgumentList(a, "17-9:17-29", "ARGUMENT_COMMAS=17-26:17-26, END_OF_ARGUMENT_LIST=17-29:17-29");
        testArgumentList(a, "17-15:17-25", "ARGUMENT_COMMAS=17-22:17-22, END_OF_ARGUMENT_LIST=17-25:17-25");

        assertFalse(a.hasNext());
    }

    @Language("java")
    public static final String INPUT8 = """
            package a.b;
            class C {
                void m(int a, int b, int c) {
                }
                void s(int only) {
                }
            }
            """;

    /*
    Completes the coverage of the formal-parameter keys PRECEDING_COMMA / SUCCEEDING_COMMA /
    END_OF_PARAMETER_LIST. test2/test6 only have two-parameter lists, so no parameter ever carries BOTH
    commas, and a one-parameter list (empty comma map) is never exercised. Here:
      - 'int b' is a middle parameter -> PRECEDING_COMMA + SUCCEEDING_COMMA (the missing combination)
      - 'int a' first -> SUCCEEDING_COMMA only; 'int c' last -> PRECEDING_COMMA only
      - 'int only' single parameter -> empty comma map
    */
    @Test
    public void test8() {
        SourceCodeScan sourceCodeScan = new SourceCodeScan(new RuntimeImpl());
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT8, false);
        Iterator<Map.Entry<Source, Map<Object, Object>>> a = r.argumentLists().entrySet().iterator();

        testArgumentList(a, "3-5:4-5", "END_OF_PARAMETER_LIST=3-31:3-31");        // m(...)
        testArgumentList(a, "3-12:3-16", "SUCCEEDING_COMMA=3-17:3-17");           // int a  (first)
        testArgumentList(a, "3-19:3-23", "PRECEDING_COMMA=3-17:3-17, SUCCEEDING_COMMA=3-24:3-24"); // int b (middle: BOTH)
        testArgumentList(a, "3-26:3-30", "PRECEDING_COMMA=3-24:3-24");            // int c  (last)

        testArgumentList(a, "5-5:6-5", "END_OF_PARAMETER_LIST=5-20:5-20");        // s(...)
        testArgumentList(a, "5-12:5-19", "");                                     // int only (single: no commas)

        assertFalse(a.hasNext());
    }

    @Language("java")
    public static final String INPUT9 = """
            package a.b;
            public sealed interface I permits A, B {
            }
            final class A implements I {
            }
            final class B implements I {
            }
            """;

    @Test
    public void test9() {
        SourceCodeScan sourceCodeScan = new SourceCodeScan(new RuntimeImpl());
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT9, false);
        Iterator<Map.Entry<Source, String>> k = r.keywords().entrySet().iterator();
        testKeyword(k, "1-1:1-7", "package");
        testKeyword(k, "2-1:2-6", "public");
        testKeyword(k, "2-8:2-13", "sealed");
        testKeyword(k, "2-15:2-23", "interface");
        testKeyword(k, "2-27:2-33", "permits");
        testKeyword(k, "4-1:4-5", "final");
        testKeyword(k, "4-7:4-11", "class");
        testKeyword(k, "4-15:4-24", "implements");
        testKeyword(k, "6-1:6-5", "final");
        testKeyword(k, "6-7:6-11", "class");
        testKeyword(k, "6-15:6-24", "implements");
        assertFalse(k.hasNext());
    }

    // collects, across all argumentLists entries, the Source(s) recorded under a given sentinel key
    private static List<Source> findDetail(SourceCodeScan.Result r, Object key) {
        List<Source> result = new ArrayList<>();
        for (Map<Object, Object> map : r.argumentLists().values()) {
            Object v = map.get(key);
            if (v instanceof Source s) {
                result.add(s);
            } else if (v instanceof List<?> list) {
                for (Object o : list) result.add((Source) o);
            }
        }
        return result;
    }

    @Language("java")
    public static final String INPUT10 = """
            package a.b;
            class C {
                int x = 5;
                int y;
            }
            """;

    /*
    Drives SUCCEEDING_EQUALS, the position of the '=' in a field declarator. SourceCodeScan does not yet
    record it (scanFieldDeclaration only collects comments), so this fails until it does. The driver does
    not assume which Source the implementation keys the entry by; it just asserts that exactly one
    SUCCEEDING_EQUALS is recorded, at the '=' of 'int x = 5;' (3-11), and none for the initialiser-less
    'int y;'.
    */
    @Test
    public void test10() {
        SourceCodeScan sourceCodeScan = new SourceCodeScan(new RuntimeImpl());
        SourceCodeScan.Result r = sourceCodeScan.go(INPUT10, false);
        List<Source> equalsSigns = findDetail(r, DetailedSources.SUCCEEDING_EQUALS);
        assertEquals(1, equalsSigns.size(), "expected one SUCCEEDING_EQUALS, for 'int x = 5;'");
        assertEquals("3-11:3-11", equalsSigns.getFirst().compact2());
    }

}
