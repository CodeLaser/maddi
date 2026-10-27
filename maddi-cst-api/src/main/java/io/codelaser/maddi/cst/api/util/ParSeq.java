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

package io.codelaser.maddi.cst.api.util;

import io.codelaser.maddi.cst.api.info.InfoMap;

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
