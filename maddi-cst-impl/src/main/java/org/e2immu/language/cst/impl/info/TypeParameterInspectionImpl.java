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

import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.type.TypeParameterImpl;

import java.util.ArrayList;
import java.util.List;

public class TypeParameterInspectionImpl extends InspectionImpl implements TypeParameterInspection {

    private final List<ParameterizedType> typeBounds;

    public TypeParameterInspectionImpl(Inspection inspection, List<ParameterizedType> typeBounds) {
        super(null, inspection.comments(), inspection.source(), inspection.isSynthetic(),
                inspection.annotations(), null);
        this.typeBounds = typeBounds;
    }

    @Override
    public List<ParameterizedType> typeBounds() {
        return typeBounds;
    }

    @Override
    public boolean typeBoundsAreSet() {
        return true;
    }

    public static class Builder extends InspectionImpl.Builder<TypeParameter.Builder> implements TypeParameterInspection, TypeParameter.Builder {
        private List<ParameterizedType> typeBounds = new ArrayList<>();
        private final TypeParameterImpl typeParameter;
        private boolean typeBoundsAreSet;

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
            TypeParameterInspection tpi = new TypeParameterInspectionImpl(this, List.copyOf(typeBounds));
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
    }
}
