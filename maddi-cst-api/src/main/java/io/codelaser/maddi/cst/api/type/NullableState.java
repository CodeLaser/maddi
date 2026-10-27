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

package io.codelaser.maddi.cst.api.type;

/**
 * Nullability of a {@link ParameterizedType}, as a first-class dimension of the type.
 * <p>
 * Languages that do not track nullability in their type system (Java) always produce
 * {@link #UNSPECIFIED}, which is the default everywhere; this keeps their behaviour unchanged.
 * Kotlin distinguishes {@code String} (non-null) from {@code String?} (nullable); the Kotlin
 * front-end records {@link #NULLABLE} for the latter. {@link #NONNULL} is reserved for an explicit
 * non-null guarantee (e.g. {@code @NotNull}); it is not yet emitted by default.
 */
public enum NullableState {
    UNSPECIFIED,
    NONNULL,
    NULLABLE,
}
