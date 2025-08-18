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
 * An annotation on a type (computed on a class, contracted on an interface) that indicates that no method of the type
 * modifies its parameters.
 * <p>
 * The annotation on a field, parameter or method indicates a dynamic value, typically computed for concrete
 * implementations of functional interfaces.
 * <p>
 * The annotation can be contracted on a parameter to indicate that all implementations of the (abstract) type
 * must be containers themselves. This is particularly useful for functional interfaces, where the annotation will
 * enforce that implementations are not parameter-modifying.
 *
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface Container {

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
     * Some containers are used as "builders" for immutable classes.
     * This parameter shows that there is a build method.
     * <p>
     * This parameter is currently used in a decorative way only.
     *
     * @return the class for which this container is the builder
     */
    Class<?> builds() default Object.class;

    /**
     * If present with value <code>true</code>, the decision-making process of this annotation was
     * not conclusive.
     *
     * @return <code>true</code> when the decision-making process was cut short, and this value was chosen based
     * on incomplete information.
     */
    boolean inconclusive() default false;

    /**
     * implied: if true, the annotation is generated internally for verification or educational purposes,
     * but not output, because it is implied.
     * For example, there is no point in annotating every return value of type {@link java.lang.String} with
     * <code>@ImmutableContainer</code>...
     * <p>
     * internal or demonstration use only!
     *
     * @return true when the annotation is not really necessary
     */
    boolean implied() default false;

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
