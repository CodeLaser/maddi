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


import org.e2immu.annotation.rare.IgnoreModifications;
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
    // analyzer overlay for the initializer, same story as InfoImpl.propertyValueMap: derived metadata filled
    // after commitment, manual hidden content (road-to-immutability §050) -- every Info analysis store carries
    // @IgnoreModifications
    @IgnoreModifications
    private final PropertyValueMap analysisOfInitializer = new PropertyValueMapImpl();

    // private: the Builder is the only construction route, so every caller of this constructor is inside
    // this primary type. That is what lets the analyzer verify, rather than believe, that the collections it
    // stores are immutable. See docs/dynamic-immutability-feasibility.md.
    private FieldInspectionImpl(Inspection inspection, Set<FieldModifier> fieldModifiers, Expression initializer) {
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
           // copy the rest of the inspection state, or it is silently reset to defaults on the new field.
           // synthetic in particular distinguishes an enum constant (see TypePrinterImpl.enumConstantStream),
           // so dropping it makes a translated enum print as 'static final E X = new E()' instead of 'X'.
           setAccess(fi.access());
           setSynthetic(fi.isSynthetic());
           setSource(fi.source());
           if (fi.comments() != null) addComments(fi.comments());
           if (fi.annotations() != null) addAnnotations(fi.annotations());
           setJavaDoc(fi.javaDoc());
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
                if (fieldModifier.isInternal()) return AccessEnum.INTERNAL;
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
