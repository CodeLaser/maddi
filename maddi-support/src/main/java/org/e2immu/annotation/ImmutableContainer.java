/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, Bart Naudts, https://github.com/CodeLaser/maddi
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

package org.e2immu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shorthand for the combination of {@link Immutable} and {@link Container}.
 * It indicates that the type is effectively or eventually immutable, and a container at the same time.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface ImmutableContainer {

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
     * Marker for eventual immutability.
     *
     * @return when the type is effectively immutable, set the empty string.
     * When it is eventually immutable, return a boolean expression of strings from <code>@Mark</code>
     * values on some modifying methods of the type. After these have been called, the
     * type will become effectively immutable.
     */
    String after() default "";

    /**
     * If present with value <code>true</code>, the decision-making process of this annotation was
     * not conclusive.
     *
     * @return <code>true</code> when the decision-making process was cut short, and this value was chosen based
     * on incomplete information.
     */
    boolean inconclusive() default false;

    /**
     * hidden content
     *
     * @return true when the type has hidden content, i.e., it is not deeply or recursively immutable.
     */
    boolean hc() default false;

    /**
     * constant value, when deeply immutable.
     *
     * @return string representation of the constant, without the quotes in the case of a string. Use null for the
     * null value, and use constant = true for the explicit test of returning the empty string.
     * <p>
     * To test the absence of a constant value, simply omit the parameter.
     * You cannot test the empty string as a constant value.
     */
    String value() default "";

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
