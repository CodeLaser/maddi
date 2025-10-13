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

package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestFullyQualified extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import a.b.X.Y.Z;
            class X {
                void method() {
                   java.lang.System.out.println("?");
                }
                static class Y {
                    static class Z {
            
                    }
                }
                static Z make1() {
                    return new X.Y.Z();
                }
                static Z make2() {
                    return new Y.Z();
                }
                static Z make3() {
                    return new Z();
                }
                static Z make4() {
                    return new a.b.X.Y.Z();
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
        MethodCall methodCall = (MethodCall) methodInfo.methodBody().statements().getFirst().expression();
        if (methodCall.object() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
            TypeExpression typeExpression = (TypeExpression) fr.scope();
            ParameterizedType pt = typeExpression.parameterizedType();
            assertEquals("System", pt.detailedString());
            assertEquals("5-8:5-23", typeExpression.source().detailedSources().detail(pt).compact2());
            assertNull(typeExpression.source().detailedSources().associatedObject(pt.typeInfo()));
        } else fail();
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make1", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    cc.source().detailedSources().associatedObject(pt.typeInfo());
            assertEquals("13-20:13-24", cc.source().detailedSources().detail(pt.typeInfo()).compact2());
            assertEquals(2, tis.size());
            assertEquals("TypeInfoSource[typeInfo=a.b.X.Y, source=@13:20-13:22]", tis.getFirst().toString());
            assertEquals("TypeInfoSource[typeInfo=a.b.X, source=@13:20-13:20]", tis.getLast().toString());
        }
        {
            MethodInfo make2 = typeInfo.findUniqueMethod("make2", 0);
            ConstructorCall cc = (ConstructorCall) make2.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    cc.source().detailedSources().associatedObject(pt.typeInfo());
            assertEquals("16-20:16-22", cc.source().detailedSources().detail(pt.typeInfo()).compact2());
            assertEquals(1, tis.size());
            assertEquals("TypeInfoSource[typeInfo=a.b.X.Y, source=@16:20-16:20]", tis.getFirst().toString());
        }
        {
            MethodInfo make3 = typeInfo.findUniqueMethod("make3", 0);
            ConstructorCall cc = (ConstructorCall) make3.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            assertNull(cc.source().detailedSources().associatedObject(pt.typeInfo()));
        }
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make4", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    cc.source().detailedSources().associatedObject(pt.typeInfo());
            assertEquals("22-20:22-28", cc.source().detailedSources().detail(pt.typeInfo()).compact2());
            assertEquals(2, tis.size());
            assertEquals("TypeInfoSource[typeInfo=a.b.X.Y, source=@22:20-22:26]", tis.getFirst().toString());
            assertEquals("TypeInfoSource[typeInfo=a.b.X, source=@22:20-22:24]", tis.getLast().toString());
        }
    }


    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            import a.b.X.Y.Z;
            class X {
                static class Y {
                    static class Z<T> {
                    }
                }
                static Z make1() {
                    return new X.Y.Z<String>();
                }
                static Z make2() {
                    return new Y.Z<X>();
                }
                static Z make3() {
                    return new Z();
                }
                static Z make4() {
                    return new a.b.X.Y.Z();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2, JavaInspectorImpl.DETAILED_SOURCES);
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make1", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z<String>", pt.detailedString());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    cc.source().detailedSources().associatedObject(pt.typeInfo());
            assertEquals("9-20:9-24", cc.source().detailedSources().detail(pt.typeInfo()).compact2());
            assertEquals(2, tis.size());
            assertEquals("TypeInfoSource[typeInfo=a.b.X.Y, source=@9:20-9:22]", tis.getFirst().toString());
            assertEquals("TypeInfoSource[typeInfo=a.b.X, source=@9:20-9:20]", tis.getLast().toString());
        }
        {
            MethodInfo make2 = typeInfo.findUniqueMethod("make2", 0);
            ConstructorCall cc = (ConstructorCall) make2.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z<a.b.X>", pt.detailedString());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    cc.source().detailedSources().associatedObject(pt.typeInfo());
            assertEquals("12-20:12-22", cc.source().detailedSources().detail(pt.typeInfo()).compact2());
            assertEquals(1, tis.size());
            assertEquals("TypeInfoSource[typeInfo=a.b.X.Y, source=@12:20-12:20]", tis.getFirst().toString());
        }
        {
            MethodInfo make3 = typeInfo.findUniqueMethod("make3", 0);
            ConstructorCall cc = (ConstructorCall) make3.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            assertNull(cc.source().detailedSources().associatedObject(pt.typeInfo()));
        }
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make4", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    cc.source().detailedSources().associatedObject(pt.typeInfo());
            assertEquals("18-20:18-28", cc.source().detailedSources().detail(pt.typeInfo()).compact2());
            assertEquals(2, tis.size());
            assertEquals("TypeInfoSource[typeInfo=a.b.X.Y, source=@18:20-18:26]", tis.getFirst().toString());
            assertEquals("TypeInfoSource[typeInfo=a.b.X, source=@18:20-18:24]", tis.getLast().toString());
        }
    }


    @Language("java")
    public static final String INPUT3 = """
            package a.b;
            class X {
                @Docstring(embedding = "1, 2, 3")
                void method() {
                   java.lang.System.out.println("?");
                }
                @X.Docstring(embedding = "1, 2, 3")
                void method2() {
                   java.lang.System.out.println("?");
                }
                @a.b.X.Docstring(embedding = "1, 2, 3")
                void method3() {
                   java.lang.System.out.println("?");
                }
                public @interface Docstring {
                    String embedding();
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3, new JavaInspectorImpl.ParseOptionsBuilder()
                .setDetailedSources(true).build());
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
            AnnotationExpression sw = methodInfo.annotations().getFirst();
            assertEquals("a.b.X.Docstring", sw.typeInfo().fullyQualifiedName());
            assertNull(sw.source().detailedSources().associatedObject(sw.typeInfo()));
            assertNull(sw.source().detailedSources().detail(sw.typeInfo().packageName()));
        }
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method2", 0);
            AnnotationExpression sw = methodInfo.annotations().getFirst();
            assertEquals("a.b.X.Docstring", sw.typeInfo().fullyQualifiedName());

            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    sw.source().detailedSources().associatedObject(sw.typeInfo());
            assertEquals("7-6:7-16", sw.source().detailedSources().detail(sw.typeInfo()).compact2());
            assertEquals(1, tis.size());
            assertEquals("TypeInfoSource[typeInfo=a.b.X, source=@7:6-7:6]", tis.getFirst().toString());
            assertNull(sw.source().detailedSources().detail(sw.typeInfo().packageName()));
        }
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method3", 0);
            AnnotationExpression sw = methodInfo.annotations().getFirst();
            assertEquals("a.b.X.Docstring", sw.typeInfo().fullyQualifiedName());

            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    sw.source().detailedSources().associatedObject(sw.typeInfo());
            assertEquals("11-6:11-20", sw.source().detailedSources().detail(sw.typeInfo()).compact2());
            assertEquals(1, tis.size());
            assertEquals("TypeInfoSource[typeInfo=a.b.X, source=@11:6-11:10]", tis.getFirst().toString());
            assertEquals("11-6:11-8", sw.source().detailedSources().detail(sw.typeInfo().packageName()).compact2());
        }
    }


    @Language("java")
    public static final String INPUT3b = """
            package a.b;
            class X {
                @Docstring
                void method() {
                   java.lang.System.out.println("?");
                }
                @X.Docstring
                void method2() {
                   java.lang.System.out.println("?");
                }
                @a.b.X.Docstring
                void method3() {
                   java.lang.System.out.println("?");
                }
                public @interface Docstring {
                }
            }
            """;

    @Test
    public void test3b() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3b, new JavaInspectorImpl.ParseOptionsBuilder()
                .setDetailedSources(true).build());
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
            AnnotationExpression sw = methodInfo.annotations().getFirst();
            assertEquals("a.b.X.Docstring", sw.typeInfo().fullyQualifiedName());
            assertNull(sw.source().detailedSources().associatedObject(sw.typeInfo()));
            assertNull(sw.source().detailedSources().detail(sw.typeInfo().packageName()));
        }
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method2", 0);
            AnnotationExpression sw = methodInfo.annotations().getFirst();
            assertEquals("a.b.X.Docstring", sw.typeInfo().fullyQualifiedName());

            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    sw.source().detailedSources().associatedObject(sw.typeInfo());
            assertEquals("7-6:7-16", sw.source().detailedSources().detail(sw.typeInfo()).compact2());
            assertEquals(1, tis.size());
            assertEquals("TypeInfoSource[typeInfo=a.b.X, source=@7:6-7:6]", tis.getFirst().toString());
            assertNull(sw.source().detailedSources().detail(sw.typeInfo().packageName()));
        }
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method3", 0);
            AnnotationExpression sw = methodInfo.annotations().getFirst();
            assertEquals("a.b.X.Docstring", sw.typeInfo().fullyQualifiedName());

            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    sw.source().detailedSources().associatedObject(sw.typeInfo());
            assertEquals("11-6:11-20", sw.source().detailedSources().detail(sw.typeInfo()).compact2());
            assertEquals(1, tis.size());
            assertEquals("TypeInfoSource[typeInfo=a.b.X, source=@11:6-11:10]", tis.getFirst().toString());
            assertEquals("11-6:11-8", sw.source().detailedSources().detail(sw.typeInfo().packageName()).compact2());
        }
    }


    @Language("java")
    public static final String INPUT4 = """
            class X {
                  interface I {
                      interface J {
                          int get();
                      }
                  }
                  static class II implements I {
                      int method0(X.II.J j) {
                          return j.get();
                      }
                      int method1(II.J j) {
                          return j.get();
                      }
                      int method2(I.J j) {
                          return j.get();
                      }
                      int method3(J j) {
                          return j.get();
                      }
                  }
              }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4, JavaInspectorImpl.DETAILED_SOURCES);
        TypeInfo II = typeInfo.findSubType("II");
        {
            MethodInfo methodInfo = II.findUniqueMethod("method3", 1);
            ParameterInfo first = methodInfo.parameters().getFirst();
            ParameterizedType pt = first.parameterizedType();
            assertNull(first.source().detailedSources().associatedObject(pt.typeInfo()));
        }
        {
            MethodInfo methodInfo = II.findUniqueMethod("method2", 1);
            ParameterInfo first = methodInfo.parameters().getFirst();
            ParameterizedType pt = first.parameterizedType();
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    first.source().detailedSources().associatedObject(pt.typeInfo());
            assertEquals("14-23:14-25", first.source().detailedSources().detail(pt.typeInfo()).compact2());
            assertEquals(1, tis.size());
            assertEquals("TypeInfoSource[typeInfo=X.I, source=@14:23-14:23]", tis.getFirst().toString());
        }
        {
            MethodInfo methodInfo = II.findUniqueMethod("method1", 1);
            ParameterInfo first = methodInfo.parameters().getFirst();
            ParameterizedType pt = first.parameterizedType();
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    first.source().detailedSources().associatedObject(pt.typeInfo());
            assertEquals("11-23:11-26", first.source().detailedSources().detail(pt.typeInfo()).compact2());
            assertEquals(1, tis.size());
            assertEquals("TypeInfoSource[typeInfo=X.II, source=@11:23-11:24]", tis.getFirst().toString());
        }
        {
            MethodInfo methodInfo = II.findUniqueMethod("method0", 1);
            ParameterInfo first = methodInfo.parameters().getFirst();
            ParameterizedType pt = first.parameterizedType();
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    first.source().detailedSources().associatedObject(pt.typeInfo());
            assertEquals("8-23:8-28", first.source().detailedSources().detail(pt.typeInfo()).compact2());
            assertEquals(2, tis.size());
            assertEquals("TypeInfoSource[typeInfo=X.II, source=@8:23-8:26]", tis.getFirst().toString());
            assertEquals("TypeInfoSource[typeInfo=X, source=@8:23-8:23]", tis.getLast().toString());
        }
    }

}
