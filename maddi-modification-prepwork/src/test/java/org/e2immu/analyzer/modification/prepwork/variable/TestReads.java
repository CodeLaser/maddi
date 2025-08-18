package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.statement.WhileStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestReads extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class X {
                public static int arrayIndexOf(byte[] array, byte[] subsequence, int fromIndex) {
                    if (subsequence.length == 0)
                        return 0;
                    else {
                        int j;
                        int stop = (array.length - subsequence.length) + 1;
                        while (fromIndex < stop) {
                            if (array[fromIndex] == subsequence[0]) {
                                j = 1;
                                while (j < subsequence.length) {
                                    if (array[fromIndex + j] == subsequence[j])
                                        j++;
                                    else
                                        break;
                                }
                                if (j == subsequence.length)
                                    return fromIndex;
                            }
                            fromIndex++;
                        }
                        return -1;
                    }
                }
            }
            """;

    @DisplayName("nested while, array access")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo method = X.findUniqueMethod("arrayIndexOf", 3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        IfElseStatement ie0 = (IfElseStatement) method.methodBody().statements().get(0);
        Statement outerWhile = ie0.elseBlock().statements().get(2);
        WhileStatement innerWhile = (WhileStatement) outerWhile.block().statements().get(0).block().statements().get(1);
        IfElseStatement ieInInner = (IfElseStatement) innerWhile.block().statements().get(0);
        VariableData vd = VariableDataImpl.of(ieInInner);
        ParameterInfo fromIndex = method.parameters().get(2);
        VariableInfo viFromIndex = vd.variableInfo(fromIndex);
        assertEquals("0.1.2-E, 0.1.2.0.0-E, 0.1.2.0.0.0.1.0.0-E, 0.1.2;E", viFromIndex.reads().toString());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Arrays;
            public class X {
                public static float method(float[] array, float f) {
                   return Arrays.stream(array).filter(v -> {
                       System.out.println("in filter, value "+v);
                       return v == f;
                   }).findFirst().orElseThrow();
                }
            }
            """;

    @DisplayName("reads in lambdas")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        Statement returnStatement = method.methodBody().lastStatement();
        VariableData vd = VariableDataImpl.of(returnStatement);
        ParameterInfo f = method.parameters().get(1);
        VariableInfo vif = vd.variableInfo(f);
        assertEquals("0", vif.reads().toString());

        MethodCall mc1 = (MethodCall) returnStatement.expression();
        MethodCall mc2 = (MethodCall) mc1.object();
        MethodCall mc3 = (MethodCall) mc2.object();
        Lambda lambda = (Lambda) mc3.parameterExpressions().get(0);
        MethodInfo test = lambda.methodInfo();
        Statement return2 = test.methodBody().lastStatement();

        VariableData vd2 = VariableDataImpl.of(return2);
        VariableInfo vif2 = vd2.variableInfo(f);
        assertEquals("1", vif2.reads().toString());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Arrays;import java.util.function.Predicate;
            public class X {
                static class Auto implements AutoCloseable {
                    Auto(float f) { }
                    @Override
                    public void close() {
                    }
                }
                public static float method(float[] array, float f) {
                    try(Auto a = new Auto(f)) {
                       return Arrays.stream(array).filter(new Predicate<Float>() {
                           @Override
                           public boolean test(Float v) {
                               System.out.println("in filter, value "+v);
                               return internalEquals(v);
                           }
                           private boolean internalEquals(float v) {
                               return f == v;
                           }
                       }).findFirst().orElseThrow();
                   }
                }
            }
            """;

    @DisplayName("reads in anonymous type + try/resource")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        Statement returnStatement = method.methodBody().lastStatement().block().lastStatement();
        VariableData vd = VariableDataImpl.of(returnStatement);
        ParameterInfo f = method.parameters().get(1);
        VariableInfo vif = vd.variableInfo(f);
        assertEquals("0+0, 0.0.0", vif.reads().toString());

        MethodCall mc1 = (MethodCall) returnStatement.expression();
        MethodCall mc2 = (MethodCall) mc1.object();
        MethodCall mc3 = (MethodCall) mc2.object();
        ConstructorCall cc = (ConstructorCall) mc3.parameterExpressions().get(0);
        MethodInfo test = cc.anonymousClass().findUniqueMethod("internalEquals", 1);
        Statement return2 = test.methodBody().lastStatement();

        VariableData vd2 = VariableDataImpl.of(return2);
        VariableInfo vif2 = vd2.variableInfo(f);
        assertEquals("0", vif2.reads().toString());
    }

    @Language("java")
    public static final String INPUT4 = """
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import java.util.List;
            import java.util.regex.Pattern;
            import java.util.stream.Collectors;
            import java.util.stream.Stream;
            class X{
                private List<File> find(Path sourceDirectory) {
                    try(Stream<Path> sourceDocumentCandidates=Files.walk(sourceDirectory)) {
                        return sourceDocumentCandidates.filter(Files::isRegularFile)
                            .filter(path -> {
                                String s = sourceDirectory.relativize(path).toString();
                                return s.isEmpty();
                            })
                            .map(Path::toFile).collect(Collectors.toList());
                    } catch(IOException e){
                        throw new RuntimeException();
                    }
                }
            }
            """;

    @DisplayName("variant with LVC, that fails")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.doPrimaryType(X);
        MethodInfo find = X.findUniqueMethod("find", 1);
        ParameterInfo sourceDirectory = find.parameters().get(0);
        Statement last = find.methodBody().lastStatement();
        VariableData vdLast = VariableDataImpl.of(last);
        VariableInfo viSd = vdLast.variableInfo(sourceDirectory);
        assertEquals("0+0, 0.0.0", viSd.reads().toString());
    }

    @Language("java")
    public static final String INPUT5 = """
            import java.io.IOException;
            import java.io.InputStream;
            import java.nio.ByteBuffer;
            import java.nio.channels.ReadableByteChannel;
            
            public class X {
            
                public static ReadableByteChannel newChannel(final InputStream in) {
                    return new ReadableByteChannel() {
            
                        public boolean open = true;
            
                        public int read(ByteBuffer dst) throws IOException {
                            assert dst.hasArray();
                            byte[] array = dst.array();
                            int arrayOffset = dst.arrayOffset();
                            int totalRead = 0;
                            int bytesRead = 0;
                            int bytesToRead;
                            while ((bytesToRead = dst.remaining()) > 0) {
                                if ((totalRead > 0) && !(in.available() > 0)) {
                                    break;
                                }
                                int pos = dst.position();
                                bytesRead = in.read(array, arrayOffset + pos, bytesToRead);
                                if (bytesRead < 0) {
                                    break;
                                } else {
                                    dst.position(pos + bytesRead);
                                    totalRead += bytesRead;
                                }
                            }
                            if ((bytesRead < 0) && (totalRead == 0)) {
                                return -1;
                            }
                            return totalRead;
                        }
            
                        public boolean isOpen() {
                            return open;
                        }
            
                        public void close() throws IOException {
                            in.close();
                            open = false;
                        }
                    };
                }
            }
            """;

    @DisplayName("copyReadsFromAnonymousMethod for field initializer 'true'")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.doPrimaryType(X);
    }

}
