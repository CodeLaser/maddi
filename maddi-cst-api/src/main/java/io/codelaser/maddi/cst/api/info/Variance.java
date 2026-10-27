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

package io.codelaser.maddi.cst.api.info;

/**
 * Declaration-site variance of a {@link TypeParameter}.
 * <p>
 * Java has no declaration-site variance (its variance is use-site, via wildcards in
 * {@link io.codelaser.maddi.cst.api.type.ParameterizedType}); Java type parameters are therefore
 * always {@link #INVARIANT}, the default everywhere. Kotlin annotates the parameter itself:
 * {@code class Box<out T>} is {@link #COVARIANT}, {@code class Sink<in T>} is {@link #CONTRAVARIANT}.
 */
public enum Variance {
    INVARIANT,
    COVARIANT,      // Kotlin `out T`
    CONTRAVARIANT,  // Kotlin `in T`
}
