package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.MethodReference;
import org.e2immu.language.cst.api.expression.TypeExpression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestMethodReference extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.HashMap;
            import java.util.Map;
            import java.util.function.Supplier;
            
            public class MethodReference_0 {
            
                private Map<String, Integer> make(Supplier<Map<String, Integer>> supplier) {
                    return supplier.get();
                }
            
                public void method() {
                    Map<String, Integer> map = make(HashMap::new);
                    map.put("a", 1);
                }
            }
            """;

    // in this example typesOfParametersFromMethod == typesOfParametersFromForward;
    //                 returnTypeFromMethod == more specific, but does not have TPs
    //                 returnTypeFromForward == more generic, but with concrete TPs
    @DisplayName("concrete type of MR from forward; constructor")
    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
        LocalVariableCreation lvc0 = (LocalVariableCreation) methodInfo.methodBody().statements().getFirst();
        MethodCall mc = (MethodCall) lvc0.localVariable().assignmentExpression();
        MethodReference mr = (MethodReference) mc.parameterExpressions().getFirst();
        assertEquals("Type java.util.function.Supplier<java.util.HashMap<String,Integer>>",
                mr.parameterizedType().toString());
        if (mr.scope() instanceof TypeExpression te) {
            assertEquals("14-41:14-47", te.source().compact2());
        } else fail();
    }

    @Language("java")
    private static final String INPUT1A = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.HashMap;
            import java.util.Map;
            import java.util.function.Supplier;
            
            public class MethodReference_0 {
            
                private Map<String, Integer> make(Supplier<Map<String, Integer>> supplier) {
                    return supplier.get();
                }
            
                public void method() {
                    Map<String, Integer> map = make(HashMap<String,Integer>::new);
                    map.put("a", 1);
                }
            }
            """;

    // in this example typesOfParametersFromMethod == typesOfParametersFromForward;
    //                 returnTypeFromMethod == more specific, with concrete TPs
    //                 returnTypeFromForward == more generic, with concrete TPs
    @DisplayName("concrete type of MR from forward; constructor")
    @Test
    public void test1A() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1A);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
        LocalVariableCreation lvc0 = (LocalVariableCreation) methodInfo.methodBody().statements().getFirst();
        MethodCall mc = (MethodCall) lvc0.localVariable().assignmentExpression();
        MethodReference mr = (MethodReference) mc.parameterExpressions().getFirst();
        assertEquals("Type java.util.function.Supplier<java.util.HashMap<String,Integer>>",
                mr.parameterizedType().toString());
    }

    @Language("java")
    private static final String INPUT1B = """
            import java.util.List;
            import java.util.stream.IntStream;
            import java.util.stream.Stream;
            public class MethodReference_0B {
               static <T> Stream<T> m7(List<T> list) {
                    return IntStream.of(3).mapToObj(list::get);
                }
            }
            """;

    // in this example typesOfParametersFromMethod == typesOfParametersFromForward;
    //                 returnTypeFromMethod == returnTypeFromForward
    @Test
    @DisplayName("concrete type of MR from method")
    public void test1B() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1B, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("m7", 1);
        ReturnStatement rs = (ReturnStatement) methodInfo.methodBody().statements().getFirst();
        MethodCall mc = (MethodCall) rs.expression();
        MethodReference mr = (MethodReference) mc.parameterExpressions().getFirst();
        assertEquals("Type java.util.function.IntFunction<T>",
                mr.parameterizedType().toString());

        assertEquals("6-47:6-49", mr.source().detailedSources().detail(mr.methodInfo().name()).compact2());
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.Map;
            import java.util.Objects;
            
            public class MethodReference_1 {
            
                public boolean method(Map<String, Integer> map) {
                    return map.entrySet().stream()
                            .filter(Objects::nonNull)
                            .anyMatch(e -> e.getKey().length() == 3);
                }
            }
            """;

    // in this example typesOfParametersFromMethod == Object, while
    //                 typesOfParametersFromForward == Map.Entry<String, Integer>
    //                 returnTypeFromMethod == returnTypeFromForward == boolean
    @DisplayName("concrete type of MR from forward; method")
    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
        ReturnStatement rs = (ReturnStatement) methodInfo.methodBody().statements().getFirst();
        MethodCall anyMatch = (MethodCall) rs.expression();
        MethodCall filter = (MethodCall) anyMatch.object();
        MethodReference mr = (MethodReference) filter.parameterExpressions().getFirst();
        assertEquals("Type java.util.function.Predicate<java.util.Map.Entry<String,Integer>>",
                mr.parameterizedType().toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.NotNull;
            
            import java.util.HashMap;
            import java.util.Map;
            import java.util.function.Function;
            
            public class MethodReference_2 {
                private final Map<String, Integer> map = new HashMap<>();
            
                public MethodReference_2(int i) {
                    map.put("" + i, i);
                }
            
                public int get(String s) {
                    return map.get(s);
                }
            
                @NotNull
                @NotModified
                public Function<String, Integer> getFunction() {
                    return map::get;
                }
            
                @NotNull
                @NotModified
                public Function<String, Integer> getFunction2() {
                    return this::get;
                }
            
                @NotNull
                @NotModified
                public static Function<String, Integer> getFunction3() {
                    return String::length;
                }
            
                @NotNull
                @NotModified
                public static Function<String, Integer> getFunction4() {
                    return java.lang.String::length;
                }
            }
            """;

    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }


    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.Arrays;
            import java.util.Set;
            import java.util.stream.Collectors;
            import java.util.stream.Stream;
            
            // test is here because the crash shows in the method reference "contains"; problem is probably in forwarding of type info
            public class MethodReference_3 {
                public static boolean isEmpty(Object[] array) {
                    return array == null || array.length == 0;
                }
            
                // everything is fine when .parallel is excluded, or the 2nd part of the disjunction is left out
                // k must be a String (T of Stream = String; with parallel in between: = T)
                public Set<String> method(Set<String> keysWithPrefix, String[] pattern) {
                    return keysWithPrefix
                            .stream()
                            .parallel()
                            .filter(k -> isEmpty(pattern) || Arrays.stream(pattern).parallel().anyMatch(k::contains))
                            .collect(Collectors.toSet());
                }
            }
            """;

    @Test
    public void test4() {
        javaInspector.parse(INPUT4);
    }


    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.Set;
            import java.util.stream.Collectors;
            
            public class MethodReference_3B {
                public static boolean isEmpty(Object[] array) {
                    return array == null || array.length == 0;
                }
            
            
                public Set<String> method(Set<String> keysWithPrefix) {
                    return keysWithPrefix
                            .stream()
                            .parallel()
                            .filter("abc"::contains)
                            .collect(Collectors.toSet());
                }
            }
            """;

    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }


    @Language("java")
    private static final String INPUT6 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.Objects;
            import java.util.stream.Collectors;
            import java.util.stream.Stream;
            
            public class MethodReference_3C {
            
                record R2(long l) {}
                record R1(R2 r2) {
                    long l() {
                        return r2.l();
                    }
                }
            
                // result of .map(R1::new) is not of correct type
                private Map<Long, R1> method(List<R2> r2) {
                    Map<Long, R1> result = new HashMap<>();
                    if(r2 != null) {
                        result = r2
                                .stream()
                                .filter(Objects::nonNull)
                                .map(R1::new)
                                .filter(r -> Integer.MIN_VALUE != r.l())
                                .collect(Collectors.toMap(R1::l, x -> x));
                    }
                    return result;
                }
            
                // this one works fine
                private Map<Long, R1> method2(List<R2> r2) {
                    Map<Long, R1> result = new HashMap<>();
                    if(r2 != null) {
                        Stream<R1> r1Stream = r2
                                .stream()
                                .filter(Objects::nonNull)
                                .map(R1::new);
                        result = r1Stream
                                .filter(r -> Integer.MIN_VALUE != r.l())
                                .collect(Collectors.toMap(R1::l, x -> x));
                    }
                    return result;
                }
            }
            """;

    @Test
    public void test6() {
        javaInspector.parse(INPUT6);
    }


    @Language("java")
    private static final String INPUT7 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.List;
            import java.util.Map;
            
            public class MethodReference_4 {
            
                private static void biConsumer(String s, List<Integer> list) {
                    System.out.println(s + ": " + list.size());
                }
            
                public void method(Map<String, List<Integer>> map) {
                    map.forEach(MethodReference_4::biConsumer);
                }
            
                // ---------
                private static String biFunction(String s1, String s2) {
                    return s1.length() > s2.length() ? s1 : s2;
                }
            
                private static Map.Entry<String, String> biFunction(Map.Entry<String, String> e1, Map.Entry<String, String> e2) {
                    return e1.getKey().length() > e2.getKey().length() ? e1 : e2;
                }
            
                public int method2(Map<String, String> map) {
                    return map.entrySet().stream().reduce(MethodReference_4::biFunction).orElseThrow().getKey().length();
                }
            }
            """;

    @Test
    public void test7() {
        javaInspector.parse(INPUT7);
    }


    @Language("java")
    private static final String INPUT8 = """
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import java.util.Collections;
            import java.util.List;
            import java.util.regex.Pattern;
            import java.util.stream.Collectors;
            import java.util.stream.Stream;
            
            public class SourceDocumentFinder {
              private List<File> find(Path sourceDirectory, Pattern sourceDocumentPattern) {
                try (Stream<Path> sourceDocumentCandidates = Files.walk(sourceDirectory)) {
                  return sourceDocumentCandidates
                      .filter(Files::isRegularFile)
                      .filter(path -> sourceDocumentPattern.matcher(path.getFileName().toString()).matches())
                      .filter(
                          path -> {
                            for (Path part : sourceDirectory.relativize(path)) {
                              char firstCharacter = part.toString().charAt(0);
                              if (firstCharacter == '_' || firstCharacter == '.') {
                                return false;
                              }
                            }
                            return true;
                          })
                      .map(Path::toFile)
                      .collect(Collectors.toList());
                } catch (IOException e) {
                  return Collections.emptyList();
                }
              }
            }
            """;

    @DisplayName("method reference with varargs")
    @Test
    public void test8() {
        TypeInfo typeInfo = javaInspector.parse(INPUT8);
        MethodInfo method1 = typeInfo.findUniqueMethod("find", 2);
        ReturnStatement rs = (ReturnStatement) method1.methodBody().statements().getFirst().block().statements().get(0);
        MethodCall collect = (MethodCall) rs.expression();
        MethodCall map = (MethodCall) collect.object();
        MethodCall filter3 = (MethodCall) map.object();
        MethodCall filter2 = (MethodCall) filter3.object();
        MethodCall filter1 = (MethodCall) filter2.object();
        if (filter1.parameterExpressions().getFirst() instanceof MethodReference mr) {
            assertEquals("java.nio.file.Files.isRegularFile(java.nio.file.Path,java.nio.file.LinkOption...)",
                    mr.methodInfo().fullyQualifiedName());
        } else fail();
        VariableExpression ve = (VariableExpression) filter1.object();
        assertEquals("sourceDocumentCandidates", ve.variable().simpleName());
    }
}
