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
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.io.CodecImpl;
import org.e2immu.language.inspection.api.resource.MD5FingerPrint;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
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

    /** The v1 analysisFingerprint of a primary type: analyzer output only ({@code VARIABLE_DATA} excluded). */
    public static FingerPrint of(Runtime runtime, TypeInfo primaryType) {
        return MD5FingerPrint.compute(dump(runtime, primaryType, ANALYZER_OUTPUT_ONLY));
    }
}
