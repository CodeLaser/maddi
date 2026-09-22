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

package io.codelaser.maddi.kotlin.realm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * <b>First-one-wins, counted.</b> A flat classpath — and a realm's jar list, which is one — answers every class
 * name from the FIRST entry that has it. When two entries carry the same class, the later one's copy is dead,
 * and nothing says so until a method it has and the winner lacks is called. That is G46: {@code kotlin-compiler}
 * at entry 148 shadowed 174 of ANTLR's 215 classes at entry 197, and checkstyle died on a {@code CharStreams}
 * with one method left.
 * <p>
 * This reads the entries in the order the loader would, and reports, per entry, how many of its classes an
 * EARLIER entry already provides. It loads nothing. Two call sites use it: a distribution asserts on its own
 * {@code lib/} and {@code lib-k2/}, and a consumer asserts on the classpath it assembled.
 * <p>
 * ⚠ It counts NAMES, not bytes: two identical copies of a class are reported too, because the question is which
 * jar answers, and "identical today" is a property of two versions that nothing keeps true.
 */
public final class ClasspathCensus {

    /** One classpath entry that loses classes to an earlier one. */
    public record Shadowed(Path entry, int shadowedClasses, int classes, Path firstShadower, String example) {
    }

    private ClasspathCensus() {
    }

    /** Every entry that loses at least one class to an earlier entry, in classpath order. */
    public static List<Shadowed> census(List<Path> classpathInOrder) {
        Map<String, Path> answeredBy = new HashMap<>();
        List<Shadowed> result = new ArrayList<>();
        for (Path entry : classpathInOrder) {
            Set<String> classes = classEntries(entry);
            int shadowed = 0;
            Path firstShadower = null;
            String example = null;
            for (String name : classes) {
                Path earlier = answeredBy.putIfAbsent(name, entry);
                if (earlier != null) {
                    shadowed++;
                    if (firstShadower == null) {
                        firstShadower = earlier;
                        example = name;
                    }
                }
            }
            if (shadowed > 0) {
                result.add(new Shadowed(entry, shadowed, classes.size(), firstShadower, example));
            }
        }
        return result;
    }

    /** The entries that carry [classEntry] (e.g. {@code kotlinx/coroutines/Job.class}), in classpath order. */
    public static List<Path> providers(List<Path> classpathInOrder, String classEntry) {
        return classpathInOrder.stream().filter(e -> classEntries(e).contains(classEntry)).toList();
    }

    /** A path-separated classpath, as {@code -Dmaddi.k2.classpath} or {@code java.class.path} carries it. */
    public static List<Path> split(String classpath) {
        List<Path> entries = new ArrayList<>();
        for (String s : classpath.split(java.io.File.pathSeparator)) {
            if (!s.isBlank()) entries.add(Path.of(s));
        }
        return entries;
    }

    private static Set<String> classEntries(Path entry) {
        Set<String> names = new LinkedHashSet<>();
        try {
            if (Files.isDirectory(entry)) {
                try (Stream<Path> walk = Files.walk(entry)) {
                    walk.filter(p -> p.toString().endsWith(".class"))
                            .map(p -> entry.relativize(p).toString().replace('\\', '/'))
                            .filter(ClasspathCensus::isLoadableName)
                            .forEach(names::add);
                }
            } else if (Files.isRegularFile(entry)) {
                try (ZipFile zip = new ZipFile(entry.toFile())) {
                    zip.stream().map(java.util.zip.ZipEntry::getName)
                            .filter(n -> n.endsWith(".class"))
                            .filter(ClasspathCensus::isLoadableName)
                            .forEach(names::add);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read classpath entry " + entry, e);
        }
        return names;
    }

    /** not module-info, and not a multi-release overlay: neither is answered by first-one-wins */
    private static boolean isLoadableName(String name) {
        return !name.endsWith("module-info.class") && !name.startsWith("META-INF/");
    }
}
