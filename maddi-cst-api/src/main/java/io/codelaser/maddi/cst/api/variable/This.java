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

import io.codelaser.maddi.cst.api.info.TypeInfo;

/**
 * The pseudo-variable representing the {@code this} or {@code super} reference.
 * <p>
 * Every non-static method implicitly has a {@code This} variable bound to the enclosing type.
 * Inner classes can carry a qualified {@code this} (e.g. {@code Outer.this}), which is
 * expressed by setting {@link #explicitlyWriteType()} to the outer type.
 * When the reference is {@code super} rather than {@code this}, {@link #writeSuper()} returns
 * {@code true}.
 */
public interface This extends Variable {

    /** Returns the type this pseudo-variable refers to. */
    TypeInfo typeInfo();

    /**
     * Returns the type that should be written explicitly in front of {@code .this} for a
     * qualified reference (e.g. {@code Outer} in {@code Outer.this}),
     * or {@code null} when no explicit qualification is needed.
     */
    TypeInfo explicitlyWriteType();

    /**
     * Returns {@code true} if this pseudo-variable should be printed as {@code super}
     * rather than {@code this}.
     */
    boolean writeSuper();
}
