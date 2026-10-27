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
package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.inspection.kotlin.KotlinInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.kotlin.api.KotlinFrontEnds;
import io.codelaser.maddi.kotlin.api.KotlinReferenceRecall;
import io.codelaser.maddi.kotlin.realm.K2Realm;
import io.codelaser.maddi.run.config.util.JsonStreaming;
import io.codelaser.maddi.util.corpus.Corpora;
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
        Path config = Corpora.oss("detekt").requireConfig();
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
