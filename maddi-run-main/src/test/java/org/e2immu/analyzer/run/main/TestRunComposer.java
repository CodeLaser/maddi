/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.run.main;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRunComposer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestRunComposer.class);

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.shallow")).setLevel(Level.DEBUG);
    }

    @Disabled
    @Test
    public void test() {
        File file = new File("build/test-aapi-skeleton");
        if (file.delete()) {
            LOGGER.info("Deleting {}", file);
        }
        Main.main(new String[]{
                "--classpath=jmod:java.base",
                "--source=none",
                "--annotated-api-packages=java.util.",
                "--annotated-api-target-package=org.e2immu.aapi",
                "--annotated-api-target-dir=" + file.getAbsolutePath()
        });
        assertTrue(file.canRead());
        File pkg = new File(file, "org/e2immu/aapi");
        File javaIo = new File(pkg, "JavaIo.java");
        assertFalse(javaIo.canRead());
        File javaUtil = new File(pkg, "JavaUtil.java");
        assertTrue(javaUtil.canRead());
        File javaUtilConcurrent = new File(pkg, "JavaUtilConcurrent.java");
        assertTrue(javaUtilConcurrent.canRead());
    }
}
