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

package org.e2immu.analyzer.ide.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain-JSON DTOs mirroring the daemon's {@code DaemonProtocol} payloads, on the plugin side.
 * Kept structurally identical (field names) so Jackson maps them directly; no maddi types cross over.
 */
public final class AnalysisModel {
    private AnalysisModel() {
    }

    // ---- request ----

    public record SourceRoot(String name, String path, boolean test) {
    }

    public record ClasspathEntry(String path, String scope) {
    }

    public record AnalyzeConfig(String workingDirectory,
                                String sdkHome,
                                String sourceEncoding,
                                List<String> jmods,
                                List<SourceRoot> sources,
                                List<ClasspathEntry> classpath,
                                List<String> restrictToPackages,
                                boolean parallel,
                                boolean warnNearMisses) {
    }

    // ---- result ----

    /**
     * A located finding. {@code severity} is only ERROR or WARN; {@code category} is the analyzer's
     * free-form kebab-case discriminator ({@code contract-violation}, {@code near-miss-container},
     * {@code parse}, …) and is what tells apart findings that share a severity — see {@link #isNearMiss}.
     */
    public record Finding(String uri,
                          Integer beginLine, Integer beginCol, Integer endLine, Integer endCol,
                          String severity, String category, String message,
                          List<Finding> causes) {
    }

    /** Category prefix of the analyzer's advisory near-miss warnings ({@code near-miss-container}, …). */
    public static final String NEAR_MISS_PREFIX = "near-miss-";

    /**
     * Is this an advisory near-miss ("one member away from @Container") rather than a defect? These arrive
     * as WARN, like other warnings, so only the category separates them; front-ends render them at their
     * platform's weakest level so a suggestion never looks like a problem.
     */
    public static boolean isNearMiss(Finding finding) {
        return finding != null && finding.category() != null
               && finding.category().startsWith(NEAR_MISS_PREFIX);
    }

    /** One rendered annotation with polarity (POSITIVE/NEGATIVE/NEUTRAL) and context-default-ness. */
    public record Annotation(String text, String polarity, boolean contextDefault) {
    }

    public record ElementAnnotation(String uri,
                                    Integer beginLine, Integer beginCol, Integer endLine, Integer endCol,
                                    String kind,
                                    String fqn,
                                    List<String> displayAnnotations,
                                    List<Annotation> annotations,
                                    Map<String, String> properties) {
    }

    public record Result(String requestId,
                         List<Finding> findings,
                         List<ElementAnnotation> elementAnnotations,
                         List<String> initializationProblems,
                         int parseErrorCount,
                         int hintsLoaded,
                         long elapsedMillis,
                         String outcome) {
    }

    /**
     * How settled the values on screen are — the rung of the ladder a front-end should tell the user about.
     * <p>
     * The distinction is not cosmetic. Values refine monotonically, so nothing shown is ever wrong, but a
     * value can still <em>strengthen</em>: positive type-immutability in particular typically only resolves
     * in the analyzer's last (cycle-breaking) pass. So a partially-streamed view systematically understates
     * {@code @Immutable} on types, and a run that stopped at the iteration cap may understate anything —
     * neither of which is visible from the annotations themselves.
     */
    public enum Certainty {
        /** Streamed mid-run: established, but the analysis may still strengthen it. */
        PROVISIONAL,
        /** The fixpoint was certified: these values are final. */
        FINAL,
        /** The run stopped at the iteration cap or on a plateau: best available, not certified. */
        BEST_AVAILABLE,
        /** No fixpoint ran at all (e.g. the parse failed), so there is nothing to be certain about. */
        UNKNOWN,
    }

    public static final String OUTCOME_CERTIFIED = "CERTIFIED";
    public static final String OUTCOME_UNKNOWN = "UNKNOWN";

    /**
     * The certainty of a completed result. {@code null}/absent outcome is treated as UNKNOWN rather than
     * assumed final: an older daemon that does not send the field must not have its values presented as
     * certified.
     */
    public static Certainty certaintyOf(Result result) {
        if (result == null || result.outcome() == null || OUTCOME_UNKNOWN.equals(result.outcome())) {
            return Certainty.UNKNOWN;
        }
        return OUTCOME_CERTIFIED.equals(result.outcome()) ? Certainty.FINAL : Certainty.BEST_AVAILABLE;
    }

    /** A short phrase for a status line or tooltip; null when there is nothing worth saying. */
    public static String certaintyLabel(Certainty certainty) {
        return switch (certainty) {
            case PROVISIONAL -> "provisional — the analysis is still running";
            case FINAL -> null; // the normal case; saying so on every hint would be noise
            case BEST_AVAILABLE -> "best available — the analysis did not reach a fixpoint";
            case UNKNOWN -> "no completed analysis";
        };
    }

    /**
     * Analysis values established after one pass, streamed before the run finishes so the editor can be
     * annotated early. Arrives as a non-terminal {@code partialResult} frame, i.e. through
     * {@code DaemonClient.analyze}'s status consumer — see {@link #PARTIAL_RESULT}.
     * <p>
     * Merge these by element rather than replacing wholesale: {@code elements} is what one pass analyzed
     * (everything on the first, a shrinking subset after), not a complete picture. Nothing here is ever
     * retracted — values are write-once and only strengthen — so a merge never has to remove an annotation
     * a previous frame provided. {@code certain} means the fixpoint was certified and the values are final.
     */
    public record PartialResult(String requestId,
                                int iteration,
                                boolean fullPass,
                                boolean certain,
                                List<ElementAnnotation> elements) {
    }

    /** Frame type of a streamed {@link PartialResult}, as it appears in the status consumer. */
    public static final String PARTIAL_RESULT = "partialResult";

    /**
     * Fold a streamed pass into what is on screen, giving the result a front-end should now display.
     * <p>
     * Merges by element rather than replacing: a frame carries the elements of one pass, so replacing would
     * make everything the previous passes established disappear. An element is identified by {@code kind} +
     * {@code fqn} — its position can move as the user types, its identity cannot. Existing entries are
     * updated in place so the display order stays stable across frames, and unseen elements are kept:
     * values are write-once and only strengthen, so a pass that did not mention an element says nothing
     * against it.
     * <p>
     * Findings are carried over untouched; partial frames never contain any (guard findings are computed
     * after the fixpoint).
     *
     * @param current the result being displayed, or null before the first frame
     * @return a new result; neither argument is modified
     */
    public static Result merge(Result current, PartialResult partial) {
        Map<String, ElementAnnotation> byIdentity = new LinkedHashMap<>();
        if (current != null && current.elementAnnotations() != null) {
            for (ElementAnnotation e : current.elementAnnotations()) byIdentity.put(identity(e), e);
        }
        if (partial.elements() != null) {
            for (ElementAnnotation e : partial.elements()) byIdentity.put(identity(e), e);
        }
        List<ElementAnnotation> merged = List.copyOf(byIdentity.values());
        if (current == null) {
            // a run in progress has no outcome yet: UNKNOWN, so certaintyOf never calls it final
            return new Result(partial.requestId(), List.of(), merged, List.of(), 0, 0, 0L, OUTCOME_UNKNOWN);
        }
        return new Result(current.requestId(), current.findings(), merged,
                current.initializationProblems(), current.parseErrorCount(), current.hintsLoaded(),
                current.elapsedMillis(), current.outcome());
    }

    private static String identity(ElementAnnotation e) {
        return e.kind() + " " + e.fqn();
    }
}
