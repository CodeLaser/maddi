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

import org.e2immu.analyzer.modification.prepwork.MethodAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.io.CodecImpl;
import org.e2immu.language.inspection.api.resource.MD5FingerPrint;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The <em>analysisFingerprint</em>: an MD5 over the canonical cst-io codec dump of a primary type's <em>analyzer
 * output</em>. Two analyses whose fingerprints match produced the same cross-type-observable result, so a dependent
 * need not be recomputed (early cutoff). See {@code analysis-rewiring.md}.
 * <p>
 * v1 hashes only the analyzer output: {@link WriteAnalysisResults} already walks Info-level analysis (type / method /
 * field / parameter) in canonical sorted order with fqn/index-stable references and no source positions, which is
 * exactly the verdicts plus the link summaries ({@code METHOD_LINKS}, field {@code LINKS}). We additionally exclude
 * the prepwork-internal, per-variable properties ({@code VARIABLE_DATA}, which is also copied onto the method's
 * analysis, and {@code VARIABLES_OF_ENCLOSING_METHOD}): they are recomputed anyway and are rename-noisy, and a
 * dependent never reads them. A normalised (name-blanked) {@code VARIABLE_DATA} can be folded back in later.
 */
public class AnalysisFingerprint {

    /** Prepwork-internal, per-variable/per-statement detail — excluded from the v1 fingerprint. */
    public static final Set<String> EXCLUDED_PROPERTY_KEYS = Set.of(
            VariableDataImpl.VARIABLE_DATA.key(),
            MethodAnalyzer.VARIABLES_OF_ENCLOSING_METHOD.key());

    /** The v1 property selection: everything the codec dumps except the prepwork-internal keys above. */
    public static final Predicate<Property> ANALYZER_OUTPUT_ONLY =
            p -> !EXCLUDED_PROPERTY_KEYS.contains(p.key());

    /**
     * The tier the early-cutoff skip <em>carries</em> onto a fingerprint-stable rewired type: exactly the
     * cross-type-derived properties (the expensive link/analyzer output), leaving out both the parse-time tier (the
     * rewire phase already carried it) and the intrinsic tier (prepwork re-derives it, so carrying it would double-set
     * against a re-prep). This is the precise replacement for the earlier {@code ANALYZER_OUTPUT_ONLY ∧ ¬carryOnRewire}
     * approximation, which still carried the intrinsic {@code partOfConstructionType} / {@code finalField} /
     * {@code instanceOfScope} / {@code statementAlwaysEscapes}. See {@link Property#analysisTier()}.
     */
    public static final Predicate<Property> CROSS_TYPE_DERIVED_ONLY =
            p -> p.analysisTier() == Property.AnalysisTier.CROSS_TYPE_DERIVED;

    /** No normalization: hash the dump verbatim. */
    public static final List<FingerprintNormalizer> RAW = List.of();

    /**
     * The default profile. An ordered pipeline of {@link FingerprintNormalizer}s applied to the dump before hashing;
     * each widens the class of edits the fingerprint ignores. More are expected over time (rename-invariance, …) —
     * add them here, or pass a custom list to {@link #of(Runtime, TypeInfo, List)}.
     */
    public static final List<FingerprintNormalizer> DEFAULT = List.of(new SourcePositionNormalizer());

    private AnalysisFingerprint() {
    }

    /** The canonical serialised analysis dump for {@code primaryType}, honouring {@code propertyPredicate}. */
    public static String dump(Runtime runtime, TypeInfo primaryType, Predicate<Property> propertyPredicate) {
        Codec codec = new PrepWorkCodec(runtime, null).codec();
        WriteAnalysisResults war = new WriteAnalysisResults(runtime, ti -> true, propertyPredicate);
        Codec.EncodedValue ev = war.encodePrimaryType(codec, new CodecImpl.ContextImpl(), primaryType);
        if (ev == null) return "";
        StringWriter sw = new StringWriter();
        try {
            ((CodecImpl.E) ev).write(sw, 0, true);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sw.toString();
    }

    /** Apply a normalizer pipeline to a dump. */
    public static String normalize(String dump, List<FingerprintNormalizer> normalizers) {
        String result = dump;
        for (FingerprintNormalizer normalizer : normalizers) {
            result = normalizer.normalize(result);
        }
        return result;
    }

    /** The analysisFingerprint of a primary type: analyzer output only, with the {@link #DEFAULT} normalizers. */
    public static FingerPrint of(Runtime runtime, TypeInfo primaryType) {
        return of(runtime, primaryType, DEFAULT);
    }

    /** The analysisFingerprint of a primary type with an explicit normalizer profile (e.g. {@link #RAW}). */
    public static FingerPrint of(Runtime runtime, TypeInfo primaryType, List<FingerprintNormalizer> normalizers) {
        return MD5FingerPrint.compute(normalize(dump(runtime, primaryType, ANALYZER_OUTPUT_ONLY), normalizers));
    }

    /**
     * A source set's rollup fingerprint: the hash of its primary types' (fqn-sorted) {@link #DEFAULT}-normalized
     * analysis dumps. Two runs whose rollups match produced the same analyzer output for the whole set, so a
     * dependent set need not be re-analyzed — the coarse cross-source-set early cutoff.
     */
    public static FingerPrint ofSourceSet(Runtime runtime, Collection<TypeInfo> primaryTypes) {
        StringBuilder sb = new StringBuilder();
        primaryTypes.stream().sorted(Comparator.comparing(TypeInfo::fullyQualifiedName)).forEach(pt ->
                sb.append(pt.fullyQualifiedName()).append('=')
                        .append(normalize(dump(runtime, pt, ANALYZER_OUTPUT_ONLY), DEFAULT)).append('\n'));
        return MD5FingerPrint.compute(sb.toString());
    }

    /**
     * Compute each source set's rollup fingerprint from the analyzed primary types and store it on the set,
     * SetOnce-guarded (skipped where already set, e.g. loaded from persisted results). Activates the dormant
     * {@link SourceSet#analysisFingerPrintOrNull()} hook; the value persists via the analysis-results JSON. Returns
     * the per-set fingerprints for logging. This is the <em>storage</em> half of early cutoff; the compare half
     * (skip re-analysis of a set whose stored fingerprint is unchanged) follows once carry lands. See
     * {@code analysis-rewiring.md}.
     */
    public static Map<SourceSet, FingerPrint> storePerSourceSet(Runtime runtime, Collection<TypeInfo> primaryTypes) {
        Map<SourceSet, List<TypeInfo>> bySet = new HashMap<>();
        for (TypeInfo pt : primaryTypes) {
            bySet.computeIfAbsent(pt.compilationUnit().sourceSet(), s -> new ArrayList<>()).add(pt);
        }
        Map<SourceSet, FingerPrint> result = new LinkedHashMap<>();
        bySet.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .forEach(e -> {
                    FingerPrint fp = ofSourceSet(runtime, e.getValue());
                    if (e.getKey().analysisFingerPrintOrNull() == null) {
                        e.getKey().setAnalysisFingerPrint(fp);
                    }
                    result.put(e.getKey(), fp);
                });
        return result;
    }
}
