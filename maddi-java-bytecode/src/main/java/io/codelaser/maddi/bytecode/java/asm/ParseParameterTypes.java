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

package io.codelaser.maddi.bytecode.java.asm;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.resource.ByteCodeInspector;

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
