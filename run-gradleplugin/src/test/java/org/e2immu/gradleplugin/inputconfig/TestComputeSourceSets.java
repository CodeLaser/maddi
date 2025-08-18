package org.e2immu.gradleplugin.inputconfig;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestComputeSourceSets {

    @Test
    public void test() {
        ComputeSourceSets css = new ComputeSourceSets(Path.of(".").toAbsolutePath());
        Path workingDirectory = css.getWorkingDirectory();
        assertTrue(workingDirectory.isAbsolute());
        assertTrue(workingDirectory.toString().endsWith("/run-gradleplugin/."));
        Path srcMainJava = Path.of("src/main/java");
        assertTrue(Files.isDirectory(srcMainJava));
        assertFalse(srcMainJava.isAbsolute());
        Path absSrcMainJava = srcMainJava.toAbsolutePath();
        assertTrue(absSrcMainJava.isAbsolute());
        // Path relativeAgain = css.improveRelativePathKeepAbsolute(absSrcMainJava.toFile());
        // assertEquals(srcMainJava, relativeAgain);
    }
}
