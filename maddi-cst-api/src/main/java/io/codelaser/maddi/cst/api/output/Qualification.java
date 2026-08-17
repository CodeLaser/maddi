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
