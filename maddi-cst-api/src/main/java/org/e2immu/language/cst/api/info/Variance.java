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

package org.e2immu.language.cst.api.info;

/**
 * Declaration-site variance of a {@link TypeParameter}.
 * <p>
 * Java has no declaration-site variance (its variance is use-site, via wildcards in
 * {@link org.e2immu.language.cst.api.type.ParameterizedType}); Java type parameters are therefore
 * always {@link #INVARIANT}, the default everywhere. Kotlin annotates the parameter itself:
 * {@code class Box<out T>} is {@link #COVARIANT}, {@code class Sink<in T>} is {@link #CONTRAVARIANT}.
 */
public enum Variance {
    INVARIANT,
    COVARIANT,      // Kotlin `out T`
    CONTRAVARIANT,  // Kotlin `in T`
}
