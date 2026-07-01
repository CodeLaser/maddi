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

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestModifierDetailedSources extends CommonTest {

    // be careful changing this string; the assertions depend on exact column positions
    @Language("java")
    private static final String INPUT = """
            package a.b;
            public abstract class C {
              public static final int FIELD = 3;
              protected synchronized void m(final int p, String q) { }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);

        // type modifiers + nature keyword
        DetailedSources tds = typeInfo.source().detailedSources();
        assertEquals("2-1:2-6", tds.detail(runtime.typeModifierPublic()).compact2());
        assertEquals("2-8:2-15", tds.detail(runtime.typeModifierAbstract()).compact2());
        assertEquals("2-17:2-21", tds.detail(typeInfo.typeNature()).compact2()); // "class"

        // field modifiers
        FieldInfo field = typeInfo.getFieldByName("FIELD", true);
        DetailedSources fds = field.source().detailedSources();
        assertEquals("3-3:3-8", fds.detail(runtime.fieldModifierPublic()).compact2());
        assertEquals("3-10:3-15", fds.detail(runtime.fieldModifierStatic()).compact2());
        assertEquals("3-17:3-21", fds.detail(runtime.fieldModifierFinal()).compact2());

        // method modifiers
        MethodInfo m = typeInfo.findUniqueMethod("m", 2);
        DetailedSources mds = m.source().detailedSources();
        assertEquals("4-3:4-11", mds.detail(runtime.methodModifierProtected()).compact2());
        assertEquals("4-13:4-24", mds.detail(runtime.methodModifierSynchronized()).compact2());

        // parameter 'final' (no modifier object, so keyed by the FINAL sentinel)
        ParameterInfo p = m.parameters().getFirst();
        assertEquals("p", p.name());
        assertTrue(p.isFinal());
        assertEquals("4-33:4-37", p.source().detailedSources().detail(DetailedSources.FINAL).compact2());

        // the second parameter is not final
        ParameterInfo q = m.parameters().get(1);
        assertEquals("q", q.name());
        assertFalse(q.isFinal());
        DetailedSources qds = q.source().detailedSources();
        assertTrue(qds == null || qds.detail(DetailedSources.FINAL) == null);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class Outer {
              interface I {}
              enum E { X }
              record R(int x) {}
              @interface A {}
            }
            """;

    @Test
    public void testNatures() {
        TypeInfo outer = scan("a.b.Outer", INPUT2);

        TypeInfo i = outer.findSubType("I", true);
        assertEquals("3-3:3-11", i.source().detailedSources().detail(i.typeNature()).compact2());

        TypeInfo e = outer.findSubType("E", true);
        assertEquals("4-3:4-6", e.source().detailedSources().detail(e.typeNature()).compact2());

        TypeInfo r = outer.findSubType("R", true);
        assertEquals("5-3:5-8", r.source().detailedSources().detail(r.typeNature()).compact2());

        // @interface: like the hand-written parser, the nature keyword is the 'interface' token (the '@' is
        // a separate preceding token and is not included)
        TypeInfo a = outer.findSubType("A", true);
        assertEquals("6-4:6-12", a.source().detailedSources().detail(a.typeNature()).compact2());
    }

    @Language("java")
    String INPUT3 = """
            package a.b;
            
            record X(int i, String s) {
                public X(int i) {
                    this(i, "");
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo X = scan("a.b.X", INPUT3);
        MethodInfo Xc = X.findConstructor(1);
        MethodModifier mm = Xc.methodModifiers().stream().findFirst().orElseThrow();
        assertTrue(mm.isPublic());
        Source publicSrc = Xc.source().detailedSources().detail(mm);
        assertEquals("4-5:4-10", publicSrc.compact2());
    }

    // the method's name identifier is retrievable via detail(method.simpleName()) (== detail(method.name())),
    // the form a language-agnostic consumer uses. Lookup is by object identity.
    @Test
    public void testMethodNameDetail() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        MethodInfo m = typeInfo.findUniqueMethod("m", 2);
        Source mName = m.source().detailedSources().detail(m.simpleName());
        assertNotNull(mName);
        assertEquals("4-31:4-31", mName.compact2()); // 'm'
        assertSame(mName, m.source().detailedSources().detail(m.name()));
    }

    // a constructor's name is retrievable via simpleName() and points at the type-name token. (The name() is the
    // MethodInfo.CONSTRUCTOR_NAME "<init>" constant; javac happens to intern it too, so this case worked already,
    // but it is a useful regression guard.) See TestParameterInfoSource for the discriminating known-method case.
    @Test
    public void testConstructorNameDetail() {
        TypeInfo X = scan("a.b.X", INPUT3);
        MethodInfo Xc = X.findConstructor(1);
        Source cName = Xc.source().detailedSources().detail(Xc.simpleName());
        assertNotNull(cName, "constructor name source must be retrievable via simpleName()");
        assertEquals("4-12:4-12", cName.compact2()); // the 'X' after 'public '
    }

    @Language("java")
    String INPUT5 = """
            package a.b;
            record R(String string, int count) {
            }
            """;

    // A record component is modelled by javac as a constructor parameter, not a field declaration, so its FIELD
    // must still expose the component-name identifier via detail(field.simpleName()).
    @Test
    public void testRecordComponentFieldNameDetail() {
        TypeInfo R = scan("a.b.R", INPUT5);
        FieldInfo string = R.getFieldByName("string", true);
        Source sName = string.source().detailedSources().detail(string.simpleName());
        assertNotNull(sName, "record component field name must be retrievable via simpleName()");
        assertEquals("2-17:2-22", sName.compact2()); // 'string' in 'record R(String string, int count)'

        FieldInfo count = R.getFieldByName("count", true);
        Source cName = count.source().detailedSources().detail(count.simpleName());
        assertNotNull(cName);
        assertEquals("2-29:2-33", cName.compact2()); // 'count'
    }
}
