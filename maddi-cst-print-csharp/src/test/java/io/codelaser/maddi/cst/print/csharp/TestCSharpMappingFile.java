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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A mapping file's errors, each with its file and line, and the parts of the format that only a client's file uses:
 * {@code load} from a path, a {@code compat} source beside it, {@code exclude} of a package. And the library choice's
 * errors ({@link CSharpLibraries#choose(String)}).
 */
public class TestCSharpMappingFile extends CommonJavaToCSharp {

    private static String error(String text) {
        return assertThrows(IllegalArgumentException.class, () -> CSharpMappingFile.parse(text, "m.csmap"))
                .getMessage();
    }

    @Test
    public void errors() {
        assertEquals("m.csmap: no mapping directive", error("# nothing\n"));
        assertEquals("m.csmap:1: the first directive must be: mapping <id>", error("type a.B -> B"));
        assertEquals("m.csmap:2: a second mapping directive", error("mapping x\nmapping y"));
        assertEquals("m.csmap:2: unknown directive mebmer", error("mapping x\nmebmer a.B.c/0 -> $0.C()"));
        assertEquals("m.csmap:2: expected: member <key> -> <value>", error("mapping x\nmember a.B.c/0 $0.C()"));
        assertEquals("m.csmap:2: expected: <key> [?guard]...", error("mapping x\nmember a.B c/0 -> $0.C()"));
        assertEquals("m.csmap:2: no guard ?nope", error("mapping x\nmember a.B.c/0 ?nope -> $0.C()"));
        assertEquals("m.csmap:2: expected: nuget <package> <version>", error("mapping x\nnuget Some.Package"));
        assertEquals("m.csmap:2: expected: exclude <type or package>", error("mapping x\nexclude a.B a.C"));
        assertEquals("m.csmap:2: expected: exclude <type or package>", error("mapping x\nexclude"));
        assertEquals("m.csmap:2: expected: <annotation type> [?on=target] [?element[=value]]",
                error("mapping x\nannotation a.B c -> C"));
        assertEquals("m.csmap:2: no target bogus; there are [type, field, method, constructor, parameter]",
                error("mapping x\nannotation a.B ?on=bogus -> C"));
        assertEquals("m.csmap:3: a second field line for a.B.C", error("mapping x\nfield a.B.C -> 1\nfield a.B.C -> 2"));
        assertEquals("m.csmap:3: a second override line for a.B.c/0",
                error("mapping x\noverride a.B.c/0 -> C\noverride a.B.c/0 -> D"));
    }

    /** A type or field mapping has one namespace: a second one is an error, not dropped. */
    @Test
    public void oneNamespace() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CSharpMappingFile.parse("mapping x\ntype a.B -> B using A, C", "two.csmap"));
        assertEquals("two.csmap:2: type takes one namespace, not A, C", e.getMessage());
    }

    /** A member's {@code ->?}: C# declares its result nullable; only a member's. */
    @Test
    public void nullableResult() {
        CSharpMappingFile file = CSharpMappingFile.parse("""
                mapping x
                member a.B.c/0 ->? $0.C()
                member a.B.d/0 -> $0.D()
                """, "nullable.csmap");
        assertTrue(file.memberRule("a.B.c/0", null, null).nullableResult());
        assertFalse(file.memberRule("a.B.d/0", null, null).nullableResult());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CSharpMappingFile.parse("mapping x\ntype a.B ->? B", "type.csmap"));
        assertEquals("type.csmap:2: ->? is a member's", e.getMessage());
    }

    /** A client's file from a path: its {@code compat} source is the file beside it. */
    @Test
    public void loadWithCompat(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("Extra.cs"), "namespace Extra; public static class Helper { }\n");
        Path file = dir.resolve("client.csmap");
        Files.writeString(file, """
                mapping client:extra
                nuget Some.Package 1.2.3
                compat Extra.cs
                """);
        CSharpMappingFile mapping = CSharpMappingFile.load(file);
        assertEquals("client:extra", mapping.id());
        assertEquals(List.of(new LibraryMapping.NuGetPackage("Some.Package", "1.2.3")),
                mapping.requirements().packages());
        LibraryMapping.Source source = mapping.requirements().sources().getFirst();
        assertEquals("Extra.cs", source.fileName());
        assertTrue(source.content().get().contains("class Helper"));
    }

    /** {@code exclude} of a package leaves out its types and those of its subpackages, not a package that starts so. */
    @Test
    public void excludePackage() {
        CSharpMappingFile mapping = CSharpMappingFile.parse("mapping x\nexclude org.gen\n", "x.csmap");
        assertTrue(mapping.excludes(type("org.gen", "A")));
        assertTrue(mapping.excludes(type("org.gen.sub", "B")));
        assertFalse(mapping.excludes(type("org.general", "C")));
    }

    private TypeInfo type(String pkg, String name) {
        String java = "package " + pkg + "; public class " + name + " { }\n";
        return javaInspector.parse(Map.of(pkg + "." + name, java), new JavaInspector.ParseOptions.Builder().build())
                .parseResult().primaryTypes().iterator().next();
    }

    @Test
    public void chooseErrors() {
        assertEquals("expected library=option, not slf4j", assertThrows(IllegalArgumentException.class,
                () -> CSharpLibraries.choose("slf4j")).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CSharpLibraries.choose("log4j=serilog"))
                .getMessage().startsWith("no library log4j; there are "));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CSharpLibraries.choose("slf4j=log4net"))
                .getMessage().startsWith("no option log4net for slf4j; there are "));
        // blank: the defaults
        assertNotNull(CSharpLibraries.choose(" "));
    }
}
