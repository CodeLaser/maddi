/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.MethodAnalyzer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestLambda extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.stream.IntStream;
            public class X {
                void method(byte[] b) {
                    IntStream.iterate(0, i -> i < b.length, i -> i + 1);
                }
            }
            """;


    @DisplayName("lambda parameters")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 1);
        Statement s0 = method.methodBody().statements().get(0);
        VariableData vd0 = VariableDataImpl.of(s0);
        // we don't want the variables that have been created in the lambda. We do get b
        assertEquals("a.b.X.method(byte[]):0:b", vd0.knownVariableNamesToString());
        Lambda lambda = (Lambda) ((MethodCall) s0.expression()).parameterExpressions().get(1);
        assertTrue(lambda.methodInfo().typeInfo().analysis().getOrDefault(MethodAnalyzer.VARIABLES_OF_ENCLOSING_METHOD,
                MethodAnalyzer.EMPTY_VARIABLE_INFO_MAP).isEmpty());
        assertEquals("a.b.X.$0.test(int)", lambda.methodInfo().fullyQualifiedName());
        Statement l0 = lambda.methodInfo().methodBody().statements().get(0);
        assertEquals("return i<b.length;", l0.print(runtime.qualificationFullyQualifiedNames()).toString());
        VariableData vdL0 = VariableDataImpl.of(l0);
        assertEquals("a.b.X.$0.test(int), a.b.X.$0.test(int):0:i, a.b.X.method(byte[]):0:b",
                vdL0.knownVariableNamesToString());

        Lambda lambda2 = (Lambda) ((MethodCall) s0.expression()).parameterExpressions().get(2);
        assertEquals("a.b.X.$1.applyAsInt(int)", lambda2.methodInfo().fullyQualifiedName());
    }


    @Language("java")
    private static final String INPUT2 = """
            import java.io.*;
            import java.util.*;
            
            public class Function10927145_file1830095 {
            
                public static void method(final String extension, File currentDir, List<File> filesList) {
                    String lc = extension.toLowerCase();
                    String[] files = currentDir.list(new FilenameFilter() {
            
                        public boolean accept(File dir, String name) {
                            File f = new File(dir, name);
                            return f.isDirectory() || name.endsWith(lc);
                        }
                    });
                    for (String filename : files) {
                        File f = new File(currentDir, filename);
                        if (f.isDirectory())
                            method(extension, f, filesList);
                        else
                            filesList.add(f);
                    }
                }
            }
            """;


    @DisplayName("created in lambda")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 3);
        LocalVariableCreation lvc = (LocalVariableCreation) method.methodBody().statements().get(1);
        MethodCall mc = (MethodCall) lvc.localVariable().assignmentExpression();
        ConstructorCall cc = (ConstructorCall) mc.parameterExpressions().get(0);
        TypeInfo anon = cc.anonymousClass();
        assertEquals("lc", anon.analysis().getOrDefault(MethodAnalyzer.VARIABLES_OF_ENCLOSING_METHOD,
                MethodAnalyzer.EMPTY_VARIABLE_INFO_MAP).sortedByFqn());
        VariableData vd1 = VariableDataImpl.of(lvc);
        VariableInfo vi1Lc = vd1.variableInfo("lc");
        assertEquals("1", vi1Lc.reads().toString());
        assertEquals("D:0, A:[0]", vi1Lc.assignments().toString());
    }

    @Language("java")
    private static final String INPUT3 = """
            import java.io.File;
            import java.net.URI;
            
            public class X {
            
                public void open(File file) throws Exception {
                    if (file != null) {
                        File fixedFile = new File(file.getAbsoluteFile().toString()) {
            
                            public URI toURI() {
                                try {
                                    return new URI("file://" + getAbsolutePath());
                                } catch (Exception e) {
                                    return super.toURI();
                                }
                            }
                        };
                    }
                }
            }
            """;

    @DisplayName("created in anonymous type, with parameters in constructor call")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);
        MethodInfo methodInfo = X.findUniqueMethod("open", 1);
        VariableData vd = VariableDataImpl.of(methodInfo.methodBody().statements().get(0).block().statements().get(0));
        // important: test 'file' parameter present
        assertEquals("X.open(java.io.File):0:file, fixedFile", vd.knownVariableNamesToString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.stream.IntStream;
            public class X {
                void method(byte[] b) {
                    int max = b.length/2;
                    IntStream.iterate(0, i -> i < max, i -> i + 1);
                }
            }
            """;


    @DisplayName("lambda parameters and closure")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 1);
        Statement s1 = method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        // we don't want the variables that have been created in the lambda. We do get b, max
        assertEquals("a.b.X.method(byte[]):0:b, max", vd1.knownVariableNamesToString());
        Lambda lambda = (Lambda) ((MethodCall) s1.expression()).parameterExpressions().get(1);
        assertEquals("max", lambda.methodInfo().typeInfo().analysis()
                .getOrDefault(MethodAnalyzer.VARIABLES_OF_ENCLOSING_METHOD,
                        MethodAnalyzer.EMPTY_VARIABLE_INFO_MAP).sortedByFqn());
        assertEquals("a.b.X.$0.test(int)", lambda.methodInfo().fullyQualifiedName());
        Statement l0 = lambda.methodInfo().methodBody().statements().get(0);
        assertEquals("return i<max;", l0.print(runtime.qualificationFullyQualifiedNames()).toString());
        VariableData vdL0 = VariableDataImpl.of(l0);
        assertEquals("a.b.X.$0.test(int), a.b.X.$0.test(int):0:i, max", vdL0.knownVariableNamesToString());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.stream.IntStream;
            public class X {
                void method(int k) {
                    int[] max = new int[] { k };
                    IntStream.iterate(0, i -> i < max[0], i -> i + 1);
                }
            }
            """;


    @DisplayName("lambda parameters and closure, 2")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 1);
        Statement s1 = method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        // we don't want the variables that have been created in the lambda. We do get b, max
        assertEquals("a.b.X.method(int):0:k, max, max[0]", vd1.knownVariableNamesToString());
        Lambda lambda = (Lambda) ((MethodCall) s1.expression()).parameterExpressions().get(1);
        assertEquals("max", lambda.methodInfo().typeInfo().analysis()
                .getOrDefault(MethodAnalyzer.VARIABLES_OF_ENCLOSING_METHOD,
                        MethodAnalyzer.EMPTY_VARIABLE_INFO_MAP).sortedByFqn());
        assertEquals("a.b.X.$0.test(int)", lambda.methodInfo().fullyQualifiedName());
        Statement l0 = lambda.methodInfo().methodBody().statements().get(0);
        assertEquals("return i<max[0];", l0.print(runtime.qualificationFullyQualifiedNames()).toString());
        VariableData vdL0 = VariableDataImpl.of(l0);
        assertEquals("a.b.X.$0.test(int), a.b.X.$0.test(int):0:i, max, max[0]", vdL0.knownVariableNamesToString());

        VariableInfo max0 = vd1.variableInfo("max[0]");
        assertEquals("1", max0.reads().toString());
        assertEquals("D:0, A:[]", max0.assignments().toString());

        VariableInfo max = vd1.variableInfo("max");
        assertEquals("1", max.reads().toString());
        assertEquals("D:0, A:[0]", max.assignments().toString());
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.util.stream.IntStream;
            import java.util.List;
            
            public abstract class X {
            
                private record R(String r) {}
                private record Wrapper(String w) {}
                abstract List<Wrapper> diff(Wrapper[] w1, Wrapper[] w2);
            
                public List<Wrapper> diffLines(List<R> list1, List<R> list2) {
                    Wrapper[] r1 = list1.stream().map(r -> new Wrapper(r.r)).toArray(Wrapper[]::new);
                    Wrapper[] r2 = list2.stream().map(r -> new Wrapper(r.r)).toArray(Wrapper[]::new);
                    List<Wrapper> rawDiffs = diff(r1, r2);
                    for (int i = 0; i < rawDiffs.size(); ++i) {
                        System.out.println(rawDiffs.get(i));
                    }
                    return List.copyOf(rawDiffs);
                }
            }
            """;


    @DisplayName("lambda parameters and closure, 3")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("diffLines", 2);
        Statement s1 = method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        // we don't want the variables that have been created in the lambda. We do get b, max
        assertEquals("""
                a.b.X.diffLines(java.util.List<a.b.X.R>,java.util.List<a.b.X.R>):0:list1, a.b.X.diffLines(java.util.List<a.b.X.R>,java.util.List<a.b.X.R>):1:list2, r1, r2\
                """, vd1.knownVariableNamesToString());
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.util.function.Function;
            public class X {
                Long m(Function<String, Long> function) {
                    return function.apply("s");
                }
                record  R(long id) {}
                R makeR(String s) {
                    return new R(s.length());
                }
                void method() {
                    Long id = m(status -> {
                      String string = status.toLowerCase();
                      return makeR(string).id;
                    });
                    int[] array = { 10, 20, 0, 15, 0, 5 };
                    for (int i = 0; i < array.length; i++) {
                      final int index = i;
                      array[index] ++;
                    }
                }
            }
            """;

    @DisplayName("variable in lambda")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

    }

}
