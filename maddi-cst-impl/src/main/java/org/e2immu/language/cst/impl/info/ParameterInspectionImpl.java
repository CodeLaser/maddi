package org.e2immu.language.cst.impl.info;


import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.ParameterInfo;

public class ParameterInspectionImpl extends InspectionImpl implements ParameterInspection {

    private final boolean varArgs;
    private final boolean isFinal;

    public ParameterInspectionImpl(Inspection inspection, boolean isFinal, boolean varArgs) {
        super(inspection.access(), inspection.comments(), inspection.source(), inspection.isSynthetic(),
                inspection.annotations(), null);
        this.varArgs = varArgs;
        this.isFinal = isFinal;
    }

    @Override
    public boolean isFinal() {
        return isFinal;
    }

    @Override
    public boolean isVarArgs() {
        return varArgs;
    }

    public static class Builder extends InspectionImpl.Builder<ParameterInfo.Builder>
            implements ParameterInspection, ParameterInfo.Builder {
        private boolean varArgs;
        private boolean isFinal;
        private final ParameterInfoImpl parameterInfo;

        public Builder(ParameterInfoImpl parameterInfo) {
            this.parameterInfo = parameterInfo;
        }

        @Override
        public boolean isVarArgs() {
            return varArgs;
        }

        @Override
        public boolean isFinal() {
            return isFinal;
        }

        @Override
        public boolean hasBeenCommitted() {
            return parameterInfo.hasBeenCommitted();
        }

        @Override
        public Builder computeAccess() {
            throw new UnsupportedOperationException("there are no access modifiers for parameters");
        }

        @Override
        public void commit() {
            ParameterInspection pi = new ParameterInspectionImpl(this, isFinal, varArgs);
            parameterInfo.commit(pi);
        }

        @Override
        @Fluent
        public Builder setIsFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        @Override
        @Fluent
        public Builder setVarArgs(boolean varArgs) {
            this.varArgs = varArgs;
            return this;
        }
    }
}
