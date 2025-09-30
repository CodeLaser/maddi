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
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.resource.ByteCodeInspector;

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
