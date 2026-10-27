/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.impl.info;


import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.info.ParameterInfo;

public class ParameterInspectionImpl extends InspectionImpl implements ParameterInspection {

    private final boolean varArgs;
    private final boolean isFinal;

    // private: the Builder is the only construction route, so every caller of this constructor is inside
    // this primary type. That is what lets the analyzer verify, rather than believe, that the collections it
    // stores are immutable. See docs/design/dynamic-immutability-feasibility.md.
    private ParameterInspectionImpl(Inspection inspection, boolean isFinal, boolean varArgs) {
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
