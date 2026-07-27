package org.e2immu.analyzer.modification.link.impl.graph;

/**
 * Thrown when the source-level link computation of one method is abandoned, so that
 * {@code LinkComputerImpl.doMethod} can fall back to the method's shallow summary and stamp
 * {@code DEGRADED_ANALYSIS_METHOD}.
 * <p>
 * Two independent guards abandon a method, and they want opposite answers when a corpus degrades in bulk:
 * {@link Reason#EXPANSION_ROUNDS} means the graph refused to settle (structural), while
 * {@link Reason#WORK_CEILING} means it was settling but too slowly (a budget, tunable with
 * {@code -Dmaddi.workCeiling}). Both used to throw {@code UnsupportedOperationException("cycle protection")},
 * which made them indistinguishable in the log — on elasticsearch, 111 degraded methods could not be
 * attributed to either guard. The message keeps the {@code "cycle protection"} prefix so existing
 * log greps and the catch in {@code LinkComputerImpl} keep working.
 */
public class DegradedAnalysisException extends UnsupportedOperationException {
    /** the prefix shared by every reason; {@code LinkComputerImpl} recognizes the contract by this class. */
    public static final String PREFIX = "cycle protection";

    public enum Reason {
        /** the per-statement graph expansion loop did not reach a fixed point within its round limit. */
        EXPANSION_ROUNDS("expansion rounds"),
        /** the per-method edge-visit budget in {@link IncrementalFixpointEngine} ran out. */
        WORK_CEILING("work ceiling");

        private final String label;

        Reason(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final Reason reason;

    public DegradedAnalysisException(Reason reason) {
        super(PREFIX + ": " + reason.label());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
