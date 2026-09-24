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
 * Pulsar's shaded client in miniature (2026-09-24), ON DISK, like {@link TestAntLayout}.
 * <pre>
 *   pulsar/pulsar-common/build/classes/java/main          org.apache.pulsar.common.*
 *   pulsar/pulsar-client/build/classes/java/main          org.apache.pulsar.client.impl.*
 *   pulsar/pulsar-client-shaded/build/libs/pulsar-client-1.0.jar
 *                                                         BOTH of those, UNRELOCATED, plus the relocated
 *                                                         third-party code under org.apache.pulsar.shade.*
 *   pulsar/tests/pulsar-client-shade-test/.../test        compiled against the shaded jar ONLY
 * </pre>
 * No destination holds a majority of the jar, so {@link JarContentOwner} (rightly) gives it to none, and until
 * this test it became an opaque library. That cost the real pulsar parse 200+ compilation units: the shade test
 * depends on no reactor source set, so it may be parsed FIRST, and it then loads org.apache.pulsar.client.impl.*
 * out of the jar -- after which pulsar-broker, which compiles against the pulsar-client SOURCES, failed with
 * "Cannot map javac's type 'org.apache.pulsar.client.impl.ClientCnx' onto a TypeInfo". The 2026-09-17
 * configuration, made before content decided, claimed the jar by NAME for pulsar-client/main: the ordering was
 * right, but the relocated classes were gone, and the shade test's six files could not compile.
 * <p>
 * A jar made of several destinations' classes plus code of its own is a COMPOSITE: its consumer depends on every
 * destination that contributed (so their sources are parsed and committed first), and on the jar itself, as a
 * library, for what no destination produced.
 */
public class TestShadedJar {

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

    private record Build(CompileListToSourceSets.Result result, String common, String client, String shadeTest,
                         String netty) {
        SourceSet set(String destination) {
            return result.jSourceSets().stream().filter(js -> js.invocation().destination().equals(destination))
                    .findFirst().orElseThrow().sourceSet();
        }

        List<String> libraries() {
            return result.jars().stream().map(SourceSet::name).sorted().toList();
        }

        List<String> deps(String destination) {
            return set(destination).dependencies().stream().map(SourceSet::name).toList();
        }
    }

    private static Build build(Path dir) throws IOException {
        Path root = dir.resolve("pulsar");
        String common = root + "/pulsar-common/build/classes/java/main";
        String client = root + "/pulsar-client/build/classes/java/main";
        String shaded = root + "/pulsar-client-shaded/build/libs/pulsar-client-1.0.jar";
        String shadeTest = root + "/tests/pulsar-client-shade-test/build/classes/java/test";
        String netty = root + "/netty/build/classes/java/main"; // a control: a plain library consumer
        String nettyJar = dir.resolve("m2/netty-buffer-4.2.jar").toString();

        classFile(Path.of(common), "org/apache/pulsar/common/protocol/Commands.class");
        classFile(Path.of(client), "org/apache/pulsar/client/impl/ClientCnx.class");
        classFile(Path.of(client), "org/apache/pulsar/client/impl/MessageIdImpl.class");
        jar(Path.of(shaded),
                "org/apache/pulsar/common/protocol/Commands.class",
                "org/apache/pulsar/client/impl/ClientCnx.class",
                "org/apache/pulsar/client/impl/MessageIdImpl.class",
                "org/apache/pulsar/shade/io/netty/buffer/ByteBuf.class",
                "org/apache/pulsar/shade/io/netty/buffer/Unpooled.class",
                "org/apache/pulsar/shade/io/netty/util/Recycler.class",
                "org/apache/pulsar/shade/com/google/common/collect/Lists.class");
        jar(Path.of(nettyJar), "io/netty/buffer/ByteBuf.class", "io/netty/buffer/Unpooled.class");

        CompileListToSourceSets.Result r = new CompileListToSourceSets(root.toString()).compute(List.of(
                new Invocation(common, List.of(root + "/pulsar-common/src/main/java"), List.of(nettyJar)),
                new Invocation(client, List.of(root + "/pulsar-client/src/main/java"), List.of(common, nettyJar)),
                new Invocation(netty, List.of(root + "/netty/src/main/java"), List.of(nettyJar)),
                new Invocation(shadeTest, List.of(root + "/tests/pulsar-client-shade-test/src/test/java"),
                        List.of(shaded))));
        return new Build(r, common, client, shadeTest, netty);
    }

    @DisplayName("a shaded jar's consumer depends on every reactor source set that contributed classes to it")
    @Test
    public void consumerDependsOnTheContributors(@TempDir Path dir) throws IOException {
        Build b = build(dir);
        List<String> deps = b.deps(b.shadeTest());
        assertTrue(deps.contains(b.set(b.client()).name()), "the pulsar-client sources must be parsed first: " + deps);
        assertTrue(deps.contains(b.set(b.common()).name()), "and the pulsar-common ones: " + deps);
    }

    @DisplayName("and on the jar itself, as a library, for the relocated classes no source set produced")
    @Test
    public void theJarStaysALibrary(@TempDir Path dir) throws IOException {
        Build b = build(dir);
        assertTrue(b.libraries().contains("pulsar-client-1.0.jar"), "libraries: " + b.libraries());
        assertTrue(b.deps(b.shadeTest()).contains("pulsar-client-1.0.jar"),
                "org.apache.pulsar.shade.* is only in the jar: " + b.deps(b.shadeTest()));
    }

    @DisplayName("CONTROL: a library that shares no class with any destination gains no source-set dependency")
    @Test
    public void plainLibraryIsUntouched(@TempDir Path dir) throws IOException {
        Build b = build(dir);
        assertEquals(List.of("netty-buffer-4.2.jar"), b.deps(b.netty()));
    }
}
