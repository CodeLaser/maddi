/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, Bart Naudts, https://github.com/CodeLaser/maddi
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
