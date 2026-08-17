package io.codelaser.maddi.run.openjdkmain.javac;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.codelaser.maddi.run.config.util.JsonStreaming;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
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
        // write into the (git-ignored) build directory, NOT into src/test/resources: the serialized configuration
        // embeds machine-specific absolute paths (~/.m2, the local test-oss checkout) and non-deterministic list
        // order, so writing it into the source tree dirtied a committed file on every run. Nothing reads the file;
        // the write only exercises the serialization path.
        File dir = Path.of("build", "test-generated", "inputConfiguration").toFile();
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File file = new File(dir, "timefold-solver.json");
        objectMapper.writerFor(InputConfigurationImpl.class).writeValue(file, inputConfiguration);
    }
}
