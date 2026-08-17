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

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestSubTypes extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Iterator;

            public class SubType_0 {
                public static Iterable<Integer> makeIterator(int n) {
                    int lower = 0;

                    return new Iterable<>() {

                        @Override
                        public Iterator<Integer> iterator() {
                            return new Iterator<>() {
                                private int i = n;

                                @Override
                                public boolean hasNext() {
                                    return i >= lower;
                                }

                                @Override
                                public Integer next() {
                                    return i--;
                                }
                            };
                        }
                    };
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
    }
    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.junit.jupiter.api.Test;

            import static org.junit.jupiter.api.Assertions.assertEquals;

            public class SubType_1 {

                static class Clazz<T> {
                    private final T t;

                    public Clazz(T t) {
                        this.t = t;
                    }

                    class Sub<S> {
                        private final S s;

                        public Sub(S s) {
                            this.s = s;
                        }

                        public S getS() {
                            return s;
                        }

                        @Override
                        public String toString() {
                            return s + "=" + t;
                        }
                    }

                    public T getT() {
                        return t;
                    }
                }

                @Test
                public void test() {
                    Clazz<Integer> clazz = new Clazz<>(3);
                    Clazz<Integer>.Sub<Character> sub = clazz.new Sub<Character>('a');
                    assertEquals("a=3", sub.toString());
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
    }

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.language.inspection.integration.java.importhelper.SubType_4Helper;

            public class SubType_4 {

                void method(String strings) {
                    SubType_4Helper b = new SubType_4Helper();
                    b.set(createD(strings));
                }

                private org.e2immu.language.inspection.integration.java.importhelper.SubType_4Helper.D createD(String strings) {
                    org.e2immu.language.inspection.integration.java.importhelper.SubType_4Helper.D d
                            = new org.e2immu.language.inspection.integration.java.importhelper.SubType_4Helper.D();
                    System.out.println(d + " = " + strings);
                    return d;
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3);
    }


    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.language.inspection.integration.java.importhelper.SubType_3Helper;

            public class SubType_3B {

                private interface PP extends SubType_3Helper.PP {
                    void oneMoreMethod();
                }

                private final PP pp = makePP();

                void method() {
                    new SubType_3Helper().someMethod(pp);
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
    public void test4() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4);
    }


    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.language.inspection.integration.java.importhelper.SubType_3Helper;

            public class SubType_3C {

                private interface PP extends org.e2immu.language.inspection.integration.java.importhelper.SubType_3Helper.PP {
                    void oneMoreMethod();
                }

                private final PP pp = makePP();

                void method() {
                  //  new SubType_3Helper().someMethod(pp);
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
    public void test5() {
        TypeInfo typeInfo = javaInspector.parse(INPUT5);
    }
}
