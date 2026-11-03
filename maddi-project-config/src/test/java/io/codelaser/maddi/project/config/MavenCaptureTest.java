/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.project.config;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 A real Maven build, captured twice: a main and a test source set from the -X log, the pom hashed, and the capture run
 again on a stale pom. Slow, and skipped without an mvn on the PATH; it needs the compiler plugin in the local
 repository or the network.
 */
@Tag("slow")
public class MavenCaptureTest {

    @Test
    public void mainAndTest(@TempDir Path dir) throws Exception {
        Assumptions.assumeTrue(onPath("mvn"), "no mvn on the PATH");
        Path root = Files.createDirectories(dir.resolve("demo")).toRealPath(); // as Maven names it: /var is /private/var
        Files.writeString(root.resolve("pom.xml"), """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>x</groupId><artifactId>demo</artifactId><version>1</version>
                  <properties>
                    <maven.compiler.release>21</maven.compiler.release>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  </properties>
                </project>
                """);
        Path main = Files.createDirectories(root.resolve("src/main/java/x"));
        Files.writeString(main.resolve("Counter.java"), "package x;\npublic class Counter { int n; void inc() { n++; } }\n");
        Path test = Files.createDirectories(root.resolve("src/test/java/x"));
        Files.writeString(test.resolve("CounterCheck.java"), "package x;\nclass CounterCheck { Counter c; }\n");

        var store = new ProjectConfiguratorTest.MemoryRecords(dir.resolve("work"));
        ProjectConfigurator configurator = new ProjectConfigurator(store);
        ProjectRecord record = configurator.configure("demo",
                new ProjectConfigurator.FromCapture(CaptureSpec.maven(root)), CaptureListener.NONE);
        Path work = store.workDirectory("demo");
        assertEquals(2, Files.readAllLines(work.resolve("compile.log")).size(), "main and test compiled");
        String buildLog = Files.readString(work.resolve("build.log"));
        assertTrue(buildLog.contains("BUILD SUCCESS"), buildLog);
        assertFalse(buildLog.contains("[DEBUG]"), "no debug chatter");
        List<Path> dirs = BuildFileCheck.sourceDirectoriesOf(record.inputConfiguration());
        assertTrue(dirs.contains(root.resolve("src/main/java")) && dirs.contains(root.resolve("src/test/java")),
                dirs::toString);
        assertTrue(BuildFileCheck.buildFilesOf(record.inputConfiguration()).sha256().containsKey("pom.xml"));

        Files.writeString(root.resolve("pom.xml"), Files.readString(root.resolve("pom.xml")) + "<!-- changed -->\n");
        ProjectConfigurator.Opened opened = configurator.open("demo", false, CaptureListener.NONE);
        assertEquals("regenerated, as the build changed: pom.xml (changed)", opened.action());
        assertEquals(BuildFileCheck.State.FRESH, opened.after().state());
    }

    private static boolean onPath(String tool) {
        for (String dir : System.getenv().getOrDefault("PATH", "").split(java.io.File.pathSeparator)) {
            if (Files.isExecutable(Path.of(dir, tool))) return true;
        }
        return false;
    }
}
