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
