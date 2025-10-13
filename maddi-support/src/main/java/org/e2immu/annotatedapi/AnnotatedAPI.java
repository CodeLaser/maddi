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

package org.e2immu.annotatedapi;

/**
 * Helper methods used in Annotated API classes.
 * These methods are known to the analys er in a hard-wired way; they exist to support
 * the computation of instance state using companion methods.
 */
public class AnnotatedAPI {

    /**
     * The method analyser replaces the method call <code>isFact(clause)</code>
     * with the boolean value <code>true</code> when
     * the clause is present in the current instance state of the object.
     * <p>
     * This method, hard-wired into the method analyser, is to be used in companion methods, see for example <code>JavaUtil</code>
     * in the <code>e2immu/annotatedAPI</code> project.
     * <p>
     * The method does not return identity; it is not modifying.
     *
     * @param b the clause
     * @return specialised inline method
     */

    public static boolean isFact(boolean b) {
        throw new UnsupportedOperationException();
    }

    /**
     * The method analyser replaces the method call <code>isKnown(true)</code>
     * with a boolean to test if the current instance state is keeping track of clauses that represent
     * elements added to a collection. It keeps <code>isKnown(false)</code> as is.
     * <p>
     * This method, hard-wired into the method analyser, is to be used in companion methods, see for example <code>JavaUtil</code>
     * in the <code>e2immu/annotatedAPI</code> project.
     *
     * @param test true when testing, false when generating a clause for the state
     * @return true when absence of information means knowing the negation
     */
    public static boolean isKnown(boolean test) {
        throw new UnsupportedOperationException();
    }
}
