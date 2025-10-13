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

package org.e2immu.annotation.type;

import org.e2immu.annotation.Container;
import org.e2immu.annotation.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that a type is an <em>extension class</em> of another type <code>E</code>.
 * The following criteria are used:
 * <ol>
 * <li>the class is immutable or constant;</li>
 * <li>all non-private static methods with parameters (and there must be at least one) must have a 1st parameter:
 * <ol>
 *     <li>of type <code>E</code>, the type being extended,</li>
 *     <li>which is {@link NotNull};</li>
 * </ol>
 * <li>non-private static methods without parameters must return a value of type <code>E</code>, and must
 * also be {@link NotNull}.</li>
 * </ol>
 * Extension classes will often not be {@link Container}, because a modification of the first parameter
 * is pretty common.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface ExtensionClass {

    /**
     * Parameter to mark that the annotation should be absent, or present.
     * In verification mode, <code>absent=true</code> means that an error will be raised
     * if the analyser computes the annotation. In contract mode, it guarantees absence of the annotation.
     *
     * @return <code>true</code> when the annotation should be absent (verification mode) or must be absent (contract mode).
     */
    boolean absent() default false;

    /**
     * Parameter to set contract mode, even if the annotation occurs in a context
     * where verification mode is normal. Use <code>contract=true</code>
     * to override the computation of the analyser.
     *
     * @return <code>true</code> when switching to contract mode.
     */
    boolean contract() default false;

    /**
     * The type being extended (<code>E</code>); currently for decorative use only.
     *
     * @return The type being extended.
     */
    Class<?> of();

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
