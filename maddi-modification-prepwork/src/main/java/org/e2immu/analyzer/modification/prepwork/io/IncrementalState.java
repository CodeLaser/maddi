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

package org.e2immu.analyzer.modification.prepwork.io;

import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Task #35 phase C: the state a checkpointed run leaves behind so the NEXT run can do incremental
 * early-cutoff at the right granularity —
 * <ul>
 * <li>{@code analysisFingerprints}: primary type FQN -> analysis OUTPUT fingerprint
 *     ({@link AnalysisFingerprint}); the prior-fingerprint side of the EarlyCutoffWorklist's
 *     frontier;</li>
 * <li>{@code consumers}: primary type FQN -> FQNs of primary types that CONSUMED one of its
 *     elements' summaries during analysis (the ConsumptionEdgeRecorder's edges, lifted to primary
 *     types and reversed). The wake relation inside a giant SCC — measured (Phase A, fernflower)
 *     median 5 direct consumers vs a use-graph one-hop that can reach thousands.</li>
 * </ul>
 * Persisted as one JSON file next to the checkpoint. Consumption edges are a PERFORMANCE device;
 * the verify-certify net remains the correctness device (design §3.4) — an under-recorded edge
 * surfaces as a verification-pass change.
 */
public record IncrementalState(Map<String, String> analysisFingerprints,
                               Map<String, Set<String>> consumers) {
    private static final Logger LOGGER = LoggerFactory.getLogger(IncrementalState.class);

    public static final String FILE_NAME = "incremental-state.json";

    /** lift the recorder's element-level edges (consumer -> consumed) to primary types, reversed */
    public static IncrementalState capture(Runtime runtime,
                                           Collection<TypeInfo> primaryTypes,
                                           Map<Info, Set<Info>> elementConsumerToConsumed) {
        Map<String, String> fingerprints = new TreeMap<>();
        for (TypeInfo pt : primaryTypes) {
            try {
                FingerPrint fp = AnalysisFingerprint.of(runtime, pt);
                if (!fp.isNoFingerPrint()) fingerprints.put(pt.fullyQualifiedName(), fp.toString());
            } catch (RuntimeException | AssertionError e) {
                // the known codec tail (fieldIndex & friends): a type without a fingerprint is
                // simply always-recomputed on resume — degraded, never wrong
                LOGGER.debug("No fingerprint for {}: {}", pt, e.toString());
            }
        }
        Map<String, Set<String>> consumers = new TreeMap<>();
        elementConsumerToConsumed.forEach((consumer, consumeds) -> {
            TypeInfo consumerPrimary = consumer.typeInfo() == null ? null : consumer.typeInfo().primaryType();
            if (consumerPrimary == null) return;
            String consumerFqn = consumerPrimary.fullyQualifiedName();
            for (Info consumed : consumeds) {
                TypeInfo consumedPrimary = consumed.typeInfo() == null ? null : consumed.typeInfo().primaryType();
                if (consumedPrimary == null || consumedPrimary == consumerPrimary) continue;
                consumers.computeIfAbsent(consumedPrimary.fullyQualifiedName(), _ -> new TreeSet<>())
                        .add(consumerFqn);
            }
        });
        return new IncrementalState(fingerprints, consumers);
    }

    public void save(File directory) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n \"fingerprints\": {");
        boolean first = true;
        for (Map.Entry<String, String> e : analysisFingerprints.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append("\n  \"").append(e.getKey()).append("\": \"").append(e.getValue()).append('"');
        }
        sb.append("\n },\n \"consumers\": {");
        first = true;
        for (Map.Entry<String, Set<String>> e : consumers.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append("\n  \"").append(e.getKey()).append("\": [");
            boolean f2 = true;
            for (String c : e.getValue()) {
                if (!f2) sb.append(',');
                f2 = false;
                sb.append('"').append(c).append('"');
            }
            sb.append(']');
        }
        sb.append("\n }\n}\n");
        Files.writeString(new File(directory, FILE_NAME).toPath(), sb.toString());
        LOGGER.info("Incremental state: {} fingerprints, {} consumed types with consumers -> {}",
                analysisFingerprints.size(), consumers.size(), FILE_NAME);
    }

    /** tolerant: a missing or unparseable file yields the empty state (cold behavior) */
    public static IncrementalState load(File directory) {
        File file = new File(directory, FILE_NAME);
        Map<String, String> fingerprints = new TreeMap<>();
        Map<String, Set<String>> consumers = new TreeMap<>();
        if (!file.canRead()) return new IncrementalState(fingerprints, consumers);
        try {
            String json = Files.readString(file.toPath());
            // minimal parser for exactly the format save() writes: FQNs and base64 fingerprints
            // contain neither quotes nor backslashes
            int cs = json.indexOf("\"consumers\"");
            String fpPart = json.substring(0, cs < 0 ? json.length() : cs);
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\"([^\"]+)\":\\s*\"([^\"]*)\"").matcher(fpPart);
            while (m.find()) {
                if (!"fingerprints".equals(m.group(1))) fingerprints.put(m.group(1), m.group(2));
            }
            if (cs >= 0) {
                java.util.regex.Matcher c = java.util.regex.Pattern
                        .compile("\"([^\"]+)\":\\s*\\[([^]]*)]").matcher(json.substring(cs));
                while (c.find()) {
                    Set<String> set = new TreeSet<>();
                    java.util.regex.Matcher e = java.util.regex.Pattern
                            .compile("\"([^\"]+)\"").matcher(c.group(2));
                    while (e.find()) set.add(e.group(1));
                    consumers.put(c.group(1), set);
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot load incremental state from {}: {} — cold behavior", file, e.toString());
            fingerprints.clear();
            consumers.clear();
        }
        return new IncrementalState(fingerprints, consumers);
    }
}
