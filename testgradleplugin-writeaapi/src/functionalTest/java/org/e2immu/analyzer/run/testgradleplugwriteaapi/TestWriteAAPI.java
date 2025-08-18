package org.e2immu.analyzer.run.testgradleplugwriteaapi;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestWriteAAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestWriteAAPI.class);

    @Test
    public void test() throws IOException {
        LOGGER.info("In test!");
        File dir = new File("/tmp/testWriteAnnotatedAPIDir/org/e2immu/testwrite");
        File ju = new File(dir, "/JavaUtil.java");
        assertTrue(ju.canRead());
        File jio = new File(dir, "JavaIo.java");
        assertFalse(jio.canRead());

        File cf = new File(dir, "/ComFoo.java");
        assertTrue(cf.canRead());

        String comFooContent = Files.readString(cf.toPath());
        assertTrue(comFooContent.contains("class SomeInterface$"));
        assertTrue(comFooContent.contains("class SomeTestInterface$"));
    }
}
