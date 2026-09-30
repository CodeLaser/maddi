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

package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.run.config.report.ExitCode;
import io.codelaser.maddi.run.config.util.JavaModules;
import io.codelaser.maddi.run.config.util.JsonStreaming;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.apache.commons.cli.Option;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>"One entry point" as a test.</b> {@code bin/maddi-kotlin} must be a strict SUPERSET of {@code bin/maddi}:
 * the same command line, the same behaviour on a project with no Kotlin in it, plus Kotlin. Until that held,
 * the Kotlin claim was a claim about a second tool with a different (much smaller) flag surface.
 */
/**
 * The tests of {@code TestOneEntryPoint} (maddi-run-kotlin) that run an analysis step, moved here in split stage 3
 * with a copy of its helpers; the option-parsing and parse-error tests stay there.
 */
public class TestOneEntryPointAnalysis {

    private static Set<String> longOptions(org.apache.commons.cli.Options options) {
        return options.getOptions().stream().map(Option::getLongOpt).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * ⭐ The real superset property: given the same arguments on a project with no {@code .kt} file, the mixed
     * CLI must produce the same exit code as the Java one — because it runs the same {@code RunAnalyzer}.
     */
    @Test
    public void aJavaOnlyProjectBehavesLikeTheJavaCli(@TempDir Path tmp) throws Exception {
        String[] args = javaOnlyProject(tmp, "Ok", "package x;\npublic class Ok { public int i; }\n");
        assertEquals(io.codelaser.maddi.run.openjdkmain.Main.execute(args), Main.execute(args));
        assertEquals(ExitCode.OK, Main.execute(args));
    }

    /** ⭐ ...and the other half: the option that moved from refused to honoured actually writes. */
    @Test
    public void theResultsDirectoryIsHonouredOnAKotlinProject(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("results");
        String[] args = concat(kotlinProject(tmp),
                "--analysis-steps", "modification", "--analysis-results-dir", out.toString());
        assertEquals(0, Main.execute(args));
        assertTrue(Files.isDirectory(out), "honoured, so results must be written");
        try (var walk = Files.walk(out)) {
            assertTrue(walk.anyMatch(Files::isRegularFile), "the results directory is empty");
        }
    }

    /** …and the same project without that option runs. The refusal must be about the option, not the project. */
    @Test
    public void theSameKotlinProjectRunsWithoutIt(@TempDir Path tmp) throws Exception {
        assertEquals(ExitCode.OK, Main.execute(concat(kotlinProject(tmp), "--analysis-steps", "prep")));
    }

    /** The explicit {@code --source}/{@code --jmod} route, which the Kotlin CLI did not have at all. */
    /**
     * {@code --skip-kotlin-sources} must mean the same thing on both CLIs. Here it is also the escape hatch
     * for the options the mixed pipeline refuses: ask for the Java half, get the whole Java feature set.
     */
    @Test
    public void skipKotlinSourcesTakesTheJavaRouteAndUnlocksItsOptions(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("results");
        String[] args = concat(kotlinProject(tmp), "--skip-kotlin-sources",
                "--analysis-steps", "prep", "--analysis-results-dir", out.toString());
        assertEquals(ExitCode.OK, Main.execute(args), "the Java pipeline honours --analysis-results-dir");
    }

    private static String[] javaOnlyProject(Path tmp, String name, String source) throws Exception {
        Path src = tmp.resolve("src");
        Files.createDirectories(src.resolve("x"));
        Files.writeString(src.resolve("x/" + name + ".java"), source);
        return new String[]{"--source", src.toString(), "--jmod", "java.base",
                "--analysis-steps", "prep"};
    }

    private static String[] kotlinProject(Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Files.createDirectories(kDir.resolve("a"));
        Files.writeString(kDir.resolve("a/Foo.kt"), "package a\nclass Foo(val id: Int)\n");
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).build();
        File configFile = tmp.resolve("input-configuration.json").toFile();
        // the JDK modules go IN the configuration: the mixed pipeline supplies them itself, the Java pipeline
        // (which --skip-kotlin-sources routes to) reads them from here, and one command line drives both.
        // ⚠ --jmod cannot do this: only the explicit --source route reads it (warned about, since today).
        JsonStreaming.objectMapper().writerFor(InputConfigurationImpl.class).writeValue(configFile,
                new InputConfigurationImpl.Builder().addSourceSets(kotlinSet)
                        .addClassPathParts(JavaModules.javaModuleSourceSets("java.base")).build());
        return new String[]{"--input-configuration", configFile.getAbsolutePath()};
    }

    private static String[] concat(String[] first, String... rest) {
        String[] all = java.util.Arrays.copyOf(first, first.length + rest.length);
        System.arraycopy(rest, 0, all, first.length, rest.length);
        return all;
    }
}
