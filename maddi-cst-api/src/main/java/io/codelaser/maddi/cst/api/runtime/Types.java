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

package io.codelaser.maddi.cst.api.runtime;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.TypeInfo;

import java.util.stream.Stream;

public interface Types {

    TypeInfo getFullyQualified(String name, boolean complain);

    default TypeInfo getFullyQualified(Class<?> clazz, boolean complain) {
        return getFullyQualified(clazz.getCanonicalName(), complain);
    }
    default TypeInfo getFullyQualified(Class<?> clazz, boolean complain, SourceSet sourceSetOfRequest) {
        return getFullyQualified(clazz.getCanonicalName(), complain, sourceSetOfRequest);
    }

    default TypeInfo getFullyQualified(String name, boolean complain, SourceSet sourceSetOfRequest) {
       return getFullyQualified(name, complain); // ignore the request
    }

    /**
     * Every loaded copy of a type, one per source set: a library a project carries in two versions has two
     * (CodeLaser/maddi-mod#37). Nothing is loaded. Empty when none is, and by default, which means "this runtime does
     * not tell copies apart; resolve with {@link #getFullyQualified(String, boolean, SourceSet)}".
     */
    default java.util.List<TypeInfo> loadedCopies(String fullyQualifiedName) {
        return java.util.List.of();
    }

    TypeInfo syntheticFunctionalType(int inputParameters, boolean hasReturnValue);

    // separate from getFullyQualified, as these have been preloaded

    AnnotationExpression maddiAnnotation(String fullyQualifiedName);

    Stream<AnnotationExpression> maddiAnnotations();

    String e2aAbsent();

    String e2aContract();

    String e2aContent();

    String e2aImplied();

    String e2aHiddenContent();

    String e2aValue();

    String e2aPar();

    String e2aSeq();

    String e2aMulti();

    String e2aAfter();

    String e2aBefore();

    String e2aConstruction();

    String e2aInconclusive();

    String e2aHcParameters();
}
