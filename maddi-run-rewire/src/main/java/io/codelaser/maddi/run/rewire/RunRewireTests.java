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

package io.codelaser.maddi.run.rewire;

import io.codelaser.maddi.callgraph.ComputeCallGraph;
import io.codelaser.maddi.callgraph.PrimaryTypeUseGraph;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.graph.G;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.InvalidationState.*;

public class RunRewireTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunRewireTests.class);
    private static final int ITERATIONS = 200;
    public static final long SEED = 15;

    private final InputConfiguration inputConfiguration;
    private final JavaInspector javaInspector;
    private final ParseResult parseResult;
    private final G<Info> initialGraph;

    public RunRewireTests(InputConfiguration inputConfiguration,
                          JavaInspector javaInspector,
                          ParseResult parseResult,
                          G<Info> graph) {
        this.inputConfiguration = inputConfiguration;
        this.javaInspector = javaInspector;
        this.parseResult = parseResult;
        this.initialGraph = graph;
    }

    public void go() {
        Random random = new Random(SEED);
        List<TypeInfo> list = parseResult.primaryTypes().stream().sorted(Comparator.comparing(TypeInfo::fullyQualifiedName)).toList();
        G<Info> infoGraph = initialGraph;
        for (int i = 0; i < ITERATIONS; ++i) {
            LOGGER.info("Compute primary type use graph, iteration {}", i);
            TypeInfo typeToRewire = list.get(random.nextInt(list.size()));
            Path path = Path.of(typeToRewire.compilationUnit().uri().getSchemeSpecificPart());
            Path absolutePath = path.toAbsolutePath();
            boolean stop = false;
            try {
                String content = Files.readString(path);
                boolean restored = false;
                try {
                    LOGGER.info("Modifying {}", absolutePath);
                    String updatedContent = "// some comment\n\n" + content;
                    // this will trigger a change of fingerprint
                    stop |= write(path, updatedContent);
                    if (!stop) {
                        infoGraph = reloadSources(typeToRewire, infoGraph);
                        LOGGER.info("Restoring {}", absolutePath);
                        stop |= write(path, content);
                        restored = true;
                        /*
                        The analyzer must follow the file BACK, or its fingerprints stop describing the disk: after
                        the re-parse above it holds the fingerprint of the EDITED file, which is what the next
                        reloadSources compares against. Two things go wrong if we leave it there. The file stays
                        "changed" for ever, so every later iteration invalidates it along with the one it meant to
                        pick. And when this file is picked again, the very same edit produces the very same content
                        and hence the very same fingerprint the analyzer already holds -- so it is told, correctly,
                        that nothing changed, and the assertion in reloadSources fires.
                        Reverting is a rewire in its own right, so this is a second exercise, not overhead.
                         */
                        if (!stop) infoGraph = reloadSources(typeToRewire, infoGraph);
                    }
                } finally {
                    if (!restored) {
                        // whatever went wrong, never leave the file edited
                        LOGGER.info("Restoring {}", absolutePath);
                        stop |= write(path, content);
                    }
                }
            } catch (IOException ioe) {
                LOGGER.error("Could not read {}", absolutePath, ioe);
                stop = true;
            }
            if (stop) return;
        }
    }

    private G<Info> reloadSources(TypeInfo pt, G<Info> infoGraph) throws IOException {
        LOGGER.info("Calling javaInspector.reloadSources");
        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration, Map.of());
        assert rr.sourceHasChanged().contains(pt);
        Set<TypeInfo> dependentPrimaryTypes = new PrimaryTypeUseGraph(infoGraph).dependentsOf(rr.sourceHasChanged());
        assert Collections.disjoint(dependentPrimaryTypes, rr.sourceHasChanged());
        LOGGER.info("{} has {} dependent types", pt, dependentPrimaryTypes.size());
        JavaInspector.Invalidated invalidated = ti -> {
            if (rr.sourceHasChanged().contains(ti)) return INVALID;
            if (dependentPrimaryTypes.contains(ti)) return REWIRE;
            return UNCHANGED;
        };
        JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder()
                .setLombok(inputConfiguration.containsLombok())
                .setDetailedSources(true)
                .setFailFast(true)
                .setInvalidated(invalidated)
                .setParallel(true)
                .build();
        LOGGER.info("Reparse");
        ParseResult parseResult1 = javaInspector.parse(parseOptions).parseResult();
        LOGGER.info("Recompute call graph");
        ComputeCallGraph ccg = new ComputeCallGraph(javaInspector.runtime(), parseResult1,
                _ -> false);
        return ccg.go().graph();
    }

    private static boolean write(Path path, String content) {
        try {
            Files.writeString(path, content);
            return false;
        } catch (IOException ioe) {
            LOGGER.error("Could not write {}", path.toAbsolutePath(), ioe);
            return true;
        }
    }
}
