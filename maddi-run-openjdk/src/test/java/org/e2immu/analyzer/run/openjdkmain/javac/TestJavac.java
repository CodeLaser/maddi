package org.e2immu.analyzer.run.openjdkmain.javac;

import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class TestJavac {

    @Test
    public void testE2ImmuMvn() throws IOException {
        Path path = Path.of("src/test/resources/mvnE2ImmuMvnPlugin.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(1, javacList.size());
        Javac j = javacList.getFirst();
        assertEquals(21, j.release());
        assertEquals("/Users/bnaudts/git/analyzer-runmvn/e2immu-run-mvnplugin/target/classes", j.destination());
        assertEquals(55, j.classpath().size());
        assertEquals(54, j.classpath().stream().filter(cp -> cp.contains(".m2") && cp.endsWith(".jar")).count());
        assertEquals(2, j.sourcePath().size());
        assertEquals("/Users/bnaudts/git/analyzer-runmvn/e2immu-run-mvnplugin/target/generated-sources/annotations",
                j.generatedSourceFilesDestination());
        assertEquals("UTF-8", j.encoding());
    }

    @Test
    public void testJFocusMvn() throws IOException {
        Path path = Path.of("src/test/resources/mvnJFocusRefactorMvnPlugin.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(1, javacList.size());
        Javac j = javacList.getFirst();
        assertEquals(21, j.release());
        assertEquals("/Users/bnaudts/git/jfocus-refactormvn/codelaser-refactor-mvnplugin/target/classes", j.destination());
        assertEquals(53, j.classpath().size());
        assertEquals(52, j.classpath().stream().filter(cp -> cp.contains(".m2") && cp.endsWith(".jar")).count());
        assertEquals(2, j.sourcePath().size());
        assertEquals("/Users/bnaudts/git/jfocus-refactormvn/codelaser-refactor-mvnplugin/target/generated-sources/annotations",
                j.generatedSourceFilesDestination());
        assertEquals("UTF-8", j.encoding());
        InputConfiguration inputConfiguration = new ParseJavacList().inputConfiguration(javacList, List.of());
        String inputConfigurationJson = JsonStreaming.objectMapper().writeValueAsString(inputConfiguration);
        assertEquals("""
                {"workingDirectory":".","classPathParts":[{"name":\
                """, inputConfigurationJson.substring(0, 50));
    }

    @Test
    public void testLangchain4jMvn() throws IOException {
        Path path = Path.of("src/test/resources/mvnLangchain4j.txt.gz");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(315, javacList.size());
        List<Javac> hibernate = javacList.stream().filter(javac -> javac.destination()
                .contains("langchain4j-hibernate")).toList();
        assertEquals(3, hibernate.size());
        {
            Javac j = hibernate.getFirst();
            assertEquals(17, j.release());
            assertTrue(j.destination().endsWith("langchain4j/langchain4j-hibernate/target/classes"));
            assertEquals(11, j.classpath().size());
            assertEquals(9, j.classpath().stream().filter(cp -> cp.contains(".m2") && cp.endsWith(".jar")).count());
            assertEquals(1, j.sourcePath().size());
            assertTrue(j.sourcePath().getFirst().endsWith("langchain4j/langchain4j-hibernate/src/main/java"));
            assertTrue(j.generatedSourceFilesDestination().endsWith("langchain4j-hibernate/target/generated-sources/annotations"));
            assertEquals("UTF-8", j.encoding());
        }
        {
            Javac j = hibernate.get(1);
            assertEquals(17, j.release());
            assertTrue(j.destination().endsWith("langchain4j/langchain4j-hibernate/target/classes"));
            assertEquals(11, j.classpath().size());
            assertEquals(9, j.classpath().stream().filter(cp -> cp.contains(".m2") && cp.endsWith(".jar")).count());
            assertEquals(2, j.sourcePath().size());
            assertTrue(j.sourcePath().getFirst().endsWith("langchain4j/langchain4j-hibernate/src/main/java"));
            assertTrue(j.sourcePath().getLast().endsWith("langchain4j/langchain4j-hibernate/target/generated-sources/annotations"));
            assertTrue(j.generatedSourceFilesDestination().endsWith("langchain4j-hibernate/target/generated-sources/annotations"));
            assertEquals("UTF-8", j.encoding());
        }
        {
            Javac j = hibernate.getLast();
            assertEquals(17, j.release());
            assertTrue(j.destination().endsWith("langchain4j/langchain4j-hibernate/target/test-classes"));
            assertEquals(96, j.classpath().size());
            assertEquals(89, j.classpath().stream().filter(cp -> cp.contains(".m2") && cp.endsWith(".jar")).count());
            assertEquals(2, j.sourcePath().size());
            assertTrue(j.sourcePath().getFirst().endsWith("langchain4j/langchain4j-hibernate/src/test/java"));
            assertTrue(j.sourcePath().getLast().endsWith("langchain4j/langchain4j-hibernate/target/generated-test-sources/annotations"));
            assertTrue(j.generatedSourceFilesDestination().endsWith("langchain4j-hibernate/target/generated-test-sources/test-annotations"));
            assertEquals("UTF-8", j.encoding());
        }
    }

    @Test
    public void testCfpMvn() throws IOException {
        Path path = Path.of("src/test/resources/mvnCfp2.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(3, javacList.size());
        {
            Javac j = javacList.getFirst();
            assertEquals(21, j.release());
            assertEquals("/Users/bnaudts/git/callforpapers/target/classes", j.destination());
            assertEquals(270, j.classpath().size());
            assertEquals(269, j.classpath().stream().filter(cp -> cp.contains(".m2") && cp.endsWith(".jar")).count());
            assertEquals(2, j.sourcePath().size());
            assertEquals("/Users/bnaudts/git/callforpapers/src/main/java", j.sourcePath().getFirst());
            assertEquals("/Users/bnaudts/git/callforpapers/target/generated-sources/annotations", j.sourcePath().getLast());
            assertEquals("/Users/bnaudts/git/callforpapers/target/generated-sources/annotations", j.generatedSourceFilesDestination());
            assertEquals("UTF-8", j.encoding());
        }
        {
            Javac j = javacList.get(1);
            assertEquals(21, j.release());
            assertEquals("/Users/bnaudts/git/callforpapers/target/classes", j.destination());
            assertEquals(270, j.classpath().size());
            assertEquals(269, j.classpath().stream().filter(cp -> cp.contains(".m2") && cp.endsWith(".jar")).count());
            assertEquals(3, j.sourcePath().size());
            assertEquals("/Users/bnaudts/git/callforpapers/src/main/java", j.sourcePath().getFirst());
            assertEquals("/Users/bnaudts/git/callforpapers/target/generated-sources/annotations", j.sourcePath().get(1));
            assertEquals("/Users/bnaudts/git/callforpapers/target/generated-sources/annotations", j.sourcePath().getLast());
            assertEquals("/Users/bnaudts/git/callforpapers/target/generated-sources/annotations", j.generatedSourceFilesDestination());
            assertEquals("UTF-8", j.encoding());
        }
        {
            Javac j = javacList.get(2);
            assertEquals(21, j.release());
            assertEquals("/Users/bnaudts/git/callforpapers/target/test-classes", j.destination());
            assertEquals(361, j.classpath().size());
            assertEquals(359, j.classpath().stream().filter(cp -> cp.contains(".m2") && cp.endsWith(".jar")).count());
            assertEquals(2, j.sourcePath().size());
            assertEquals("/Users/bnaudts/git/callforpapers/src/test/java", j.sourcePath().getFirst());
            assertEquals("/Users/bnaudts/git/callforpapers/target/generated-test-sources/test-annotations", j.sourcePath().getLast());
            assertEquals("/Users/bnaudts/git/callforpapers/target/generated-test-sources/test-annotations", j.generatedSourceFilesDestination());
            assertEquals("UTF-8", j.encoding());
        }
        InputConfiguration inputConfiguration = new ParseJavacList().parse(path);
        assertEquals(379, inputConfiguration.classPathParts().size());
        assertEquals(2, inputConfiguration.sourceSets().size());
    }

    @Test
    public void testGradleAnalyzerRun() throws IOException {
        Path path = Path.of("src/test/resources/gradleAnalyzerRunTestClasses.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(44, javacList.size());
        Javac j = javacList.getLast();
        assertEquals("/Users/bnaudts/git/analyzer-modification/e2immu-modification-linkedvariables/build/classes/java/test",
                j.destination());
        assertEquals("/Users/bnaudts/git/analyzer-modification/e2immu-modification-linkedvariables/build/generated/sources/annotationProcessor/java/test",
                j.generatedSourceFilesDestination());

        InputConfiguration inputConfiguration = new ParseJavacList().parse(path);
        JsonStreaming.objectMapper().writerFor(InputConfigurationImpl.class)
                .withDefaultPrettyPrinter().writeValue(new File("build/inputConfigurationAnalyzerRun.json"), inputConfiguration);
    }


    @Test
    public void testGradleJFocusTransform() throws IOException {
        Path path = Path.of("src/test/resources/gradleJFocusTransform.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(50, javacList.size());

        InputConfiguration inputConfiguration = new ParseJavacList().parse(path);
        SourceSet stdBaseUtilMain = inputConfiguration.sourceSets().stream()
                .filter(set -> "codelaser-stdbase-util/main".equals(set.name())).findFirst().orElseThrow();
        assertEquals("""
                codelaser-stdbase-api/main:/Users/bnaudts/git/jfocus-stdbase/codelaser-stdbase-api/src/main/java
                maddi-cst-api/main:/Users/bnaudts/git/maddi/maddi-cst-api/src/main/java
                maddi-support/main:/Users/bnaudts/git/maddi/maddi-support/src/main/java
                maddi-util/main:/Users/bnaudts/git/maddi/maddi-util/src/main/java
                slf4j-api-2.0.17.jar[external]:[]\
                """, stdBaseUtilMain.dependencies().stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
    }


    @Test
    public void testGradleJFocusRefactor() throws IOException {
        Path path = Path.of("src/test/resources/gradleJFocusRefactor.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(85, javacList.size());
      /*  Javac j = javacList.getLast();
        assertEquals("/Users/bnaudts/git/analyzer-modification/e2immu-modification-linkedvariables/build/classes/java/test",
                j.destination());
        assertEquals("/Users/bnaudts/git/analyzer-modification/e2immu-modification-linkedvariables/build/generated/sources/annotationProcessor/java/test",
                j.generatedSourceFilesDestination());
*/
        InputConfiguration inputConfiguration = new ParseJavacList().parse(path);
        JsonStreaming.objectMapper().writerFor(InputConfigurationImpl.class)
                .withDefaultPrettyPrinter().writeValue(new File("build/inputConfigurationJFocusRefactor.json"), inputConfiguration);
    }

    @Disabled
    @Test
    public void testGradleSpringCore() throws IOException {
        Path path = Path.of("src/test/resources/gradleSpringCore.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(3, javacList.size());
        InputConfiguration inputConfiguration = new ParseJavacList().parse(path, List.of("jdk.jfr"));
        assertEquals(89, inputConfiguration.classPathParts().size());
        assertTrue(inputConfiguration.classPathParts().stream().anyMatch(set -> "jdk.jfr".equals(set.name())));
    }

    @Disabled
    @Test
    public void testMvnGson() throws IOException {
        Path path = Path.of("src/test/resources/mvnGson.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(5, javacList.size());
        InputConfiguration inputConfiguration = new ParseJavacList().parse(path, List.of());
        assertEquals(36, inputConfiguration.classPathParts().size());
        assertTrue(inputConfiguration.classPathParts().stream().anyMatch(set -> "jdk.jfr".equals(set.name())));
    }

    @Disabled
    @Test
    public void testMvnNacos() throws IOException {
        Path path = Path.of("src/test/resources/mvnNacos.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(71, javacList.size());
        InputConfiguration inputConfiguration = new ParseJavacList().parse(path, List.of());
        assertEquals(198, inputConfiguration.classPathParts().size());
        Set<String> names = inputConfiguration.sourceSets().stream().map(SourceSet::name).collect(Collectors.toUnmodifiableSet());
        assertTrue(names.contains("api/main"));
        assertTrue(names.contains("api/test-classes"));
        assertTrue(names.contains("common/main"));
        assertTrue(names.contains("common/test-classes"));
    }

    @Disabled
    @Test
    public void testMvnDubbo() throws IOException {
        Path path = Path.of("src/test/resources/mvnDubbo.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(216, javacList.size());
        InputConfiguration inputConfiguration = new ParseJavacList().parse(path, List.of());
        assertEquals(574, inputConfiguration.classPathParts().size());
        assertEquals(139, inputConfiguration.sourceSets().size());
        Set<String> names = inputConfiguration.sourceSets().stream().map(SourceSet::name).collect(Collectors.toUnmodifiableSet());
        assertEquals(inputConfiguration.sourceSets().size(), names.size(), "Names should be unique");
    }

    @Disabled
    @Test
    public void testGradleJUnitFramework() throws IOException {
        Path path = Path.of("src/test/resources/gradleJunitFramework.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(31, javacList.size());
        InputConfiguration inputConfiguration = new ParseJavacList().parse(path, List.of());
        assertEquals(31, javacList.size());
        assertEquals(111, inputConfiguration.classPathParts().size());
        for (SourceSet set : inputConfiguration.sourceSets().stream().sorted(Comparator.comparing(SourceSet::name)).toList()) {
            System.out.println(set.name() + "\t" + set.sourceDirectories());
        }
        List<String> sourceSetNames = inputConfiguration.sourceSets().stream().map(SourceSet::name).toList();
        List<String> sourceSetNamesUnique = inputConfiguration.sourceSets().stream().map(SourceSet::name).distinct().toList();
        assertEquals(sourceSetNames.size(), sourceSetNamesUnique.size());
    }

    @Disabled
    @Test
    public void testGradleSofico() throws IOException {
        Path path = Path.of("src/test/resources/sofico-compile.txt.gz");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(2, javacList.size());
        Javac j1 = javacList.getFirst();
        assertEquals("/Users/bnaudts/gradleBuildDirectory/build/classes/java/main", j1.destination());
        assertEquals("/Users/bnaudts/gradleBuildDirectory/build/generated/sources/annotationProcessor/java/main",
                j1.generatedSourceFilesDestination());
        assertEquals(428, j1.classpath().size());
        assertEquals("[]", j1.sourcePath().toString()); // we have source files, but no source path
        assertEquals(21338, j1.sourceFiles().size());
        assertEquals(17, j1.sourceRelease());
        assertEquals(17, j1.targetRelease());

        Javac j2 = javacList.getLast();
        assertEquals("/Users/bnaudts/gradleBuildDirectory/build/classes/java/test", j2.destination());
        assertEquals("/Users/bnaudts/gradleBuildDirectory/build/generated/sources/annotationProcessor/java/test",
                j2.generatedSourceFilesDestination());
        assertEquals(430, j2.classpath().size());
        assertEquals("[]", j2.sourcePath().toString()); // we have source files, but no source path
        assertEquals(640, j2.sourceFiles().size());
        assertEquals(17, j2.sourceRelease());
        assertEquals(17, j2.targetRelease());

        JavacListToSourceSets.Result result = new JavacListToSourceSets().compute(javacList);
        assertEquals(2, result.jSourceSets().size());

        SourceSet setMain = result.jSourceSets().getFirst().sourceSet();
        assertEquals("java/main", setMain.name());
        assertEquals(1, setMain.sourceDirectories().size()); // computed source path
        assertEquals("/Users/bnaudts/gradleBuildDirectory/source", setMain.sourceDirectories().getFirst().toString());

        SourceSet setTest = result.jSourceSets().getLast().sourceSet();
        assertEquals("java/test", setTest.name());
        assertEquals(1, setTest.sourceDirectories().size()); // computed source path
        assertEquals("/Users/bnaudts/gradleBuildDirectory/test", setTest.sourceDirectories().getFirst().toString());

    }

    @Disabled
    @Test
    public void testGradleMultiProject2() throws IOException {
        Path path = Path.of("src/test/resources/gradleMultiProject2.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(6, javacList.size());
        InputConfiguration inputConfiguration = new ParseJavacList().parse(path, List.of());
        assertEquals(6, javacList.size());
        assertEquals(23, inputConfiguration.classPathParts().size());
        for (SourceSet set : inputConfiguration.sourceSets().stream().sorted(Comparator.comparing(SourceSet::name)).toList()) {
            System.out.println(set.name() + "\t" + set.sourceDirectories());
        }
        List<String> sourceSetNames = inputConfiguration.sourceSets().stream().map(SourceSet::name).toList();
        List<String> sourceSetNamesUnique = inputConfiguration.sourceSets().stream().map(SourceSet::name).distinct().toList();
        assertEquals(sourceSetNames.size(), sourceSetNamesUnique.size());

        SourceSet sp2Test = inputConfiguration.sourceSets().stream().filter(set -> "subproject2/test".equals(set.name())).findFirst().orElseThrow();
        assertEquals("""
                hamcrest-core-1.3.jar[external]:[]
                junit-4.13.jar[external]:[]
                subproject1.jar[external]:[]
                subproject1/test[test]:/Users/bnaudts/git/jfocus-refactor-server/projects/multiProject2/subproject1/src/test/java
                subproject2/main:/Users/bnaudts/git/jfocus-refactor-server/projects/multiProject2/subproject2/src/main/java\
                """, sp2Test.dependencies().stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
    }

}
