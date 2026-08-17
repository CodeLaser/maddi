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

package org.e2immu.analyzer.ide.daemon;

import java.util.List;
import java.util.Map;

/**
 * Wire protocol between the IntelliJ plugin (JBR 21) and the maddi analysis daemon (JDK 25+).
 * <p>
 * Transport is a loopback TCP socket carrying newline-delimited JSON (NDJSON): one JSON object
 * per line, UTF-8. Every message is an object with a {@code "type"} discriminator; requests are
 * dispatched on that field, responses are these records serialized with the type added.
 * <p>
 * All payloads are primitives / strings / nested records only — no maddi types leak across the
 * boundary, which is what keeps the plugin safely on JBR 21 while maddi runs on JDK 25.
 */
public final class DaemonProtocol {
    private DaemonProtocol() {
    }

    public static final int PROTOCOL_VERSION = 1;

    // ---- message type discriminators ----
    public static final String T_HANDSHAKE = "handshake";
    public static final String T_HANDSHAKE_ACK = "handshakeAck";
    public static final String T_ANALYZE_PROJECT = "analyzeProject";
    public static final String T_STATUS = "status";
    /**
     * Values established so far, sent zero or more times before the terminal {@link #T_RESULT}. Deliberately
     * NOT a repeatable {@code result}: clients loop until a terminal frame, so a new non-terminal type reaches
     * them as an ordinary streamed frame, while a second {@code result} would be mistaken for the whole run.
     */
    public static final String T_PARTIAL_RESULT = "partialResult";
    public static final String T_RESULT = "result";
    public static final String T_ERROR = "error";
    public static final String T_PING = "ping";
    public static final String T_PONG = "pong";
    public static final String T_CANCEL = "cancel";
    public static final String T_SHUTDOWN = "shutdown";
    public static final String T_BYE = "bye";

    // ---- responses (serialized; DaemonMain adds the "type" field) ----

    public record HandshakeAck(int protocolVersion, String daemonVersion, String maddiVersion) {
    }

    public record Pong(long nowNanos) {
    }

    /** Coarse progress for a long analysis. phase ∈ initialize|parse|prep|order|analyze|collect. */
    public record Status(String requestId, String phase, String message,
                         Integer typesDone, Integer typesTotal) {
    }

    /** Daemon stays alive after an error. kind ∈ parse|analyze|internal|busy|bad-request. */
    public record ErrorMsg(String requestId, String kind, String message) {
    }

    // ---- analysis request/result (payloads used from M1 onwards) ----

    /** One source or test root fed to maddi. */
    public record SourceRoot(String name, String path, boolean test) {
    }

    /** One classpath entry: a jar or a (hot) compiler-output directory. scope ∈ compile|test|runtime|test-runtime. */
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

    public record AnalyzeProject(String requestId, AnalyzeConfig config) {
    }

    /** A located finding (guard violation, crash, parse error) with a recursive why-chain. */
    public record Finding(String uri,
                          Integer beginLine, Integer beginCol, Integer endLine, Integer endCol,
                          String severity, String category, String message,
                          List<Finding> causes) {
    }

    /**
     * One rendered annotation with the metadata the inline-hints filter needs.
     * polarity ∈ POSITIVE (proven-stronger: @NotModified/@Immutable/@Container/@Independent/@NotNull/@Final),
     * NEGATIVE (baseline: @Modified/@Mutable/@Dependent/@Nullable), NEUTRAL (@GetSet/@Identity/@Fluent/…).
     * contextDefault = the value is implied by the enclosing declaration (a container's @NotModified params,
     * a plain method's @Modified, …), so it can be hidden as clutter; surprising values (a @Modified param in a
     * @Container) are NOT context defaults.
     */
    public record Annotation(String text, String polarity, boolean contextDefault) {
    }

    /** Computed analysis for one declaration. kind ∈ TYPE|METHOD|FIELD|PARAMETER. */
    public record ElementAnnotation(String uri,
                                    Integer beginLine, Integer beginCol, Integer endLine, Integer endCol,
                                    String kind,
                                    String fqn,
                                    List<String> displayAnnotations,
                                    List<Annotation> annotations,
                                    Map<String, String> properties) {
    }

    /**
     * How the fixpoint iteration ended, which decides whether the values in a result are final.
     * <ul>
     *   <li>{@code CERTIFIED} — the fixpoint was reached: the values are final.</li>
     *   <li>{@code MAX_ITERATIONS} — stopped at the iteration cap: best available, not certified.</li>
     *   <li>{@code PLATEAU} — stopped on the oscillation plateau: best available, not certified.</li>
     *   <li>{@code UNKNOWN} — the run produced no terminal phase (e.g. it ended at parse errors).</li>
     * </ul>
     * Worth surfacing rather than assuming: a value that stopped at the cap looks exactly like a certified
     * one, and only this says otherwise.
     */
    public static final String OUTCOME_CERTIFIED = "CERTIFIED";
    public static final String OUTCOME_UNKNOWN = "UNKNOWN";

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
     * Analysis values established after one pass of the fixpoint iteration, for display before the run ends.
     * Safe to show: values are write-once and refine monotonically, so nothing here is ever retracted — a
     * later frame (or the terminal {@code result}) can only strengthen what a previous one said.
     * <p>
     * Carries annotations only, no findings: guard findings are computed once, after the fixpoint, so there
     * is nothing partial to report about them.
     * <p>
     * {@code elements} over-approximates what changed — the whole analysis order on the first pass, the
     * shrinking dirty set afterwards — so a consumer merges by element rather than treating a frame as a
     * complete picture. {@code certain} marks the run's values as final (the fixpoint was certified); until
     * then a displayed value is established but may still strengthen.
     */
    public record PartialResult(String requestId,
                                int iteration,
                                boolean fullPass,
                                boolean certain,
                                List<ElementAnnotation> elements) {
    }
}
