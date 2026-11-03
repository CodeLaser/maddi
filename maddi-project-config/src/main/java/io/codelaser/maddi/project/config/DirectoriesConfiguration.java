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

package io.codelaser.maddi.project.config;

import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.run.config.util.JsonStreaming;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * A configuration from {@link Directories}: one source set per source directory ({@code main}, or {@code main/<dir>}
 * when there are several; {@code test} likewise), the tests seeing the main sets. Its build files are the Gradle and
 * Maven files under the working directory, hashed now, so that a change to the build is noticed, though nothing can
 * make the configuration again.
 */
public final class DirectoriesConfiguration {
    private DirectoriesConfiguration() {
    }

    public static InputConfiguration of(Directories d) throws IOException {
        Path base = d.workingDirectory().toAbsolutePath().normalize();
        List<Path> main = resolve(base, d.sourceDirectories());
        List<Path> test = resolve(base, d.testSourceDirectories());
        for (Path dir : concat(main, test)) {
            if (!Files.isDirectory(dir)) throw new IOException("no source directory " + dir);
        }
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();
        builder.setWorkingDirectory(base.toString());
        for (Path dir : main) builder.addSource(name("main", base, dir, main.size()), dir.toString());
        for (Path dir : test) builder.addTestSource(name("test", base, dir, test.size()), dir.toString());
        resolve(base, d.classPath()).forEach(p -> builder.addClassPath(p.toString()));
        resolve(base, d.testClassPath()).forEach(p -> builder.addTestClassPath(p.toString()));
        d.jdkModules().forEach(builder::addJmodToClassPath);
        if (d.sdkHome() != null) builder.setAlternativeJREDirectory(d.sdkHome());
        if (d.sourceRelease() > 0) builder.setSourceRelease(d.sourceRelease());
        if (d.sourceEncoding() != null) builder.setSourceEncoding(d.sourceEncoding());
        InputConfiguration configuration = builder.build();
        return configuration.withBuildFiles(BuildFileCheck.hashDefault(base, concat(main, test)));
    }

    /** As {@link #of}, written to {@code output}. */
    public static InputConfiguration write(Directories d, Path output) throws IOException {
        InputConfiguration configuration = of(d);
        if (output.toAbsolutePath().getParent() != null) Files.createDirectories(output.toAbsolutePath().getParent());
        Path tmp = output.resolveSibling(output.getFileName() + ".tmp");
        JsonStreaming.objectMapper().writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), configuration);
        Files.move(tmp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return configuration;
    }

    private static String name(String kind, Path base, Path dir, int count) {
        if (count == 1) return kind;
        String relative = dir.startsWith(base) ? base.relativize(dir).toString() : dir.toString();
        return kind + "/" + relative.replace(dir.getFileSystem().getSeparator(), "/");
    }

    private static List<Path> resolve(Path base, List<String> paths) {
        return paths.stream().map(p -> base.resolve(p).normalize()).toList();
    }

    private static List<Path> concat(List<Path> a, List<Path> b) {
        List<Path> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }
}
