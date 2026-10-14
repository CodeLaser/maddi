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

package io.codelaser.maddi.inspection.resource;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.resource.BuildFiles;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Takes the {@link BuildFiles} of an input configuration: the hashes are the files' contents NOW, at capture time,
 * so a configuration captured before an edit and registered after it still reads as stale.
 * <p>
 * Each glob is searched from its literal prefix ({@code maddi/} for {@code maddi/**}{@code /build.gradle.kts}), not
 * from the root: a root such as {@code ~/git}, the common parent of the captured repositories, holds far more than
 * the analysed code. For the same reason module-info is not a root-wide glob: only the {@code module-info.java} at
 * the top of one of the configuration's source directories counts, which is where javac reads it, each as a literal
 * glob.
 */
public final class BuildFilesCapture {

    private BuildFilesCapture() {
    }

    /** Nothing declared: the server reports that staleness cannot be checked. The module-infos are watched all the same. */
    public static BuildFiles none(Path root, InputConfiguration configuration) {
        return capture(BuildFiles.NONE, root, List.of(), List.of(), configuration);
    }

    /** As {@link #capture(String, Path, List, Collection, InputConfiguration)}, without module-infos. */
    public static BuildFiles capture(String declaredBy, Path root, List<String> globs, Collection<Path> namedFiles) {
        return capture(declaredBy, root, globs, namedFiles, null);
    }

    /**
     * @param declaredBy    one of {@link BuildFiles}' producers
     * @param root          the directory the globs are relative to; made absolute
     * @param globs         patterns relative to root
     * @param namedFiles    files the producer knows it read, hashed whether or not a glob matches them (an included
     *                      build outside root, by a {@code ../} path)
     * @param configuration the configuration the build files shaped: the module-info at the top of each of its source
     *                      directories is added, as a literal glob; null for none
     */
    public static BuildFiles capture(String declaredBy, Path root, List<String> globs, Collection<Path> namedFiles,
                                     InputConfiguration configuration) {
        Path absoluteRoot = root.toAbsolutePath().normalize();
        Set<String> allGlobs = new LinkedHashSet<>(globs);
        List<Path> named = new ArrayList<>(namedFiles);
        for (Path moduleInfo : moduleInfos(configuration)) {
            named.add(moduleInfo);
            if (moduleInfo.startsWith(absoluteRoot)) allGlobs.add(key(absoluteRoot.relativize(moduleInfo)));
        }
        Map<String, String> sha256 = new TreeMap<>();
        for (String glob : allGlobs) {
            List<PathMatcher> matchers = new ArrayList<>();
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
            // '**/x' matches x at the root too, as in .gitignore; Java's glob wants a separator
            if (glob.startsWith("**/")) matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob.substring(3)));
            Path base = absoluteRoot.resolve(literalPrefix(glob)).normalize();
            if (Files.isRegularFile(base)) {
                Path relative = absoluteRoot.relativize(base);
                if (matchers.stream().anyMatch(m -> m.matches(relative))) sha256.put(key(relative), sha256(base));
            } else if (Files.isDirectory(base)) {
                walk(absoluteRoot, base, matchers, sha256);
            }
        }
        for (Path file : named) {
            Path absolute = absoluteRoot.resolve(file).normalize();
            if (Files.isRegularFile(absolute)) sha256.put(key(absoluteRoot.relativize(absolute)), sha256(absolute));
        }
        return new BuildFiles(declaredBy, absoluteRoot.toString(), List.copyOf(allGlobs), sha256,
                Instant.now().toString());
    }

    // the module-info.java at the top of each source directory of the configuration's own sources
    private static List<Path> moduleInfos(InputConfiguration configuration) {
        if (configuration == null) return List.of();
        Path workingDirectory = configuration.workingDirectory() == null ? Path.of(".")
                : configuration.workingDirectory();
        Set<Path> found = new LinkedHashSet<>();
        for (SourceSet sourceSet : configuration.sourceSets()) {
            if (sourceSet.externalLibrary()) continue;
            for (Path directory : sourceSet.sourceDirectories()) {
                Path moduleInfo = workingDirectory.resolve(directory).resolve("module-info.java")
                        .toAbsolutePath().normalize();
                if (Files.isRegularFile(moduleInfo)) found.add(moduleInfo);
            }
        }
        return List.copyOf(found);
    }

    // the leading segments without a wildcard: where the glob's files can be
    public static String literalPrefix(String glob) {
        StringBuilder prefix = new StringBuilder();
        for (String segment : glob.split("/")) {
            if (segment.chars().anyMatch(c -> c == '*' || c == '?' || c == '[' || c == '{')) break;
            if (!prefix.isEmpty()) prefix.append('/');
            prefix.append(segment);
        }
        return prefix.toString();
    }

    private static void walk(Path root, Path base, List<PathMatcher> matchers, Map<String, String> sha256) {
        try {
            Files.walkFileTree(base, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return !dir.equals(base) && BuildFiles.EXCLUDED_DIRECTORIES.contains(dir.getFileName().toString())
                            ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    Path relative = root.relativize(file);
                    if (matchers.stream().anyMatch(m -> m.matches(relative))) sha256.put(key(relative), sha256(file));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list the build files under " + base, e);
        }
    }

    // '/' whatever the platform: the server matches the same globs
    private static String key(Path relative) {
        return relative.toString().replace(relative.getFileSystem().getSeparator(), "/");
    }

    static String sha256(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) > 0; ) digest.update(buffer, 0, n);
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot hash " + file, e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
