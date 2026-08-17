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

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.ForwardType;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.api.parser.MethodTypeParameterMap;
import org.e2immu.language.inspection.api.parser.TypeParameterMap;

public record ForwardTypeImpl(ParameterizedType type,
                              boolean erasure,
                              boolean erasureOnFailure,
                              TypeParameterMap extra) implements ForwardType {

    public ForwardTypeImpl(ParameterizedType type, boolean erasure, TypeParameterMap extra) {
        this(type, erasure, erasure, extra);
    }

    @Override
    public MethodTypeParameterMap computeSAM(Runtime runtime, GenericsHelper genericsHelper, TypeInfo primaryType) {
        if (type == null || type.isVoid()) return null;
        MethodTypeParameterMap sam = genericsHelper.findSingleAbstractMethodOfInterface(type, false);
        if (sam != null) {
            return sam.expand(runtime, primaryType, type.initialTypeParameterMap());
        }
        return null;
    }

    @Override
    public boolean isVoid(Runtime runtime, GenericsHelper genericsHelper) {
        if (type == null || type.typeInfo() == null) return false;
        if (type.isVoid()) return true;
        MethodInfo sam = type.typeInfo().singleAbstractMethod();
        if (sam == null) return false;
        MethodTypeParameterMap samMap = genericsHelper.findSingleAbstractMethodOfInterface(type, true);
        assert samMap != null;
        return samMap.getConcreteReturnType(runtime).isVoid();
    }

    @Override
    public String toString() {
        return "[FWD: " + (type == null ? "null" : type.detailedString()) + ", erasure? " + erasure + "]";
    }
}
