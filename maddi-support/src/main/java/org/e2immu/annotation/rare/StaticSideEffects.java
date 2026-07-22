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

package org.e2immu.annotation.rare;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method (or constructor) has a <em>static side effect</em>: it modifies static/global state belonging to a
 * type other than its own primary type. This is the global-escape counterpart of {@link IgnoreModifications}
 * (road-to-immutability §050, "Ignoring modifications as manual hidden content"): {@code @IgnoreModifications}
 * governs content the type <em>holds</em>; {@code @StaticSideEffects} governs state it does <em>not</em> hold.
 * <p>
 * The analyzer <em>computes</em> this property for source methods that assign to, or make a modifying call on,
 * another type's static field ({@code StaticSideEffectAnalyzerImpl}). But some global reconfigurations happen
 * through a library method whose effect is not visible from source — the canonical case is
 * {@code System.setOut(other)}, which replaces the process-wide {@code System.out}. Nothing in the JDK source
 * reachable to the analyzer says so, so the only way to record it is this <b>contracted</b> annotation on the
 * safe surface (an AAPI declaration; see {@code maddi-aapi-archive .../jdk/JavaLang.java}). A caller of such a
 * method inherits the static side effect transitively.
 * <p>
 * Like {@link IgnoreModifications}, this annotation is always contracted on a library surface, never computed
 * onto the library method itself; on source methods it is computed and never trusted from an annotation.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface StaticSideEffects {
    // contract: true, absent: false

    /**
     * Any explanation for the presence of this annotation in this particular place.
     */
    String comment() default "";
}
