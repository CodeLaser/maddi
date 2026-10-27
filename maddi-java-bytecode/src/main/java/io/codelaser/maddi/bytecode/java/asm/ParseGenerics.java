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
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.resource.ByteCodeInspector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class ParseGenerics<T extends Info> {
    private final Runtime runtime;
    private final ByteCodeInspector.TypeParameterContext typeParameterContext;
    private final SourceSet sourceSetOfRequest;
    private final SourceSet nearestSourceSet;
    private final T owner;
    private final LocalTypeMap localTypeMap;
    private final LocalTypeMap.LoadMode loadMode;
    private final String signature;
    private final NewTypeParameterCreator<T> newTypeParameterCreator;
    private final Consumer<TypeParameter> addTypeParameter;
    private final boolean createStub;

    public static final char COLON = ':';
    public static final char GT_END_TYPE_PARAMS = '>';

    private int startPos;
    private int endPos;
    private final List<TypeParameter> typeParameters = new ArrayList<>();
    private boolean more;
    private boolean typeNotFoundError;

    @FunctionalInterface
    interface NewTypeParameterCreator<T extends Info> {
        TypeParameter create(int index, String name, T owner);
    }

    ParseGenerics(Runtime runtime,
                  ByteCodeInspector.TypeParameterContext typeParameterContext,
                  SourceSet sourceSetOfRequest,
                  SourceSet nearestSourceSet,
                  T owner,
                  LocalTypeMap localTypeMap,
                  LocalTypeMap.LoadMode loadMode,
                  NewTypeParameterCreator<T> newTypeParameterCreator,
                  Consumer<TypeParameter> addTypeParameter,
                  String signature,
                  boolean createStub) {
        this.runtime = runtime;
        this.typeParameterContext = typeParameterContext;
        this.owner = owner;
        this.localTypeMap = localTypeMap;
        this.loadMode = loadMode;
        this.signature = signature;
        this.newTypeParameterCreator = newTypeParameterCreator;
        this.addTypeParameter = addTypeParameter;
        this.createStub = createStub;
        this.sourceSetOfRequest = sourceSetOfRequest;
        this.nearestSourceSet = nearestSourceSet;
    }

    int goReturnEndPos() {
        int infiniteLoopProtection = 0;
        while (true) {
            startPos = 1;
            int index = 0;
            typeNotFoundError = false;
            do {
                boolean error = iterativelyParseGenerics(index, infiniteLoopProtection == 0);
                if (error) {
                    return -1; // error state
                }
                ++index;
            } while (more);
            if (!typeNotFoundError) break;
            infiniteLoopProtection++;
            if (infiniteLoopProtection > 100) {
                throw new UnsupportedOperationException("In infinite loop");
            }
        }
        for (TypeParameter typeParameter : typeParameters) {
            typeParameter.builder().commit();
            addTypeParameter.accept(typeParameter);
        }
        return endPos;
    }

    private boolean iterativelyParseGenerics(int index, boolean firstIteration) {
        int end = signature.indexOf(COLON, startPos);
        char atEnd = COLON;

        // example for extends keyword: sig='<T::Ljava/lang/annotation/Annotation;>(Ljava/lang/Class<TT;>;)TT;' for
        // method getAnnotation in java.lang.reflect.AnnotatedElement

        String name = signature.substring(startPos, end);
        TypeParameter typeParameter;
        if (firstIteration) {
            typeParameter = newTypeParameterCreator.create(index, name, owner);
            typeParameters.add(typeParameter);
            typeParameterContext.add(typeParameter);
        } else {
            typeParameter = typeParameters.get(index);
        }
        List<ParameterizedType> typeBounds = new ArrayList<>();

        while (atEnd == COLON) {
            char charAfterColon = signature.charAt(end + 1);
            if (charAfterColon == COLON) { // this can happen max. once, when there is no class extension, but there are interface extensions
                end++;
            }
            ParameterizedTypeFactory.Result result = ParameterizedTypeFactory.from(runtime, sourceSetOfRequest,
                    nearestSourceSet,
                    typeParameterContext, localTypeMap, loadMode, signature.substring(end + 1), createStub);
            if (result == null) return true; // unable to load type
            if (result.parameterizedType.typeInfo() == null
                || !result.parameterizedType.typeInfo().isJavaLangObject()) {
                typeBounds.add(result.parameterizedType);
            }

            end = result.nextPos + end + 1;
            atEnd = signature.charAt(end);

            typeNotFoundError = typeNotFoundError || result.typeNotFoundError;
        }

        typeParameter.builder().setTypeBounds(List.copyOf(typeBounds));

        if (GT_END_TYPE_PARAMS == atEnd) {
            more = false;
            endPos = end;
        } else {
            more = true;
            startPos = end;
        }
        return false;
    }
}
