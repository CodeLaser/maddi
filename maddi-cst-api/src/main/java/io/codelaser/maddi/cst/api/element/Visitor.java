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

/**
 * Callback interface for recursively visiting CST nodes via {@link Element#visit(Visitor)}.
 * <p>
 * Each node kind has a {@code before*} / {@code after*} pair. The {@code before*} method
 * returns {@code true} to continue descending into child nodes, or {@code false} to skip
 * them. All methods have no-op default implementations, so implementors only need to
 * override the hooks they care about.
 * <p>
 * The traversal order is: module → statements → expressions → variables (scope chains).
 */
public interface Visitor {

    /**
     * Called before visiting a {@link ModuleInfo}.
     * @return {@code true} to descend into the module's directives; {@code false} to skip.
     */
    default boolean beforeModule(ModuleInfo moduleInfo) {
        return true;
    }

    /** Called after all children of a {@link ModuleInfo} have been visited. */
    default void afterModule() {
    }

    /**
     * Called before visiting a {@link Statement}.
     * @return {@code true} to descend into the statement's sub-elements; {@code false} to skip.
     */
    default boolean beforeStatement(Statement statement) {
        return true;
    }

    /** Called after all children of a {@link Statement} have been visited. */
    default void afterStatement(Statement statement) {
    }

    /**
     * Called when entering the {@code n}-th sub-block (e.g. the then-branch or else-branch of
     * an {@code if}) to allow the visitor to track block nesting.
     */
    default void startSubBlock(int n) {
    }

    /** Called when leaving the {@code n}-th sub-block. */
    default void endSubBlock(int n) {
    }

    /**
     * Called before visiting an {@link Expression}.
     * @return {@code true} to descend into the expression's sub-expressions; {@code false} to skip.
     */
    default boolean beforeExpression(Expression expression) {
        return true;
    }

    /** Called after all children of an {@link Expression} have been visited. */
    default void afterExpression(Expression expression) {
    }

    /**
     * Called before visiting a {@link Variable} (including variables inside scope chains).
     * @return {@code true} to descend further into the variable's scope; {@code false} to stop.
     */
    default boolean beforeVariable(Variable variable) {
        return true;
    }

    /** Called after a {@link Variable} and its scope have been visited. */
    default void afterVariable(Variable variable) {
    }

    /**
     * Called before visiting a {@link JavaDoc} comment.
     * @return {@code true} to descend into the Javadoc's tags; {@code false} to skip.
     */
    default boolean beforeJavaDoc(JavaDoc javaDoc) {
        return true;
    }

    /** Called after a {@link JavaDoc} comment has been visited. */
    default void afterJavaDoc(JavaDoc javaDoc) {
    }

    /**
     * Catch-all hook called before visiting any {@link Element} not covered by the more
     * specific hooks above.
     * @return {@code true} to descend into the element's children; {@code false} to skip.
     */
    default boolean beforeElement(Element element) {
        return true;
    }

    /** Catch-all hook called after any {@link Element} not covered by the more specific hooks above. */
    default void afterElement(Element element) {
    }
}
