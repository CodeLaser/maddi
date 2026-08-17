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

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseSubType extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              interface I {
                 int i();
              }
              private static class II implements I {
                 int i() {
                   return 3;
                 }
              }
              interface J {
                 boolean j();
              }
              private static class JJ extends II implements I, J {
                 int j() {
                   return true;
                 }
              }
              public static void main(String[] args) {
                System.out.println("hello");
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        assertEquals("C", typeInfo.simpleName());
        assertEquals("a.b.C", typeInfo.fullyQualifiedName());
        assertEquals(4, typeInfo.subTypes().size());

        TypeInfo subType1 = typeInfo.subTypes().get(0);
        assertTrue(subType1.typeNature().isInterface());
        assertEquals(1, subType1.methods().size());
        MethodInfo i = subType1.methods().get(0);
        assertEquals("i", i.name());
        assertTrue(i.methodBody().isEmpty());

        TypeInfo subType2 = typeInfo.subTypes().get(1);
        assertEquals("a.b.C.II", subType2.fullyQualifiedName());
        assertTrue(subType2.typeNature().isClass());
        assertTrue(subType2.isStatic());
        assertFalse(subType2.isPublic());
        assertTrue(subType2.isPrivate());
        assertEquals(1, subType2.methods().size());
        MethodInfo i2 = subType2.methods().get(0);
        assertEquals("i", i.name());
        Block block = i2.methodBody();
        assertEquals(1, block.size());

        TypeInfo jj= typeInfo.findSubType("JJ");
        assertEquals("I,II,J",
                jj.superTypesExcludingJavaLangObject().stream().map(TypeInfo::simpleName).sorted()
                        .collect(Collectors.joining(",")));
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            public class SubType_5 {

                interface Iterator<T> {
                    T next();
                }

                static class ArrayList<T> {

                    private final T t;

                    public ArrayList(T t) {
                        this.t = t;
                    }

                    private class Itr implements Iterator<T> {
                        @Override
                        public T next() {
                            return t;
                        }
                    }
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = parse(INPUT2);
    }


    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;

            public class SubType_3 {

                public static class O {
                    public interface PP {
                        void theFirstMethod();
                    }

                    void someMethod(PP pp) {

                    }
                }

                private interface PP extends O.PP {
                    void oneMoreMethod();
                }

                private final PP pp = makePP();

                void method() {
                    new O().someMethod(pp);
                }

                private PP makePP() {
                    return new PP() {
                        @Override
                        public void theFirstMethod() {

                        }

                        @Override
                        public void oneMoreMethod() {

                        }
                    };
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = parse(INPUT3);
    }

}
