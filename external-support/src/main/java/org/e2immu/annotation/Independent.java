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
 * Annotation to indicate <em>absence of linking</em>, or that only transfer of hidden content is allowed.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD,
        ElementType.TYPE_PARAMETER})
public @interface Independent {

    boolean hc() default false;

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
     * <code>@Independent</code>...
     * <p>
     * internal or demonstration use only!
     *
     * @return true when the annotation is not really necessary
     */
    boolean implied() default false;


    /**
     * used to mark a dependence on other parameters
     *
     * @return the parameters, starting from 0
     */
    int[] dependentParameters() default {};

    /*
     * used to mark sharing of hidden content with other parameters
     */
    int[] hcParameters() default {};

    /**
     * Indicates that a parameter shares hidden content with the return value of the method
     *
     * @return true when the parameter is linked to the return value, sharing hidden content
     */

    boolean hcReturnValue() default false;

    /**
     * Indicates that the return value of the method is dependent on this parameter,
     * i.e. modifications in the parameter argument result in modifications to the result value.
     *
     * @return true when the return value of the method is dependent on this parameter.
     */
    boolean dependentReturnValue() default false;

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
