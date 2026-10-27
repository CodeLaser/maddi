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

package io.codelaser.maddi.inspection.impl.parser;

import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.parser.HierarchyHelper;

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
