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

package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.HierarchyHelper;

public class HierarchyHelperImpl implements HierarchyHelper {

    enum IsMyselfEnum implements IsMyself {

        NO,
        PTA, // in same primary type analyzer,
        YES;

        @Override
        public boolean isYes() {
            return YES == this;
        }

        @Override
        public boolean isNo() {
            return NO == this;
        }

        @Override
        public boolean inSamePrimaryType() {
            return PTA == this;
        }

        public boolean toFalse(Property property) {
            throw new UnsupportedOperationException();
        }
/*
            return switch (property) {
                case CONTAINER, CONTEXT_CONTAINER -> this == PTA || this == YES;
                case IMMUTABLE, INDEPENDENT -> this == YES;
                default -> false;
            };
        }*/
    }

    @Override
    public IsMyself isMyself(TypeInfo me, ParameterizedType type) {
        TypeInfo bestType = type.bestTypeInfo();
        return isMyself(me, bestType);
    }

    @Override
    public IsMyself isMyself(TypeInfo me, TypeInfo bestType) {
        if (bestType == null) return IsMyselfEnum.NO;
        if (me.equals(bestType)) return IsMyselfEnum.YES;
        TypeInfo primaryVariable = bestType.primaryType();
        TypeInfo primaryMyself = me.primaryType();
        if (primaryMyself.equals(primaryVariable)) {
            if (bestType.isInterface()) return IsMyselfEnum.NO;

            // in the same compilation unit, analysed at the same time
            boolean inHierarchy = parentalHierarchyContains(bestType, me) ||
                                  parentalHierarchyContains(me, bestType) ||
                                  nonStaticallyEnclosingTypesContains(bestType, me) ||
                                  nonStaticallyEnclosingTypesContains(me, bestType);
            if (inHierarchy) return IsMyselfEnum.YES;
            // must be symmetrical: see e.g. Basics_24
            if (me.fieldsAccessedInRestOfPrimaryType()
                || bestType.fieldsAccessedInRestOfPrimaryType()) {
                return IsMyselfEnum.PTA;
            }
        }
        return IsMyselfEnum.NO;
    }

    @Override
    public boolean nonStaticallyEnclosingTypesContains(TypeInfo me, TypeInfo target) {
        if (me.compilationUnitOrEnclosingType().isLeft()) return false;
        if (me.isStatic()) return false;
        TypeInfo enclosing = me.compilationUnitOrEnclosingType().getRight();
        if (enclosing.equals(target)) return true;
        return nonStaticallyEnclosingTypesContains(enclosing, target);
    }

    @Override
    public boolean parentalHierarchyContains(TypeInfo me, TypeInfo target) {
        ParameterizedType parent = me.parentClass();
        if (parent == null || parent.isJavaLangObject()) return false;
        if (target.equals(parent.typeInfo())) return true;
        return parentalHierarchyContains(parent.typeInfo(), target);
    }

    @Override
    public TypeInfo recursivelyImplements(TypeInfo me, String fqn) {
        if (me.fullyQualifiedName().equals(fqn)) return me;
        ParameterizedType parentClass = me.parentClass();
        if (parentClass != null && !parentClass.isJavaLangObject()) {
            TypeInfo res = recursivelyImplements(parentClass.typeInfo(), fqn);
            if (res != null) return res;
        }
        for (ParameterizedType implemented : me.interfacesImplemented()) {
            TypeInfo res = recursivelyImplements(implemented.typeInfo(), fqn);
            if (res != null) return res;
        }
        return null;
    }
}
