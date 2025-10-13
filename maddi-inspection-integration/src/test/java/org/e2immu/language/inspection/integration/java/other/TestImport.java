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

package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.cst.impl.info.TypePrinterImpl;
import org.e2immu.language.cst.print.FormatterImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestImport extends CommonTest {

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
            import org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel;
            public class Import_0 { public void method() { System.out.println(RLevel.LEVEL + ": " + RMultiLevel.Effective.E1); } }
            """;

    @Test
    public void test0() {
        TypeInfo typeInfo = javaInspector.parse(INPUT0);
        Qualification qualification = javaInspector.runtime().qualificationQualifyFromPrimaryType();
        OutputBuilder ob = new TypePrinterImpl(typeInfo, false).print(new ImportComputerImpl(), qualification, true);
        Formatter formatter = new FormatterImpl(javaInspector.runtime(), FormattingOptionsImpl.DEFAULT);
        String s = formatter.write(ob);

        assertEquals(OUTPUT1, s);
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
        javaInspector.parse(INPUT1);
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
        javaInspector.parse(INPUT2);
    }

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
        javaInspector.parse(INPUT3);
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
        javaInspector.parse(INPUT4);
    }

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
        javaInspector.parse(INPUT5);
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
        javaInspector.parse(INPUT6);
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
        javaInspector.parse(INPUT7);
    }

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
        TypeInfo typeInfo = javaInspector.parse(INPUT8);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
        if (methodInfo.methodBody().statements().get(1) instanceof LocalVariableCreation lvc) {
            TypeInfo findLoopResult = lvc.localVariable().parameterizedType().typeInfo();
            assertEquals("FindLoopResult", findLoopResult.simpleName());
            assertTrue(findLoopResult.parentClass().isJavaLangObject());
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
        javaInspector.parse(INPUT9);
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
        javaInspector.parse(INPUT10);
    }

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
        javaInspector.parse(INPUT11);
    }

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
        javaInspector.parse(INPUT12);
    }

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
        javaInspector.parse(INPUT13);
    }

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
        javaInspector.parse(INPUT14);
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
        javaInspector.parse(INPUT15);
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
        javaInspector.parse(INPUT16);
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
        javaInspector.parse(INPUT17);
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
        TypeInfo X = javaInspector.parse(INPUT18);
        MethodInfo method = X.findUniqueMethod("method", 2);
        assertEquals("Type java.security.cert.Certificate", method.parameters().get(0).parameterizedType().toString());
    }


    @Language("java")
    private static final String INPUT19 = """
            package a.b;
            
            import java.util.LinkedList;
            import java.util.Map;
            import org.springframework.util.ConcurrentReferenceHashMap.Entry;
            import org.springframework.util.ConcurrentReferenceHashMap.Reference;
            
            public class X {
                static class TestWeakConcurrentCache<K, V> extends ConcurrentReferenceHashMap<K, V> {
                    LinkedList<MockReference<K, V>> queue;
                    @Override
                    protected ReferenceManager createReferenceManager() {
                        return new ReferenceManager() {
                            @Override
                            public Reference<K, V> createReference(Entry<K, V> entry, int hash, Reference<K, V> next) {
                                return new MockReference<>(entry, hash, next, TestWeakConcurrentCache.this.queue);
                            }
                            @Override
                            public Reference<K, V> pollForPurge() {
                                return TestWeakConcurrentCache.this.queue.isEmpty() ? null : TestWeakConcurrentCache.this.queue.removeFirst();
                            }
                        };
                    }
            
                }
                static class MockReference<K, V> {
                    public MockReference(Entry<K, V> entry, int hash, Reference<K, V> next, LinkedList<MockReference<K, V>> queue) {
                        // ..
                    }
                    public void method(Entry<K, V> entry) {
                      // ...
                    }
                }
            }
            """;

    @Test
    public void test19() {
        TypeInfo X = javaInspector.parse(INPUT19);
        {
            TypeInfo mockRef = X.findSubType("MockReference");
            MethodInfo method = mockRef.findUniqueMethod("method", 1);
            assertEquals("Type org.springframework.util.ConcurrentReferenceHashMap.Entry<K,V>",
                    method.parameters().getFirst().parameterizedType().toString());
        }
        {
            TypeInfo testWeak = X.findSubType("TestWeakConcurrentCache");
            MethodInfo create = testWeak.findUniqueMethod("createReferenceManager", 0);
            ConstructorCall cc = (ConstructorCall) create.methodBody().statements().getFirst().expression();
            MethodInfo createRef = cc.anonymousClass().findUniqueMethod("createReference", 3);
            assertEquals("Type org.springframework.util.ConcurrentReferenceHashMap.Entry<K,V>",
                    createRef.parameters().getFirst().parameterizedType().toString());
        }
    }


    @Language("java")
    private static final String INPUT20 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.lang.ref.ReferenceQueue;
            import java.lang.ref.WeakReference;
            import java.util.concurrent.ConcurrentMap;

            public class X<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
                protected static final class Entry<K, V> implements Map.Entry<K, V> {
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

                private static final class WeakEntryReference<K, V> extends WeakReference<Entry<K, V>> implements Reference<K, V> {

                		public WeakEntryReference(Entry<K, V> entry, int hash, Reference<K, V> next, ReferenceQueue<Entry<K, V>> queue) {
                			super(entry, queue);
                		}
                	}
            }
            """;

    @Test
    public void test20() {
        TypeInfo X = javaInspector.parse(INPUT20);
        TypeInfo referenceManager = X.findSubType("ReferenceManager");
        MethodInfo createRef = referenceManager.findUniqueMethod("createReference", 3);
        assertEquals("Type a.b.X.Entry<K,V>", createRef.parameters().getFirst().parameterizedType().toString());
        assertEquals("Type a.b.X.Reference<K,V>", createRef.parameters().get(2).parameterizedType().toString());
        TypeInfo weakEntryRef = X.findSubType("WeakEntryReference");
        MethodInfo weakEntryRefConstructor = weakEntryRef.findConstructor(4);
        assertEquals("Type a.b.X.Reference<K,V>", weakEntryRef.interfacesImplemented().getFirst().toString());
        assertEquals("Type a.b.X.Entry<K,V>", weakEntryRefConstructor.parameters().getFirst().parameterizedType().toString());
        assertEquals("Type a.b.X.Reference<K,V>", weakEntryRefConstructor.parameters().get(2).parameterizedType().toString());
    }

}
