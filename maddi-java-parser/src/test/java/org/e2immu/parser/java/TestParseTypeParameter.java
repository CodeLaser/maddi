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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseTypeParameter extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public interface C<T> {
              T t();
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo t = typeInfo.findUniqueMethod("t", 0);
        assertEquals("T", t.returnType().fullyQualifiedName());
        assertTrue(t.returnType().isTypeParameter());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public interface C<T, @SuppressWarnings("?") U extends Class<?>> {
              T t();
              U u();
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = parse(INPUT2);
        MethodInfo u = typeInfo.findUniqueMethod("u", 0);
        ParameterizedType pt = u.returnType();
        TypeParameter tp = pt.typeParameter();
        assertEquals("U", tp.simpleName());
        assertNull(pt.typeInfo());
        assertNull(pt.wildcard());
        assertTrue(pt.parameters().isEmpty());
        assertTrue(pt.isTypeParameter());
        assertEquals("main::a.b.C[U]", pt.typeParameter().descriptor());

        assertEquals(1, tp.typeBounds().size());
        ParameterizedType typeBound = tp.typeBounds().getFirst();
        assertEquals(1, typeBound.parameters().size());
        assertSame(runtime.parameterizedTypeWildcard(), typeBound.parameters().getFirst());
        assertNull(typeBound.wildcard());
        assertNull(typeBound.typeParameter());
        assertEquals("java.lang.Class", typeBound.typeInfo().fullyQualifiedName());
        assertEquals("@SuppressWarnings(\"?\") U extends Class<?>", pt.fullyQualifiedName());
    }

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;
            
            public class TypeParameter_3 {
                enum Visibility {
                    NONE;
                }
            
                interface SerializationConfig {
                    VisibilityChecker<?> getDefaultVisibilityChecker();
                }
            
                // from com.fasterxml.jackson.databind.introspect
                interface VisibilityChecker<T extends VisibilityChecker<T>> {
                    T withGetterVisibility(Visibility v);
            
                    T withSetterVisibility(Visibility v);
                }
            
                static class ObjectMapper {
                    public void setVisibilityChecker(VisibilityChecker<?> vc) {
            
                    }
            
                    public SerializationConfig getSerializationConfig() {
                        return null;
                    }
            
                }
            
                private final ObjectMapper mapper = new ObjectMapper();
            
                // CRASH
                public void method1() {
                     mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker().
                             withGetterVisibility(Visibility.NONE).
                             withSetterVisibility(Visibility.NONE));
                 }
            
                // NO METHOD FOUND
                public void method2() {
                    VisibilityChecker<?> o = mapper.getSerializationConfig().getDefaultVisibilityChecker().
                            withGetterVisibility(Visibility.NONE);
                    mapper.setVisibilityChecker(o.withSetterVisibility(Visibility.NONE));
                }
            
                public void method3() {
                    VisibilityChecker<?> o = mapper.getSerializationConfig().getDefaultVisibilityChecker().
                            withGetterVisibility(Visibility.NONE);
                    VisibilityChecker<?> vc = o.withSetterVisibility(Visibility.NONE);
                    mapper.setVisibilityChecker(vc);
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = parse(INPUT3);
        TypeInfo objectMapper = typeInfo.findSubType("ObjectMapper");
        MethodInfo setVc = objectMapper.findUniqueMethod("setVisibilityChecker", 1);
        MethodInfo u = typeInfo.findUniqueMethod("method1", 0);
        if (u.methodBody().statements().getFirst() instanceof ExpressionAsStatement eas && eas.expression() instanceof MethodCall mc) {
            assertSame(setVc, mc.methodInfo());
        } else fail();
    }

    @Language("java")
    public static final String INPUT4 = """
            package a.b;
            import java.util.Hashtable;
            class X {
            public static <K, V> Hashtable<K, V> copy(Hashtable<K, V> map) {
              Hashtable<K, V> copy = new Hashtable<K, V>();
              return copy;
            }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = parse(INPUT4);
        MethodInfo copy = typeInfo.findUniqueMethod("copy", 1);
        assertEquals(2, copy.typeParameters().size());
        assertEquals("a.b.X.copy(java.util.Hashtable<K,V>)", copy.fullyQualifiedName());
        assertEquals("""
                public static <K,V> Hashtable<K,V> copy(Hashtable<K,V> map){Hashtable<K,V> copy=new Hashtable<K,V>();return copy;}\
                """, copy.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        Block newBody = runtime.emptyBlock();
        MethodInfo copyNewBody = copy.withMethodBody(newBody);
        assertEquals("""
                public static <K,V> Hashtable<K,V> copy(Hashtable<K,V> map){}\
                """, copyNewBody.print(runtime.qualificationQualifyFromPrimaryType()).toString());
    }


    @Language("java")
    public static final String INPUT5 = """
            package a.b;
            class X {
              class Class$<@SuppressWarnings("?") T, U, V> {
            
              }
            }
            """;

    @Test
    public void test5() {
        TypeInfo typeInfo = parse(INPUT5, true);
        TypeInfo clazz = typeInfo.findSubType("Class$");
        TypeParameter tp0 = clazz.typeParameters().getFirst();
        assertEquals(1, tp0.annotations().size());

        TypeParameter tp1 = clazz.typeParameters().get(1);
        TypeParameter tp2 = clazz.typeParameters().getLast();

        DetailedSources ds0 = tp0.source().detailedSources();
        assertEquals("3-40:3-40", ds0.detail(DetailedSources.SUCCEEDING_COMMA).compact2());
        assertNull(ds0.detail(DetailedSources.PRECEDING_COMMA));
        DetailedSources ds1 = tp1.source().detailedSources();
        assertEquals("3-40:3-40", ds1.detail(DetailedSources.PRECEDING_COMMA).compact2());
        assertEquals("3-43:3-43", ds1.detail(DetailedSources.SUCCEEDING_COMMA).compact2());
        DetailedSources ds2 = tp2.source().detailedSources();
        assertEquals("3-43:3-43", ds2.detail(DetailedSources.PRECEDING_COMMA).compact2());
        assertNull(ds2.detail(DetailedSources.SUCCEEDING_COMMA));
    }

}
