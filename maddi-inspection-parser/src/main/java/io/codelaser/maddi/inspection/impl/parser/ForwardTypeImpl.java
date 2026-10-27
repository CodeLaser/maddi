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

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.parser.ForwardType;
import io.codelaser.maddi.inspection.api.parser.GenericsHelper;
import io.codelaser.maddi.inspection.api.parser.MethodTypeParameterMap;
import io.codelaser.maddi.inspection.api.parser.TypeParameterMap;

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
