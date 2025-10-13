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

package org.e2immu.language.cst.api.info;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface Info extends Element {

    String info();

    Access access();

    CompilationUnit compilationUnit();

    String simpleName();

    String fullyQualifiedName();

    boolean isSynthetic();

    TypeInfo typeInfo();

    boolean hasBeenAnalyzed();

    JavaDoc javaDoc();

    List<? extends Info> translate(TranslationMap translationMap);

    interface Builder<B extends Builder<?>> extends Element.Builder<B> {
        Stream<AnnotationExpression> annotationStream();

        @Fluent
        B setAccess(Access access);

        @Fluent
        B setAnnotationExpression(int index, AnnotationExpression annotationExpression);

        AnnotationExpression haveAnnotation(String fullyQualifiedName);

        @Fluent
        B setSynthetic(boolean synthetic);

        boolean hasBeenCommitted();

        // once all the modifiers have been set
        @Fluent
        B computeAccess();

        @Fluent
        B setJavaDoc(JavaDoc javaDoc);

        void commit();
    }

}
