package org.e2immu.analyzer.modification.link;

import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.List;

public interface LinkComputer {
    // tests only!
    void doPrimaryType(TypeInfo primaryType);

    // can be called multiple times
    MethodLinkedVariables doMethod(MethodInfo methodInfo);

    int propertiesChanged();

    void reset();

    /**
     * Which fixpoint engine to use. {@link #LEGACY} is optimize's algorithm (the current default);
     * {@link #INCREMENTAL} is the witness-based incremental engine being ported from branch 'sv'.
     * Inert until Phase 3 of sv-integration-plan.md wires it into LinkComputerImpl.
     */
    enum Engine {LEGACY, INCREMENTAL}

    /*
     objectGraphLinks: store and propagate the coarse object-graph natures (∩ ≤ ≥) in the closure. None of
     linking's three applications (modification propagation via §m, same-type/VL2O for interface extraction,
     new-object tracking) consumes them — they exist for the full-fidelity content-containment OUTPUT the tests
     assert. Their transitive web is quadratic on deeply recursive generic structures (TestParSeqLinkBench:
     48.7s with, ~1s without), so PRODUCTION switches them off.
     */
    record Options(boolean recurse, boolean forceShallow, boolean checkDuplicateNames, boolean trackObjectCreations,
                   boolean objectGraphLinks, Engine engine) {
        public static final Options TEST = new Options(true, false, true,
                false, true, Engine.LEGACY);
        public static final Options PRODUCTION = new Options(true, false, false,
                true, false, Engine.LEGACY);
        public static final Options FORCE_SHALLOW = new Options(true, true, true,
                false, true, Engine.LEGACY);

        public static class Builder {
            boolean recurse;
            boolean forceShallow;
            boolean checkDuplicateNames;
            boolean trackObjectCreations;
            boolean objectGraphLinks = true;
            Engine engine = Engine.LEGACY;

            public Builder setObjectGraphLinks(boolean objectGraphLinks) {
                this.objectGraphLinks = objectGraphLinks;
                return this;
            }

            public Builder setCheckDuplicateNames(boolean checkDuplicateNames) {
                this.checkDuplicateNames = checkDuplicateNames;
                return this;
            }

            public Builder setForceShallow(boolean forceShallow) {
                this.forceShallow = forceShallow;
                return this;
            }

            public Builder setRecurse(boolean recurse) {
                this.recurse = recurse;
                return this;
            }

            public Builder setTrackObjectCreations(boolean trackObjectCreations) {
                this.trackObjectCreations = trackObjectCreations;
                return this;
            }

            public Builder setEngine(Engine engine) {
                this.engine = engine;
                return this;
            }

            public Options build() {
                return new Options(recurse, forceShallow, checkDuplicateNames, trackObjectCreations,
                        objectGraphLinks, engine);
            }
        }

    }

    interface ListOfLinks extends Value {
        List<Links> list();
    }
}
