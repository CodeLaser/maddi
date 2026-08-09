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

        /**
         * ⛔ <b>JLS 9.3: EVERY FIELD DECLARED IN THE BODY OF AN INTERFACE IS IMPLICITLY PUBLIC, STATIC AND
         * FINAL</b>, and the modifier list carries only what was WRITTEN — so a constant declared
         * {@code int X = 1;} arrived here with no modifiers and was recorded as PACKAGE.
         * <p>
         * The rule was already applied twice in this package and to two of the three member kinds:
         * {@link MethodInspectionImpl.Builder#computeAccess} makes an interface method PUBLIC without a
         * modifier, a nested type carries a materialised PUBLIC modifier, and {@code FieldInfoImpl.isPropertyFinal}
         * quotes this very sentence of the JLS — ten lines from the accessor that got it wrong. Only the field's
         * ACCESS was missed.
         * <p>
         * ⛔⛔ <b>AND IT IS GUARDED ON {@code isSynthetic()}, WHICH IS NOT A DETAIL: THE JLS TALKS ABOUT FIELDS
         * DECLARED IN THE BODY OF AN INTERFACE, AND MADDI ALSO ATTACHES FIELDS TO ONE.</b>
         * {@code CreateSyntheticFieldsForGetSet} gives {@code java.util.List} a non-static
         * {@code _synthetic_list}, and {@code FieldInfoImpl.isStatic}'s javadoc already records that this is
         * exactly why the implicit-STATIC rule is applied where the declaration is read rather than in the
         * accessor — <i>"an accessor cannot tell those from a constant"</i>. The BUILDER can: {@code setSynthetic}
         * runs before {@code computeAccess} on that path. Measured, and it is why the guard is here: without it a
         * full maddi test run <b>rewrote the annotated-API archive</b>, giving {@code java.util.List} a new
         * {@code F_synthetic_list} entry — a modelling artefact promoted into the published API annotations.
         * <p>
         * ⚠ Like the method rule, this does NOT combine with the owner's access. The two are different
         * questions: {@code access()} answers "what does this declaration say", and whether the enclosing type
         * can be reached is the caller's to ask. Combining would report an interface constant as PACKAGE
         * whenever the interface itself is package-private, which is the answer that produced gap {@code #173} —
         * a pre-widen that "widens" a member already as public as Java allows.
         */
        @Override
        public Builder computeAccess() {
            if (fieldInfo.owner().isInterface() && !isSynthetic()) {
                setAccess(AccessEnum.PUBLIC);
                return this;
            }
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
