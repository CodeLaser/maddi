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
package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.inspection.kotlin.KotlinInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.kotlin.api.KotlinFrontEnds;
import io.codelaser.maddi.kotlin.api.KotlinReferenceRecall;
import io.codelaser.maddi.kotlin.realm.K2Realm;
import io.codelaser.maddi.run.config.util.JsonStreaming;
import io.codelaser.maddi.run.openjdkmain.TestOssCorpus;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How much of what a source editor must update is visible in detekt's Kotlin CST: every project reference K2 resolves,
 * graded against the CST by {@link KotlinReferenceRecall}. A measurement, not a gate: it asserts only that it measured
 * something, and writes the full report to {@code build/reports/reference-recall/detekt.txt}.
 */
@Tag("slow")
public class TestDetektReferenceRecall {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDetektReferenceRecall.class);

    /** A floor on measured references, so an absent or empty parse cannot pass as a measurement. */
    private static final int REFERENCE_FLOOR = 10_000;

    @Test
    public void measure() throws IOException {
        Path config = TestOssCorpus.config("detekt");
        Assumptions.assumeTrue(Files.exists(config),
                () -> "requires the detekt corpus checkout with its input configuration at "
                      + config.toAbsolutePath().normalize()
                      + "; generate it with `task corpus:config:detekt` at the repo root");
        KotlinInspector inspector = new KotlinInspector(new RuntimeImpl());
        inspector.initialize(JsonStreaming.objectMapper().readValue(config.toFile(), InputConfigurationImpl.class));

        // ⭐ through the realm, like the shipped CLI: the compiler is not on this JVM's classpath
        K2Realm.installIfAbsent();
        KotlinReferenceRecall recall = KotlinFrontEnds.get().referenceRecall(6);
        inspector.parseFromConfiguration(List.of(recall));

        String report = recall.report();
        Path out = Path.of("build/reports/reference-recall/detekt.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report);
        LOGGER.info("detekt reference recall ({}):\n{}", out.toAbsolutePath(), report);
        assertTrue(recall.rowCount() >= REFERENCE_FLOOR,
                "measured only " + recall.rowCount() + " project references; expected at least " + REFERENCE_FLOOR);
    }
}
