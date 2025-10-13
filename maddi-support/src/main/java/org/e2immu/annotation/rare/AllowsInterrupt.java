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
 * Annotation indicating that this method or constructor increases the statement time,
 * in other words, allows the execution to be interrupted.
 * <p>
 * Please see the <em>e2immu manual</em> for a discussion of statement times.
 * <p>
 * Default value is true. Methods can be annotated with <code>@AllowsInterrupt(false)</code> to explicitly
 * mark that they do not interrupt.
 * <p>
 * External methods not annotated will not interrupt.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface AllowsInterrupt {
    /**
     * Parameter to indicate whether the method allows for interrupts, or not.
     *
     * @return <code>true</code> when the method allows for interrupts.
     */
    boolean value() default true;

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
