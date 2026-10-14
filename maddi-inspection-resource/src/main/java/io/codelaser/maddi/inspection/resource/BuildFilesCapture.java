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

import io.codelaser.maddi.inspection.api.resource.BuildFiles;

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
 */
public final class BuildFilesCapture {

    private BuildFilesCapture() {
    }

    /** Nothing declared: the server reports that staleness cannot be checked. Module-info is watched all the same. */
    public static BuildFiles none(Path root) {
        return capture(BuildFiles.NONE, root, List.of(), List.of());
    }

    /**
     * @param declaredBy one of {@link BuildFiles}' producers
     * @param root       the directory the globs are relative to; made absolute
     * @param globs      patterns relative to root; {@link BuildFiles#MODULE_INFO} is added when missing
     * @param namedFiles files the producer knows it read, hashed whether or not a glob matches them (an included
     *                   build outside root, by a {@code ../} path)
     */
    public static BuildFiles capture(String declaredBy, Path root, List<String> globs, Collection<Path> namedFiles) {
        Path absoluteRoot = root.toAbsolutePath().normalize();
        Set<String> allGlobs = new LinkedHashSet<>(globs);
        allGlobs.add(BuildFiles.MODULE_INFO);
        List<PathMatcher> matchers = new ArrayList<>();
        for (String glob : allGlobs) {
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
            // '**/x' matches x at the root too, as in .gitignore; Java's glob wants a separator
            if (glob.startsWith("**/")) matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob.substring(3)));
        }
        Map<String, String> sha256 = new TreeMap<>();
        if (Files.isDirectory(absoluteRoot)) {
            try {
                Files.walkFileTree(absoluteRoot, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        return !dir.equals(absoluteRoot)
                               && BuildFiles.EXCLUDED_DIRECTORIES.contains(dir.getFileName().toString())
                                ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        Path relative = absoluteRoot.relativize(file);
                        if (matchers.stream().anyMatch(m -> m.matches(relative))) {
                            sha256.put(key(relative), sha256(file));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot list the build files under " + absoluteRoot, e);
            }
        }
        for (Path named : namedFiles) {
            Path file = absoluteRoot.resolve(named).normalize();
            if (Files.isRegularFile(file)) sha256.put(key(absoluteRoot.relativize(file)), sha256(file));
        }
        return new BuildFiles(declaredBy, absoluteRoot.toString(), List.copyOf(allGlobs), sha256,
                Instant.now().toString());
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
