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

package io.codelaser.maddi.inspection.integration.java.print;

import io.codelaser.maddi.cst.api.info.ImportComputer;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A static method of an interface is not inherited (JLS 8.4.8): called from a type implementing or extending the
 * interface, it keeps its qualifier (GitHub CodeLaser/maddi#168). A superclass's static method is inherited, and is
 * printed bare.
 */
public class TestStaticInterfaceMethodQualifier extends CommonTest2 {

    @Language("java")
    private static final String ITEM = """
            package proj.v1;
            public interface Item {
                static Item from(Object o) { return null; }
                String name();
            }
            """;

    @Language("java")
    private static final String BASE = """
            package proj;
            public class Base {
                protected static String base(Object o) { return "b"; }
            }
            """;

    @Language("java")
    private static final String USE = """
            package proj;
            import proj.v1.Item;
            import java.util.Comparator;
            public class Use {
                public static final class Impl extends Base implements Item {
                    private final Object delegate;
                    public Impl(Object delegate) { this.delegate = delegate; }
                    @Override public String name() { return Item.from(delegate).name() + base(delegate); }
                }
                public static final class ByName implements Comparator<String> {
                    @Override public int compare(String a, String b) {
                        return Comparator.<String>naturalOrder().compare(a, b) + Comparator.reverseOrder().hashCode();
                    }
                }
                public interface Sub extends Item {
                    default Item again() { return Item.from(this); }
                }
            }
            """;

    @Test
    public void test() throws IOException {
        ParseResult parseResult = init(Map.of("proj.v1.Item", ITEM, "proj.Base", BASE, "proj.Use", USE));
        TypeInfo typeInfo = parseResult.findType("proj.Use");
        ImportComputer ic = javaInspector.importComputer(Integer.MAX_VALUE, typeInfo.compilationUnit().sourceSet());
        String printed = javaInspector.print2(typeInfo.compilationUnit(), (Qualification.Decorator) null, ic);
        assertTrue(printed.contains("return Item.from(delegate).name() + base(delegate);"), printed);
        assertTrue(printed.contains("Comparator.reverseOrder()"), printed);
        assertTrue(printed.contains("return Item.from(this);"), printed);
    }
}
