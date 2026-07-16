package org.e2immu.analyzer.modification.common.util;

import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.analysis.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iteration-tolerant property write. The controlled-overwrite policy only allows strengthening (e.g.
 * unmodified false→true, @Mutable→@FinalFields); an attempted WEAKENING across analyzer iterations (a premature
 * optimistic conclusion contradicted once callee summaries on a call cycle become available) threw and — under
 * per-element fault isolation — cost the element its whole analysis, while still leaving the stale strong value
 * in place. This helper keeps the stronger existing value and logs, which strictly dominates the crash.
 * <p>
 * OPEN DESIGN QUESTION (real-world corpus, timefold-solver): whether the iterating analyzer should instead allow
 * the weakening direction for the evidence-accumulating properties (UNMODIFIED_*, NON_MODIFYING_METHOD,
 * IMMUTABLE_TYPE), making 'modified'/'@Mutable' absorbing. That changes verdicts and is not decided here.
 */
public final class TolerantWrite {
    private static final Logger LOGGER = LoggerFactory.getLogger(TolerantWrite.class);

    private TolerantWrite() {
    }

    // convergence diagnosis: successful (value-changing) writes per property key, reset per iteration by the
    // iterating analyzer. Covers every setAllowControlledOverwrite in link+analyzer (they all route through
    // here); plain set() sites (write-once) do not oscillate and are not counted.
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.LongAdder>
            CHANGES = new java.util.concurrent.ConcurrentHashMap<>();

    public static java.util.Map<String, Long> changeCounts() {
        java.util.Map<String, Long> result = new java.util.TreeMap<>();
        CHANGES.forEach((k, v) -> result.put(k, v.sum()));
        return result;
    }

    public static void resetChangeCounts() {
        CHANGES.clear();
    }

    /** convergence diagnosis for write sites that do not go through this class (plain set() + counter) */
    public static void count(String key) {
        CHANGES.computeIfAbsent(key, _ -> new java.util.concurrent.atomic.LongAdder()).increment();
    }

    /**
     * Write-once {@code set()} with the same diagnosis/worklist bookkeeping as the controlled-overwrite path.
     * Without target attribution here, the worklist missed dependents of the abstract-method batch's decisions
     * (run13: 138 verdicts more conservative than the full-re-analysis baseline).
     */
    public static <V extends Value> void setOnce(PropertyValueMap analysis, Property property, V value,
                                                 Object context) {
        analysis.set(property, value);
        CHANGES.computeIfAbsent(property.key(), _ -> new java.util.concurrent.atomic.LongAdder()).increment();
        if (context != null && !(context instanceof String) && !INTERNAL_PROPERTIES.contains(property.key())) {
            CHANGED_TARGETS.add(context);
        }
    }

    // element-INTERNAL (statement-level) properties: their changes are invisible to dependents and must not
    // propagate through the worklist (a ParameterInfo context can reach here via UNMODIFIED_VARIABLE)
    private static final java.util.Set<String> INTERNAL_PROPERTIES =
            java.util.Set.of("unmodifiedVariable", "downcastVariable", "variablesLinkedToObject",
                    "linkedVariablesArguments");

    // worklist support: the targets (Info / ParameterInfo contexts) of value-CHANGING writes this iteration.
    // Captures writes that land on an element OTHER than the one currently being processed (the link computer's
    // on-demand recursion writing a callee's METHOD_LINKS) — counter-delta attribution alone misses those.
    private static final java.util.Set<Object> CHANGED_TARGETS =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public static java.util.Set<Object> changedTargets() {
        synchronized (CHANGED_TARGETS) {
            return new java.util.HashSet<>(CHANGED_TARGETS);
        }
    }

    public static void resetChangedTargets() {
        CHANGED_TARGETS.clear();
    }

    // gate UNMODOWN=1: allow the true->false / weakening direction for the evidence-accumulating modification
    // properties (see the downgrade branch below); OFF by default pending the verdict A/B
    private static final boolean UNMODOWN = System.getenv("UNMODOWN") != null;
    private static final java.util.Set<String> EVIDENCE_ACCUMULATING = java.util.Set.of(
            "unmodifiedVariable", "unmodifiedParameter", "unmodifiedField", "nonModifyingMethod");

    public static <V extends Value> boolean setAllowControlledOverwrite(PropertyValueMap analysis,
                                                                        Property property,
                                                                        V value) {
        return setAllowControlledOverwrite(analysis, property, value, "?");
    }

    public static <V extends Value> boolean setAllowControlledOverwrite(PropertyValueMap analysis,
                                                                        Property property,
                                                                        V value,
                                                                        Object context) {
        if (analysis.haveAnalyzedValueFor(property)) {
            V current = analysis.getOrDefault(property, value);
            if (!current.equals(value) && !current.overwriteAllowed(value)) {
                if (UNMODOWN && EVIDENCE_ACCUMULATING.contains(property.key())) {
                    // EXPERIMENT (gate UNMODOWN=1): for the UNMODIFIED_* family, modification evidence only
                    // accumulates across iterations — 'modified' (false) is absorbing, so the legal refinement
                    // direction is true->false, OPPOSITE to Bool's default policy. Apply the downgrade.
                    LOGGER.debug("Downgrading {} {} -> {} on {}", property.key(), current, value, context);
                    boolean downgraded = analysis.overwrite(property, value);
                    if (downgraded) {
                        CHANGES.computeIfAbsent(property.key(), _ -> new java.util.concurrent.atomic.LongAdder())
                                .increment();
                        if (context != null && !(context instanceof String) && !INTERNAL_PROPERTIES.contains(property.key())) {
                            CHANGED_TARGETS.add(context);
                        }
                    }
                    return downgraded;
                }
                LOGGER.warn("Keeping {}={}, refusing downgrade to {} on {}", property.key(), current, value, context);
                return false;
            }
        }
        boolean changed = analysis.setAllowControlledOverwrite(property, value);
        if (changed) {
            CHANGES.computeIfAbsent(property.key(), _ -> new java.util.concurrent.atomic.LongAdder()).increment();
            if (context != null && !(context instanceof String) && !INTERNAL_PROPERTIES.contains(property.key())) {
                CHANGED_TARGETS.add(context);
            }
        }
        return changed;
    }
}
