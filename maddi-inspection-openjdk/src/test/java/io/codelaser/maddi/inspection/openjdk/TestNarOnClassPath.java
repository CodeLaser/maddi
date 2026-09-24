package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A library archive that is not named {@code .jar} maps its types onto the library's source set.
 * <p>
 * Apache BookKeeper publishes {@code circe-checksum} and {@code cpu-affinity} as {@code .nar} (a jar with native
 * libraries beside the classes), and Apache Pulsar puts both on javac's class path. The configuration already carries
 * them (SourceSetImpl.ARCHIVE_EXTENSIONS), javac reads them, but ClassSymbolScanner recognised a class file's archive
 * with a pattern that matched {@code .jar} only: every type read from the {@code .nar} fell through to "off the
 * classpath", and the parse of pulsar-common failed on "Cannot map javac's type
 * 'com.scurrilous.circe.checksum.Crc32cIntChecksum' ... onto a TypeInfo" (slowTest 2026-09-22,
 * TestCycleMeasurementRatchetPulsar). The {@code .jar} case is the control: same bytes, other name.
 */
public class TestNarOnClassPath {

    @Language("java")
    private static final String LIB = """
            package com.scurrilous.circe.checksum;
            public final class Crc32cIntChecksum {
                public static int computeChecksum(byte[] data) { return data.length; }
            }
            """;

    @Language("java")
    private static final String USER = """
            package c.d;
            import com.scurrilous.circe.checksum.Crc32cIntChecksum;
            public class User {
                public int use(byte[] data) { return Crc32cIntChecksum.computeChecksum(data); }
            }
            """;

    @TempDir
    Path root;

    @DisplayName("control: a type read from a .jar maps onto that library")
    @Test
    public void jar() throws IOException {
        archiveTypeMapsOntoItsLibrary("circe-checksum-4.18.0.jar");
    }

    @DisplayName("a type read from a .nar maps onto that library, as one from a .jar does")
    @Test
    public void nar() throws IOException {
        archiveTypeMapsOntoItsLibrary("circe-checksum-4.18.0.nar");
    }

    private void archiveTypeMapsOntoItsLibrary(String archiveName) throws IOException {
        Path libSrc = Files.createDirectories(root.resolve("lib-src/com/scurrilous/circe/checksum"));
        Files.writeString(libSrc.resolve("Crc32cIntChecksum.java"), LIB);
        Path libClasses = Files.createDirectories(root.resolve("lib-classes"));
        compile(List.of(libSrc.resolve("Crc32cIntChecksum.java")), libClasses);
        Path archive = root.resolve(archiveName);
        pack(libClasses, archive);

        Path userSrc = Files.createDirectories(root.resolve("user-src/c/d"));
        Files.writeString(userSrc.resolve("User.java"), USER);

        // the shape the generated pulsar configuration has: the archive as a library, named after its file, and a
        // dependency of the source set that reads it
        SourceSet lib = new SourceSetImpl.Builder().setName(archiveName)
                .setSourceDirectories(List.of())
                .setUri(archive.toUri())
                .setLibrary(true).setExternalLibrary(true)
                .build();
        SourceSet user = new SourceSetImpl.Builder().setName("user")
                .setSourceDirectories(List.of(root.resolve("user-src")))
                .setUri(root.resolve("user-classes").toUri())
                .setDependencies(List.of(lib))
                .build();

        JavaInspector javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(new InputConfigurationImpl.Builder()
                .addSourceSets(user)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPathParts(lib)
                .build());
        Summary summary = javaInspector.parse(Map.of(),
                new JavaInspector.ParseOptions.Builder().setFailFast(false).build());

        // vacuity first: the user's source set must have parsed at all
        assertEquals(List.of("c.d.User"), summary.types().stream().map(TypeInfo::fullyQualifiedName).toList(),
                "the source set must actually have parsed: " + messages(summary));
        assertFalse(summary.haveErrors(), "the parse must be clean: " + messages(summary));

        TypeInfo crc = javaInspector.compiledTypesManager()
                .typeIfLoaded("com.scurrilous.circe.checksum.Crc32cIntChecksum", user);
        assertNotNull(crc, "the library type must be known");
        assertEquals(archiveName, crc.compilationUnit().sourceSet().name(),
                "and belong to the archive it was read from");
    }

    private static List<String> messages(Summary summary) {
        return Stream.concat(summary.parseExceptions().stream(), summary.parseWarnings().stream())
                .map(e -> String.valueOf(e.getMessage())).toList();
    }

    private static void pack(Path classes, Path archive) throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(archive));
             Stream<Path> files = Files.walk(classes)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                out.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, out);
                out.closeEntry();
            }
        }
    }

    private static void compile(List<Path> files, Path outputDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(files);
            assertTrue(compiler.getTask(null, fm, null, List.of(), null, units).call(),
                    "could not compile the library sources");
        }
    }
}
