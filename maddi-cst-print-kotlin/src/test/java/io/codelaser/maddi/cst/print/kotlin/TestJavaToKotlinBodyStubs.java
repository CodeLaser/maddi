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
package io.codelaser.maddi.cst.print.kotlin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;

/**
 * Bodies.STUB: the analysis's declarations, each method's body {@code TODO("maddi:<id>")} for another translator (an
 * LLM) to write, and {@code maddi-bodies.json} with what it needs: the Java lines, the declaration, the printer's own
 * translation, and the declarations of the translated methods the body calls.
 */
public class TestJavaToKotlinBodyStubs extends CommonJavaToKotlin {

    @Language("java")
    private static final String SOURCE = """
            package a;
            import java.util.*;
            public class Shop {
                private final Map<String, Integer> stock = new HashMap<>();
                private final String name;
                public Shop(String name) {
                    this.name = name;
                }
                public int count(String sku) {
                    Integer n = stock.get(sku);
                    return n == null ? 0 : n;
                }
                public void add(String sku, int k) {
                    stock.merge(sku, k, Integer::sum);
                    Runnable r = new Runnable() {
                        public void run() { System.out.println(count(sku)); }
                    };
                    r.run();
                }
                public String name() {
                    return name;
                }
                static Shop of(String name) {
                    return new Shop(name);
                }
            }
            """;

    private static final KotlinPrintOptions STUB = KotlinPrintOptions.DEFAULT.withBodies(KotlinPrintOptions.Bodies.STUB);

    @Test
    public void stubs() {
        String kotlin = kotlin(SOURCE, STUB);
        assertTrue(kotlin.contains("open fun count(sku: String): Int { TODO(\"maddi:a.Shop.count(String)\") }"), kotlin);
        assertTrue(kotlin.contains("TODO(\"maddi:a.Shop.add(String,int)\")"), kotlin);
        assertTrue(kotlin.contains("TODO(\"maddi:a.Shop.name()\")"), kotlin);
        assertTrue(kotlin.contains("TODO(\"maddi:a.Shop.of(String)\")"), kotlin);
        // the anonymous class is in add's body, which is a stub: no stub of its own, and nothing of it printed
        assertFalse(kotlin.contains("run()"), kotlin);
        assertFalse(kotlin.contains("println"), kotlin);
        // the constructor initialises the properties: printed (here as the primary constructor)
        assertTrue(kotlin.contains("open class Shop(private val name: String)"), kotlin);
        compiles(kotlin);
    }

    @Test
    public void printedAsBefore() {
        // one parse per source: the same class in another package, printed
        String stubbed = kotlin(SOURCE, STUB);
        String printed = kotlin(SOURCE.replace("package a;", "package b;"), KotlinPrintOptions.DEFAULT)
                .replace("package b", "package a");
        assertTrue(printed.contains("println"), printed);
        assertFalse(printed.contains("TODO("), printed);
        // the declarations are the same: every line of the stubbed file that is not a stub is in the printed one
        for (String line : stubbed.lines().toList()) {
            if (!line.contains("TODO(")) assertTrue(printed.contains(line.strip()), line + "\n----\n" + printed);
        }
    }

    @Test
    public void manifest() throws IOException {
        List<TypeInfo> types = List.copyOf(javaInspector.parse(Map.of("a.Shop", SOURCE),
                new JavaInspector.ParseOptions.Builder().build()).parseResult().primaryTypes());
        Path src = Files.createTempDirectory("maddi-stubs");
        KotlinTranslator.Result result = KotlinTranslator.translate(javaInspector.runtime(), types, STUB, src);
        assertEquals(List.of(), result.crashes());
        assertEquals(List.of("a.Shop.count(String)", "a.Shop.add(String,int)", "a.Shop.name()", "a.Shop.of(String)"),
                result.stubs().stream().map(KotlinBodyStub::id).toList());

        String json = Files.readString(src.resolve(KotlinTranslator.BODIES_FILE));
        KotlinBodyStub add = result.stubs().get(1);
        assertEquals("a/Shop.kt", add.kotlinFile());
        assertEquals(13, add.javaBeginLine());
        assertEquals(19, add.javaEndLine());
        assertEquals("open fun add(sku: String, k: Int)", add.declaration());
        // the printer's translation: the fallback, anonymous class included
        assertTrue(add.printed().startsWith("open fun add(sku: String, k: Int) {"), add.printed());
        assertTrue(add.printed().contains("println"), add.printed());
        // count, called from the anonymous class: a translated method, with its declaration; merge and println are
        // the library's, not listed
        assertEquals(List.of(new KotlinBodyStub.Callee("a.Shop.count(String)", "open fun count(sku: String): Int")),
                add.callees());

        // the fields the bodies read, as the properties they are: here the class's own, one of them the primary
        // constructor's
        KotlinBodyStub count = result.stubs().get(0);
        assertEquals(List.of(new KotlinBodyStub.Callee("a.Shop.stock", "private val stock: MutableMap<String, Int>")),
                count.fields());
        KotlinBodyStub name = result.stubs().get(2);
        assertEquals(List.of(new KotlinBodyStub.Callee("a.Shop.name", "private val name: String")), name.fields());

        KotlinBodyStub of = result.stubs().get(3);
        assertEquals(List.of(new KotlinBodyStub.Callee("a.Shop.<init>(String)", "constructor(private val name: String)")),
                of.callees());

        assertTrue(json.contains("\"id\": \"a.Shop.add(String,int)\""), json);
        assertTrue(json.contains("\"javaLines\": [13, 19]"), json);
        assertTrue(json.contains("\"fields\": [\n      {\"id\": \"a.Shop.stock\""), json);
    }
}
