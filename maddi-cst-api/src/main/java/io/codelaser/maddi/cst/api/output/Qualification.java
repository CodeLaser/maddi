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

package io.codelaser.maddi.cst.api.output;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.ImportStatement;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.List;

/**
 * This type helps determine how variables and types are qualified.
 * Its implementations are mutable.
 * <p>
 * Decorators can add comments or annotations to certain elements.
 */
public interface Qualification {
    boolean doNotQualifyImplicit();

    boolean isFullyQualifiedNames();

    boolean isSimpleOnly();

    TypeNameRequired qualifierRequired(TypeInfo typeInfo);

    boolean qualifierRequired(MethodInfo methodInfo);

    boolean qualifierRequired(Variable variable);

    TypeNameRequired typeNameRequired();

    interface Decorator {
        List<Comment> comments(Element element);

        List<AnnotationExpression> annotations(Element element);

        List<ImportStatement> importStatements();
    }

    Decorator decorator();

    // write actions

    void addField(FieldInfo fieldInfo);

    void addUnqualifiedType(TypeInfo typeInfo);

    void addThis(This thisVar);

    void addMethodUnlessOverride(MethodInfo methodInfo);
}
