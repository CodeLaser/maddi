package org.e2immu.analyzer.run.openjdkmain.javac;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJavacTimefoldSolverCore {
    @Test
    public void test() throws IOException {
        Path path = Path.of("src/test/resources/javac/mvnTimefold-solver.txt.gz");
        List<Javac> javacList = new ParseJavacList().javacLines(path);
        assertEquals(65, javacList.size());

        JavacListToSourceSets extract = new JavacListToSourceSets();
        JavacListToSourceSets.Result result = extract.compute(javacList);
        List<JavacListToSourceSets.JSourceSet> sourceSets = result.jSourceSets();
        assertEquals(javacList.size(), sourceSets.size());

        InputConfiguration inputConfiguration = new ParseJavacList().inputConfiguration(javacList, List.of("java.sql"));
        ObjectMapper objectMapper = JsonStreaming.objectMapper();
        File file = Path.of("src/test/resources/inputConfiguration/timefold-solver.json").toFile();
        objectMapper.writerFor(InputConfigurationImpl.class).writeValue(file, inputConfiguration);
    }
}
