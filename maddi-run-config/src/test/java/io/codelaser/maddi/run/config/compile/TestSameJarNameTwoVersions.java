package io.codelaser.maddi.run.config.compile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.element.SourceSet;

/**
 * Two source sets compiled against two versions of one distribution, whose jars share their file names: maddi-dist's
 * maddi-intellij (IntelliJ 2025.3) and maddi-intellij-j2k (2026.2.3) both have '.../plugins/Kotlin/lib/kotlin-plugin.jar'.
 * Each set must depend on its own jar. Mapped onto the first jar of that name, j2k was parsed against 2025.3, and
 * J2kStarter, which calls a 2026.2 handler, was dropped on every load of the audit.
 */
public class TestSameJarNameTwoVersions {

    private static final String ROOT = "/checkout/maddi-dist";
    private static final String CACHE = "/home/u/.gradle/caches/transforms";
    private static final String OLD = CACHE + "/63907d4e/transformed/idea-2025.3/plugins/Kotlin/lib/kotlin-plugin.jar";
    private static final String NEW = CACHE + "/2c0a7ff8/transformed/idea-2026.2.3/plugins/Kotlin/lib/kotlin-plugin.jar";

    private record Invocation(String destination, List<String> sourcePath, List<String> classpath,
                              List<String> modulePath) implements CompileInvocation {
        @Override
        public List<String> sourceFiles() {
            return List.of();
        }

        @Override
        public String encoding() {
            return null;
        }
    }

    private static List<String> dependencies(CompileListToSourceSets.Result r, String name) {
        return r.jSourceSets().stream().map(CompileListToSourceSets.JSourceSet::sourceSet)
                .filter(s -> name.equals(s.name())).findFirst().orElseThrow()
                .dependencies().stream().map(s -> s.uri().toString()).sorted().toList();
    }

    @DisplayName("two jars of one file name in two versions are two libraries, each set depending on its own")
    @Test
    public void test() {
        CompileListToSourceSets.Result r = new CompileListToSourceSets(ROOT).compute(List.of(
                new Invocation(ROOT + "/maddi-intellij/build/classes/java/main",
                        List.of(ROOT + "/maddi-intellij/src/main/java"), List.of(OLD), null),
                new Invocation(ROOT + "/maddi-intellij-j2k/build/classes/java/main",
                        List.of(ROOT + "/maddi-intellij-j2k/src/main/java"), List.of(NEW), null)));

        assertEquals(List.of("file:" + OLD), dependencies(r, "maddi-intellij/main"));
        assertEquals(List.of("file:" + NEW), dependencies(r, "maddi-intellij-j2k/main"));
        // the first keeps the plain name; the second is named by the shortest path tail that tells it apart
        List<String> names = r.jars().stream().map(SourceSet::name).sorted().toList();
        assertEquals(List.of("idea-2026.2.3/plugins/Kotlin/lib/kotlin-plugin.jar", "kotlin-plugin.jar"), names);
    }
}
