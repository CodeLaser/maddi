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

package org.e2immu.language.cst.api.util;

import org.e2immu.language.cst.api.info.InfoMap;

import java.util.Comparator;
import java.util.List;

public interface ParSeq<T> {
    /**
     * Useful as a condition before calling <code>sortParallels</code>.
     *
     * @return false if the ParSeq is simply a sequence; true if it contains any parallel groups.
     */
    boolean containsParallels();

    /**
     * Sort a list of items, of a completely unrelated type, according to the ParSeq.
     * Inside parallel groups, use the comparator.
     *
     * @param items      the input
     * @param comparator the comparator for parallel groups
     * @param <X>        the type of the input
     * @return a new list of items, sorted accordingly.
     */
    <X> List<X> sortParallels(List<X> items, Comparator<X> comparator);
}
