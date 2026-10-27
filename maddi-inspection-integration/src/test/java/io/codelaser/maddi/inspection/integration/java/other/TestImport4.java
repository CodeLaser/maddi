/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class TestImport4 extends CommonTest2 {


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
    String USE = """
            package c;
            import static a.b.i.ValueImpl.IndependentImpl.*;
            public class Use {
                interface BB extends Bool { }
                Bool bool;
            }
            """;

    @Test
    public void testImport() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.Value", INTERFACE, "a.b.i.ValueImpl", IMPLEMENTATION,
                "c.Use", USE);
        init(sourcesByFqn);
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
        Map<String, String> sourcesByFqn = Map.of("a.b.Value", INTERFACE, "a.b.i.ValueImpl", IMPLEMENTATION,
                "c.Use", USE_FAIL_1);
        assertThrows(Summary.FailFastException.class, () -> init(sourcesByFqn));
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
        Map<String, String> sourcesByFqn = Map.of("a.b.Value", INTERFACE, "a.b.i.ValueImpl", IMPLEMENTATION,
                "c.Use", USE_FAIL_2);
        assertThrows(Summary.FailFastException.class, () -> init(sourcesByFqn));
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
        Map<String, String> sourcesByFqn = Map.of("a.b.Value", INTERFACE, "a.b.i.ValueImpl", IMPLEMENTATION,
                "c.Use", USE_FAIL_3);
        assertThrows(Summary.FailFastException.class, () -> init(sourcesByFqn));
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
    public void testImport4() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.c.d.Value", INTERFACE_4, "a.b.c.d.i.ValueImpl", IMPLEMENTATION_4,
                "c.Use", USE_4);
        ParseResult parseResult = init(sourcesByFqn);
        TypeInfo value = parseResult.findType("a.b.c.d.Value");
        assertNotNull(value);
        TypeInfo valueImpl = parseResult.findType("a.b.c.d.i.ValueImpl");
        assertNotNull(valueImpl);
    }

}
