package io.codelaser.maddi.run.config.compile;

import io.codelaser.maddi.cst.api.element.SourceSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROBE: an external library whose FILE NAME begins with a reactor module's directory name.
 *
 * <p>timefold ships a module at {@code persistence/jackson}, and its {@code service/definition} module compiles
 * against {@code jackson-annotations-2.22.jar} from the local repository. The generated configuration carries
 * neither that jar nor {@code jackson-core} / {@code jackson-databind}, while javac's own classpath for that
 * source set carries all three — and javac then reports
 * {@code package com.fasterxml.jackson.annotation does not exist} fifty times in one file.
 */
public class TestExternalJarNamedLikeAModule {

    private static final String ROOT = "/checkout/timefold";

    private record Invocation(String destination, List<String> sourcePath, List<String> classpath,
                              List<String> modulePath) implements CompileInvocation {
        Invocation(String destination, List<String> sourcePath, List<String> classpath) {
            this(destination, sourcePath, classpath, null);
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

    private static final String PJ_MAIN = ROOT + "/persistence/jackson/target/classes";
    private static final String PJ_JAR = ROOT + "/persistence/jackson/target/timefold-solver-jackson-1.0.jar";
    private static final String SD_MAIN = ROOT + "/service/definition/target/classes";

    private static final String M2 = "/home/u/.m2/repository/com/fasterxml/jackson/core";
    private static final String JACKSON_ANNOTATIONS = M2 + "/jackson-annotations/2.22/jackson-annotations-2.22.jar";
    private static final String JACKSON_CORE = M2 + "/jackson-core/2.21.4/jackson-core-2.21.4.jar";

    /**
     * ⚠ ON THE MODULE PATH, which is where it bites. The same jars on {@code -classpath} were always fine:
     * {@code handleClasspath} turns an unmatched archive into a library there. The module-path branch consults
     * {@code jarFileToDestination} first, and a NAME-based claim by a similarly named reactor module makes the
     * jar vanish from the parse entirely.
     */
    @DisplayName("an external jar whose name starts with a module directory name stays a library")
    @Test
    public void externalJarIsNotAbsorbedByASimilarlyNamedModule() {
        CompileListToSourceSets.Result r = new CompileListToSourceSets(ROOT).compute(List.of(
                new Invocation(PJ_MAIN, List.of(ROOT + "/persistence/jackson/src/main/java"), List.of()),
                new Invocation(SD_MAIN, List.of(ROOT + "/service/definition/src/main/java"),
                        List.of(PJ_JAR), List.of(JACKSON_ANNOTATIONS, JACKSON_CORE))));

        List<String> libs = r.jars().stream().map(SourceSet::name).sorted().toList();
        List<String> deps = r.jSourceSets().stream().map(CompileListToSourceSets.JSourceSet::sourceSet)
                .filter(s -> "service/definition/main".equals(s.name()))
                .findFirst().orElseThrow().dependencies().stream().map(SourceSet::name).sorted().toList();

        System.out.println("libraries      : " + libs);
        System.out.println("sd/main depends: " + deps);

        assertTrue(libs.stream().anyMatch(n -> n.contains("jackson-annotations")),
                "jackson-annotations must survive as a library: libs=" + libs + " deps=" + deps);
        assertTrue(libs.stream().anyMatch(n -> n.contains("jackson-core")),
                "jackson-core must survive as a library: libs=" + libs + " deps=" + deps);
        assertTrue(deps.contains("persistence/jackson/main"),
                "and the reactor sibling's own jar must still map to its source set: " + deps);
    }
}
