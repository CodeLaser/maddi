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

package io.codelaser.maddi.run.openjdkmain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code --translate kotlin|csharp}: the translators behind the CLI. Here without the analysis
 * ({@code --analysis-steps none}), which needs maddi-mod on the class path; the translation with the nullability
 * pass's facts is maddi-mod's TestTranslateWithFacts.
 */
public class TestTranslateOption {

    private static Path project(Path tmp) throws Exception {
        Path src = tmp.resolve("src");
        Files.createDirectories(src.resolve("p"));
        Files.writeString(src.resolve("p/Box.java"), """
                package p;
                public class Box {
                    private final String name;
                    public Box(String name) { this.name = name; }
                    public String name() { return name; }
                }
                """);
        return src;
    }

    private static int translate(Path src, String language, Path out) throws Exception {
        return Main.execute(new String[]{
                "--" + Main.SOURCE, src.toString(),
                "--" + Main.JMOD, "java.base",
                "--" + Main.ANALYSIS_STEPS, Main.AS_NONE,
                "--" + Main.TRANSLATE, language,
                "--" + Main.TRANSLATE_OUTPUT, out.toString()});
    }

    @Test
    public void kotlin(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("kotlin");
        assertEquals(Main.EXIT_OK, translate(project(tmp), Main.TRANSLATE_KOTLIN, out));
        String box = Files.readString(out.resolve("p/Box.kt"));
        assertTrue(box.contains("class Box"), box);
        assertTrue(Files.readString(out.resolve("translation-report.txt"))
                .startsWith("kotlin: 1 types, 1 files, 0 printer crashes"));
    }

    @Test
    public void csharp(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("csharp");
        assertEquals(Main.EXIT_OK, translate(project(tmp), Main.TRANSLATE_CSHARP, out));
        String box = Files.readString(out.resolve("p/Box.cs"));
        assertTrue(box.contains("class Box"), box);
        assertTrue(Files.isRegularFile(out.resolve("Translated.csproj")));
    }

    @Test
    public void unknownLanguageIsRefused(@TempDir Path tmp) throws Exception {
        assertEquals(Main.EXIT_UNSUPPORTED_OPTION, translate(project(tmp), "scala", tmp.resolve("out")));
        assertFalse(Files.exists(tmp.resolve("out")));
    }

    @Test
    public void anOutputDirectoryIsRequired(@TempDir Path tmp) throws Exception {
        assertEquals(Main.EXIT_UNSUPPORTED_OPTION, Main.execute(new String[]{
                "--" + Main.SOURCE, project(tmp).toString(),
                "--" + Main.JMOD, "java.base",
                "--" + Main.TRANSLATE, Main.TRANSLATE_KOTLIN}));
    }
}
