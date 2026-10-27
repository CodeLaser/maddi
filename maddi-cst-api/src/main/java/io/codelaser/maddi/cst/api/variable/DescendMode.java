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

package io.codelaser.maddi.cst.api.variable;

/**
 * Controls whether variable traversal should recurse into the scope of a variable.
 * <p>
 * Used when collecting the set of variables referenced by an expression: {@link #isYes()}
 * means collect variables transitively from scopes (e.g. the scope of a {@link FieldReference}),
 * while {@link #isNo()} means collect only the top-level variable without descending.
 */
public interface DescendMode {
    /** Returns {@code true} if traversal should recurse into variable scopes. */
    boolean isYes();

    /** Returns {@code true} if traversal should stop at the top-level variable. */
    boolean isNo();
}
