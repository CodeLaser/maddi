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

package io.codelaser.maddi.cst.impl.info;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.info.*;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.type.TypeNature;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface TypeInspection extends Inspection {

    interface MethodMap {
        /** @throws java.util.NoSuchElementException when the type has no such method */
        MethodInfo get(String name, int numParams, Supplier<String> paramFqnCsv);

        /**
         * As {@link #get}, answering {@code null} instead of throwing — for the callers whose next step is to
         * LOAD what is not there yet, and for whom "absent" is therefore an answer rather than a failure.
         */
        MethodInfo getOrNull(String name, int numParams, Supplier<String> paramFqnCsv);
    }

    MethodMap methodMap();

    List<FieldInfo> fields();

    List<TypeParameter> typeParameters();

    boolean isAbstract();

    Stream<MethodInfo> methodStream();

    List<MethodInfo> constructors();

    TypeNature typeNature();

    /**
     * only returns null for JLO, primitives.
     *
     * @return the parent of this type
     */
    ParameterizedType parentClass();

    List<ParameterizedType> interfacesImplemented();

    MethodInfo singleAbstractMethod();

    List<TypeInfo> subTypes();

    Set<TypeModifier> modifiers();

    MethodInfo enclosingMethod();

    List<TypeInfo> permittedWhenSealed();

    int anonymousTypes();

    boolean isFinal();

    List<Comment> trailingComments();
}
