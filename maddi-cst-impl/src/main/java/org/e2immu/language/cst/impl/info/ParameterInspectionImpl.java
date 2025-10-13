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
