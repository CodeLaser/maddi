package org.e2immu.language.cst.impl.info;


import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.Access;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.FieldModifier;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;

import java.util.HashSet;
import java.util.Set;


public class FieldInspectionImpl extends InspectionImpl implements FieldInspection {
    private final Set<FieldModifier> fieldModifiers;
    private final Expression initializer;
    private final PropertyValueMap analysisOfInitializer = new PropertyValueMapImpl();

    public FieldInspectionImpl(Inspection inspection, Set<FieldModifier> fieldModifiers, Expression initializer) {
        super(inspection.access(), inspection.comments(), inspection.source(), inspection.isSynthetic(),
                inspection.annotations(), inspection.javaDoc());
        this.fieldModifiers = fieldModifiers;
        assert initializer != null; // use empty expression if you want an absence of initializer.
        this.initializer = initializer;
    }

    @Override
    public Expression initializer() {
        return initializer;
    }

    @Override
    public PropertyValueMap analysisOfInitializer() {
        return analysisOfInitializer;
    }

    @Override
    public Set<FieldModifier> fieldModifiers() {
        return fieldModifiers;
    }

    public static class Builder extends InspectionImpl.Builder<FieldInfo.Builder> implements FieldInfo.Builder, FieldInspection {
        private final FieldInfoImpl fieldInfo;
        private final Set<FieldModifier> fieldModifiers = new HashSet<>();
        private Expression initializer;

        public Builder(FieldInfoImpl fieldInfo) {
            this.fieldInfo = fieldInfo;
        }

        public Builder(FieldInfoImpl fieldInfo, FieldInspection fi) {
           this.fieldInfo = fieldInfo;
           this.initializer = fi.initializer();
           this.fieldModifiers.addAll(fi.fieldModifiers());
        }

        @Override
        public Builder computeAccess() {
            Access fromType = fieldInfo.owner().access();
            Access fromModifier = accessFromFieldModifier();
            Access combined = fromModifier.combine(fromType);
            setAccess(combined);
            return this;
        }

        private Access accessFromFieldModifier() {
            for (FieldModifier fieldModifier : fieldModifiers) {
                if (fieldModifier.isProtected()) return AccessEnum.PROTECTED;
                if (fieldModifier.isPrivate()) return AccessEnum.PRIVATE;
                if (fieldModifier.isPublic()) return AccessEnum.PUBLIC;
            }
            return AccessEnum.PACKAGE;
        }

        @Override
        public Builder addFieldModifier(FieldModifier fieldModifier) {
            fieldModifiers.add(fieldModifier);
            return this;
        }

        @Override
        public Builder setInitializer(Expression initializer) {
            this.initializer = initializer;
            return this;
        }

        @Override
        public void commit() {
            fieldInfo.commit(new FieldInspectionImpl(this, Set.copyOf(fieldModifiers), initializer));
        }

        @Override
        public Expression initializer() {
            return initializer;
        }

        @Override
        public Set<FieldModifier> fieldModifiers() {
            return fieldModifiers;
        }

        @Override
        public boolean hasBeenCommitted() {
            return fieldInfo.hasBeenCommitted();
        }

        @Override
        public PropertyValueMap analysisOfInitializer() {
            throw new UnsupportedOperationException();
        }
    }
}
