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

    record Options(boolean recurse, boolean forceShallow, boolean checkDuplicateNames, boolean trackObjectCreations) {
        public static final Options TEST = new Options(true, false, true,
                false);
        public static final Options PRODUCTION = new Options(true, false, false,
                true);
        public static final Options FORCE_SHALLOW = new Options(true, true, true,
                false);

        public static class Builder {
            boolean recurse;
            boolean forceShallow;
            boolean checkDuplicateNames;
            boolean trackObjectCreations;

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

            public Options build() {
                return new Options(recurse, forceShallow, checkDuplicateNames, trackObjectCreations);
            }
        }

    }

    interface ListOfLinks extends Value {
        List<Links> list();
    }
}
