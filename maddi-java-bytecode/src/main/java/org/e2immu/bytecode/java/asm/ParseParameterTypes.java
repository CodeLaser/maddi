/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.bytecode.java.asm;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.resource.ByteCodeInspector;

import java.util.ArrayList;
import java.util.List;

record ParseParameterTypes(Runtime runtime,
                           LocalTypeMap findType,
                           LocalTypeMap.LoadMode loadMode) {
    public static final char CARET_THROWS = '^';
    public static final char CLOSE_BRACKET = ')';

    record Result(List<ParameterizedType> parameterTypes, ParameterizedType returnType,
                  List<ParameterizedType> exceptionTypes) {
    }

    Result parseParameterTypesOfMethod(ByteCodeInspector.TypeParameterContext typeContext,
                                       SourceSet sourceSetOfRequest, SourceSet nearestSourceSet,
                                       String signature, boolean createStub) {
        List<ParameterizedType> parameterTypes = new ArrayList<>();
        List<ParameterizedType> exceptionTypes = new ArrayList<>();
        ParameterizedType returnType = null;

        int startPos;
        boolean doParameters;
        if (signature.startsWith("()")) {
            startPos = 2;
            doParameters = false;
        } else {
            startPos = 1;
            doParameters = true;
        }
        boolean doExceptionTypes = false;
        while (true) {
            String startOfType = signature.substring(startPos);
            ParameterizedTypeFactory.Result result = ParameterizedTypeFactory.from(runtime, sourceSetOfRequest,
                    nearestSourceSet, typeContext, findType, loadMode, startOfType, createStub);
            if (result == null) return null;
            int end = startPos + result.nextPos;

            if (doParameters) parameterTypes.add(result.parameterizedType);
            else if (doExceptionTypes) exceptionTypes.add(result.parameterizedType);
            else returnType = result.parameterizedType;

            if (end >= signature.length()) {
                break;
            } else {
                char atEnd = signature.charAt(end);
                if (atEnd == CARET_THROWS) {
                    doExceptionTypes = true;
                    startPos = end + 1;
                } else {
                    if (atEnd == CLOSE_BRACKET) {
                        doParameters = false;
                        startPos = end + 1;
                    } else {
                        startPos = end;
                    }
                }
            }
        }
        return new Result(parameterTypes, returnType, exceptionTypes);
    }
}
