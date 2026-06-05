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

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class TestImport4 extends CommonTest {


    @Language("java")
    String INTERFACE = """
            package a.b;
            public interface Value {
                void common();
            
                interface Bool extends Value {
                    void boolMethod();
                }
                interface Immutable extends Value {
                    void immutableMethod();
                }
                interface Independent extends Value {
                    void independentMethod();
                }
            }
            """;

    @Language("java")
    String IMPLEMENTATION = """
            package a.b.i;
            import a.b.Value;
            public class ValueImpl implements Value {
                public void common() { }
                public static class BoolImpl extends ValueImpl implements Bool {
                    public void boolMethod() { }
                }
                public static class ImmutableImpl extends ValueImpl implements Immutable {
                    public void immutableMethod() { }
                }
                public static class IndependentImpl extends ValueImpl implements Independent {
                    public static final Independent INDEPENDENT = new IndependentImpl();
                    public void independentMethod() { }
                }
            }
            """;

    @Language("java")
    String USE = """
            package c;
            import static a.b.i.ValueImpl.IndependentImpl.*;
            public class Use {
                interface BB extends Bool { }
                Bool bool;
            }
            """;

    @Test
    public void testImport() {
        scan(false, "a.b.Value", INTERFACE, "a.b.i.ValueImpl", IMPLEMENTATION,
                "c.Use", USE);
    }

    @Language("java")
    String USE_FAIL_1 = """
            package c;
            import static a.b.i.ValueImpl.IndependentImpl.*;
            public class Use {
                IndependentImpl ii;
            }
            """;

    @Test
    public void testImportFail1() {
        // DIFFERS from maddi parser
        //assertThrows(Summary.FailFastException.class, () -> scan(false, "a.b.Value", INTERFACE,
        //        "a.b.i.ValueImpl", IMPLEMENTATION, "c.Use", USE_FAIL_1));
        Map<String, TypeInfo> map = scan(false, "a.b.Value", INTERFACE, "a.b.i.ValueImpl", IMPLEMENTATION, "c.Use",
                USE_FAIL_1);
        TypeInfo use = map.get("c.Use");
        FieldInfo ii = use.getFieldByName("ii", true);
        assertEquals("Type a.b.i.ValueImpl.IndependentImpl", ii.type().toString());
    }

    @Language("java")
    String USE_FAIL_2 = """
            package c;
            import static a.b.i.ValueImpl.IndependentImpl.*;
            public class Use {
                Value value;
            }
            """;

    @Test
    public void testImportFail2() {
        assertThrows(Summary.FailFastException.class, () -> scan(false, "a.b.Value", INTERFACE,
                "a.b.i.ValueImpl", IMPLEMENTATION, "c.Use", USE_FAIL_2));
    }


    @Language("java")
    String USE_FAIL_3 = """
            package c;
            import static a.b.i.ValueImpl.IndependentImpl.*;
            public class Use {
                ValueImpl value;
            }
            """;

    @Test
    public void testImportFail3() {
        assertThrows(Summary.FailFastException.class, () -> scan(false, "a.b.Value", INTERFACE,
                "a.b.i.ValueImpl", IMPLEMENTATION, "c.Use", USE_FAIL_3));
    }


    @Language("java")
    String INTERFACE_4 = """
            package a.b
            .c .d;
            public interface Value {
                void common();
            
                interface Bool extends Value {
                    void boolMethod();
                }
                interface Immutable extends Value {
                    void immutableMethod();
                }
                interface Independent extends Value {
                    void independentMethod();
                }
            }
            """;

    @Language("java")
    String IMPLEMENTATION_4 = """
            package a .b.c.d. i ;
            import a.b.c.
              d.Value;
            public class ValueImpl implements Value {
                public void common() { }
                static class BoolImpl extends ValueImpl implements Bool {
                    public void boolMethod() { }
                }
                static class ImmutableImpl extends ValueImpl implements Immutable {
                    public void immutableMethod() { }
                }
                static class IndependentImpl extends ValueImpl implements Independent {
                    public static final Independent INDEPENDENT = new IndependentImpl();
                    public void independentMethod() { }
                }
            }
            """;

    @Language("java")
    String USE_4 = """
            package c;
            import static a. b.c.d .i.ValueImpl.IndependentImpl.*;
            public class Use {
                interface BB extends Bool { }
                Bool bool;
            }
            """;

    @Test
    public void testImport4() {
        Map<String, TypeInfo> parseResult = scan(false, "a.b.c.d.Value", INTERFACE_4,
                "a.b.c.d.i.ValueImpl", IMPLEMENTATION_4, "c.Use", USE_4);
        TypeInfo value = parseResult.get("a.b.c.d.Value");
        assertNotNull(value);
        TypeInfo valueImpl = parseResult.get("a.b.c.d.i.ValueImpl");
        assertNotNull(valueImpl);
    }

}
