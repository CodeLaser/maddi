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

package org.e2immu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation occurring on a parameter or a method.
 * <p>
 * On a parameter, it indicates that the parameter is exchangeable with other parameters in the same commutation group,
 * i.e., when present on the first two parameters of a method, <code>method(a, b, c)</code> is fully equivalent to
 * <code>method(b, a, c)</code>.
 * The two standard examples are <code>min</code> and <code>max</code> in the {@link java.lang.Math} class,
 * and the two-parameter <code>of(x, y)</code> method in {@link java.util.Set}.
 * <p>
 * When applied to methods, it indicates that the order of execution is interchangeable.
 * Currently, this system applies to modifying methods only.
 *
 * <p>
 * The parameters help describe the commutation properties:
 * <ul>
 * <li>Without this annotation, the default is sequential; in a formula: S(main).
 * <li>With this annotation, without parameters, a parallel group is created: @Commutable is equivalent to S(main)P(default).
 * <li>Two different parallel groups can be created using the 'par' parameter: @Commutable(par="group1") implies S(main)P(group1).
 * <li>A sequential group within the default parallel group is created using the 'seq' parameter: @Commutable(seq="s")
 * implies S(main)P(default)S(s).
 * <li>The 'multi' parameter indicates that multiple calls to the same method can be grouped by the method mentioned in the parameter;
 * see e.g. multiple calls to `Collection.add()` can be replaced by a single `Collection.addAll()` call.
 * </ul>
 * <p>
 * The parameters either take a group name, or a comma separated list of group name and method parameter index.
 * <p>
 * Note that setters (annotated with {@link org.e2immu.annotation.method.GetSet}), are commutable in the
 * default parallel group. The <code>@Commutable</code> annotation is implied.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface Commutable {
    String multi() default "";

    String seq() default "";

    boolean contract() default false;

    // for setters
    boolean implied() default false;

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
