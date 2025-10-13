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

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestConstructor2 extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class Key<K,V> {
              private final K key;
              private final Class<V> clazz;
              private Key(K key, Class<V> clazz){
                this.key = key;
                this.clazz = clazz;
              }
              public static <K,V> Key<K,V> create(K key, Class<V> clazz){
                return new Key<K,V>(key, clazz);
              }
            }
            """;

    @Test
    public void test1() {
        javaInspector.parse(INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class X {
              record Pair<F, G>(F f, G g) {
              }
            
              record R<F, G>(Pair<F, G> pair) {
                public R {
                  assert pair != null;
                }
              }
            
              static <X, Y> R<Y, X> reverse7(R<X, Y> r) {
                return new R(new Pair(r.pair.g, r.pair.f));
              }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        MethodInfo reverse7 = X.findUniqueMethod("reverse7", 1);
        ConstructorCall cc = (ConstructorCall) reverse7.methodBody().statements().getFirst().expression();
        assertEquals("Type a.b.X.R<Y,X>", cc.parameterizedType().toString());
        assertEquals(2, cc.parameterizedType().parameters().size());
        ConstructorCall cc2 = (ConstructorCall) cc.parameterExpressions().getFirst();
        assertEquals("Type a.b.X.Pair<Y,X>", cc2.parameterizedType().toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            public class X {
                static class MyException {
                   public MyException(long id, Throwable theCause) {
                   }
                   public MyException(long id, String... args){
                   }
                   public MyException(long id, boolean logTrace, String... args){
                   }
                   public MyException(long id, String[] args, Throwable theCause) {
                   }
                   public MyException(long id, String[] args, Throwable theCause, boolean logTrace) {
                   }
                   public MyException(long id, long[] args) {
                   }
                   public MyException(long id, long[] args, Throwable theCause) {
                   }
                   public MyException(long id, int[] args) {
                   }
                }
                void method(String msg, Exception e) {
                    throw new MyException(3L, new String[] { "a" + msg }, e);
                }
                void method2(String msg) {
                    throw new MyException(3L, new String[] { "a", "b"}, new RuntimeException(msg+" abc"));
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);

    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.Arrays;import java.util.stream.Stream;public class X {
                static class B {
                    B(String s) {}
                    B(String s1, String s2) {}
                    B(String... strings) {}
                    B(B b, String... strings) {}
                }
                static class A extends B {
                   A(String s) { super(mod(s)); }
                   A(String s1, String s2) { super(mod(s1), s2); }
                   A(String s1, String... strings) { super(append(new String[] { s1 }, strings)); }
                }
                static String mod(String in) { return in.repeat(2); }
                static <T> T[] append(T[] t1, T[] t2) {
                    return Stream.concat(Arrays.stream(t1), Arrays.stream(t2)).toArray(); 
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            record R(int k) {
              R {
                  assert k > 0;
              }
              R() {
                  this(3);
              }
            }
            """;

    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        assertEquals(2, X.constructors().size());
        MethodInfo c0 = X.findConstructor(0);
        assertFalse(c0.isCompactConstructor());
        assertEquals(1, c0.methodBody().statements().size());
        assertInstanceOf(ExplicitConstructorInvocation.class, c0.methodBody().lastStatement());

        MethodInfo c1 = X.findConstructor(1);
        assertTrue(c1.isCompactConstructor());
        // parameters are synthetically copied
        assertEquals(1, c1.parameters().size());
        assertEquals("k", c1.parameters().getFirst().name());
        // and assignments are added in the background
        assertEquals(2, c1.methodBody().statements().size());
    }



    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            class X {
                    static class Data {
                        long v1;
                        long v2;
                        long v3;
                        long v4;
                        long v5;
                    }
                    private final Data data;
                	public X(String[] tokens) {
                		super();
                		if (tokens.length != 8) {
                			throw new RuntimeException();
                		}
                		if (Long.parseLong(tokens[0]) != 8) {
                			throw new RuntimeException();
                		}
                		this.data = new Data();
                		int i = 1;
                		if (tokens[i].length() > 0) {
                			this.data.v1 = Long.parseLong(tokens[i]);
                		}
                		i++;
                		if (tokens[i].length() > 0) {
                			this.data.v2 = Double.parseDouble(tokens[i]);
                		}
                		i++;
                		if (tokens[i].length() > 0) {
                			this.data.v3 = Long.parseLong(tokens[i]);
                		}
                		i++;
                		if (tokens[i].length() > 0) { 
                			this.data.v4 = Double.parseDouble(tokens[i]);
                		}
                		i++;
                		if (tokens[i].length() > 0) { 
                			this.data.v5 = Double.parseDouble(tokens[i]);
                		}
                		i++;
                		
                	}
            }
            """;

    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        MethodInfo method = X.findConstructor(1);
        Statement s0 = method.methodBody().statements().getFirst();
        assertEquals("00@11:9-42:9", s0.source().toString());
    }
}
