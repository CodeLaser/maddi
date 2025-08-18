package org.e2immu.analyzer.run.testgradleplugwriteaapi;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRunAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestRunAnalyzer.class);

    @Test
    public void test() {
        LOGGER.info("In test!");
        File tt = new File("build/e2immu/OrgE2immuTestTest.json");
        assertTrue(tt.canRead());

        File tm = new File("build/e2immu/OrgE2immuTestMain.json");
        assertTrue(tm.canRead());

        File ti = new File("build/e2immu/OrgE2immuTestIgnore.json");
        assertFalse(ti.canRead());
    }
}
