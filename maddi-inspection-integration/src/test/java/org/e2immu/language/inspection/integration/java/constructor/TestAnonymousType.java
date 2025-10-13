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

package org.e2immu.language.inspection.integration.java.constructor;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnonymousType extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;
            import java.util.function.Predicate;

            public class AnonymousType_0 {

                private String mask;

                public static List<String> method(List<String> list, String mask) {
                    Predicate<String> stringPredicate = new Predicate<String>() {
                        @Override
                        public boolean test(String s) {
                            // must be the 'mask' parameter, not the field!
                            return !s.isEmpty() && s.charAt(0) == mask.charAt(0);
                        }
                    };
                    return list.stream().filter(stringPredicate).toList();
                }

                class NonStatic {
                    @Override
                    public String toString() {
                        // at the same time, the 'mask' field must be accessible
                        return mask;
                    }
                }
            }
            """;


    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        if (method.methodBody().statements().get(0) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            TypeInfo anon = cc.anonymousClass();
            MethodInfo test = anon.findUniqueMethod("test", 1);
            if (test.methodBody().statements().get(0) instanceof ReturnStatement rs
                && rs.expression() instanceof And and
                && and.expressions().get(1) instanceof BinaryOperator eq
                && eq.rhs() instanceof MethodCall mc) {
                Expression object = mc.object();
                if (object instanceof VariableExpression ve) {
                    assertInstanceOf(ParameterInfo.class, ve.variable());
                } else fail();
            } else {
                fail();
            }
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;


            import java.util.ArrayList;
            import java.util.List;
            import java.util.function.Supplier;

            // copied from Independent1_10; issues with functional interface computation
            public class AnonymousType_1<T> {
                List<T> list = new ArrayList<>();

                @SafeVarargs
                static <T> AnonymousType_1<T> of(T... ts) {
                    AnonymousType_1<T> result = new AnonymousType_1<>();
                    result.fill(new Supplier<>() {
                        int i;

                        @Override
                        public T get() {
                            return i < ts.length ? ts[i++] : null;
                        }
                    });
                    return result;
                }

                private void fill(Supplier<T> supplier) {
                    T t;
                    while ((t = supplier.get()) != null) list.add(t);
                }
            }
            """;


    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.io.*;
            public class X {
                public InputStream readFully(InputStream in, String charset, StringBuilder sb) throws IOException {
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    InputStream is = new FilterInputStream(in) {

                        @Override
                        public int read() throws IOException {
                            int read = super.read();
                            if (read >= 0)
                                bos.write(read);
                            return read;
                        }

                        @Override
                        public int read(byte[] arg0, int arg1, int arg2) throws IOException {
                            int read = super.read(arg0, arg1, arg2);
                            if (read > 0)
                                bos.write(arg0, arg1, read);
                            return read;
                        }
                    };
                    // some code commented out
                    bos.close();
                    return new ByteArrayInputStream(bos.toByteArray());
                }
            }
            """;

    @DisplayName("New anonymous class with argument")
    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }


    @Language("java")
    private static final String INPUT3b = """
            package a.b;
            import java.io.*;
            public class X {
                public InputStream readFully(InputStream in, String charset, StringBuilder sb) throws IOException {
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    Reader reader = new InputStreamReader(new FilterInputStream(in) {

                        @Override
                        public int read() throws IOException {
                            int read = super.read();
                            if (read >= 0)
                                bos.write(read);
                            return read;
                        }

                        @Override
                        public int read(byte[] arg0, int arg1, int arg2) throws IOException {
                            int read = super.read(arg0, arg1, arg2);
                            if (read > 0)
                                bos.write(arg0, arg1, read);
                            return read;
                        }
                    }, charset);
                    int read;
                    while ((read = reader.read()) >= 0) {
                        sb.append((char) read);
                    }
                    bos.close();
                    return new ByteArrayInputStream(bos.toByteArray());
                }
            }
            """;

    @DisplayName("New anonymous class with argument, inside other constructor call")
    @Test
    public void test3b() {
        TypeInfo X = javaInspector.parse(INPUT3b);
        MethodInfo mi = X.findUniqueMethod("readFully", 3);
        LocalVariableCreation lvcReader = (LocalVariableCreation) mi.methodBody().statements().get(1);
        ConstructorCall newInputStreamReader = (ConstructorCall) lvcReader.localVariable().assignmentExpression();
        ConstructorCall newFIS = (ConstructorCall) newInputStreamReader.parameterExpressions().get(0);
        assertEquals("in", newFIS.parameterExpressions().get(0).toString());
        TypeInfo anon = newFIS.anonymousClass();
        assertNotNull(anon);
        MethodInfo read = anon.findUniqueMethod("read", 0);
        assertTrue(read.isPublic()); // overrides interface method, so must be public
        assertFalse(read.isPubliclyAccessible()); // because in private anonymous type
        LocalVariableCreation readLvc = (LocalVariableCreation) read.methodBody().statements().get(0);
        MethodCall callSuper = (MethodCall) readLvc.localVariable().assignmentExpression();
        assertEquals("java.io.FilterInputStream.read()", callSuper.methodInfo().fullyQualifiedName());
    }
}
