package io.codelaser.maddi.inspection.openjdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

/**
 * Three source sets on the same two jars: their javac file managers share one archive file system per jar
 * ({@link SharedArchives}), and the types of both jars still resolve. On laser1's refactor server one model held
 * 10,479 archive file systems for 908 jars, each opened by every source set that had it on its class path.
 */
public class TestSharedArchives {

    private static URI jarOf(Class<?> clazz) throws URISyntaxException {
        return clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
    }

    private static String fileName(URI uri) {
        return Path.of(uri).getFileName().toString();
    }

    @DisplayName("source sets on the same jars share one archive file system per jar")
    @Test
    public void test(@TempDir Path dir) throws IOException, URISyntaxException {
        assumeTrue(SharedArchives.available(), "com.sun.tools.javac.file is not exported to this test JVM");
        // a class-path source set is matched by its jar's file name
        SourceSet slf4j = new SourceSetImpl.Builder().setName(fileName(jarOf(Logger.class))).setUri(jarOf(Logger.class))
                .setExternalLibrary(true).build();
        SourceSet annotations = new SourceSetImpl.Builder().setName(fileName(jarOf(NotNull.class)))
                .setUri(jarOf(NotNull.class))
                .setExternalLibrary(true).build();
        List<SourceSet> sets = new java.util.ArrayList<>();
        for (String name : List.of("a", "b", "c")) {
            Path src = dir.resolve(name + "/src");
            Files.createDirectories(src.resolve(name));
            Files.writeString(src.resolve(name + "/Uses.java"), """
                    package %s;
                    import org.jetbrains.annotations.NotNull;
                    import org.slf4j.Logger;
                    import org.slf4j.LoggerFactory;
                    public class Uses {
                        private static final Logger LOGGER = LoggerFactory.getLogger(Uses.class);
                        public @NotNull String name() { LOGGER.info("x"); return "%s"; }
                    }
                    """.formatted(name, name));
            sets.add(new SourceSetImpl.Builder().setName(name + "/main").setSourceDirectories(List.of(src))
                    .setUri(dir.resolve(name + "/classes").toUri())
                    .setDependencies(List.of(slf4j, annotations)).build());
        }
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sets.toArray(SourceSet[]::new))
                .addClassPath("jmod:java.base")
                .addClassPathParts(slf4j, annotations)
                .build();
        JavaInspectorImpl javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        ParseResult parseResult = javaInspector.parse(Map.of(), JavaInspectorImpl.FAIL_FAST).parseResult();
        for (String name : List.of("a", "b", "c")) {
            assertNotNull(parseResult.findType(name + ".Uses"), name);
        }
        assertEquals(2, javaInspector.sharedArchiveCount(), "one archive file system per jar, not one per source set");
        javaInspector.invalidateAllSources();
        assertEquals(0, javaInspector.sharedArchiveCount());
    }
}
