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

package org.e2immu.language.cst.api.runtime;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.TypeInfo;

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

    TypeInfo syntheticFunctionalType(int inputParameters, boolean hasReturnValue);

    // separate from getFullyQualified, as these have been preloaded

    AnnotationExpression e2immuAnnotation(String fullyQualifiedName);

    Stream<AnnotationExpression> e2immuAnnotations();

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
