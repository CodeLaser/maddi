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

package org.e2immu.language.cst.api.element;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.Variable;

public interface Visitor {

    default boolean beforeModule(ModuleInfo moduleInfo) {
        return true; // go deeper;
    }

    default void afterModule() {
        // do nothing
    }

    default boolean beforeStatement(Statement statement) {
        return true; // go deeper
    }

    default void afterStatement(Statement statement) {
        // do nothing
    }

    default void startSubBlock(int n) {
        // do nothing
    }

    default void endSubBlock(int n) {
        // do nothing
    }

    default boolean beforeExpression(Expression expression) {
        return true; // go deeper
    }

    default void afterExpression(Expression expression) {
        // do nothing
    }

    default boolean beforeVariable(Variable variable) {
        return true;
    }

    default void afterVariable(Variable variable) {
        // do nothing
    }

    default boolean beforeJavaDoc(JavaDoc javaDoc) {
        return true;
    }

    default void afterJavaDoc(JavaDoc javaDoc) {
        // do nothing
    }

    default boolean beforeElement(Element element) {
        return true;
    }

    default void afterElement(Element element) {
    }
}
