package io.codelaser.maddi.run.config.compile;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A build at {@code -source 8} compiles without module encapsulation; the parse does not. The set then states
 * the exports the build never had to write. See {@link EncapsulatedImports}.
 */
public class TestEncapsulatedImports {

    private record Invocation(String destination, List<String> sourcePath, int release, int sourceRelease)
            implements CompileInvocation {
        @Override
        public List<String> classpath() {
            return List.of();
        }

        @Override
        public List<String> modulePath() {
            return List.of();
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

    private static final String KERBEROS = """
            package a.b;

            import java.util.List;
            import sun.security.krb5.Config;
            import static sun.security.util.SecurityConstants.ALL_PERMISSION;
            import no.such.pkg.Thing;

            public class Kerberos {
                // import sun.misc.Unsafe; -- after the first type: not an import
            }
            """;

    private static SourceSet only(Path root, int release, int sourceRelease) throws Exception {
        Path src = root.resolve("m/src/main/java/a/b");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Kerberos.java"), KERBEROS);
        Invocation invocation = new Invocation(root.resolve("m/target/classes").toString(),
                List.of(root.resolve("m/src/main/java").toString()), release, sourceRelease);
        InputConfiguration ic = CompileListToInputConfiguration.build(
                new CompileListToSourceSets(root.toString()).compute(List.of(invocation)), List.of());
        return ic.sourceSets().stream().filter(s -> !s.externalLibrary()).findFirst().orElseThrow();
    }

    @DisplayName("-source 8, no --release: the encapsulated JDK packages the sources import are exported to the set")
    @Test
    public void belowTheModuleSystem(@TempDir Path root) throws Exception {
        assertEquals(List.of("java.base/sun.security.util=ALL-UNNAMED",
                        "java.security.jgss/sun.security.krb5=ALL-UNNAMED"),
                only(root, 0, 8).addExports());
    }

    @DisplayName("CONTROL: -source 17 compiled WITH encapsulation, and so does the parse")
    @Test
    public void aModularSourceLevelGetsNone(@TempDir Path root) throws Exception {
        assertEquals(List.of(), only(root, 0, 17).addExports());
    }

    @DisplayName("CONTROL: --release 8 goes through ct.sym, which never had the package: nothing to open")
    @Test
    public void aReleaseGetsNone(@TempDir Path root) throws Exception {
        assertEquals(List.of(), only(root, 8, 8).addExports());
    }

    @DisplayName("an exported package, and one the JDK does not have, are not encapsulated")
    @Test
    public void whatIsNotEncapsulated() {
        assertNull(EncapsulatedImports.encapsulated("java.util"));
        assertNull(EncapsulatedImports.encapsulated("no.such.pkg"));
    }
}
