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

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestImport extends CommonTest {

    @Language("java")
    private static final String RLEVEL = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public class RLevel {
                public static final String LEVEL = "?";
            }
            """;
    @Language("java")
    private static final String RMULTILEVEL = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public class RMultiLevel {
                public enum Effective {
                    E1, E2;
                    public static Effective of(int index) {
                        return index == 1 ? E1: E2;
                    }
                }
                public enum Level {
                    ONE, TWO, THREE
                }
            }
            """;

    @Language("java")
    private static final String INPUT0 = """
            package org.e2immu.analyser.resolver.testexample;
            import org.e2immu.language.inspection.integration.java.importhelper.RLevel;
            import static org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel.Effective.E1;
            public class Import_0 {
                public void method() {
                    System.out.println(RLevel.LEVEL+": "+E1);
                }
            }
            """;

    @Language("java")
    private static final String OUTPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            import org.e2immu.language.inspection.integration.java.importhelper.RLevel;
            import org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel.Effective;
            public class Import_0 {public void method() { System.out.println(RLevel.LEVEL + ": " + Effective.E1); } }
            """;

    @Language("java")
    private static final String OUTPUT1bis = """
            package org.e2immu.analyser.resolver.testexample;
            import org.e2immu.language.inspection.integration.java.importhelper.RLevel;
            import org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel;
            public class Import_0 {public void method() { System.out.println(RLevel.LEVEL + ": " + RMultiLevel.Effective.E1); } }
            """;
    public static final String RLEVEL_FQN = "org.e2immu.language.inspection.integration.java.importhelper.RLevel";
    public static final String RMULTILEVEL_FQN = "org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel";

    @Test
    public void test0() {
        String I0 = "org.e2immu.analyser.resolver.testexample.Import_0";
        TypeInfo typeInfo = scan(false,
                RLEVEL_FQN, RLEVEL,
                RMULTILEVEL_FQN, RMULTILEVEL,
                I0, INPUT0).get(I0);
        MethodInfo method = typeInfo.findUniqueMethod("method", 0);
        Statement s0 = method.methodBody().statements().getFirst();
        assertEquals("""
                java.lang.System[I]
                java.lang.System[E]
                java.io.PrintStream[I]
                org.e2immu.language.inspection.integration.java.importhelper.RLevel[I]
                org.e2immu.language.inspection.integration.java.importhelper.RLevel[E]
                java.lang.String[I]
                org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel.Effective[I]
                org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel.Effective[E]
                org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel.Effective[I]\
                """, s0.typesReferenced(_ -> true).map(Object::toString)
                .collect(Collectors.joining("\n")));
    }

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            import org.e2immu.language.inspection.integration.java.importhelper.RLevel;
            import static org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel.Effective.E1;
            public class Import_1 {
                public void method() {
                    System.out.println(RLevel.LEVEL+": "+E1);
                }
            }
            """;

    @Test
    public void test1() {
        scan(false, RLEVEL_FQN, RLEVEL,
                RMULTILEVEL_FQN, RMULTILEVEL,
                "org.e2immu.analyser.resolver.testexample.Import_1", INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;
            import org.e2immu.language.inspection.integration.java.importhelper.RLevel;
            
            import static org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel.Effective;
            public class Import_2 {
                public void method() {
                    System.out.println(RLevel.LEVEL+": "+Effective.E1);
                }
            }
            """;

    @Test
    public void test2() {
        scan(false,
                RLEVEL_FQN, RLEVEL,
                RMULTILEVEL_FQN, RMULTILEVEL, "org.e2immu.analyser.resolver.testexample.Import_2", INPUT2);
    }


    @Language("java")
    private static final String RTYPEI = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public interface RTypeInspection {
                enum Methods {
                    A, B, C
                }
            }
            """;

    @Language("java")
    private static final String RTYPEIIMPL = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public class RTypeInspectionImpl implements RTypeInspection {
            }
            """;

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;
            import org.e2immu.language.inspection.integration.java.importhelper.RTypeInspectionImpl;
            public class Import_3 {
                // this is bad coding, we should refer to Methods directly via the interface, as in Import_4
                public void method() {
                    System.out.println(RTypeInspectionImpl.Methods.B);
                }
            }""";

    @Test
    public void test3() {
        scan(false,
                "org.e2immu.language.inspection.integration.java.importhelper.RTypeInspection", RTYPEI,
                "org.e2immu.language.inspection.integration.java.importhelper.RTypeInspectionImpl", RTYPEIIMPL,
                "org.e2immu.analyser.resolver.testexample.Import_3", INPUT3);
    }

    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;
            import org.e2immu.language.inspection.integration.java.importhelper.RTypeInspection;
            public class Import_4 {
                public void method() {
                    System.out.println(RTypeInspection.Methods.B);
                }
            }
            """;

    @Test
    public void test4() {
        scan(false,
                "org.e2immu.language.inspection.integration.java.importhelper.RTypeInspection", RTYPEI,
                "org.e2immu.analyser.resolver.testexample.Import_4", INPUT4);
    }


    @Language("java")
    private static final String REE = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public interface RErasureExpression extends RExpression {
            }
            """;

    @Language("java")
    private static final String REX = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public interface RExpression extends RElement, Comparable<RExpression> {
                enum MethodStatic {
                    A, B, C
                }
            }
            """;

    @Language("java")
    private static final String RE = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public interface RElement {
                enum DescendMode {
                    NO,
                    YES,
                    YES_INCLUDE_THIS
                }
                void doSomething(DescendMode descendMode);
            }
            """;

    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;
            import org.e2immu.language.inspection.integration.java.importhelper.*;
            
            import java.util.Map;
            // very similar to Import_3,4; do not change the * in the imports!!
            public class Import_5 {
                public void method() {
                    Map<String, RErasureExpression.MethodStatic> map = Map.of("abc", RExpression.MethodStatic.B) ;
                }
            }
            """;

    @Test
    public void test5() {
        scan(false,
                "org.e2immu.analyser.resolver.testexample.RElement", RE,
                "org.e2immu.analyser.resolver.testexample.RExpression", REX,
                "org.e2immu.analyser.resolver.testexample.RErasureExpression", REE,
                "org.e2immu.analyser.resolver.testexample.Import_5", INPUT5);
    }

    @Language("java")
    private static final String INPUT6 = """
            package org.e2immu.analyser.resolver.testexample;
            import ch.qos.logback.classic.Level;
            import ch.qos.logback.classic.LoggerContext;
            import org.slf4j.LoggerFactory;
            public class Import_6 {
                public void test() {
                    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                    loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO);
                }
            }
            """;

    @Test
    public void test6() {
        scan("org.e2immu.analyser.resolver.testexample.Import_6", INPUT6);
    }

    @Language("java")
    private static final String INPUT7 = """
            package org.e2immu.analyser.resolver.testexample;
            import java.io.File;
            import java.io.FileOutputStream;
            import java.io.IOException;
            import java.io.OutputStream;
            public class Import_7 {
                public void method() throws IOException {
                    try(OutputStream outputStream = new FileOutputStream(File.createTempFile("x", "txt"))) {
                        outputStream.write(34);
                    }
                }
            }
            """;

    @Test
    public void test7() {
        scan("org.e2immu.analyser.resolver.testexample.Import_7", INPUT7);
    }


    @Language("java")
    private static final String RA = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public interface RAnalysis {
            }
            """;

    @Language("java")
    private static final String RSA = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public interface RStatementAnalysis extends RAnalysis, Comparable<RStatementAnalysis> {
                FindLoopResult create(int line);
                record FindLoopResult(RStatementAnalysis statementAnalysis, int line) {
                }
            }
            """;

    @Language("java")
    private static final String RSAI = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public class RStatementAnalysisImpl implements RStatementAnalysis {
                @Override
                public FindLoopResult create(int line) {
                    return new FindLoopResult(this, line);
                }
                @Override
                public int compareTo(RStatementAnalysis o) {
                    return 0;
                }
            }
            """;

    @Language("java")
    private static final String INPUT8 = """
            package org.e2immu.analyser.resolver.testexample;
            import org.e2immu.language.inspection.integration.java.importhelper.RStatementAnalysisImpl;
            public class Import_8 {
                public void method() {
                    RStatementAnalysisImpl sa = new RStatementAnalysisImpl();
                    RStatementAnalysisImpl.FindLoopResult findLoopResult = sa.create(3);
                }
            }
            """;

    @Test
    public void test8() {
        String I8 = "org.e2immu.analyser.resolver.testexample.Import_8";
        TypeInfo typeInfo = scan(false,
                "org.e2immu.language.inspection.integration.java.importhelper.RAnalysis", RA,
                "org.e2immu.language.inspection.integration.java.importhelper.RStatementAnalysis", RSA,
                "org.e2immu.language.inspection.integration.java.importhelper.RStatementAnalysisImpl", RSAI,
                I8, INPUT8).get(I8);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
        if (methodInfo.methodBody().statements().get(1) instanceof LocalVariableCreation lvc) {
            TypeInfo findLoopResult = lvc.localVariable().parameterizedType().typeInfo();
            assertEquals("FindLoopResult", findLoopResult.simpleName());
            assertEquals("Type Record", findLoopResult.parentClass().toString());
            TypeInfo enclosing = findLoopResult.compilationUnitOrEnclosingType().getRight();
            assertEquals("RStatementAnalysis", enclosing.simpleName());
            assertEquals(2, enclosing.superTypesExcludingJavaLangObject().size());
        } else fail();
    }

    @Language("java")
    private static final String INPUT9 = """
            package org.e2immu.analyser.resolver.testexample;
            import static java.lang.System.out;
            import static java.util.Arrays.stream;
            import static org.junit.jupiter.api.Assertions.assertEquals;
            public class Import_9 {
                public static void test1() {
                    int[] integers = {1, 2, 3};
                    int sum = stream(integers).sum();
                    out.println("Sum is " + sum);
                    assertEquals(6, sum);
                }
            }
            """;

    @Test
    public void test9() {
        scan("org.e2immu.analyser.resolver.testexample.Import_9", INPUT9);
    }

    @Language("java")
    private static final String INPUT10 = """
            package org.e2immu.analyser.resolver.testexample;
            // IMPORTANT: keep this import static...* statement!
            import org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel;
            import java.util.Set;
            import static org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel.Effective.*;
            public class Import_10 {
                record ChangeData(Set<Integer> statementTimes) {
                }
                // Purpose of the test: the "of" method has to belong to "Set" and not to Effective.
                public void method1(int statementTime) {
                    ChangeData changeData = new ChangeData(Set.of(statementTime));
                }
                // completely irrelevant but here we use the enum constants
                public Boolean method2(RMultiLevel.Effective effective) {
                    if(effective == E1) {
                        return true;
                    }
                    if(effective == E2) {
                        return false;
                    }
                    return null;
                }
            }
            """;

    @Test
    public void test10() {
        scan(false, RMULTILEVEL_FQN, RMULTILEVEL,
                "org.e2immu.analyser.resolver.testexample.Import_10", INPUT10);
    }


    @Language("java")
    private static final String II = """
            package org.e2immu.language.inspection.integration.java.importhelper.a;
            import java.util.Iterator;
            public class ImplementsIterable<T> implements Iterable<T> {
                public static final int INT = 3;
                @Override
                public Iterator<T> iterator() {
                    return null;
                }
            }
            """;

    @Language("java")
    private static final String INPUT11 = """
            package org.e2immu.analyser.resolver.testexample;
            import java.util.Map;
            import java.util.TreeMap;
            import java.util.function.BiConsumer;
            import static org.e2immu.language.inspection.integration.java.importhelper.a.ImplementsIterable.INT;
            public class Import_11 {
                interface Variable {
                }
            
                interface DV {
                }
            
                private static class Node {
                    Map<Variable, DV> dependsOn;
                    final Variable variable;
            
                    private Node(Variable v) {
                        variable = v;
                    }
                }
            
                private final Map<Variable, Node> nodeMap = new TreeMap<>();
            
                public void visit(BiConsumer<Variable, Map<Variable, DV>> consumer) {
                    nodeMap.values().forEach(n -> consumer.accept(n.variable, n.dependsOn));
                }
            
                final int I = INT;
            }
            """;

    @Test
    public void test11() {
        scan(false,
                "org.e2immu.language.inspection.integration.java.importhelper.a.ImplementsIterable", II,
                "org.e2immu.analyser.resolver.testexample.Import_11", INPUT11);
    }


    @Language("java")
    private static final String AF = """
            package org.e2immu.language.inspection.integration.java.importhelper.access;
            public interface Filter {
                Result filter(String s);
                enum Result {
                    ACCEPT, NEUTRAL, DENY;
                }
            }
            """;

    @Language("java")
    private static final String AAF = """
            package org.e2immu.language.inspection.integration.java.importhelper.access;
            public abstract class AbstractFilter implements Filter {
            }
            """;

    @Language("java")
    private static final String INPUT12 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import org.e2immu.language.inspection.integration.java.importhelper.access.AbstractFilter;
            import org.e2immu.language.inspection.integration.java.importhelper.access.Filter;
            
            public class Import_12 {
            
                public Filter method() {
                    return new AbstractFilter() {
                        public Result filter(String s) {
                            return Result.ACCEPT;
                        }
                    };
                }
            }
            """;

    @Test
    public void test12() {
        scan(false,
                "org.e2immu.language.inspection.integration.java.importhelper.access.AbstractFilter", AAF,
                "org.e2immu.language.inspection.integration.java.importhelper.access.Filter", AF,
                "org.e2immu.analyser.resolver.testexample.Import_12", INPUT12);
    }


    @Language("java")
    private static final String AEH = """
            package org.e2immu.language.inspection.integration.java.importhelper.a;
            public class ErrorHandler {
                public static int handle(String s) {
                    return s.length();
                }
            }
            """;
    @Language("java")
    private static final String BEH = """
            package org.e2immu.language.inspection.integration.java.importhelper.b;
            public interface ErrorHandler {
                void error(String s);
            }
            """;

    @Language("java")
    private static final String IEH = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            import org.e2immu.language.inspection.integration.java.importhelper.b.ErrorHandler;
            public class ImplementsErrorHandler implements ErrorHandler {
                @Override
                public void error(String s) {
                }
            }
            """;


    // priority of imports: the explicit ErrorHandler in 'a' gets priority over the supertype of ImplementsErrorHandler
    // which lives in 'b'
    @Language("java")
    private static final String INPUT13 = """
            package org.e2immu.analyser.resolver.testexample;
            import org.e2immu.language.inspection.integration.java.importhelper.ImplementsErrorHandler;
            import org.e2immu.language.inspection.integration.java.importhelper.a.ErrorHandler;
            public class Import_13 {
                ImplementsErrorHandler errorHandler = new ImplementsErrorHandler();
                public int method(String s) {
                  return  ErrorHandler.handle(s);
                }
            }
            """;

    @Test
    public void test13() {
        scan(false,
                "org.e2immu.language.inspection.integration.java.importhelper.a.ErrorHandler", AEH,
                "org.e2immu.language.inspection.integration.java.importhelper.b.ErrorHandler", BEH,
                "org.e2immu.language.inspection.integration.java.importhelper.ImplementsErrorHandler", IEH,
                "org.e2immu.analyser.resolver.testexample.Import_13", INPUT13);
    }

    @Language("java")
    private static final String PROPERTIES = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public class Properties {
                public static final String P = "p";
                public static String p() {
                    return P;
                }
                public String method(int i) {
                    return "hello " + i;
                }
            }
            """;


    // priority of explicit import over * import lower down
    @Language("java")
    private static final String INPUT14 = """
            package org.e2immu.analyser.resolver.testexample;
            import org.e2immu.language.inspection.integration.java.importhelper.Properties;
            // IMPORTANT: keep the "import java.util.*" here, do not "Organize imports" it away.
            import java.util.*;
            public class Import_14 {
                public String method() {
                   Properties properties = new  Properties();
                   return properties.method(3);
                }
            }
            """;

    @Test
    public void test14() {
        scan(false,
                "org.e2immu.language.inspection.integration.java.importhelper.Properties", PROPERTIES,
                "org.e2immu.analyser.resolver.testexample.Import_14", INPUT14);
    }

    @Language("java")
    private static final String INPUT15 = """
            package org.e2immu.analyser.resolver.testexample;
            // NO IMPORTS HERE!!
            public class Import_15 {
                public String method1() {
                    return org.e2immu.language.inspection.integration.java.importhelper.Properties.P;
                }
                public String method2() {
                    return  org.e2immu.language.inspection.integration.java.importhelper.Properties.p();
                }
            }
            """;

    @Test
    public void test15() {
        scan(false,
                "org.e2immu.language.inspection.integration.java.importhelper.Properties", PROPERTIES,
                "org.e2immu.analyser.resolver.testexample.Import_15", INPUT15);
    }

    @Language("java")
    private static final String INPUT16 = """
            package org.e2immu.analyser.resolver.testexample;
            import java.util.LinkedList;
            import java.util.List;
            import java.util.NavigableSet;
            public class Import_16 {
            
                // NOTE: j.u.NavigableSet derives from j.u.SortedSet!
                interface SortedSet<T> extends NavigableSet<T> {
                }
            
                public void method(List<NavigableSet<String>> in, SortedSet<Integer> set) {
                    List list = new LinkedList();
                    in.stream().map(s -> s.headSet("a")).forEach(s -> list.add(s));
                    System.out.println(set);
                }
            
            }
            """;

    @Test
    public void test16() {
        scan("org.e2immu.analyser.resolver.testexample.Import_16", INPUT16);
    }


    @Language("java")
    private static final String INPUT17 = """
            package a.b;
            public class X {
                public static int size(java.util.List<java.util.Set<Integer>> listOfSets) {
                    return listOfSets.stream().mapToInt(java.util.Collection::size).sum();
                }
            }
            """;

    @Test
    public void test17() {
        scan("a.b.X", INPUT17);
    }

    @Language("java")
    private static final String INPUT18 = """
            import java.io.DataOutputStream;
            import java.io.IOException;
            import java.security.*; // unused, j.s.c.Certificate has priority over j.s.Certificate!!!
            import java.security.cert.Certificate;
            import java.security.cert.CertificateEncodingException;
            
            public class X {
            
                public void method(Certificate cert, DataOutputStream dOut) throws IOException {
                    try {
                        byte[] cEnc = cert.getEncoded();
                        dOut.writeUTF(cert.getType());
                        dOut.writeInt(cEnc.length);
                        dOut.write(cEnc);
                    } catch (CertificateEncodingException ex) {
                        throw new IOException(ex.toString());
                    }
                }
            }
            """;

    @DisplayName("Override asterisk import with explicit one")
    @Test
    public void test18() {
        TypeInfo X = scan("X", INPUT18);
        MethodInfo method = X.findUniqueMethod("method", 2);
        assertEquals("Type java.security.cert.Certificate", method.parameters().get(0).parameterizedType().toString());
    }

    @Language("java")
    private static final String INPUT20 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.lang.ref.ReferenceQueue;
            import java.lang.ref.WeakReference;
            import java.util.concurrent.ConcurrentMap;
            
            public abstract class X<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
                protected abstract class Entry<K, V> implements Map.Entry<K, V> {
                }
                protected interface Reference<K, V> {
                    // ...
                }
                protected class ReferenceManager {
                 private final ReferenceQueue<Entry<K, V>> queue = new ReferenceQueue<>();
            
                    public Reference<K, V> createReference(Entry<K, V> entry, int hash, Reference<K, V> next) {
            
                        return new WeakEntryReference<>(entry, hash, next, this.queue);
            
                    }
                }
            
                private class WeakEntryReference<K, V> extends WeakReference<Entry<K, V>> implements Reference<K, V> {
            
                		public WeakEntryReference(Entry<K, V> entry, int hash, Reference<K, V> next, ReferenceQueue<Entry<K, V>> queue) {
                			super(entry, queue);
                		}
                	}
            }
            """;

    @Test
    public void test20() {
        TypeInfo X = scan("a.b.X", INPUT20);
        TypeInfo referenceManager = X.findSubType("ReferenceManager");
        MethodInfo createRef = referenceManager.findUniqueMethod("createReference", 3);
        assertEquals("a.b.X.Entry", createRef.parameters().getFirst().parameterizedType().typeInfo().toString());
        assertEquals("Type a.b.X.Reference<K,V>", createRef.parameters().get(2).parameterizedType().toString());
        TypeInfo weakEntryRef = X.findSubType("WeakEntryReference");
        MethodInfo weakEntryRefConstructor = weakEntryRef.findConstructor(4);
        assertEquals("Type a.b.X.Reference<K,V>", weakEntryRef.interfacesImplemented().getFirst().toString());
        assertEquals("Type Entry<K,V>", weakEntryRefConstructor.parameters().getFirst().parameterizedType().toString());
        ParameterInfo pi = weakEntryRefConstructor.parameters().get(2);
        assertEquals("Type a.b.X.Reference<K,V>", pi.parameterizedType().toString());

        assertEquals("source::a.b.X.Reference<K,V>", pi.parameterizedType().descriptor());
        ParameterizedType pt = pi.parameterizedType().withParameters(List.of(runtime.integerTypeInfo().asParameterizedType(),
                runtime.stringParameterizedType().copyWithArrays(1)));
        assertEquals("source::a.b.X.Reference<java.lang.Integer,java.lang.String[]>", pt.descriptor());
    }

}
