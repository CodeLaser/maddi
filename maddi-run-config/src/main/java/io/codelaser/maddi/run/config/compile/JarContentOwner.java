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

package io.codelaser.maddi.run.config.compile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Which compile destination a packaged jar was built from, decided by what the jar CONTAINS rather than by its
 * name or its directory.
 *
 * <p>⛔ <b>NAME AND PATH ARE CONVENTIONS, AND ANT HAS NEITHER.</b> {@link CompileListToSourceSets} maps a jar to a
 * sibling source set by path (maven's {@code target/}) and by name (gradle's {@code build/libs/<module>-1.0.jar}).
 * Cassandra's Ant build broke both at once (2026-09-18):
 * <ul>
 *   <li>the root build unit is called {@code cassandra}, so the name rule claimed {@code cassandra-accord-*.jar}
 *       and {@code cassandra-driver-core-*.jar} — third-party libraries Ant copies into {@code build/lib/jars},
 *       inside the build root — for {@code cassandra/main}. A claimed jar is never made a library, so both left
 *       the parse, and with them every {@code accord.*} type ~200 main files import;</li>
 *   <li>the unit tests compile against {@code build/apache-cassandra-7.0-SNAPSHOT.jar}, which neither rule maps to
 *       {@code build/classes/main}; it became a library, so the whole of main entered the parse twice, once from
 *       source and once from bytecode.</li>
 * </ul>
 *
 * <p>When the jar and the destinations exist on disk, their contents answer the question exactly: a jar is a
 * destination's output iff most of its class files are that destination's class files. When they do not exist
 * (a compile log read away from its build, or a unit-test fixture) the answer is {@link Optional#empty()} and
 * the caller keeps its naming rules — this can only correct them, never replace them with a guess.
 */
class JarContentOwner {
    private static final Logger LOGGER = LoggerFactory.getLogger(JarContentOwner.class);

    /** A jar whose class files are mostly (more than half) in one destination belongs to it. */
    private static final double MAJORITY = 0.5;

    private final List<String> destinations;
    private final Map<String, Integer> classCount = new HashMap<>();
    private Map<String, List<String>> destinationsByClassFile;

    JarContentOwner(Collection<String> destinations) {
        this.destinations = List.copyOf(new LinkedHashSet<>(destinations));
    }

    /**
     * {@code Optional.empty()}: the contents cannot decide (jar unreadable, or no destination exists on disk).
     * {@code Optional.of(Owner.NONE)}: they decide that the jar belongs to no destination.
     */
    Optional<Owner> ownerOf(String jar) {
        Map<String, List<String>> index = index();
        if (index.isEmpty()) return Optional.empty();
        List<String> classFiles = classFiles(jar);
        if (classFiles == null || classFiles.isEmpty()) return Optional.empty();
        Map<String, Integer> hits = new HashMap<>();
        for (String classFile : classFiles) {
            for (String destination : index.getOrDefault(classFile, List.of())) {
                hits.merge(destination, 1, Integer::sum);
            }
        }
        // ⚠ ONE CLASS FILE CAN LIVE IN SEVERAL DESTINATIONS: cassandra's unit-test javac recompiles the simulator
        // sources, so build/test/classes holds every class of simulator-asm.jar too. Of the destinations holding
        // most of the jar, the jar's own is the one it covers most completely.
        return Optional.of(hits.entrySet().stream()
                .filter(e -> e.getValue() > MAJORITY * classFiles.size())
                .max(Comparator.comparingDouble(e -> (double) e.getValue() / classCount.get(e.getKey())))
                .map(e -> new Owner(e.getKey()))
                .orElse(Owner.NONE));
    }

    /** The owning destination, or {@code null} for {@link #NONE}. */
    record Owner(String destination) {
        static final Owner NONE = new Owner(null);
    }

    private Map<String, List<String>> index() {
        if (destinationsByClassFile == null) {
            destinationsByClassFile = new HashMap<>();
            for (String destination : destinations) {
                Path root = Path.of(destination);
                if (!Files.isDirectory(root)) continue;
                try (Stream<Path> walk = Files.walk(root)) {
                    walk.filter(p -> p.toString().endsWith(".class")).forEach(p -> {
                        destinationsByClassFile.computeIfAbsent(root.relativize(p).toString().replace('\\', '/'),
                                k -> new ArrayList<>(1)).add(destination);
                        classCount.merge(destination, 1, Integer::sum);
                    });
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("Cannot index class files of {}: {}", destination, e.toString());
                }
            }
        }
        return destinationsByClassFile;
    }

    private static List<String> classFiles(String jar) {
        if (!Files.isRegularFile(Path.of(jar))) return null;
        try (ZipFile zip = new ZipFile(jar)) {
            return zip.stream().map(ZipEntry::getName)
                    .filter(n -> n.endsWith(".class") && !n.startsWith("META-INF/"))
                    .toList();
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot read {} to decide which source set built it: {}", jar, e.toString());
            return null;
        }
    }
}
