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

package io.codelaser.maddi.cst.api.element;

import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;

import java.util.List;

/**
 * Represents a pattern used in an {@code instanceof} type test or a {@code switch} entry.
 * <p>
 * Three mutually exclusive forms exist:
 * <ol>
 *   <li><b>Unnamed pattern</b> ({@code _} with no type) — {@link #unnamedPattern()} is {@code true},
 *       all other fields are null / empty.</li>
 *   <li><b>Type pattern</b> (e.g. {@code Circle c} or {@code Circle _}) — {@link #localVariable()} is
 *       set; {@link #recordType()} and {@link #patterns()} are null / empty.</li>
 *   <li><b>Record deconstruction pattern</b> (e.g. {@code Box<String>(Point p, Color c)}) —
 *       {@link #recordType()} and {@link #patterns()} are set; {@link #localVariable()} is null.</li>
 * </ol>
 */
public interface RecordPattern extends Element {

    /** Returns {@code true} for the bare unnamed wildcard pattern {@code _} (no type annotation). */
    boolean unnamedPattern();

    /**
     * Returns the local variable bound by a type pattern (e.g. {@code Circle c} or {@code Circle _}),
     * or {@code null} for unnamed and record-deconstruction patterns.
     */
    LocalVariable localVariable();

    /**
     * Returns the record type being deconstructed in a record-deconstruction pattern
     * (e.g. {@code Box<String>} in {@code Box<String>(…)}), or {@code null} otherwise.
     */
    ParameterizedType recordType();

    /**
     * Returns the nested component patterns for a record-deconstruction pattern,
     * or an empty list for type and unnamed patterns.
     */
    List<RecordPattern> patterns();

    /**
     * Returns the type being matched by this pattern — the declared type for a type pattern,
     * or the record type for a deconstruction pattern.
     */
    ParameterizedType parameterizedType();

    /** Returns a translated copy of this pattern as described by {@code translationMap}. */
    RecordPattern translate(TranslationMap translationMap);

    interface Builder extends Element.Builder<Builder> {
        Builder setUnnamedPattern(boolean unnamedPattern);

        Builder setLocalVariable(LocalVariable localVariable);

        Builder setRecordType(ParameterizedType recordType);

        Builder setPatterns(List<RecordPattern> patterns);

        RecordPattern build();
    }
}
