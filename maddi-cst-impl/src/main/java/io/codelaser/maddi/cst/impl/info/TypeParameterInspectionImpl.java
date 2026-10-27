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

import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.info.Variance;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.type.TypeParameterImpl;

import java.util.ArrayList;
import java.util.List;

// Note: we store the bounds as separate types, rather than as an intersection type
public class TypeParameterInspectionImpl extends InspectionImpl implements TypeParameterInspection {

    private final List<ParameterizedType> typeBounds;
    private final Variance variance;

    // private: the Builder is the only construction route, so every caller of this constructor is inside
    // this primary type. That is what lets the analyzer verify, rather than believe, that the collections it
    // stores are immutable. See docs/design/dynamic-immutability-feasibility.md.
    private TypeParameterInspectionImpl(Inspection inspection, List<ParameterizedType> typeBounds, Variance variance) {
        super(null, inspection.comments(), inspection.source(), inspection.isSynthetic(),
                inspection.annotations(), null);
        this.typeBounds = typeBounds;
        this.variance = variance;
    }

    @Override
    public List<ParameterizedType> typeBounds() {
        return typeBounds;
    }

    @Override
    public Variance variance() {
        return variance;
    }

    @Override
    public boolean typeBoundsAreSet() {
        return true;
    }

    public static class Builder extends InspectionImpl.Builder<TypeParameter.Builder> implements TypeParameterInspection, TypeParameter.Builder {
        private List<ParameterizedType> typeBounds = new ArrayList<>();
        private final TypeParameterImpl typeParameter;
        private boolean typeBoundsAreSet;
        private Variance variance = Variance.INVARIANT;

        public Builder(TypeParameterImpl typeParameter) {
            this.typeParameter = typeParameter;
        }

        @Override
        public boolean typeBoundsAreSet() {
            return typeBoundsAreSet;
        }

        @Override
        public boolean hasBeenCommitted() {
            return typeParameter.hasBeenInspected();
        }

        @Override
        public Builder computeAccess() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void commit() {
            TypeParameterInspection tpi = new TypeParameterInspectionImpl(this, List.copyOf(typeBounds), variance);
            typeParameter.commit(tpi);
        }

        @Override
        public Builder setTypeBounds(List<ParameterizedType> typeBounds) {
            this.typeBounds = typeBounds;
            this.typeBoundsAreSet = true;
            return this;
        }

        @Override
        public List<ParameterizedType> typeBounds() {
            return typeBounds;
        }

        @Override
        public Builder setVariance(Variance variance) {
            this.variance = variance;
            return this;
        }

        @Override
        public Variance variance() {
            return variance;
        }
    }
}
