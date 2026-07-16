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
                LOGGER.warn("Keeping {}={}, refusing downgrade to {} on {}", property.key(), current, value, context);
                return false;
            }
        }
        return analysis.setAllowControlledOverwrite(property, value);
    }
}
