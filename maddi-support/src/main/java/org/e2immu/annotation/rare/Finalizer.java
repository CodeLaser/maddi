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

import org.e2immu.annotation.Final;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation on a method, always contracted, indicating that after calling this method,
 * no other methods may be called anymore on the object. After calling a finalizer, the
 * object has gone into a <em>final</em> state.
 * <p>
 * The analyser imposes strict rules for the life-cycle of objects with a finalizer method:
 * <ol>
 *     <li>
 *         Any field of a type with finalizers must be effectively final (marked with {@link Final}).
 *     </li>
 *     <li>
 *         A finalizer method can only be called on a field inside a method which is marked as a finalizer as well.
 *     </li>
 *     <li>
 *         A finalizer method can never be called on a parameter or any variable linked to it,
 *     </li>
 * </ol>
 * These rules allow the analyser to enforce the final state of the object.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Finalizer {

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
