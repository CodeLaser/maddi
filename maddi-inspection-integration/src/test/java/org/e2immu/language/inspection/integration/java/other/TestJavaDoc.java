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

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestJavaDoc extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                /**
                 * 
                 * @param in 
                 * @param len length of in
                 * @return 
                 * @throws IndexOutOfBoundsException
                 */
                public String method(String in, int len) {
                    if(len < 0 || len >= in.length()) throw new IndexOutOfBoundsException();
                    return in.substring(len);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 2);
        JavaDoc javaDoc = methodInfo.javaDoc();
        assertNotNull(javaDoc);
        assertEquals(4, javaDoc.tags().size());
        JavaDoc.Tag t0 = javaDoc.tags().getFirst();
        assertEquals("a.b.X.method(String,int):0:in", t0.resolvedReference().toString());
        assertEquals("5-15:5-16", t0.sourceOfReference().compact2());
        JavaDoc.Tag t1 = javaDoc.tags().get(1);
        assertEquals("a.b.X.method(String,int):1:len", t1.resolvedReference().toString());
        assertEquals("6-15:6-17", t1.sourceOfReference().compact2());

        @Language("java")
        String expected = """
                package a.b;
                class X {
                    /**
                    *
                    * @param in
                    * @param len length of in
                    * @return
                    * @throws IndexOutOfBoundsException
                    */
                
                    public String method(String in, int len) {
                        if(len < 0 ||len >= in.length()) { throw new IndexOutOfBoundsException(); }
                        return in.substring(len);
                    }
                }
                """;
        assertEquals(expected, javaInspector.print2(typeInfo));
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                /**
                 * ll {@link X#method(String,int}
                 * @param in 
                 * @param len length of in
                 * @return 
                 * @throws IndexOutOfBoundsException
                 */
                public String method(String in, int len) {
                    if(len < 0 || len >= in.length()) throw new IndexOutOfBoundsException();
                    return in.substring(len);
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 2);
        JavaDoc javaDoc = methodInfo.javaDoc();
        assertNotNull(javaDoc);
        assertEquals(5, javaDoc.tags().size());
        JavaDoc.Tag link = javaDoc.tags().getFirst();
        assertEquals("X#method(String,int", link.content());
        assertNull(link.resolvedReference());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class X {
                class A {
                    /**
                     * ll {@link A#method(String,int)}
                     * ll {@link X.A#method(String,int)}
                     * ll {@link a.b.X.A#method(String,int)}
                     * @param in 
                     * @param len length of in
                     * @return 
                     * @throws IndexOutOfBoundsException
                     */
                    public String method(String in, int len) {
                        if(len < 0 || len >= in.length()) throw new IndexOutOfBoundsException();
                        return in.substring(len);
                    }
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3, JavaInspectorImpl.DETAILED_SOURCES);
        TypeInfo A = typeInfo.findSubType("A", true);
        MethodInfo methodInfo = A.findUniqueMethod("method", 2);
        JavaDoc javaDoc = methodInfo.javaDoc();
        assertNotNull(javaDoc);
        assertEquals(7, javaDoc.tags().size());
        {
            JavaDoc.Tag link = javaDoc.tags().getFirst();
            assertEquals("A#method(String,int)", link.content());
            MethodInfo method = (MethodInfo) link.resolvedReference();
            assertEquals("a.b.X.A.method(String,int)", method.fullyQualifiedName());
            assertEquals("5-15:5-42", link.source().compact2());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    link.source().detailedSources().associatedObject(method.typeInfo());
            assertNull(tis);
        }
        {
            JavaDoc.Tag link = javaDoc.tags().get(1);
            assertEquals("X.A#method(String,int)", link.content());
            MethodInfo method = (MethodInfo) link.resolvedReference();
            assertEquals("a.b.X.A.method(String,int)", method.fullyQualifiedName());
            assertEquals("6-15:6-44", link.source().compact2());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    link.source().detailedSources().associatedObject(method.typeInfo());
            assertEquals(1, tis.size());
            assertEquals("6-22:6-22", tis.getFirst().source().compact2());
            assertNull(link.source().detailedSources().detail(method.typeInfo().packageName()));
        }
        {
            JavaDoc.Tag link = javaDoc.tags().get(2);
            assertEquals("a.b.X.A#method(String,int)", link.content());
            MethodInfo method = (MethodInfo) link.resolvedReference();
            assertEquals("a.b.X.A.method(String,int)", method.fullyQualifiedName());
            assertEquals("7-15:7-48", link.source().compact2());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    link.source().detailedSources().associatedObject(method.typeInfo());
            assertEquals(1, tis.size());
            assertEquals("7-22:7-26", tis.getFirst().source().compact2());
            assertEquals("7-22:7-24",
                    link.source().detailedSources().detail(method.typeInfo().packageName()).compact2());
        }
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class X {
                /**
                 * {@link java.util.LinkedList}
                 */
                public void method(){
                    // empty
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4, JavaInspectorImpl.DETAILED_SOURCES);
        assertNull(typeInfo.javaDoc());
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
        assertEquals(1, methodInfo.javaDoc().tags().size());
        JavaDoc.Tag tag = methodInfo.javaDoc().tags().getFirst();
        assertEquals("java.util.LinkedList", tag.resolvedReference().toString());
        assertEquals("""
                [TypeReference[typeInfo=void, explicit=true], TypeReference[typeInfo=java.util.LinkedList, explicit=true]]\
                """, methodInfo.typesReferenced().toList().toString());
        DetailedSources detailedSources = tag.source().detailedSources();
        assertNotNull(detailedSources);
        assertEquals("4-15:4-34", detailedSources.detail(tag.resolvedReference()).compact2());
        assertEquals("4-15:4-23", detailedSources.detail(((TypeInfo) tag.resolvedReference()).packageName()).compact2());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            class Y {
                /**
                 * reference to {@link #method}
                 * reference to {@link Y#method}
                 * reference to {@link a.b.Y#method(String)}
                 * @param s
                 * @return
                 */
                @Override
                int method(String s) {
                   return s.length() + 10;
                }
            }
            """;

    @Test
    public void test5() {
        TypeInfo typeInfo = javaInspector.parse(INPUT5, JavaInspectorImpl.DETAILED_SOURCES);
        assertNull(typeInfo.javaDoc());
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
        assertEquals(5, methodInfo.javaDoc().tags().size());
        {
            JavaDoc.Tag tag = methodInfo.javaDoc().tags().getFirst();
            MethodInfo object = (MethodInfo) tag.resolvedReference();
            assertEquals("a.b.Y.method(String)", object.toString());
            // 7 chars inclusive
            assertEquals("4-28:4-34", tag.sourceOfReference().compact2());
            DetailedSources detailedSources = tag.source().detailedSources();
            assertNotNull(detailedSources);
            assertEquals("4-29:4-34", detailedSources.detail(object).compact2());
            assertEquals("4-29:4-34", detailedSources.detail(object.name()).compact2());
        }
        {
            JavaDoc.Tag tag = methodInfo.javaDoc().tags().get(1);
            MethodInfo object = (MethodInfo) tag.resolvedReference();
            assertEquals("a.b.Y.method(String)", object.toString());
            assertEquals("5-28:5-35", tag.sourceOfReference().compact2());
            DetailedSources detailedSources = tag.source().detailedSources();
            assertNotNull(detailedSources);
            assertEquals("5-30:5-35", detailedSources.detail(object).compact2());
            assertEquals("5-30:5-35", detailedSources.detail(object.name()).compact2());
        }
        {
            JavaDoc.Tag tag = methodInfo.javaDoc().tags().get(2);
            MethodInfo element = (MethodInfo) tag.resolvedReference();
            assertEquals("a.b.Y.method(String)", element.toString());
            assertEquals("6-28:6-47", tag.sourceOfReference().compact2());
            DetailedSources detailedSources = tag.source().detailedSources();
            assertNotNull(detailedSources);
            assertEquals("6-28:6-32", detailedSources.detail(methodInfo.typeInfo()).compact2());
            assertEquals("6-34:6-47", detailedSources.detail(element).compact2());
            assertEquals("6-34:6-39", detailedSources.detail(element.name()).compact2());
        }
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            class Y {
                /**
                 * reference to {@link #field}
                 * reference to {@link Y#field}
                 */
                int field = 3;
            }
            """;

    @Test
    public void test6() {
        TypeInfo typeInfo = javaInspector.parse(INPUT6, JavaInspectorImpl.DETAILED_SOURCES);
        assertNull(typeInfo.javaDoc());
        FieldInfo field = typeInfo.getFieldByName("field", true);
        assertEquals(1, field.comments().size());
        assertNotNull(field.javaDoc());
        assertEquals(2, field.javaDoc().tags().size());
        {
            JavaDoc.Tag tag = field.javaDoc().tags().getFirst();
            FieldInfo object = (FieldInfo) tag.resolvedReference();
            assertEquals("a.b.Y.field", object.toString());
            // 7 chars inclusive
            assertEquals("4-28:4-33", tag.sourceOfReference().compact2());
            DetailedSources detailedSources = tag.source().detailedSources();
            assertNotNull(detailedSources);
            assertEquals("4-29:4-33", detailedSources.detail(object).compact2());
            assertEquals("4-29:4-33", detailedSources.detail(object.name()).compact2());
        }
        {
            JavaDoc.Tag tag = field.javaDoc().tags().get(1);
            FieldInfo object = (FieldInfo) tag.resolvedReference();
            assertEquals("a.b.Y.field", object.toString());
            assertEquals("5-28:5-34", tag.sourceOfReference().compact2());
            DetailedSources detailedSources = tag.source().detailedSources();
            assertNotNull(detailedSources);
            assertEquals("5-30:5-34", detailedSources.detail(object).compact2());
            assertEquals("5-30:5-34", detailedSources.detail(object.name()).compact2());
        }
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            class X {
                /**
                 * @see java.util.LinkedList
                 * @link #field
                 */
                public void method(){
                    // empty
                }
                int field;
            }
            """;

    @Test
    public void test7() {
        TypeInfo typeInfo = javaInspector.parse(INPUT7, JavaInspectorImpl.DETAILED_SOURCES);
        assertNull(typeInfo.javaDoc());
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
        assertEquals(2, methodInfo.javaDoc().tags().size());
        {
            JavaDoc.Tag tag = methodInfo.javaDoc().tags().getFirst();
            assertSame(JavaDoc.TagIdentifier.SEE, tag.identifier());
            assertEquals("java.util.LinkedList", tag.content());
            assertEquals("java.util.LinkedList", tag.resolvedReference().toString());
            assertEquals("""
                    [TypeReference[typeInfo=void, explicit=true], TypeReference[typeInfo=java.util.LinkedList, explicit=true]]\
                    """, methodInfo.typesReferenced().toList().toString());
            DetailedSources detailedSources = tag.source().detailedSources();
            assertNotNull(detailedSources);
            assertEquals("4-13:4-32", detailedSources.detail(tag.resolvedReference()).compact2());
            assertEquals("4-13:4-21", detailedSources.detail(((TypeInfo) tag.resolvedReference()).packageName()).compact2());
        }
        {
            JavaDoc.Tag tag = methodInfo.javaDoc().tags().getLast();
            assertSame(JavaDoc.TagIdentifier.LINK, tag.identifier());
            assertEquals("#field", tag.content());
            assertEquals("a.b.X.field", tag.resolvedReference().toString());
            DetailedSources detailedSources = tag.source().detailedSources();
            assertNotNull(detailedSources);
            assertEquals("5-15:5-19", detailedSources.detail(tag.resolvedReference()).compact2());
        }
    }

}
