package io.codelaser.maddi.run.config.compile;

import io.codelaser.maddi.cst.api.element.SourceSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cassandra's Ant build in miniature (2026-09-18), ON DISK, because the defects are only visible, and only
 * fixable, where the contents of the jars can be read — see {@link JarContentOwner}.
 * <pre>
 *   cassandra/build/classes/main                      main, compiled against build/lib/jars/*
 *   cassandra/build/lib/jars/cassandra-accord-1.0.jar a LIBRARY named like the root build unit
 *   cassandra/build/apache-cassandra-1.0.jar          main, packaged
 *   cassandra/build/test/classes                      the unit tests, compiled against the packaged jar
 *   cassandra/build/classes/simulator-asm             a tool, packaged as build/test/lib/jars/simulator-asm.jar
 *                                                     — and RECOMPILED into build/test/classes by the tests
 * </pre>
 */
public class TestAntLayout {

    private record Invocation(String destination, List<String> sourcePath, List<String> classpath)
            implements CompileInvocation {
        @Override
        public List<String> modulePath() {
            return null;
        }

        @Override
        public List<String> sourceFiles() {
            return List.of();
        }

        @Override
        public String encoding() {
            return null;
        }
    }

    private static void classFile(Path root, String name) throws IOException {
        Path p = root.resolve(name);
        Files.createDirectories(p.getParent());
        Files.write(p, new byte[]{(byte) 0xCA, (byte) 0xFE});
    }

    private static void jar(Path jar, String... classFiles) throws IOException {
        Files.createDirectories(jar.getParent());
        try (OutputStream os = Files.newOutputStream(jar); ZipOutputStream zip = new ZipOutputStream(os)) {
            for (String c : classFiles) {
                zip.putNextEntry(new ZipEntry(c));
                zip.write(new byte[]{(byte) 0xCA, (byte) 0xFE});
                zip.closeEntry();
            }
        }
    }

    private record Build(CompileListToSourceSets.Result result, String main, String test, String simulator) {
        SourceSet set(String destination) {
            return result.jSourceSets().stream().filter(js -> js.invocation().destination().equals(destination))
                    .findFirst().orElseThrow().sourceSet();
        }

        List<String> libraries() {
            return result.jars().stream().map(SourceSet::name).sorted().toList();
        }
    }

    private static Build build(Path dir, boolean onDisk) throws IOException {
        Path root = dir.resolve("cassandra");
        String main = root + "/build/classes/main";
        String test = root + "/build/test/classes";
        String accord = root + "/build/lib/jars/cassandra-accord-1.0.jar";
        String packaged = root + "/build/apache-cassandra-1.0.jar";
        String simulator = root + "/build/classes/simulator-asm";
        String simulatorJar = root + "/build/test/lib/jars/simulator-asm.jar";
        if (onDisk) {
            classFile(Path.of(simulator), "org/apache/cassandra/simulator/S.class");
            classFile(Path.of(test), "org/apache/cassandra/simulator/S.class");
            jar(Path.of(simulatorJar), "org/apache/cassandra/simulator/S.class");
            classFile(Path.of(main), "org/apache/cassandra/A.class");
            classFile(Path.of(main), "org/apache/cassandra/B.class");
            classFile(Path.of(test), "org/apache/cassandra/ATest.class");
            jar(Path.of(accord), "accord/Txn.class", "accord/Node.class");
            jar(Path.of(packaged), "org/apache/cassandra/A.class", "org/apache/cassandra/B.class");
        }
        CompileListToSourceSets.Result r = new CompileListToSourceSets(root.toString()).compute(List.of(
                new Invocation(main, List.of(root + "/src/java"), List.of(accord)),
                new Invocation(simulator, List.of(root + "/test/simulator/asm"), List.of(accord)),
                new Invocation(test, List.of(root + "/test/unit"), List.of(packaged, accord, simulatorJar))));
        return new Build(r, main, test, simulator);
    }

    @DisplayName("a library named like the root build unit stays a library")
    @Test
    public void libraryNamedLikeTheBuildUnit(@TempDir Path dir) throws IOException {
        Build b = build(dir, true);
        assertTrue(b.libraries().contains("cassandra-accord-1.0.jar"), "libraries: " + b.libraries());
        assertTrue(b.set(b.main()).dependencies().stream().anyMatch(d -> d.name().equals("cassandra-accord-1.0.jar")),
                "main must compile against accord: " + b.set(b.main()).dependencies());
    }

    @DisplayName("the packaged main jar on the test classpath is main's source set, not a library")
    @Test
    public void packagedJarIsTheSourceSet(@TempDir Path dir) throws IOException {
        Build b = build(dir, true);
        assertFalse(b.libraries().contains("apache-cassandra-1.0.jar"), "main would be parsed twice: " + b.libraries());
        List<String> deps = b.set(b.test()).dependencies().stream().map(SourceSet::name).toList();
        assertTrue(deps.contains("cassandra/main"), "test must depend on main's source set: " + deps);
    }

    @DisplayName("a jar whose classes live in two destinations belongs to the one it covers completely")
    @Test
    public void recompiledClassesDoNotStealTheJar(@TempDir Path dir) throws IOException {
        Build b = build(dir, true);
        assertFalse(b.libraries().contains("simulator-asm.jar"), "libraries: " + b.libraries());
        List<String> deps = b.set(b.test()).dependencies().stream().map(SourceSet::name).toList();
        assertTrue(deps.contains("cassandra/simulator-asm"), "test must depend on simulator-asm: " + deps);
    }

    @DisplayName("build/test/classes is the test set, named test")
    @Test
    public void antTestDirectory(@TempDir Path dir) throws IOException {
        Build b = build(dir, true);
        assertEquals("cassandra/main", b.set(b.main()).name());
        assertFalse(b.set(b.main()).test());
        assertEquals("cassandra/test", b.set(b.test()).name());
        assertTrue(b.set(b.test()).test());
    }

    /**
     * CONTROL: nothing on disk, so the contents cannot decide and the naming rules stand exactly as before —
     * including the claim this class corrects when the files do exist.
     */
    @DisplayName("CONTROL: without the files, the name rule is unchanged")
    @Test
    public void withoutFilesNothingChanges(@TempDir Path dir) throws IOException {
        Build b = build(dir, false);
        assertFalse(b.libraries().contains("cassandra-accord-1.0.jar"), "libraries: " + b.libraries());
        assertTrue(b.libraries().contains("apache-cassandra-1.0.jar"), "libraries: " + b.libraries());
        assertEquals("cassandra/test", b.set(b.test()).name(), "the naming fix needs no files");
    }
}
