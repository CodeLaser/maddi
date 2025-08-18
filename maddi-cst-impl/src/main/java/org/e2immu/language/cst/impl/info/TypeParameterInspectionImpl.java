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
