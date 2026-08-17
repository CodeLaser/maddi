package org.e2immu.analyzer.run.openjdkmain.javac;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestExtractBuildProjectNames {
    @Disabled
    @Test
    public void test() throws IOException {

        Path path = Path.of("src/test/resources/gradleAnalyzerRunTestClasses.txt");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(44, javacList.size());

        JavacListToSourceSets extract = new JavacListToSourceSets();
        JavacListToSourceSets.Result result = extract.compute(javacList);
        List<JavacListToSourceSets.JSourceSet> sourceSets = result.jSourceSets();
        assertEquals(javacList.size(), sourceSets.size());

        JavacListToSourceSets.JSourceSet jMain = sourceSets.get(20);
        assertEquals("e2immu-modification-linkedvariables/main", jMain.sourceSet().name());
        assertFalse(jMain.sourceSet().test());
        assertEquals("file:/Users/bnaudts/git/analyzer-modification/e2immu-modification-linkedvariables/build/classes/java/main",
                jMain.sourceSet().uri().toString());

        JavacListToSourceSets.JSourceSet jTest = sourceSets.getLast();
        assertEquals("e2immu-modification-linkedvariables/test", jTest.sourceSet().name());
        assertTrue(jTest.sourceSet().test());
        assertEquals("file:/Users/bnaudts/git/analyzer-modification/e2immu-modification-linkedvariables/build/classes/java/test",
                jTest.sourceSet().uri().toString());
        assertEquals(33, jTest.sourceSet().dependencies().size());
        assertTrue(jTest.sourceSet().dependencies().contains(jMain.sourceSet()));

        assertEquals(47, result.jars().size());
    }
}
