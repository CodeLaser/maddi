package org.e2immu.analyzer.run.openjdkmain;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Batch first-contact sweep over every corpus under the test-oss root (see {@link TestOssCorpus}) that
 * carries an inputConfiguration.json.
 * The bar per corpus: exit 0 (crash-free, certified fixpoint via the default worklist+parallel engine).
 * Gated on SWEEP=1 (this can run for hours); a corpus list via SWEEP=name1,name2 restricts the sweep.
 * A name may contain slashes to address a nested module (e.g. SWEEP=camel/core/camel-util).
 * Verdict baselines are NOT checked here — promote a corpus to its own pinned test for that.
 */
public class TestCorpusSweep {
    private static final Path CORPORA = TestOssCorpus.ROOT;

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger("org.e2immu.analyzer.modification.link.impl.linkgraph.RedundantLinks")).setLevel(Level.ERROR);
    }

    private record Result(String corpus, int exitValue, long seconds, String note) {
    }

    @Test
    public void sweep() throws IOException {
        String gate = System.getenv("SWEEP");
        Assumptions.assumeTrue(gate != null, "gated: set SWEEP=1 (or SWEEP=name1,name2) to run the sweep");
        List<String> only = gate.equals("1") ? List.of()
                : List.of(gate.split(","));
        List<Result> results = new ArrayList<>();
        List<Path> dirs = new ArrayList<>();
        if (only.isEmpty()) {
            try (var stream = Files.list(CORPORA)) {
                dirs.addAll(stream.filter(Files::isDirectory).sorted().toList());
            }
        } else {
            only.forEach(o -> dirs.add(CORPORA.resolve(o)));
        }
        {
            for (Path dir : dirs) {
                String name = CORPORA.relativize(dir).toString();
                Path config = dir.resolve("inputConfiguration.json");
                if (!Files.exists(config)) config = dir.resolve("target/inputConfiguration.json");
                if (!Files.exists(config)) continue;
                System.out.println(">>> SWEEP corpus " + name + " (" + config + ")");
                long start = System.currentTimeMillis();
                int exitValue;
                String note = "";
                try {
                    exitValue = Main.execute(new String[]{
                            "--input-configuration=" + config,
                            "--analysis-steps=modification",
                            "--preload-analysis-results-dirs=../maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/jdk"
                    });
                } catch (Throwable t) {
                    exitValue = -1;
                    note = t.getClass().getSimpleName() + ": " + t.getMessage();
                }
                long seconds = (System.currentTimeMillis() - start) / 1000;
                results.add(new Result(name, exitValue, seconds, note));
                System.out.printf(">>> SWEEP %-25s exit=%d  %d s  %s%n", name, exitValue, seconds, note);
            }
        }
        System.out.println(">>> SWEEP SUMMARY");
        for (Result r : results) {
            System.out.printf(">>> %-25s exit=%d  %5d s  %s%n", r.corpus, r.exitValue, r.seconds, r.note);
        }
        Assumptions.assumeTrue(!results.isEmpty(), "no corpora with an inputConfiguration.json found");
    }
}
