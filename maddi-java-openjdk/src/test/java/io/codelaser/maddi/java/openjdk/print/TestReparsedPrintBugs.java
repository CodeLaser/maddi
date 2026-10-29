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

package io.codelaser.maddi.java.openjdk.print;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Printed units that did not compile when re-parsed (jenkins core, timefold-solver, retrofit), on the openjdk front
 * end, printed as the issues did: {@code print2(cu, null, importComputer(n, sourceSet))}.
 */
public class TestReparsedPrintBugs extends CommonTest {

    private String print(TypeInfo typeInfo) {
        OutputBuilder ob = runtime.newCompilationUnitPrinter(typeInfo.compilationUnit(), true)
                .print(new ImportComputerImpl(Integer.MAX_VALUE, null), runtime.qualificationQualifyFromPrimaryType(null));
        return new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build()).write(ob);
    }

    // GitHub #169: a class literal in an annotation value, written qualified, prints qualified
    @Test
    public void annotationClassLiteral() {
        Map<String, TypeInfo> types = scan(false,
                "proj.ann.Restricted", "package proj.ann; public @interface Restricted { Class<?>[] value(); }",
                "proj.ann.restrictions.NoExternalUse", "package proj.ann.restrictions; public class NoExternalUse { }",
                "proj.Messages", """
                        package proj;
                        import proj.ann.Restricted;
                        @Restricted(proj.ann.restrictions.NoExternalUse.class)
                        public class Messages {
                            public static String hello() { return "hello"; }
                        }
                        """);
        String printed = print(types.get("proj.Messages"));
        boolean imported = printed.contains("import proj.ann.restrictions.NoExternalUse;");
        assertTrue(imported || !Pattern.compile("[^.]NoExternalUse\\.class").matcher(printed).find(), printed);
    }

    // GitHub #178: the type argument written in a constructor call is imported
    @Test
    public void constructorTypeArgument() {
        Map<String, TypeInfo> types = scan(false,
                "a.c.Sol", "package a.c; public class Sol { }",
                "a.b.Box", "package a.b; public class Box<S> { public Box(int i) { } }",
                "a.b.V", """
                        package a.b;
                        import a.c.Sol;
                        public class V {
                            Object m() {
                                var box = new Box<Sol>(4);
                                return box;
                            }
                        }
                        """);
        String printed = print(types.get("a.b.V"));
        assertTrue(printed.contains("import a.c.Sol;"), printed);
        assertTrue(printed.contains("new Box<Sol>(4)"), printed);
    }

    // GitHub #168: a static method of a superinterface is not inherited, and keeps its qualifier
    @Test
    public void staticInterfaceMethod() {
        Map<String, TypeInfo> types = scan(false,
                "proj.v1.Item", """
                        package proj.v1;
                        public interface Item {
                            static Item from(Object o) { return null; }
                            String name();
                        }
                        """,
                "proj.Use", """
                        package proj;
                        import proj.v1.Item;
                        import java.util.Comparator;
                        public class Use {
                            public static final class Impl implements Item {
                                private final Object delegate;
                                public Impl(Object delegate) { this.delegate = delegate; }
                                @Override public String name() { return Item.from(delegate).name(); }
                            }
                            public static final class ByName implements Comparator<String> {
                                @Override public int compare(String a, String b) {
                                    return Comparator.reverseOrder().hashCode();
                                }
                            }
                        }
                        """);
        String printed = print(types.get("proj.Use"));
        assertTrue(printed.contains("return Item.from(delegate).name();"), printed);
        assertTrue(printed.contains("Comparator.reverseOrder()"), printed);
    }

    // GitHub #175: 'X.this.apply(..)' keeps its qualifier; a bare call binds to the anonymous class's apply
    @Language("java")
    private static final String OUTER_THIS = """
            package a.b;
            import java.util.List;
            abstract class X<T> {
                abstract void apply(StringBuilder b, T value);
                void log(String s) { }
                final X<List<T>> iterable() {
                    return new X<List<T>>() {
                        @Override
                        void apply(StringBuilder b, List<T> values) {
                            if (values == null) return;
                            for (T value : values) {
                                X.this.apply(b, value);
                            }
                            log("done");
                        }
                    };
                }
            }
            """;

    @Test
    public void outerThisCall() {
        String printed = print(scan("a.b.X", OUTER_THIS));
        assertTrue(printed.contains("X.this.apply(b, value);"), printed);
        assertTrue(printed.contains(" log(\"done\");"), printed);
    }

    // GitHub #177: a text block is printed from its value, so its escapes are written again
    @Language("java")
    private static final String TEXT_BLOCK = """
            package a.b;
            public class T {
                static final String F = \"""
                        name\\\\/name, false
                        tab\\tx
                        \""";
            }
            """;

    @Test
    public void textBlockEscapes() {
        String printed = print(scan("a.b.T", TEXT_BLOCK));
        assertTrue(printed.contains("name\\\\/name, false\n"), printed);
        assertTrue(printed.contains("tab\\tx\n"), printed);
        assertFalse(printed.contains("\t"), printed);
    }
}
