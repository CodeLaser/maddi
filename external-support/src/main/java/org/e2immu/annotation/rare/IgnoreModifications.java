/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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
 * Annotation used to indicate that modifications on a field or parameter can be ignored.
 * The simple use case is <code>System.out</code>, where <code>println</code> is a modifying
 * method. To your application, however, this may not count as a modification but rather
 * as an action external to the system.
 * <p>
 * The use case for parameters is a <code>forEach</code> method which takes a consumer as a
 * parameter. Modifications inside the implementation of the <code>accept</code> method of the
 * consumer should typically not prevent the type of the <code>forEach</code> method to become a container.
 * <p>
 * This annotation is always contracted, never computed.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface IgnoreModifications {
    // contract: true, absent: false

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
